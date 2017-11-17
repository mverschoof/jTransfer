package com.jcore.jtransfer.exception;

public class StorageException extends RuntimeException {

	private static final long serialVersionUID = -7624677964054850352L;

	public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
