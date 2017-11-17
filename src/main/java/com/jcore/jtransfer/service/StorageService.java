package com.jcore.jtransfer.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.jcore.jtransfer.exception.EncryptionException;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface StorageService {

	public void init();

	/**
	 * Store a file in the upload directory in temporary file location
	 * A directory is created if it does not yet exist
	 * 
	 * @param uploadUuid
	 * @param file
	 */
	public void store(String uploadUuid, MultipartFile file);

	/**
	 * Move a directory and containing files from the temporary location to the final location
	 * 
	 * @param uploadUuid
	 */
	public void move(String uploadUuid);

	public Stream<Path> loadAll();

	public Path load(String filename);

	/**
	 * Load a file from the final location
	 * 
	 * @param uploadUuid
	 * @param fileName
	 * @return
	 */
	public Resource loadFinalAsResource(String uploadUuid, String fileName);

	/**
	 * Load a file from the temporary location
	 * 
	 * @param uploadUuid
	 * @param fileName
	 * @return
	 */
	public Resource loadTempAsResource(String uploadUuid, String fileName);

	/**
	 * Load a file from the given path
	 * 
	 * @param file
	 * @return
	 */
	public Resource loadAsResource(Path file);

	/**
	 * Load a file from the final location
	 * 
	 * @param uploadUuid
	 * @param fileName
	 * @return
	 */
	public File loadFinalAsFile(String uploadUuid, String fileName);

	/**
	 * Load a file from the temporary location
	 * 
	 * @param uploadUuid
	 * @param fileName
	 * @return
	 */
	public File loadTempAsFile(String uploadUuid, String fileName);

	/**
	 * Load a list of file names from the upload location
	 * 
	 * @param uploadUuid
	 * @return
	 */
	public List<String> getUploadFiles(String uploadUuid);

	/**
	 * Delete a file from the final location
	 * 
	 * @param uploadUuid
	 */
	public void deleteFinal(String uploadUuid);

	/**
	 * Delete a file from the temporary location
	 * 
	 * @param uploadUuid
	 */
	public void deleteTemp(String uploadUuid);

	/**
	 * Create a zip file from the files in a final upload directory
	 * 
	 * @param uploadUuid
	 * @return
	 */
	public Path createUploadZip(String uploadUuid);

	/**
	 * Get an upload from the final location
	 * @see #getUploadPath
	 * 
	 * @param uploadUuid
	 * @return
	 */
	public Path getFinalUploadPath(String uploadUuid);

	/**
	 * Get an upload from the temporary location
	 * @see #getUploadPath
	 * 
	 * @param uploadUuid
	 * @return
	 */
	public Path getTempUploadPath(String uploadUuid);

	/**
	 * Encrypt all files in an upload directory and delete the original files
	 * 
	 * @param uploadUuid
	 * @param password
	 * @throws EncryptionException 
	 */
	public void encryptUpload(String uploadUuid, String password) throws EncryptionException;

	/**
	 * Decrypt a file
	 * 
	 * @param password
	 * @param inputFile
	 * @param outputFile
	 * @throws EncryptionException 
	 */
	public File decryptFile(String uploadUuid, File inputFile) throws EncryptionException;

	/**
	 * Load and decrypt a file
	 * 
	 * @see #decryptFile(String, File)
	 * 
	 * @param password
	 * @param inputFile
	 * @param outputFile
	 * @throws EncryptionException 
	 */
	public File decryptFile(String uploadUuid, String fileName, String password) throws EncryptionException;

}