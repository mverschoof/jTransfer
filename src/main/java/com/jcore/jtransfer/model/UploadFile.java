package com.jcore.jtransfer.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "upload_file", catalog = "jtransfer")
public class UploadFile {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "UUID", unique = true, nullable = false, length = 36)
	private String uuid;

	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name="UPLOAD_ID")
	private Upload upload;

	@Column(name = "FILENAME", unique = false, nullable = false)
	private String name;

	@Column(name = "FILESIZE", nullable = false)
	private long size;

	@Column(name = "FILEHASH", nullable = false)
	private String hash;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public Upload getUpload() {
		return upload;
	}

	public void setUpload(Upload upload) {
		this.upload = upload;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getReadableFileSize() {
		return humanReadableByteCount(getSize());
	}

	/**
	 * Convert the byte value to a human readable form
	 * 
	 * @param bytes
	 * @return
	 */
	public static String humanReadableByteCount(long bytes) {
	    int unit = 1000;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = "kMGTPE".charAt(exp-1) + "";
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}
}
