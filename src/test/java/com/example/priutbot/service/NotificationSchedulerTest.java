package com.example.priutbot.service;

import com.example.priutbot.bot.PriutTelegramBot;
import com.example.priutbot.config.BotProperties;
import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.ShelterType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock
    private VolunteerService volunteerService;

    @Mock
    private ClientService clientService;

    @Mock
    private BotProperties botProperties;

    @Mock
    private PriutTelegramBot bot;

    @InjectMocks
    private NotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(botProperties.getVolunteerChatIds()).thenReturn(List.of(901L, 902L));
        lenient().when(clientService.label(any())).thenReturn("@owner");
    }

    @Test
    void overdueReportAlertsEveryVolunteer() {
        when(volunteerService.activeNewOwners()).thenReturn(List.of(owner(LocalDate.now().minusDays(3))));

        scheduler.runDailyChecks();

        verify(bot).sendText(eq(901L), contains("не присылал отчёт о питомце 3 дн."), isNull());
        verify(bot).sendText(eq(902L), contains("@owner"), isNull());
    }

    @Test
    void recentReportDoesNotAlert() {
        when(volunteerService.activeNewOwners()).thenReturn(List.of(owner(LocalDate.now().minusDays(2))));

        scheduler.runDailyChecks();

        verify(bot, never()).sendText(anyLong(), anyString(), any());
    }

    @Test
    void ownerWithoutReportsIsCheckedFromProbationStart() {
        Client client = owner(null);
        client.setProbationStartDate(LocalDate.now().minusDays(5));
        when(volunteerService.activeNewOwners()).thenReturn(List.of(client));

        scheduler.runDailyChecks();

        verify(bot).sendText(eq(901L), contains("5 дн."), isNull());
    }

    @Test
    void endedProbationAsksVolunteersForDecision() {
        Client client = owner(LocalDate.now());
        client.setId(17L);
        when(volunteerService.ownersWithProbationEndedToday()).thenReturn(List.of(client));

        scheduler.runDailyChecks();

        verify(bot).sendText(eq(901L), contains("(clientId=17)"), isNull());
        verify(bot).sendText(eq(902L), contains("закончился испытательный срок"), isNull());
    }

    private Client owner(LocalDate lastReportDate) {
        Client client = new Client();
        client.setChatId(42L);
        client.setShelterType(ShelterType.CAT);
        client.setLastReportDate(lastReportDate);
        return client;
    }
}
