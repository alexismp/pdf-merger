package org.alexismp.pdfmerger;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileOrderController {

    private final List filesToMerge;

	@Autowired
	public FileOrderController() {
        this.filesToMerge = Collections.synchronizedList(new ArrayList());
	}

	@PostMapping("/order")
	public String handleFileOrder( @RequestBody String orderedFiles ) {
        System.out.println(orderedFiles);
		return "redirect:/";
	}

}