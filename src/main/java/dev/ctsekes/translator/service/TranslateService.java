package dev.ctsekes.translator.service;

import dev.ctsekes.translator.config.Assistant;
import dev.ctsekes.translator.model.TextSegment;
import dev.ctsekes.translator.util.PDFTextLayoutStripper;
import io.micrometer.common.util.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TranslateService {

    private final Assistant assistant;

    public TranslateService(Assistant assistant) {
        this.assistant = assistant;
    }

    public byte[] translatePdf(MultipartFile file, String sourceLanguage, String targetLanguage) throws IOException {
        try (PDDocument sourceDocument = PDDocument.load(file.getInputStream());
             PDDocument translatedDocument = new PDDocument()) {
            Map<String, PDFont> fontMapping = initializeFontMapping();
            PDFTextLayoutStripper stripper = initialisePDFTextLayoutStripper();

            for (int pageIndex = 0; pageIndex < sourceDocument.getNumberOfPages(); pageIndex++) {
                processPage(sourceDocument, translatedDocument, stripper, fontMapping, sourceLanguage, targetLanguage, pageIndex);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            translatedDocument.save(outputStream);
            return saveTranslatedDocument(translatedDocument);
        }
    }

    public byte[] translateDocx(MultipartFile file, String sourceLanguage, String targetLanguage) throws IOException {
        XWPFDocument document = new XWPFDocument(file.getInputStream());

        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                XWPFParagraph paragraph = (XWPFParagraph) element;
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
            } else if (element instanceof XWPFTable) {
                XWPFTable table = (XWPFTable) element;
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
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
                    }
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

        String prompt = "Translate the following text from " + sourceLanguage + " to " + targetLanguage + ": '''" + text + "'''";

        return assistant.translate(prompt);
    }

    private Map<String, PDFont> initializeFontMapping() {
        Map<String, PDFont> fontMapping = new HashMap<>();
        fontMapping.put("Helvetica", PDType1Font.HELVETICA);
        fontMapping.put("Helvetica-Bold", PDType1Font.HELVETICA_BOLD);
        fontMapping.put("Times-Roman", PDType1Font.TIMES_ROMAN);
        fontMapping.put("Courier", PDType1Font.COURIER);

        return fontMapping;
    }

    private PDFTextLayoutStripper initialisePDFTextLayoutStripper() throws IOException {
        PDFTextLayoutStripper stripper = new PDFTextLayoutStripper();
        stripper.setSortByPosition(true);

        return stripper;
    }

    private void processPage(PDDocument sourceDocument, PDDocument translatedDocument, PDFTextLayoutStripper stripper,
                             Map<String, PDFont> fontMapping, String sourceLanguage, String targetLanguage, int pageIndex) throws IOException {
        stripper.setStartPage(pageIndex + 1);
        stripper.setEndPage(pageIndex + 1);

        PDPage sourcePage = sourceDocument.getPage(pageIndex);
        PDPage newPage = new PDPage(sourcePage.getMediaBox());
        translatedDocument.addPage(newPage);

        stripper.getText(sourceDocument);
        List<TextSegment> segments = stripper.getTextSegments();

        try (PDPageContentStream contentStream = new PDPageContentStream(translatedDocument, newPage)) {
            renderSegments(contentStream, segments, fontMapping, sourceLanguage, targetLanguage);
        }

        stripper.getTextSegments().clear();
    }

    private void renderSegments(PDPageContentStream contentStream, List<TextSegment> segments, Map<String, PDFont> fontMapping,
                                String sourceLanguage, String targetLanguage) throws IOException {
        float lastY = -1;
        float lastX = 0;
        float currentY = 0;

        for (TextSegment segment : segments) {
            if (segment.getText().trim().isEmpty()) {
                continue;
            }

            String translatedText = translate(segment.getText(), sourceLanguage, targetLanguage);
            List<String> textLines = processTextIntoLines(translatedText);

            for (String line : textLines) {
                renderTextLine(contentStream, line, segment, fontMapping, lastX, lastY);
                lastX = segment.getX() + calculateTextWidth(line, fontMapping.getOrDefault(segment.getFontName(), PDType1Font.HELVETICA), segment.getFontSize());
                lastY = currentY;
                currentY -= segment.getFontSize() * 1.2;
            }
        }
    }

    private void renderTextLine(PDPageContentStream contentStream, String line, TextSegment segment, Map<String, PDFont> fontMapping,
                                float lastX, float lastY) throws IOException {
        contentStream.beginText();
        PDFont font = fontMapping.getOrDefault(segment.getFontName(), PDType1Font.HELVETICA);
        contentStream.setFont(font, segment.getFontSize());

        if (lastY != segment.getY()) {
            contentStream.newLineAtOffset(segment.getX(), segment.getY());
            lastX = segment.getX();
        } else {
            contentStream.newLineAtOffset(segment.getX() - lastX, 0);
        }

        contentStream.showText(line);
        contentStream.endText();
    }

    private float calculateTextWidth(String text, PDFont font, float fontSize) throws IOException {
        return font.getStringWidth(text) * fontSize / 1000f;
    }

    private List<String> processTextIntoLines(String text) {
        List<String> lines = new ArrayList<>();

        String[] rawLines = text.split("\\r?\\n");

        for (String line : rawLines) {
            String cleanedLine = line.replaceAll("[\\t\\f]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (!cleanedLine.isEmpty()) {
                lines.add(cleanedLine);
            }
        }

        return lines;
    }

    private byte[] saveTranslatedDocument(PDDocument translatedDocument) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            translatedDocument.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
