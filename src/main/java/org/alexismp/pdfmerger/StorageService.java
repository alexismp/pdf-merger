package org.alexismp.pdfmerger;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
	void init();
	void store(MultipartFile file);
	void deleteAll();
}