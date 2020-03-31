package org.alexismp.pdfmerger;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface StorageService {
	void init();
	void store(MultipartFile file);
	void deleteAll();
	int numFilesSaved();
	Path getRootLocation();
	void mergeFiles(List<String> filenames);
	byte[] getMergedPDF() throws IOException;
}