package com.hs.java.pdf;

/**
 * This class represents the system exception mostly pertaining to the database operations.
 * @since v1.0
 * @author Hasan Sunasara
 *
 */
public class SystemException extends Exception {
	
	private String message;
	
	public SystemException(Throwable cause) {
		super(cause);
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	
}
