package org.alexismp.pdfmerger;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface StorageService {
	void init();
	void store(MultipartFile file);
	void deleteAll();
	int numFilesSaved();
	Path getRootLocation();
}