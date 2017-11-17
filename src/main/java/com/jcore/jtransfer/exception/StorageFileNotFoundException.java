package com.jcore.jtransfer.exception;

public class StorageFileNotFoundException extends StorageException {

	private static final long serialVersionUID = 1547221298237584134L;

	public StorageFileNotFoundException(String message) {
        super(message);
    }

    public StorageFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
