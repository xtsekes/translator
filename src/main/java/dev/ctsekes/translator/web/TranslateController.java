package dev.ctsekes.translator.web;

import dev.ctsekes.translator.service.TranslateService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/translate")
public class TranslateController {

    private final TranslateService translateService;

    public TranslateController(TranslateService translateService) {
        this.translateService = translateService;
    }

    @PostMapping
    public ResponseEntity<byte[]> translate(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sourceLanguage") String sourceLanguage,
            @RequestParam("targetLanguage") String targetLanguage
    ) {

        try {
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();

            if (contentType == null || fileName == null) {
                return ResponseEntity.badRequest().body("Invalid file.".getBytes());
            }

            byte[] translatedContent;
            // get the extension if it exists
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);

            if (contentType.equals("application/pdf") || fileName.endsWith(".pdf")) {
                // Handle PDF translation
                translatedContent = translateService.translatePdf(file, sourceLanguage, targetLanguage);
                extension = ".pdf";
            } else if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || fileName.endsWith(".docx")) {
                // Handle DOCX translation
                translatedContent = translateService.translateDocx(file, sourceLanguage, targetLanguage);
                extension = ".docx";
            } else if (extension.equals("txt")) {
                // Handle TXT translation
                translatedContent = translateService.translateTxt(file, sourceLanguage, targetLanguage);
            }
            else {
                return ResponseEntity.badRequest().body("Unsupported file type.".getBytes());
            }

            // Return the translated file
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header("Content-Disposition", "attachment; filename=\"translated-" + fileName + "." + extension + "\"")
                    .body(translatedContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to translate the file.".getBytes());
        }

    }
}