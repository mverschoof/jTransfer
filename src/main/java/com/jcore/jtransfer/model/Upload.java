package com.jcore.jtransfer.model;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "upload", catalog = "jtransfer")
public class Upload implements Serializable, Comparable<Upload> {

	private static final long serialVersionUID = -1875569088752172640L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "UUID", unique = true, nullable = false, length = 36)
	private String uuid;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "upload", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
	private List<UploadFile> files;

	@Column(name = "RECIPIENTS", nullable = true)
	private String recipients;

	@Column(name = "UPLOADED_BY", nullable = false)
	private String uploadedBy;

	@Column(name = "UPLOADED_ON", nullable = false)
	private ZonedDateTime uploadedOn;

	@Column(name = "EXPIRES_ON", nullable = false)
	private ZonedDateTime expiresOn;

	@Column(name = "PASSWORD_PROTECTED", nullable = false)
	private boolean passwordProtected;

	@Column(name = "COMPLETED", nullable = false)
	private boolean completed;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public List<UploadFile> getFiles() {
		return files;
	}

	public void setFiles(List<UploadFile> files) {
		this.files = files;
	}

	public void addFile(UploadFile file) {
		if (this.files == null) {
			this.files = new ArrayList<UploadFile>();
		}
		this.files.add(file);
	}

	public UploadFile getFile(String uuid) {
		if (this.files == null) {
			return null;
		}

		for (UploadFile uploadFile : this.files) {
			if (uploadFile.getUuid().equals(uuid)) {
				return uploadFile;
			}
		}

		return null;
	}

	public String getRecipients() {
		return recipients;
	}

	public void setRecipients(String recipients) {
		this.recipients = recipients;
	}

	public ZonedDateTime getUploadedOn() {
		return uploadedOn;
	}

	public void setUploadedOn(ZonedDateTime uploadedOn) {
		this.uploadedOn = uploadedOn;
	}

	public String getUploadedBy() {
		return uploadedBy;
	}

	public void setUploadedBy(String uploadedBy) {
		this.uploadedBy = uploadedBy;
	}

	public ZonedDateTime getExpiresOn() {
		return expiresOn;
	}

	public void setExpiresOn(ZonedDateTime expiresOn) {
		this.expiresOn = expiresOn;
	}

	public boolean isExpired() {
		// TODO: Someday we'll make it user location dependent
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam"));

		if (this.getExpiresOn().truncatedTo(ChronoUnit.DAYS).isAfter(now.truncatedTo(ChronoUnit.DAYS))) {
			return false;
		}

		return true;
	}

	public boolean isPasswordProtected() {
		return passwordProtected;
	}

	public void setPasswordProtected(boolean passwordProtected) {
		this.passwordProtected = passwordProtected;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public boolean hasRecipient(String user) {
		return this.recipients.contains(user);
	}

	public String[] getRecipientsAsArray() {
		if (this.recipients == null || this.recipients.trim().isEmpty()) {
			return new String[]{};
		}

		// First off, let's remove any whitespaces
		String r = this.recipients.replaceAll("\\s+", "");

		// Split the String into separate recipients
		return r.split(",|;");
	}

	@Override
	public int compareTo(Upload other) {
		ZonedDateTime thisEx = this.getExpiresOn();
		ZonedDateTime otherEx = other.getExpiresOn();

		if (thisEx == null && otherEx != null) {
			return 1;
		}

		if (thisEx != null && otherEx == null) {
			return -1;
		}

		if (thisEx == null && otherEx == null) {
			if (this.getUploadedOn().isAfter(other.getUploadedOn())) {
				return -1;
			} else if (this.getUploadedOn().isBefore(other.getUploadedOn())) {
				return 1;
			}

			return 0;
		}

		if (thisEx.isAfter(otherEx)) {
			return -1;
		} else if (thisEx.isBefore(otherEx)) {
			return 1;
		}

		return 0;
	}
}
