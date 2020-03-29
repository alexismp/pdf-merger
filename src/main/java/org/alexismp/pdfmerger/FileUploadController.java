package org.alexismp.pdfmerger;

// import java.util.List;
// import java.util.ArrayList;
// import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileUploadController {

	private final StorageService storageService;

	@Autowired
	public FileUploadController(StorageService storageService) {
		this.storageService = storageService;
	}

	@PostMapping("/merger")
	public String handleFileUpload( @RequestParam("file") MultipartFile file,
			                        RedirectAttributes redirectAttributes ) {
		storageService.store(file);
		redirectAttributes.addFlashAttribute("message",
				"You successfully uploaded " + file.getOriginalFilename() + "!");

        // TODO: do the merge + delete temporary files
        // gs -sDEVICE=pdfwrite -dCompatibilityLevel=1.4 -dPDFSETTINGS=/default -dNOPAUSE -dQUIET -dBATCH -dDetectDuplicateImages -dCompressFonts=true -r150 -sOutputFile=output.pdf input.pdf

        // TODO: how to preserve order?

//        storageService.deleteAll();
		return "redirect:/";
	}

}