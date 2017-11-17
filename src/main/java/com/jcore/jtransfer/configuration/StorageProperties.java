package com.jcore.jtransfer.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jtransfer")
public class StorageProperties {

	/**
	 * Directory for storing files
	 */
	private String location = System.getProperty("rootDirectory");

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}