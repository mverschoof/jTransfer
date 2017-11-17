package com.jcore.jtransfer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import com.jcore.jtransfer.configuration.StorageProperties;
import com.jcore.jtransfer.exception.EncryptionException;
import com.jcore.jtransfer.exception.StorageException;
import com.jcore.jtransfer.exception.StorageFileNotFoundException;
import com.jcore.jtransfer.util.EncryptionUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService implements StorageService {

	private final Logger log = LoggerFactory.getLogger(FileStorageService.class);

	// The file locations
	private final Path rootLocation;	// Root dir
	private final Path tempLocation;	// First upload location
	private final Path finalLocation;	// Download location (move files here after the settings form has been submitted)

	@Autowired
	public FileStorageService(StorageProperties properties) {
		this.rootLocation = Paths.get(properties.getLocation());
		this.tempLocation = Paths.get(properties.getLocation() + "/temporary");
		this.finalLocation = Paths.get(properties.getLocation() + "/final");
	}
	
//	public static void main(String[] args) {
//
//    }

	@Override
	public void init() {
		try {
			// Create the root directory
			if (Files.notExists(rootLocation, LinkOption.NOFOLLOW_LINKS)) {
				Files.createDirectory(rootLocation);
				log.info("Created new root upload location at " + rootLocation);
			} else {
				log.info("Root upload location already exists at " + rootLocation);
			}

			// Create the temporary upload directory
			if (Files.notExists(tempLocation, LinkOption.NOFOLLOW_LINKS)) {
				Files.createDirectory(tempLocation);
				log.info("Created new temporary upload location at " + tempLocation);
			} else {
				log.info("Temporary upload location already exists at " + tempLocation);
			}

			// Create the completed upload directory
			if (Files.notExists(finalLocation, LinkOption.NOFOLLOW_LINKS)) {
				Files.createDirectory(finalLocation);
				log.info("Created new final upload location at " + finalLocation);
			} else {
				log.info("Final upload location already exists at " + finalLocation);
			}
		} catch (IOException e) {
			throw new StorageException("Could not initialize storage", e);
		}
	}

	@Override
	public void store(String uploadUuid, MultipartFile file) {

		Path newFileDir = Paths.get(tempLocation + "/" + uploadUuid);
		String fileName = file.getOriginalFilename();
		Path newFilePath = Paths.get(tempLocation + "/" + uploadUuid + "/" + fileName);

		try {
			// If the file directory does not yet exist
			if (Files.notExists(newFileDir, LinkOption.NOFOLLOW_LINKS)) {
				Files.createDirectory(newFileDir);
				log.info("Created new upload location at " + newFileDir);
			} else {
				log.debug("Upload location " + newFileDir + " already exists");
			}

			// Actually place the file
			if (Files.notExists(newFilePath, LinkOption.NOFOLLOW_LINKS)) {
				Files.copy(file.getInputStream(), newFileDir.resolve(fileName));
				log.info("Copied file " + fileName + " to upload location " + newFileDir);
			} else {
				log.error("File " + fileName + " already exists in " + newFileDir);
				throw new StorageException("Bestand " + fileName + " bestaat al in " + newFileDir);
			}
		} catch (IOException e) {
			log.error("Failed to store file " + file.getOriginalFilename(), e);
			throw new StorageException("Er is een fout opgetreden bij het opslaan van het bestand " + file.getOriginalFilename());
		}
	}

	@Override
	public void move(String uploadUuid) {
		Path tempFileDir = Paths.get(tempLocation + "/" + uploadUuid);
		Path finalFileDir = Paths.get(finalLocation + "/" + uploadUuid);

		// Check if we can find the temporary directory
		if (Files.notExists(tempFileDir, LinkOption.NOFOLLOW_LINKS)) {
			throw new StorageException("Could not find corresponding file directory.");
		}

		// Check if the target directory does not exist yet (should never happen)
		if (Files.exists(finalFileDir, LinkOption.NOFOLLOW_LINKS)) {
			throw new StorageException("New file directory already exists.");
		}

		try {
			// Move the directory and the file(s) within to the final location
			Files.move(tempFileDir, finalFileDir);
		} catch (IOException e) {
			throw new StorageException("Failed to move files from " + tempFileDir + " to " + finalFileDir, e);
		}
	}

	@Override
	public Stream<Path> loadAll() {
		try {
			return Files.walk(this.rootLocation, 1)
					.filter(path -> !path.equals(this.rootLocation))
					.map(path -> this.rootLocation.relativize(path));
		} catch (IOException e) {
			throw new StorageException("Failed to read stored files", e);
		}

	}

	@Override
	public Path load(String filename) {
		return rootLocation.resolve(filename);
	}

	@Override
	public Resource loadFinalAsResource(String uploadUuid, String fileName) {
		Path resolved = this.finalLocation.resolve(uploadUuid + "/" + fileName);

		return this.loadAsResource(resolved);
	}

	@Override
	public Resource loadTempAsResource(String uploadUuid, String fileName) {
		Path resolved = this.tempLocation.resolve(uploadUuid + "/" + fileName);

		return this.loadAsResource(resolved);
	}

	@Override
	public Resource loadAsResource(Path file) {
		try {
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() && resource.isReadable()) {
				return resource;
			}
			else {
				throw new StorageFileNotFoundException("Kan het bestand " + file.getFileName() + " niet lezen of niet vinden");
			}
		} catch (MalformedURLException e) {
			throw new StorageFileNotFoundException("De link naar het bestand " + file.getFileName() + " is onjuist", e);
		}
	}

	@Override
	public List<String> getUploadFiles(String uploadUuid) {
		Path path = this.getTempUploadPath(uploadUuid);

		List<String> fileNames = new ArrayList<>();
		try {
			fileNames = Files.walk(path)
				.filter(Files::isRegularFile)
				.map(p -> p.getFileName().toString())
				.collect(Collectors.toList());
		} catch (IOException e) {
			throw new StorageException("", e);
		}

		return fileNames;
	}

	@Override
	public File loadFinalAsFile(String uploadUuid, String fileName) {
		return loadAsFile(uploadUuid, fileName, this.finalLocation);
	}

	@Override
	public File loadTempAsFile(String uploadUuid, String fileName) {
		return loadAsFile(uploadUuid, fileName, this.tempLocation);
	}

	/**
	 * Load a file from a location
	 * 
	 * @param uploadUuid
	 * @param fileName
	 * @param path
	 * @return
	 */
	private File loadAsFile(String uploadUuid, String fileName, Path path) {
		Path resolved = path.resolve(uploadUuid + "/" + fileName);
		File file = resolved.toFile();

		if (file.exists() && file.canRead()) {
			return file;
		} else {
			throw new StorageFileNotFoundException("Kan het bestand " + fileName + " niet lezen of niet vinden");
		}
	}

	@Override
	public void deleteFinal(String uploadUuid) {
		Path directory = this.getFinalUploadPath(uploadUuid);

		// TODO: Check if this is safe enough
		FileSystemUtils.deleteRecursively(directory.toFile());
	}

	@Override
	public void deleteTemp(String uploadUuid) {
		Path directory = this.getTempUploadPath(uploadUuid);

		// TODO: Check if this is safe enough
		FileSystemUtils.deleteRecursively(directory.toFile());
	}

	@Override
	public Path createUploadZip(String uploadUuid) {
		// Set the zip name
		String zipName = "jtransfer-" + uploadUuid + ".zip";

		// Get the zip path
		Path zipPath = Paths.get(this.getFinalUploadPath(uploadUuid) + "/" + zipName);

		// Check if it's not a directory
		if (Files.isDirectory(zipPath, LinkOption.NOFOLLOW_LINKS)) {
			throw new StorageFileNotFoundException("Het aan te maken zip bestand blijkt een folder te zijn");
		}

		try {
			// If it exists, delete it
			if (Files.exists(zipPath, LinkOption.NOFOLLOW_LINKS)) {
				Files.delete(zipPath);
			}

			// Create the new zip path
			return Files.createFile(zipPath);
			
		} catch (IOException e) {
			log.error("An error occurred when creating the zip file.", e);
			throw new StorageException("Er is een fout opgetreden met het aanmaken van de zip");
		}
	}

	@Override
	public Path getFinalUploadPath(String uploadUuid) {
		Path directory = Paths.get(finalLocation + "/" + uploadUuid);

		return getUploadPath(directory);
	}

	@Override
	public Path getTempUploadPath(String uploadUuid) {
		Path directory = Paths.get(tempLocation + "/" + uploadUuid);

		return getUploadPath(directory);
	}

	/**
	 * Get an upload from the given location
	 * 
	 * @param directory
	 * @return
	 */
	private Path getUploadPath(Path directory) {
		if (Files.notExists(directory, LinkOption.NOFOLLOW_LINKS)) {
			log.warn("A non existing upload was requested (" + directory.toString() + ")");
			throw new StorageException("Kan de upload niet vinden");
		}

		return directory;
	}

	@Override
	public void encryptUpload(String uploadUuid, String password) throws EncryptionException {
		// Get the files for this upload
		List<String> fileNames = this.getUploadFiles(uploadUuid);
		for (String fileName : fileNames) {
			// Encrypt each file that is not yet encrypted
			if (!fileName.endsWith(EncryptionUtils.ENCRYPTION_SUFFIX)) {
				File inputFile = this.loadTempAsFile(uploadUuid, fileName);
				File encryptedFile = new File(inputFile.getPath() + EncryptionUtils.ENCRYPTION_SUFFIX);
				EncryptionUtils.encrypt(password, inputFile, encryptedFile);
			}
		}

		// Delete the original files
		fileNames = this.getUploadFiles(uploadUuid);
		for (String fileName : fileNames) {
			// Delete the non-encrupted files
			if (!fileName.endsWith(EncryptionUtils.ENCRYPTION_SUFFIX)) {
				File file = this.loadTempAsFile(uploadUuid, fileName);
				file.delete();
			}
		}
	}

	@Override
	public File decryptFile(String uploadUuid, String fileName, String password) throws EncryptionException {
		log.debug("Loading file '" + fileName + "' for decryption");
		File file = this.loadFinalAsFile(uploadUuid, fileName + EncryptionUtils.ENCRYPTION_SUFFIX);
		return decryptFile(password, file);
	}

	@Override
	public File decryptFile(String password, File inputFile) throws EncryptionException {
		log.debug("Decrypting file '" + inputFile.getName() + "'");
		
		String newName = inputFile.getPath();
		if (newName.endsWith(EncryptionUtils.ENCRYPTION_SUFFIX)) {
//			newName = newName.replace(EncryptionUtils.ENCRYPTION_SUFFIX, EncryptionUtils.DECRYPTION_SUFFIX);
			newName = newName.replace(EncryptionUtils.ENCRYPTION_SUFFIX, "");
		}

		File decryptedFile = new File(newName);
		EncryptionUtils.decrypt(password, inputFile, decryptedFile);
		log.debug("Decrypted file '" + inputFile.getName() + "'");
		return decryptedFile;
	}
}