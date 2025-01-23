package dev.ctsekes.translator.util;

import dev.ctsekes.translator.model.TextSegment;
import lombok.Getter;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDFTextLayoutStripper extends PDFTextStripper {
    @Getter
    private List<TextSegment> textSegments = new ArrayList<>();
    private float currentY = 0;

    public PDFTextLayoutStripper() throws IOException {
        super();
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        if (textPositions.isEmpty()) {
            return;
        }

        TextPosition firstPosition = textPositions.get(0);

        float x = firstPosition.getXDirAdj();
        float y = firstPosition.getYDirAdj();

        if (Math.abs(y - currentY) > 5) {
            currentY = y;
        }

        TextSegment segment = new TextSegment(
                text.trim(),
                x,
                y,
                firstPosition.getFontSizeInPt(),
                isNewLine(firstPosition)
        );

        textSegments.add(segment);
    }

    private boolean isNewLine(TextPosition position) {
        boolean isNewLine = Math.abs(position.getYDirAdj() - currentY) > 5;
        currentY = position.getYDirAdj();
        return isNewLine;
    }
}
