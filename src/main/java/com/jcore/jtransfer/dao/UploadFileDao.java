package com.jcore.jtransfer.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.jcore.jtransfer.model.UploadFile;

public interface UploadFileDao extends CrudRepository<UploadFile, Long> {

	public UploadFile findByUuid(String uuid);

	public List<UploadFile> findByUploadUuid(String uploadUuid);

}
