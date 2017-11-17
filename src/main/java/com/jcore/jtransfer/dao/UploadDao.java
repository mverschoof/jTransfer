package com.jcore.jtransfer.dao;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.repository.CrudRepository;

import com.jcore.jtransfer.model.Upload;

@Transactional
public interface UploadDao extends CrudRepository<Upload, Long> {

	public Upload findByUuid(String uuid);
	
	public List<Upload> findByUploadedBy(String uploadedBy);

	public List<Upload> findByRecipientsContaining(String user);
}
