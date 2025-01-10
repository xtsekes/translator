package dev.ctsekes.translator.service;

import dev.ctsekes.translator.config.Assistant;
import io.micrometer.common.util.StringUtils;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class TranslateService {

    private final Assistant assistant;

    public TranslateService(Assistant assistant) {
        this.assistant = assistant;
    }

    public byte[] translatePdf(MultipartFile file, String sourceLanguage, String targetLanguage) throws IOException {
        PDDocument document = PDDocument.load(file.getInputStream());
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String text = pdfStripper.getText(document);

        // Translate the text in parts to handle large documents
        StringBuilder translatedText = new StringBuilder();
        for (int i = 0; i < text.length(); i += 500) {
            String part = text.substring(i, Math.min(i + 500, text.length()));
            translatedText.append(translate(part, sourceLanguage, targetLanguage));
        }

        // Create a new PDF document to hold the translated text
        PDDocument translatedDocument = new PDDocument();
        PDPage page = new PDPage();
        translatedDocument.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(translatedDocument, page)) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText(
                    translatedText.toString().replace("\n", "").replace("\r", "")
            );
            contentStream.endText();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        translatedDocument.save(outputStream);
        translatedDocument.close();
        document.close();

        return outputStream.toByteArray();
    }

    public byte[] translateDocx(MultipartFile file, String sourceLanguage, String targetLanguage) throws IOException {
        XWPFDocument document = new XWPFDocument(file.getInputStream());

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                String text = run.getText(0);
                if (text != null && !text.isEmpty()) {
                    StringBuilder translatedText = new StringBuilder();
                    for (int i = 0; i < text.length(); i += 500) {
                        String part = text.substring(i, Math.min(i + 500, text.length()));
                        translatedText.append(translate(part, sourceLanguage, targetLanguage));
                    }
                    run.setText(translatedText.toString(), 0);
                }
            }
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.write(outputStream);

            return outputStream.toByteArray();
        } finally {
            document.close();
        }
    }

    public byte[] translateTxt(MultipartFile file, String sourceLanguage, String targetLanguage) throws IOException {
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);

        // Translate the text in parts to handle large documents
        StringBuilder translatedText = new StringBuilder();
        for (int i = 0; i < text.length(); i += 500) {
            String part = text.substring(i, Math.min(i + 500, text.length()));
            translatedText.append(translate(part, sourceLanguage, targetLanguage));
        }

        return translatedText.toString().getBytes(StandardCharsets.UTF_8);
    }


    private String translate(String text, String sourceLanguage, String targetLanguage) {
        if (StringUtils.isEmpty(text)) {
            return "";
        }
        String prompt = "Give me the translation from " + sourceLanguage + " to " + targetLanguage +
                " for the given text and nothing else: '''" + text +"''' do not add anything else to the answer and do not provide explanations.";

        return assistant.translate(prompt);
    }
}
