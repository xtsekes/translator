package dev.ctsekes.translator.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextSegment {
    private String text;
    private float x;
    private float y;
    private float fontSize;
    private String fontName;
    private boolean isNewLine;

    public TextSegment(String text, float x, float y, float fontSize, boolean isNewLine) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.fontSize = fontSize;
        this.isNewLine = isNewLine;
    }
}
