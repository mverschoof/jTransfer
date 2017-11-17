package com.jcore.jtransfer.util;

import com.jcore.jtransfer.exception.HashingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.web.multipart.MultipartFile;

public class HashingUtils {

	private static final String MD5 = "MD5";

	/**
	 * Create a hash of the given file
	 * 
	 * @param file
	 * @return
	 * @throws HashingException
	 */
	public static String getFileHash(MultipartFile file) throws HashingException {
		try {
			return getHash(file.getInputStream());
		} catch (NoSuchAlgorithmException | IOException e) {
			throw new HashingException("An error occurred when creating the file hash", e);
		}
	}

	/**
	 * Create a hash of the given file
	 * 
	 * @param file
	 * @return
	 * @throws HashingException
	 */
	public static String getFileHash(File file) throws HashingException {
		try {
			return getHash(new FileInputStream(file));
		} catch (NoSuchAlgorithmException | IOException e) {
			throw new HashingException("An error occurred when creating the file hash", e);
		}
	}

	/**
	 * Get the hash from an input stream
	 * 
	 * @param messageDigest
	 * @param inputStream
	 * @return
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	private static String getHash(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance(MD5);
		byte[] dataBytes = new byte[1024];
		int nread = 0; 
		while ((nread = inputStream.read(dataBytes)) != -1) {
			messageDigest.update(dataBytes, 0, nread);
		};
		inputStream.close();

		return readDigest(messageDigest.digest());
	}

	/**
	 * Read a byte array and return it as hex string
	 * 
	 * @param digest
	 * @return
	 */
	private static String readDigest(byte[] digest) {
		StringBuffer buffer = new StringBuffer("");
		for (byte d : digest) {
			buffer.append(Integer.toString((d & 0xff) + 0x100, 16).substring(1));
		}
		return buffer.toString();
	}
}
