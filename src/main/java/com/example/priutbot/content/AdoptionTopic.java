package com.example.priutbot.content;

import java.util.function.Function;

/**
 * Buttons shown at Этап 2 (consultation for a potential new owner). The two
 * cynologist-related topics resolve to {@code null} for cat shelters and are
 * hidden from the keyboard in that case — see {@code Keyboards.adoptionMenu}.
 */
public enum AdoptionTopic {
    MEETING_RULES("Правила знакомства с животным", ShelterContent::meetingRulesText),
    DOCUMENTS("Список документов", ShelterContent::documentsListText),
    TRANSPORT("Как перевезти животное домой", ShelterContent::transportRecommendationsText),
    SETUP_YOUNG("Обустройство дома: щенок/котёнок", ShelterContent::puppyOrKittenSetupText),
    SETUP_ADULT("Обустройство дома: взрослое животное", ShelterContent::adultAnimalSetupText),
    SETUP_DISABLED("Обустройство дома: особые потребности", ShelterContent::disabledAnimalSetupText),
    CYNOLOGIST_ADVICE("Советы кинолога о знакомстве", ShelterContent::cynologistAdviceText),
    CYNOLOGIST_LIST("Проверенные кинологи", ShelterContent::recommendedCynologistsText),
    REFUSAL_REASONS("Почему могут отказать", ShelterContent::refusalReasonsText);

    private final String buttonLabel;
    private final Function<ShelterContent, String> textExtractor;

    AdoptionTopic(String buttonLabel, Function<ShelterContent, String> textExtractor) {
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
