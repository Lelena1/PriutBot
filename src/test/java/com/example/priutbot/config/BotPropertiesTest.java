package com.example.priutbot.config;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

class BotPropertiesTest {

    record Case(String id, String rawVolunteerChatIds, List<Long> expectedChatIds, boolean expectStartupFailure) {
    }

    static final List<Case> CASES = List.of(
            new Case("two ids", "900001,-100500", List.of(900001L, -100500L), false),
            new Case("spaces around ids", " 1 , 2 ", List.of(1L, 2L), false),
            new Case("empty list", "", List.of(), false),
            new Case("empty entry between commas", "1,,2", List.of(1L, 2L), false),
            new Case("malformed id", "1,abc", null, true),
            new Case("username instead of id", "@volunteer", null, true));

    @Test
    void volunteerChatIdsAreValidatedAtStartup() {
        SoftAssertions softly = new SoftAssertions();
        for (Case each : CASES) {
            BotProperties properties = new BotProperties();
            ReflectionTestUtils.setField(properties, "rawVolunteerChatIds", each.rawVolunteerChatIds());
            if (each.expectStartupFailure()) {
                softly.assertThatThrownBy(properties::rejectMalformedVolunteerChatIds)
                        .as(each.id())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("bot.volunteer-chat-ids");
            } else {
                softly.assertThatCode(properties::rejectMalformedVolunteerChatIds)
                        .as(each.id())
                        .doesNotThrowAnyException();
                softly.assertThat(properties.getVolunteerChatIds())
                        .as(each.id())
                        .isEqualTo(each.expectedChatIds());
            }
        }
        softly.assertAll();
    }
}
