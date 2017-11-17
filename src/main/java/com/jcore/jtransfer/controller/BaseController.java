package com.jcore.jtransfer.controller;

import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jcore.jtransfer.dao.UploadDao;
import com.jcore.jtransfer.model.Upload;
import com.jcore.jtransfer.service.StorageService;

public class BaseController {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	// FIXME: This needs to be the logged in user of course
	protected static final String TEMPUSER = "michael.verschoof@jcore.com";

	protected static final ZoneId TIMEZONE = ZoneId.of("Europe/Amsterdam");

	protected static final String ERROR_UUID = "Het upload UUID is niet gevuld";
	protected static final String ERROR_FILE = "Er is geen bestand om te uploaden";
	protected static final String ERROR_UPLOAD = "De upload kan niet gevonden worden";
	protected static final String ERROR_ACCESS = "U bent niet gemachtigd om de gevraagde upload te bekijken";
	protected static final String ERROR_COMPLETED = "De gevraagde upload is nog niet voltooid en kan daardoor nog niet bekeken worden";
	protected static final String ERROR_EXPIRED = "De gevraagde upload is verlopen en kan niet meer bekeken worden";
	protected static final String ERROR_PASSWORD = "De gevraagde upload is met een wachtwoord beveiligd maar er is geen wachtwoord opgegeven";

	protected static final String REDIRECT_UPLOAD = "redirect:/upload";
	protected static final String REDIRECT_UPLOADS = "redirect:/uploads";
	protected static final String REDIRECT_RECEIVED = "redirect:/received";

	// TODO: Field injection is evil, constructor or method is better
//	@Autowired
	protected StorageService storageService;

	// TODO: Field injection is evil, constructor or method is better
//	@Autowired
	protected UploadDao uploadDao;

	@Autowired
	public BaseController(StorageService storageService, UploadDao uploadDao) {
		this.storageService = storageService;
		this.uploadDao = uploadDao;
	}

	/**
	 * Get and validate an upload
	 * 
	 * @param uploadUuid
	 * @param uploader
	 * @return The <code>Upload</code> if the upload can be found, 
	 *         <code>null</code> if the upload cannot be found or if it is expired
	 */
	protected Upload findUpload(String uploadUuid) {
		if (!validateUuid(uploadUuid)) {
			return null;
		}

		// Find the upload
		Upload upload = this.findUploadByUuid(uploadUuid);
		if (upload == null) {
			log.error("The upload (" + uploadUuid + ") could not be found");
			return null;
		}

		return upload;
	}

	/**
	 * Check if a user is either the uploader or a recipient of an upload
	 * TODO: Switch user string with user object
	 * 
	 * @return <code>true</code> if the user is the uploader or a recipient, <code>false</code> otherwise
	 */
	protected boolean hasAccess(Upload upload, String user) {
		if (user.equals(upload.getUploadedBy()) || upload.hasRecipient(user)) {
			return true;
		}
		log.warn("Unauthorized access attempt on upload (" + upload.getUuid() + ") by " + user);
		return false;
	}

	/**
	 * Get a specific upload, if the user is allowed to view it
	 * 
	 * @param uploadUuid
	 * @param user
	 * @return
	 */
	private Upload findUploadByUuid(String uploadUuid) {
		return uploadDao.findByUuid(uploadUuid);
	}

	/**
	 * Validate the UUID
	 * 
	 * @param uuid
	 * @return <code>true</code> if it's a valid UUID, <code>false</code> otherwise
	 */
	protected boolean validateUuid(String uuid) {
		if (uuid == null || uuid.trim().isEmpty()) {
			log.error("The given UUID is either null or empty");
			return false;
		}
		if (uuid.length() != 36) {
			log.error("The given UUID (" + uuid + ") is not 36 characters long");
			return false;
		}
		return true;
	}

}
