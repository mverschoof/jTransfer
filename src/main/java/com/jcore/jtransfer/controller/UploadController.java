package com.jcore.jtransfer.controller;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jcore.jtransfer.dao.UploadDao;
import com.jcore.jtransfer.exception.EncryptionException;
import com.jcore.jtransfer.exception.HashingException;
import com.jcore.jtransfer.exception.StorageException;
import com.jcore.jtransfer.model.Upload;
import com.jcore.jtransfer.model.UploadFile;
import com.jcore.jtransfer.service.StorageService;
import com.jcore.jtransfer.util.HashingUtils;

@Controller
public class UploadController extends BaseController {

	@Autowired
	public UploadController(StorageService storageService, UploadDao uploadDao) {
		super(storageService, uploadDao);
	}

	/**
	 * Get the file upload page
	 * 
	 * @param model
	 * @return
	 */
	@RequestMapping(value = {"/", "/upload"}, method = RequestMethod.GET)
	public String upload(Model model) {

		// Generate a file ID to link the async uploaded file to the settings form
		String uploadUuid = UUID.randomUUID().toString();
		model.addAttribute("uploadUuid", uploadUuid);

		return "/upload";
	}

	/**
	 * Upload a file to the server
	 * 
	 * @param file
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String upload(@RequestParam String uploadUuid, @RequestParam("file") MultipartFile file, 
			Model model, RedirectAttributes redirectAttributes) {

		// TODO: Remove this once fileUpload is asynchronous
		model.addAttribute("uploadUuid", uploadUuid);

		if (!validateUuid(uploadUuid)) {
			model.addAttribute("errors", new String[]{ERROR_UUID});
			return REDIRECT_UPLOAD;
		}

		if (file.isEmpty()) {
			model.addAttribute("errors", new String[]{ERROR_FILE});
			return REDIRECT_UPLOAD;
		}

		// Create a new upload
		Upload upload = null;

		try {
			// Save the file on the server
			storageService.store(uploadUuid, file);

			// Set the date 
			ZonedDateTime now = ZonedDateTime.now(TIMEZONE);

			// Create the upload
			upload = new Upload();
			upload.setUuid(uploadUuid);
			upload.setUploadedBy(TEMPUSER);
			upload.setUploadedOn(now);
			upload.setExpiresOn(now.plusDays(8L));
			upload.setPasswordProtected(false);
			upload.setCompleted(false);

			// Create the upload file
			UploadFile uploadFile = new UploadFile();
			uploadFile.setUuid(UUID.randomUUID().toString());
			uploadFile.setName(file.getOriginalFilename());
			uploadFile.setSize(file.getSize());
			uploadFile.setHash(HashingUtils.getFileHash(file));
			uploadFile.setUpload(upload);
			upload.addFile(uploadFile);

			uploadDao.save(upload);

			model.addAttribute("messages", new String[]{"Bestand " + file.getOriginalFilename() + " succesvol geüpload"});
			log.debug("File " + file.getOriginalFilename() + " has been uploaded");
		} catch (StorageException e) {
			model.addAttribute("errors", new String[]{"Bestand " + file.getOriginalFilename() + " kon niet geüpload worden"});
			log.error("File " + file.getOriginalFilename() + " cannot be uploaded", e);
		} catch (HashingException e) {
			model.addAttribute("errors", new String[]{"Bestand " + file.getOriginalFilename() + " kon niet gehashed worden"});
			log.error("File " + file.getOriginalFilename() + " cannot be hashed", e);
		}

		// TODO: Return json object for error or success
		return "/upload";
	}

	/**
	 * Process the settings that go with the uploaded file(s)
	 * 
	 * @param uploadUuid
	 * @param recipients
	 * @param message
	 * @param password
	 * @param model
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(value = "/upload/settings", method = RequestMethod.POST)
	public String completeUpload(@RequestParam("uploadUuid") String uploadUuid, @RequestParam("recipients") String recipients, 
			@RequestParam("message") String message, @RequestParam("password") String password, 
			Model model, RedirectAttributes redirectAttributes) {

		// FIXME Do we want the upload uuid as a field or as a pathvariable
		// Could be @RequestMapping(value = "/upload/{uploadUuid}", method = RequestMethod.POST)
		Upload upload = findUpload(uploadUuid);
		if (upload == null) {
			model.addAttribute("errors", new String[]{ERROR_UPLOAD});
			return REDIRECT_UPLOAD;
		}
		
//		TODO: Add expires check

		// Validate the posted fields
		ArrayList<String> errors = validateUploadFields(uploadUuid, recipients, password);
		if (!errors.isEmpty()) {
			model.addAttribute("errors", errors.toArray(new String[0]));
			return REDIRECT_UPLOAD;
		}

		try {
			// Encrypt the files and delete the originals
			storageService.encryptUpload(upload.getUuid(), password);

			// Move the directory and files
			storageService.move(uploadUuid);

			// Set the recipients
			upload.setRecipients(recipients);

			// Set the value to determine if the password is set
			upload.setPasswordProtected(password != null && !password.trim().isEmpty());

			// Now the upload is completed and may be downloaded
			upload.setCompleted(true);

			// Save the upload information
			uploadDao.save(upload);


			String url = "/upload/" + uploadUuid + "/";
			// FIXME: Add mailing to the recipients



			// Add completion message to the model
			model.addAttribute("messages", new String[]{"Upload succesvol voltooid"});
			log.debug("File upload " + uploadUuid + " has successfully been completed");

			// Redirect to the upload overview page
			return REDIRECT_UPLOADS;

		} catch (StorageException e) {
			model.addAttribute("errors", new String[]{"Er is een fout opgetreden bij het plaatsen van het bestand"});
			log.error("Upload (" + uploadUuid + ") could not be moved", e);
		} catch (EncryptionException e) {
			model.addAttribute("errors", new String[]{"Er is een fout opgetreden bij de bestandsversleuteling"});
			log.error("Upload (" + uploadUuid + ") could not be encrypted", e);
		}

		return REDIRECT_UPLOAD;
	}

	/**
	 * Display the uploads page
	 * 
	 * @param model
	 * @param redirectAttributes
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value ="/uploads", method = RequestMethod.GET)
	public String getUploads(Model model, RedirectAttributes redirectAttributes) throws IOException {

		// Get the uploads of the user
		List<Upload> uploads = uploadDao.findByUploadedBy(TEMPUSER);
		Collections.sort(uploads);
		model.addAttribute("uploads", uploads);

		return "/sentUploads";
	}

	/**
	 * Display a single upload
	 * 
	 * @param uploadUuid
	 * @param model
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(value = "/upload/{uploadUuid}", method = RequestMethod.GET)
	public String getUpload(@PathVariable String uploadUuid, Model model, RedirectAttributes redirectAttributes) {

		Upload upload = findUpload(uploadUuid);
		if (upload == null) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_UPLOAD});
			return REDIRECT_UPLOADS;
		}
//		TODO: Add expires check


		// Get the upload
		if (!hasAccess(upload, TEMPUSER)) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_ACCESS});
			return REDIRECT_UPLOADS;
		}

		// Check if the upload is completed
		if (!upload.isCompleted()) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_COMPLETED});
			return REDIRECT_UPLOADS;
		}

		// Check if the upload is expired
		if (upload.isExpired()) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_EXPIRED});
			return REDIRECT_UPLOADS;
		}

		model.addAttribute("upload", upload);
		return "/sentUpload";
	}

	/**
	 * Display the received uploads page
	 * 
	 * @param model
	 * @param redirectAttributes
	 * @return
	 * @throws IOException
	 */
	@RequestMapping(value ="/received", method = RequestMethod.GET)
	public String getRecievedUploads(Model model, RedirectAttributes redirectAttributes) throws IOException {

		// Get the uploads of the user
		List<Upload> uploads = uploadDao.findByRecipientsContaining(TEMPUSER);
		Collections.sort(uploads);
		model.addAttribute("uploads", uploads);

		return "/receivedUploads";
	}

	/**
	 * Display a single upload
	 * 
	 * @param uploadUuid
	 * @param model
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(value = "/received/{uploadUuid}", method = RequestMethod.GET)
	public String getReceivedUpload(@PathVariable String uploadUuid, Model model, RedirectAttributes redirectAttributes) {

		Upload upload = findUpload(uploadUuid);
		if (upload == null) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_UPLOAD});
			return REDIRECT_RECEIVED;
		}
//		TODO: Add expires check


		// Get the upload
		if (!hasAccess(upload, TEMPUSER)) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_ACCESS});
			return REDIRECT_RECEIVED;
		}

		// Check if the upload is completed
		if (!upload.isCompleted()) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_COMPLETED});
			return REDIRECT_RECEIVED;
		}

		// Check if the upload is expired
		if (upload.isExpired()) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_EXPIRED});
			return REDIRECT_RECEIVED;
		}

		model.addAttribute("upload", upload);
		return "/receivedUpload";
	}

	/**
	 * Delete an upload
	 * 
	 * @param uploadUuid
	 * @param model
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(value = "/delete/{uploadUuid}", method = RequestMethod.GET)
	public String deleteUpload(@PathVariable String uploadUuid, Model model, RedirectAttributes redirectAttributes) {

		Upload upload = findUpload(uploadUuid);
		if (!TEMPUSER.equals(upload.getUploadedBy())) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_ACCESS});
			return REDIRECT_UPLOADS;
		}

		// Remove the actual files
		if (!upload.isCompleted()) {
			storageService.deleteTemp(uploadUuid);
		} else {
			storageService.deleteFinal(uploadUuid);
		}

		// Remove the database entries
		uploadDao.delete(upload);

		redirectAttributes.addFlashAttribute("messages", new String[]{"Upload is succesvol verwijderd"});
		return REDIRECT_UPLOADS;
	}

	/**
	 * Validate 
	 * @param upload
	 * @return
	 */
	private ArrayList<String> validateUploadFields(String uploadUuid, String recipients, String password) {
		ArrayList<String> errors = new ArrayList<String>();

		// Check if there are recipients
		if (recipients == null || recipients.isEmpty()) {
			errors.add("Er moet minimaal één ontvanger worden opgegeven");
			log.warn("No recipients are entered for upload (" + uploadUuid + ")");
		}

		// Validate recipients before continuing
		String invalidRecipients = validateRecipients(recipients);
		if (!invalidRecipients.isEmpty()) {
			errors.add("De volgende ontvangers zijn geen valide email adressen: " + invalidRecipients);
			log.warn("Invalid recipients found for upload (" + uploadUuid + ")");
		}

		// Validate the password length
		if (password != null && password.length() > 16) {
			errors.add("Het wachtwoord is te lang. De maximale lengte voor een wachtwoord is 16 karakters");
			log.warn("The password is too long for for upload (" + uploadUuid + ")");
		}

		return errors;
	}

	/**
	 * Validate a string with recipients
	 * 
	 * @param recipients
	 * @return
	 */
	private String validateRecipients(String recipients) {
		String invalidRecipients = "";

		// Get a list of sanitized recipients
		List<String> sanitizedRecipients = sanitizeRecipients(recipients);

		// Validate each email address
		EmailValidator validator = EmailValidator.getInstance();
		for (String recipient : sanitizedRecipients) {
			if (!validator.isValid(recipient)) {
				log.debug("Could not validate email " + recipient);
				invalidRecipients += (recipient + ";");
			}
		}

		// Remove the trailing ";"
		if (invalidRecipients.endsWith(";")) {
			invalidRecipients = invalidRecipients.substring(0, invalidRecipients.length() - 1);
		}

		return invalidRecipients;
	}

	/**
	 * Trim the recipients and remove any empty entries
	 * 
	 * @param recipients
	 * @return
	 */
	private List<String> sanitizeRecipients(String recipients) {
		List<String> sanitizedRecipients = new ArrayList<>();

		// Split the String into separate recipients
		String[] split = recipients.split(",|;");

		for (String recipient : split) {
			if (recipient != null && !recipient.trim().isEmpty()) {
				sanitizedRecipients.add(recipient.trim());
			}
		}

		return sanitizedRecipients;
	}

	

}
