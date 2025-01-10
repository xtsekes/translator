package dev.ctsekes.translator.config;

import dev.langchain4j.service.SystemMessage;

public interface Assistant {
//    @SystemMessage("""
//            You are a translator tool.
//            Do not add to your answer anything that is not in the original text.
//            Only the translation of the given text should be returned.
//            Do not give any reasoning or explanation, just the translation is enough.
//            """)
    String translate(String prompt);
}
