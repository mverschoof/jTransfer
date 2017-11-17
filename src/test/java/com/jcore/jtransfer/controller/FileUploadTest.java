package com.jcore.jtransfer.controller;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.jcore.jtransfer.exception.StorageFileNotFoundException;
import com.jcore.jtransfer.service.StorageService;

import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class FileUploadTest {

	// TODO: Field injection is evil, constructor or method is better
	@Autowired
	private MockMvc mvc;

	@MockBean
	private StorageService storageService;

	@Test
	public void shouldListAllFiles() throws Exception {
		given(this.storageService.loadAll())
				.willReturn(Stream.of(Paths.get("first.txt"), Paths.get("second.txt")));

		this.mvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("files",
						Matchers.contains("http://localhost/files/first.txt", "http://localhost/files/second.txt")));
	}

	@Test
	public void shouldSaveUploadedFile() throws Exception {
		String fileId = "12345";
		MockMultipartFile multipartFile = new MockMultipartFile(
				"file", "test.txt", "text/plain", "Spring Framework".getBytes());

		this.mvc.perform(fileUpload("/").file(multipartFile))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "/"));

		then(this.storageService).should().store(fileId, multipartFile);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testExistingFile() throws Exception {
		given(this.storageService.loadFinalAsResource("12345", "test.txt"))
				.willThrow(StorageFileNotFoundException.class);

		this.mvc.perform(get("/files/test.txt")).andExpect(status().isNotFound());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMissingFile() throws Exception {
		given(this.storageService.loadFinalAsResource("12345", "notexisting.txt"))
				.willThrow(StorageFileNotFoundException.class);

		this.mvc.perform(get("/files/test.txt")).andExpect(status().isNotFound());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMissingDirectory() throws Exception {
		given(this.storageService.loadFinalAsResource("56789", "test.txt"))
				.willThrow(StorageFileNotFoundException.class);

		this.mvc.perform(get("/files/test.txt")).andExpect(status().isNotFound());
	}

}
