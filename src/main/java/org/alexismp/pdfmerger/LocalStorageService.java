
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
	private final Path rootLocation = Paths.get("./tmp");
	private final String outputFilename = "output.pdf";
	private Map<String, List<Path>> allFiles;

	@Autowired
	public LocalStorageService() {
		allFiles = new ConcurrentHashMap<>();
	}

	@Override
	public void init() {
		try {
			Files.createDirectories(rootLocation);
			System.out.println("Created temporary directory...");
			// This could also be a good place to test that the binary used is actually available
		} catch (IOException e) {
			logAndThrowException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not initialize storage!", e);
		}
	}

	@Override
	public void storePDF(MultipartFile file, String idPrefix) {
		// create a unique temp directory for this set of files and an ordered list of files to merge
		List<Path> filesToMerge;
		File dirTmpFiles = new File(rootLocation + "/" + idPrefix);

		if (allFiles.containsKey(idPrefix)) {
			filesToMerge = allFiles.get(idPrefix);
		} else {
			filesToMerge = Collections.synchronizedList(new ArrayList<Path>());
			allFiles.put(idPrefix, filesToMerge);
			dirTmpFiles.mkdir();
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
				Path tmpFile = Paths.get(dirTmpFiles.getPath(), file.getOriginalFilename());
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
		Path resultFile = Paths.get(rootLocation + idPrefix + "-" + outputFilename);
		if (!resultFile.toFile().exists()) {
			logAndThrowException(HttpStatus.FORBIDDEN, "Trying to access merged PDF file that doesn't exist", null);
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
		Path resultFile = Paths.get(rootLocation + idPrefix + "-" + outputFilename);
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
				Path dir = Paths.get(rootLocation + "/" + idPrefix);
				Files.walk(dir)
						.map(Path::toFile)
						.forEach(File::delete);
				Files.delete(dir);
			} catch (IOException e) {
				System.err.println("Unable to delete all files: " + e);
			}
		}
	}

	@Override
	public int numberOfFilesToMerge(String idPrefix) {
		return allFiles.get(idPrefix).size();
	}

	private void logAndThrowException(HttpStatus status, String msg, Throwable e) {
		System.err.println(msg);
		throw new ResponseStatusException(status, msg, e);
	}
}