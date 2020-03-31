package org.alexismp.pdfmerger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalStorageService implements StorageService {
	private final Path rootLocation;
	private final Path resultFile;
	private final String outputFilename = "output.pdf";
	private List<String> filesToMerge;

	@Autowired
	public LocalStorageService() {
		this.filesToMerge = Collections.synchronizedList(new ArrayList<String>());
		this.rootLocation = Paths.get("./tmp");
		this.resultFile = Paths.get("./" + outputFilename);
	}

	@Override
	public void init() {
		try {
			Files.createDirectories(rootLocation);
			System.out.println("Created temporary directory...");
		} catch (IOException e) {
			System.out.println("Could not initialize storage - " + e);
		}
	}

	@Override
	public void storePDF(MultipartFile file) {
		String filename = file.getOriginalFilename();
		// System.out.println("About to store: " + filename);
		try {
			if (!filename.endsWith(".pdf")) {
				throw new IOException("This doesn't seem to be a PDF file: " + filename);
			} else if (file.isEmpty()) {
				throw new IOException("Failed to store empty file: " + filename);
			} else if (filename.contains("..")) { // This is a security check
				throw new IOException("Cannot store file with relative path outside current directory " + filename);
			} else {
				InputStream inputStream = file.getInputStream();
				Files.copy(inputStream, this.rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Successfully saved " + filename);
				filesToMerge.add(filename);
			}
		} catch (IOException e) {
			System.out.println("Failed to store file " + filename + " - " + e);
		}
	}

	@Override
	public void deleteTmpFiles() {
		try {
			FileUtils.cleanDirectory(rootLocation.toFile());
			filesToMerge.clear();
		} catch (IOException e) {
			System.out.println("Unable to delete all files: " + e);
		}
	}

	@Override
	public byte[] getMergedPDF() throws IOException {
		if (!resultFile.toFile().exists()) {
			throw new IOException("Nothing to see here!");
		}
		byte[] result = Files.readAllBytes(resultFile);
		byte[] resultCopy = Arrays.copyOf(result, result.length);
		Files.delete(resultFile);
		return resultCopy;
	}

	@Override
	public void mergeFiles() {
		final StringBuilder mergeCommand = new StringBuilder(
				"/usr/bin/gs -sDEVICE=pdfwrite -dCompatibilityLevel=1.4 -dPDFSETTINGS=/default -dNOPAUSE -dQUIET -dBATCH -dDetectDuplicateImages -dCompressFonts=true -r150 -sOutputFile=");

		mergeCommand.append(outputFilename);
		mergeCommand.append(" ");
		// add all files in the order they're specified
		for (final String filename : filesToMerge) {
			mergeCommand.append(rootLocation.toString());
			mergeCommand.append("/");
			mergeCommand.append(filename.replaceAll("\\s", "\\\\ ")); // escape spaces in filename
			mergeCommand.append(" ");
		}
		final ProcessBuilder builder = new ProcessBuilder();
		builder.command("sh", "-c", mergeCommand.toString());

		try {
			final Process process = builder.inheritIO().start();
			final int exitCode = process.waitFor();
			if (exitCode == 0) {
				System.out.println("Success: merged " + filesToMerge.size() + " files.");
				deleteTmpFiles();
			} else {
				System.out.println("Exited with error code : " + exitCode);
			}
		} catch (IOException | InterruptedException e) {
			System.err.println(e);
		}
	}

}