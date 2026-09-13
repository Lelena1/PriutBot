package com.example.priutbot.service;

import com.example.priutbot.bot.PriutTelegramBot;
import com.example.priutbot.config.BotProperties;
import com.example.priutbot.content.MessageTemplates;
import com.example.priutbot.entity.Client;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Daily checks that a human still has to act on (Этап 3): owners who haven't sent a
 * report in more than {@value #OVERDUE_THRESHOLD_DAYS} days, and owners whose
 * probation period has ended and now need a pass/extend/fail decision from a volunteer.
 * Nothing here decides automatically — it only alerts the volunteers.
 */
@Component
public class NotificationScheduler {

    private static final long OVERDUE_THRESHOLD_DAYS = 2;

    private final VolunteerService volunteerService;
    private final ClientService clientService;
    private final BotProperties botProperties;
    private final PriutTelegramBot bot;

    public NotificationScheduler(VolunteerService volunteerService, ClientService clientService,
                                  BotProperties botProperties, PriutTelegramBot bot) {
        this.volunteerService = volunteerService;
        this.clientService = clientService;
        this.botProperties = botProperties;
        this.bot = bot;
    }

    @Scheduled(cron = "${bot.scheduler.daily-check-cron}")
    public void runDailyChecks() {
        checkOverdueReports();
        checkProbationEnded();
    }

    private void checkOverdueReports() {
        LocalDate today = LocalDate.now();
        for (Client client : volunteerService.activeNewOwners()) {
            LocalDate baseline = client.getLastReportDate() != null
                    ? client.getLastReportDate() : client.getProbationStartDate();
            if (baseline == null) {
                continue;
            }
            long daysSince = ChronoUnit.DAYS.between(baseline, today);
            if (daysSince > OVERDUE_THRESHOLD_DAYS) {
                String shelterName = client.getShelterType() != null ? client.getShelterType().getDisplayName() : "?";
                notifyVolunteers(MessageTemplates.overdueReportAlertForVolunteers(
                        clientService.label(client), shelterName, daysSince));
            }
        }
    }

    private void checkProbationEnded() {
        for (Client client : volunteerService.ownersWithProbationEndedToday()) {
            String shelterName = client.getShelterType() != null ? client.getShelterType().getDisplayName() : "?";
            notifyVolunteers(MessageTemplates.probationEndedAlertForVolunteers(clientService.label(client), shelterName)
                    + " (clientId=" + client.getId() + ")");
        }
    }

    private void notifyVolunteers(String message) {
        for (Long volunteerChatId : botProperties.getVolunteerChatIds()) {
            bot.sendText(volunteerChatId, message, null);
        }
    }
}
