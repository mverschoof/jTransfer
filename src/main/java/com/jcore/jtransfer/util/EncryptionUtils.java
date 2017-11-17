package com.jcore.jtransfer.util;

import com.jcore.jtransfer.exception.EncryptionException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {

	private static final String SALT = "jtransfer-himalayan-salt";

	private static final String ALGORITHM = "AES";

	private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

	public static final String ENCRYPTION_SUFFIX = ".encrypted";

	public static final String DECRYPTION_SUFFIX = ".decrypted";

	/**
	 * Encrypt a file using the given key
	 * 
	 * @param key
	 * @param inputFile
	 * @param outputFile
	 * @throws EncryptionException
	 */
	public static void encrypt(String password, File inputFile, File outputFile) throws EncryptionException {
		crypt(Cipher.ENCRYPT_MODE, password, inputFile, outputFile);
	}

	/**
	 * Decrypt a file using the given key
	 * 
	 * @param key
	 * @param inputFile
	 * @param outputFile
	 * @throws EncryptionException
	 */
	public static void decrypt(String password, File inputFile, File outputFile) throws EncryptionException {
		crypt(Cipher.DECRYPT_MODE, password, inputFile, outputFile);
	}

	/**
	 * Encrypt or decrypt a file
	 * 
	 * @param cipherMode
	 * @param key
	 * @param inputFile
	 * @param outputFile
	 * @throws EncryptionException
	 */
	private static void crypt(int cipherMode, String password, File inputFile, File outputFile) throws EncryptionException {
		
		try (FileInputStream inputStream = new FileInputStream(inputFile); 
				FileOutputStream outputStream = new FileOutputStream(outputFile)) {

			// Create the salted key to either encrypt or decrypt with
			byte[] key = getKey(password);

			// Create the cipher
			Key secretKey = new SecretKeySpec(key, ALGORITHM);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(cipherMode, secretKey);

			// Read the file
			byte[] inputBytes = new byte[(int) inputFile.length()];
			inputStream.read(inputBytes);

			// Encrypt or decrypt to the ouput file
			byte[] outputBytes = cipher.doFinal(inputBytes);
			outputStream.write(outputBytes);

		} catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException 
				| BadPaddingException | IllegalBlockSizeException | IOException e) {
			throw new EncryptionException("Error encrypting/decrypting file", e);
		}
	}

	/**
	 * Create a key using the default key and the given password
	 * 
	 * @param password
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchAlgorithmException 
	 */
	private static byte[] getKey(String password) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		// Check if the password is filled
		if (password == null || password.isEmpty()) {
			password = "";
		}

		// Create the key and digest it
		byte[] key = (password + SALT).getBytes("UTF-8");
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		key = sha.digest(key);

		// Return the first 16 bytes
		return key = Arrays.copyOf(key, 16); // use only first 128 bit
	}
}
