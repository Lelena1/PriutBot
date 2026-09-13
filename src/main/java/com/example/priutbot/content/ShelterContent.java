package com.example.priutbot.content;

/**
 * Static informational texts for one shelter (Этап 1 и Этап 2). The strings below are
 * placeholders in square brackets — replace them with the real shelter's details before
 * using the bot for real. Cynologist-related fields are only relevant for dog shelters.
 */
public record ShelterContent(
        String aboutText,
        String scheduleAndAddressText,
        String securityContactText,
        String safetyRulesText,
        String meetingRulesText,
        String documentsListText,
        String transportRecommendationsText,
        String puppyOrKittenSetupText,
        String adultAnimalSetupText,
        String disabledAnimalSetupText,
        String cynologistAdviceText,
        String recommendedCynologistsText,
        String refusalReasonsText
) {
}
