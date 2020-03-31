package org.alexismp.pdfmerger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalStorageService implements StorageService {
	private final Path rootLocation;
	private final Path resultFile;
	private final String outputFilename = "output.pdf";
	private int filesSaved;

    @Autowired
    public LocalStorageService() {
        // TODO: generate unique Id? Depends on concurrency
		this.rootLocation = Paths.get("./tmp");
		this.resultFile = Paths.get("./" + outputFilename);
		// filesSaved = 0;
    }

    @Override
    public void init() {
		filesSaved = 0;
		try {
			Files.createDirectories(rootLocation);
            System.out.println("Created temporary directory...");
		}
		catch (IOException e) {
			System.out.println("Could not initialize storage - " + e);
		}        
    }

    @Override
    // TODO: add second param for prefix?
	public void store(MultipartFile file) {
		String filename = StringUtils.cleanPath(file.getOriginalFilename());
		try {
            if (!filename.endsWith(".pdf")) {
                throw new IOException("This doesn't seem to be a PDF file: " + filename);
            }
			if (file.isEmpty()) {
				throw new IOException("Failed to store empty file: " + filename);
			}
			if (filename.contains("..")) { // This is a security check
				throw new IOException( "Cannot store file with relative path outside current directory " + filename);
			}
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, this.rootLocation.resolve(filename),
					StandardCopyOption.REPLACE_EXISTING);
			}
			filesSaved++;
		}
		catch (IOException e) {
			System.out.println("Failed to store file " + filename + " - " + e);
		}        
    }

	@Override
	public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

	@Override
	public int numFilesSaved() {
		return filesSaved;
	}

	@Override
	public Path getRootLocation() {
		return rootLocation;
	}

	@Override
	public byte[] getMergedPDF() throws IOException {
		return Files.readAllBytes(resultFile);
	}

	@Override
	public void mergeFiles(List<String> filenames) {
		final StringBuilder mergeCommand = new StringBuilder(
				"/usr/bin/gs -sDEVICE=pdfwrite -dCompatibilityLevel=1.4 -dPDFSETTINGS=/default -dNOPAUSE -dQUIET -dBATCH -dDetectDuplicateImages -dCompressFonts=true -r150 -sOutputFile=");

		mergeCommand.append(outputFilename);
		mergeCommand.append(" ");
		// add all files in the order they're specified
		// final List<String> filenames = orderService.listAllFilesInOrder();
		for (final String filename : filenames) {
			mergeCommand.append(getRootLocation().toString() + "/" + filename);
			mergeCommand.append(" ");
		}
		System.out.println("### Command: " + mergeCommand.toString());
		final ProcessBuilder builder = new ProcessBuilder();
		builder.command("sh", "-c", mergeCommand.toString());

		try {
			final Process process = builder.inheritIO().start();
			final int exitCode = process.waitFor();
			System.out.println("\nExited with error code : " + exitCode);
		} catch (IOException | InterruptedException e) {
			System.err.println(e);
		}

		// storageService.deleteAll();
		// return "redirect:/";
	}

}