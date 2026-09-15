package com.example.priutbot.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class BotProperties {

    @Value("${bot.token}")
    private String token;

    @Value("${bot.username}")
    private String username;

    @Value("${bot.volunteer-chat-ids}")
    private String rawVolunteerChatIds;

    @PostConstruct
    public void rejectMalformedVolunteerChatIds() {
        try {
            getVolunteerChatIds();
        } catch (NumberFormatException malformedChatId) {
            throw new IllegalStateException(
                    "bot.volunteer-chat-ids must be a comma-separated list of Telegram chat ids: "
                            + malformedChatId.getMessage(), malformedChatId);
        }
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    /** Telegram chat IDs allowed to run volunteer-only commands and who receive alerts. */
    public List<Long> getVolunteerChatIds() {
        if (rawVolunteerChatIds == null || rawVolunteerChatIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawVolunteerChatIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
    }

    public boolean isVolunteer(Long chatId) {
        return getVolunteerChatIds().contains(chatId);
    }
}
