package com.jcore.jtransfer.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.jcore.jtransfer.dao.UploadDao;
import com.jcore.jtransfer.exception.EncryptionException;
import com.jcore.jtransfer.exception.HashingException;
import com.jcore.jtransfer.model.Upload;
import com.jcore.jtransfer.model.UploadFile;
import com.jcore.jtransfer.service.StorageService;
import com.jcore.jtransfer.util.EncryptionUtils;
import com.jcore.jtransfer.util.HashingUtils;

@Controller
public class DownloadController extends BaseController {

	@Autowired
	public DownloadController(StorageService storageService, UploadDao uploadDao) {
		super(storageService, uploadDao);
	}

	/**
	 * Serve the files in an upload as a zip file
	 * 
	 * @param uploadUuid
	 * @param model
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(value = "/download/{uploadUuid}", method = RequestMethod.GET, produces="application/zip")
	@ResponseBody
	public Object downloadFiles(@PathVariable String uploadUuid, Model model, RedirectAttributes redirectAttributes, 
			@RequestHeader(value = "referer", required = false) final String referer) {

		// Determine the redirect location which is the url where we came from
		// FIXME what if uploaduuid is empty here?
		String redirect = "redirect:/";
		if (referer != null && ("/uploads".equals(referer) || ("/upload/" + uploadUuid).equals(referer))) {
			redirect = "redirect:" + referer;
		}

		return this.serveZip(uploadUuid, null, redirect, redirectAttributes);
	}

	/**
	 * Serve the files in an upload as a zip file
	 * 
	 * @param uploadUuid
	 * @param model
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(value = "/download/{uploadUuid}", method = RequestMethod.POST, produces="application/zip")
	@ResponseBody
	public Object downloadFiles(@PathVariable String uploadUuid, @RequestParam("password") String password, 
			Model model, RedirectAttributes redirectAttributes, 
			@RequestHeader(value = "referer", required = false) final String referer) {

		// Determine the redirect location which is the url where we came from
		// FIXME what if uploaduuid is empty here?
		String redirect = "redirect:/" + uploadUuid;
		if (referer != null && ("/uploads".equals(referer) || ("/upload/" + uploadUuid).equals(referer))) {
			redirect = "redirect:" + referer;
		}

		if (password == null || password.trim().isEmpty()) {
			redirectAttributes.addFlashAttribute("errors", new String[] {"Het wachtwoord is leeg"});
			return redirect;
		}

		if (password.length() > 16) {
			redirectAttributes.addFlashAttribute("errors", new String[] {"Het wachtwoord is te lang"});
			return redirect;
		}

		return this.serveZip(uploadUuid, password, redirect, redirectAttributes);
	}

	/**
	 * Return a zip file containing the files of an upload
	 * 
	 * @param uploadUuid
	 * @param referer
	 * @param redirectAttributes
	 * @return
	 */
	private Object serveZip(String uploadUuid, String password, String redirect, RedirectAttributes redirectAttributes) {

		Upload upload = findUpload(uploadUuid);
		if (upload == null) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_UPLOAD});
			return redirect;
		}

		// Get the upload
		if (!hasAccess(upload, TEMPUSER)) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_ACCESS});
			return redirect;
		}

		// Check if the upload is completed
		if (!upload.isCompleted()) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_COMPLETED});
			return redirect;
		}

		// Check if the upload is expired
		if (upload.isExpired()) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_EXPIRED});
			return redirect;
		}

		// Check if the upload is password protected and we have no password
		if (upload.isPasswordProtected() && (password == null || password.trim().isEmpty())) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_PASSWORD});
			return redirect;
		}

		// Get the files for this upload
		List<UploadFile> files = upload.getFiles();

		try {
			// Create the file to write to
			Path zip = storageService.createUploadZip(uploadUuid);
			File zipFile = zip.toFile();

			// Define the output streams
			FileOutputStream fileOutput = new FileOutputStream(zipFile);
			ZipOutputStream zipOutput = new ZipOutputStream(fileOutput);

			// Add each upload file to the zip output stream
			for (UploadFile file : files) {
				log.debug("Decrypting '" + file.getName() + "' to zip file");
				File decryptedFile = storageService.decryptFile(uploadUuid, file.getName(), password);

				// Check if the file hash matches the decrypted file's
				String fileHash = HashingUtils.getFileHash(decryptedFile);
				if (!fileHash.equals(file.getHash())) {
					log.warn("File hash does not match for file " + file.getName() + " in upload " + uploadUuid);
					redirectAttributes.addFlashAttribute("errors", new String[]{"Het wachtwoord is onjuist"});
					return redirect;
				}

				log.debug("Writing '" + file.getName() + "' to zip file");

				addToZipFile(decryptedFile, zipOutput);

				log.debug("Written '" + file.getName() + "' to zip file");
			}

			zipOutput.close();
			fileOutput.close();

			Resource zipResource = storageService.loadFinalAsResource(uploadUuid, zipFile.getName());
			return ResponseEntity.ok().header(
					HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zip.getFileName() + "\""
					).body(zipResource);

		} catch (FileNotFoundException e) {
			log.error("Could not find a file in upload " + uploadUuid, e);
			redirectAttributes.addFlashAttribute("errors", new String[]{"Een van de bestanden kan niet gevonden worden"});
			return redirect;
		} catch (IOException e) {
			log.error("Could not read a file in upload " + uploadUuid, e);
			redirectAttributes.addFlashAttribute("errors", new String[]{"Een van de bestanden kan niet gelezen worden"});
			return redirect;
		} catch (EncryptionException e) {
			log.error("An error occurred when decrypting a file in upload" + uploadUuid, e);
			redirectAttributes.addFlashAttribute("errors", new String[]{"Een van de bestanden kan niet ontsleuteld worden"});
			return redirect;
		} catch (HashingException e) {
			log.error("An error occurred when getting the hash for a file in upload" + uploadUuid, e);
			redirectAttributes.addFlashAttribute("errors", new String[]{"Er is een fout opgetreden bij het ophalen van de bestandshash"});
			return redirect;
		}
	}

	/**
	 * Serve a single file from an upload
	 *  
	 * @param uploadUuid
	 * @param fileUuid
	 * @param model
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(value = "/download/{uploadUuid}/{fileUuid}", method = RequestMethod.GET)
	@ResponseBody
	public Object downloadFile(@PathVariable String uploadUuid, @PathVariable String fileUuid, 
			Model model, RedirectAttributes redirectAttributes) {

		// Determine the redirect location which is the url where we came from
		// FIXME what if the uploaduuid is empty here?
		String redirect = "redirect:/upload/" + uploadUuid;

		Upload upload = findUpload(uploadUuid);
		if (upload == null) {
			model.addAttribute("errors", new String[]{ERROR_UPLOAD});
			return redirect;
		}

		// Get the upload
		if (!hasAccess(upload, TEMPUSER)) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_ACCESS});
			return redirect;
		}

		// Check if the upload is completed
		if (!upload.isCompleted()) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_COMPLETED});
			return redirect;
		}

		// Check if the upload is expired
		if (upload.isExpired()) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_EXPIRED});
			return redirect;
		}

		// Get the uploaded file
		UploadFile uploadFile = upload.getFile(fileUuid);
		if (uploadFile == null) {
			log.error("File (" + fileUuid + ") could not be found");
			redirectAttributes.addFlashAttribute("errors", new String[]{"Kan het gevraagde bestand niet vinden"});
			return redirect;
		}

		// If the file is password protected, display the password page
		if (upload.isPasswordProtected()) {
			model.addAttribute("uploadUuid", uploadUuid);
			model.addAttribute("fileUuid", fileUuid);
			return redirect;
		}

		// Serve the file
		return serveFile(uploadUuid, uploadFile, null, redirectAttributes);
	}

	@RequestMapping(value = "/download/{uploadUuid}/{fileUuid}", method = RequestMethod.POST)
	@ResponseBody
	public Object downloadFile(@PathVariable String uploadUuid, @PathVariable String fileUuid, 
			@RequestParam("password") String password, RedirectAttributes redirectAttributes) {

		// Determine the redirect location which is the url where we came from
		// FIXME what if the uploaduuid is empty here?
		String redirect = "redirect:/upload/" + uploadUuid;

		if (password == null || password.trim().isEmpty()) {
			redirectAttributes.addFlashAttribute("errors", new String[] {"Het wachtwoord is leeg"});
			return redirect;
		}

		if (password.length() > 16) {
			redirectAttributes.addFlashAttribute("errors", new String[] {"Het wachtwoord is te lang"});
			return redirect;
		}

		Upload upload = findUpload(uploadUuid);
		if (upload == null) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_UPLOAD});
			return redirect;
		}

		// Get the upload
		if (!hasAccess(upload, TEMPUSER)) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_ACCESS});
			return redirect;
		}

		// Check if the upload is completed
		if (!upload.isCompleted()) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_COMPLETED});
			return redirect;
		}

		// Check if the upload is expired
		if (upload.isExpired()) {
			redirectAttributes.addFlashAttribute("errors", new String[]{ERROR_EXPIRED});
			return redirect;
		}

		// Get the uploaded file
		UploadFile uploadFile = upload.getFile(fileUuid);
		if (uploadFile == null) {
			log.error("File (" + fileUuid + ") could not be found");
			redirectAttributes.addFlashAttribute("errors", new String[]{"Kan het gevraagde bestand niet vinden"});
			return redirect;
		}

		// If the file is password protected, display the password page
		if (upload.isPasswordProtected()) {
			redirectAttributes.addFlashAttribute("uploadUuid", uploadUuid);
			redirectAttributes.addFlashAttribute("fileUuid", fileUuid);
			return redirect;
		}

		// Serve the file
		return serveFile(uploadUuid, uploadFile, null, redirectAttributes);
	}

	/**
	 * Serve the file
	 * @param uploadUuid
	 * @param uploadFile
	 * @param redirectAttributes
	 * @return
	 */
	private Object serveFile(String uploadUuid, UploadFile uploadFile, String password, RedirectAttributes redirectAttributes) {
		try {
			File encryptedFile = storageService.loadFinalAsFile(uploadUuid, uploadFile.getName() + EncryptionUtils.ENCRYPTION_SUFFIX);
			File decryptedFile = storageService.decryptFile(password, encryptedFile);
			String fileHash = HashingUtils.getFileHash(decryptedFile);
			
			if (uploadFile.getHash().equals(fileHash)) {
				
// FIXME				pak het juiste bestand op
				Resource resource = storageService.loadFinalAsResource(uploadUuid, decryptedFile.getName());
				return ResponseEntity.ok().header(
						HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\""
						).body(resource);
			} else {
				log.error("The hashes for file (" + uploadFile.getUuid() + ") do not match");
				redirectAttributes.addFlashAttribute("errors", new String[]{"Het wachtwoord is onjuist"});
				return REDIRECT_UPLOADS;
			}
			
		} catch (EncryptionException e) {
			log.error("File (" + uploadFile.getUuid() + ") could not be decrypted", e);
			redirectAttributes.addFlashAttribute("errors", new String[]{"Kan het gevraagde bestand niet ontsleutelen"});
			return REDIRECT_UPLOADS;
		} catch (HashingException e) {
			log.error("File (" + uploadFile.getUuid() + ") could not be hashed", e);
			redirectAttributes.addFlashAttribute("errors", new String[]{"Kan het gevraagde bestand niet ophalen"});
			return REDIRECT_UPLOADS;
		}
	}

	/**
	 * Add a file to the zip output stream
	 * FIXME: In the storage service?
	 * 
	 * @param file
	 * @param fileName
	 * @param zip
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void addToZipFile(File file, ZipOutputStream zip) throws FileNotFoundException, IOException {
		FileInputStream inputStream = new FileInputStream(file);
		ZipEntry zipEntry = new ZipEntry(file.getName());
		zip.putNextEntry(zipEntry);

		byte[] bytes = new byte[1024];
		int length;
		while ((length = inputStream.read(bytes)) >= 0) {
			zip.write(bytes, 0, length);
		}

		zip.closeEntry();
		inputStream.close();
	}
}
