
/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alexismp.pdfmerger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocalStorageService implements StorageService {
	private final Path rootLocation; // Initialization removed
	private final String outputFilename = "output.pdf";
	private Map<String, List<Path>> allFiles;

	// New constructor for tests and general use
	public LocalStorageService(Path rootLocation) {
		this.rootLocation = rootLocation;
		this.allFiles = new ConcurrentHashMap<>();
		// init() is not called here; will be called by Spring or explicitly in tests.
	}

	@Autowired
	public LocalStorageService() {
		this(Paths.get("./tmp")); // Delegates to the new constructor
	}

	@Override
	public void init() {
		try {
			Files.createDirectories(this.rootLocation); // Use this.rootLocation
			System.out.println("Created temporary directory at: " + this.rootLocation.toString());
			// This could also be a good place to test that the binary used is actually available
		} catch (IOException e) {
			logAndThrowException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not initialize storage!", e);
		}
	}

	@Override
	public void storePDF(MultipartFile file, String idPrefix) {
		// create a unique temp directory for this set of files and an ordered list of files to merge
		List<Path> filesToMerge;
		Path userSpecificDir = this.rootLocation.resolve(idPrefix); // Use this.rootLocation.resolve()

		if (allFiles.containsKey(idPrefix)) {
			filesToMerge = allFiles.get(idPrefix);
		} else {
			filesToMerge = Collections.synchronizedList(new ArrayList<Path>());
			allFiles.put(idPrefix, filesToMerge);
			try {
				Files.createDirectories(userSpecificDir); // Create directory using Files API
			} catch (IOException e) {
				logAndThrowException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create user specific directory " + userSpecificDir, e);
			}
		}

		String filename = file.getOriginalFilename();
		if (!filename.endsWith(".pdf")) {
			logAndThrowException(HttpStatus.BAD_REQUEST, filename + " doesn't seem to be a PDF file.", null);
		} else if (file.isEmpty()) {
			logAndThrowException(HttpStatus.NO_CONTENT, filename + " is empty!", null);
		} else if (filename.contains("..")) { // This is a security check
			logAndThrowException(HttpStatus.FORBIDDEN, "Sorry, can't navigate the filesystem... "+ filename, null);
		} else {
			try {
				InputStream inputStream = file.getInputStream();
				Path tmpFile = userSpecificDir.resolve(file.getOriginalFilename()); // Use userSpecificDir.resolve()
				Files.copy(inputStream, tmpFile, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Successfully saved " + tmpFile.toString());
				filesToMerge.add(tmpFile);
			} catch (IOException e) {
				logAndThrowException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file " + file, e);
			}
		}
	}

	@Override
	public byte[] getMergedPDF(String idPrefix) {
		Path resultFile = this.rootLocation.resolve(idPrefix + "-" + outputFilename); // Use this.rootLocation.resolve()
		if (!Files.exists(resultFile)) { // Use Files.exists()
			logAndThrowException(HttpStatus.FORBIDDEN, "Trying to access merged PDF file that doesn't exist: " + resultFile, null);
			return null;
		}
		try {
			// read files in memory before deleting file
			byte[] result = Files.readAllBytes(resultFile);
			Files.delete(resultFile);
			return result;
		} catch (IOException ioe) {
			logAndThrowException(HttpStatus.INTERNAL_SERVER_ERROR, "Error serving merged PDF, sorry!", ioe);
			return null;
		}
	}

	@Override
	public void mergeFiles(String idPrefix) {
		Path resultFile = this.rootLocation.resolve(idPrefix + "-" + outputFilename); // Use this.rootLocation.resolve()
		final StringBuilder mergeCommand = new StringBuilder("/usr/bin/pdfunite ");

		// add all files in the order they're specified
		List<Path> filesToMerge = allFiles.get(idPrefix);
		System.out.println("About to merge " + filesToMerge.size() + " files");
		for (final Path filename : filesToMerge) {
			String newFile = "'" + filename.toString() + "' ";
			mergeCommand.append(newFile);
		}
		mergeCommand.append(" ");
		mergeCommand.append(resultFile.toString()); 	// output goes last with 'pdfunite'

		final ProcessBuilder builder = new ProcessBuilder();
		builder.command("sh", "-c", mergeCommand.toString());

		try {
			final Process process = builder.inheritIO().start();
			final int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Success: merged " + filesToMerge.size() + " files.");
			} else {
				logAndThrowException(HttpStatus.INTERNAL_SERVER_ERROR,
						"Merging process exited with error code : " + exitCode, null);
			}
		} catch (IOException | InterruptedException e) {
			logAndThrowException(HttpStatus.INTERNAL_SERVER_ERROR,
					"Something went wrong trying to merge ! ", e);
		} finally {
			// clean up master Map and delete directory
			try {
				allFiles.remove(idPrefix);
				Path dir = this.rootLocation.resolve(idPrefix); // Use this.rootLocation.resolve()
				// Ensure files within the directory are deleted first
				if (Files.exists(dir) && Files.isDirectory(dir)) {
					Files.walk(dir)
							.sorted(Collections.reverseOrder()) // Delete contents first
							.map(Path::toFile)
							.forEach(File::delete);
				}
				// Files.delete(dir); // The directory itself will be deleted by @TempDir or further cleanup
			} catch (IOException e) {
				System.err.println("Unable to delete all files: " + e);
			}
		}
	}

	@Override
	public int numberOfFilesToMerge(String idPrefix) {
        List<Path> prefixedFiles = allFiles.get(idPrefix);
        if (allFiles == null || prefixedFiles == null) return 0;
		return allFiles.get(idPrefix).size();
	}

	private void logAndThrowException(HttpStatus status, String msg, Throwable e) {
		System.err.println(msg);
		throw new ResponseStatusException(status, msg, e);
	}

	public Path getRootLocation() {
		return this.rootLocation;
	}

	public List<Path> getFilesToMerge(String idPrefix) {
		return allFiles.get(idPrefix);
	}
}