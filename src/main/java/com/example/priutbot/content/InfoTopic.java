package com.example.priutbot.content;

import java.util.function.Function;

/** Buttons shown at Этап 1 (info consultation for a curious visitor). */
public enum InfoTopic {
    ABOUT("Рассказать о приюте", ShelterContent::aboutText),
    SCHEDULE("Режим работы и адрес", ShelterContent::scheduleAndAddressText),
    SECURITY("Пропуск на машину (охрана)", ShelterContent::securityContactText),
    SAFETY("Техника безопасности", ShelterContent::safetyRulesText);

    private final String buttonLabel;
    private final Function<ShelterContent, String> textExtractor;

    InfoTopic(String buttonLabel, Function<ShelterContent, String> textExtractor) {
        this.buttonLabel = buttonLabel;
        this.textExtractor = textExtractor;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }

    public String resolveText(ShelterContent content) {
        return textExtractor.apply(content);
    }
}
