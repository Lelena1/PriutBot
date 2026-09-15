package com.example.priutbot.service;

import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.ClientRole;
import com.example.priutbot.entity.ProbationStatus;
import com.example.priutbot.repository.ClientRepository;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.example.priutbot.content.MessageTemplates.PROBATION_FAILED;
import static com.example.priutbot.content.MessageTemplates.PROBATION_PASSED;
import static com.example.priutbot.content.MessageTemplates.probationExtended;
import static com.example.priutbot.entity.ClientRole.GUEST;
import static com.example.priutbot.entity.ClientRole.NEW_OWNER;
import static com.example.priutbot.entity.ClientRole.VOLUNTEER;
import static com.example.priutbot.entity.ProbationStatus.EXTENDED;
import static com.example.priutbot.entity.ProbationStatus.FAILED;
import static com.example.priutbot.entity.ProbationStatus.IN_PROGRESS;
import static com.example.priutbot.entity.ProbationStatus.PASSED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

class VolunteerServiceTest {

    static final long CLIENT_ID = 42L;
    static final long CHAT_ID = 700001L;
    static final LocalDate END_DATE = LocalDate.of(2026, 10, 15);

    record Case(String id, ClientRole role, ProbationStatus status, LocalDate probationEndDate, String decisionCode,
                boolean expectDecided, ProbationStatus expectedStatus, LocalDate expectedEndDate,
                String expectedMessage) {
    }

    static final List<Case> CASES = List.of(
            new Case("guest: pass is refused", GUEST, null, null, "pass",
                    false, null, null, null),
            new Case("guest: extend14 is refused instead of throwing", GUEST, null, null, "extend14",
                    false, null, null, null),
            new Case("volunteer's own row: pass is refused", VOLUNTEER, null, null, "pass",
                    false, null, null, null),
            new Case("owner: pass", NEW_OWNER, IN_PROGRESS, END_DATE, "pass",
                    true, PASSED, END_DATE, PROBATION_PASSED),
            new Case("owner: fail", NEW_OWNER, IN_PROGRESS, END_DATE, "fail",
                    true, FAILED, END_DATE, PROBATION_FAILED),
            new Case("owner: extend14", NEW_OWNER, IN_PROGRESS, END_DATE, "extend14",
                    true, EXTENDED, END_DATE.plusDays(14), probationExtended(14)),
            new Case("owner: extend30", NEW_OWNER, IN_PROGRESS, END_DATE, "extend30",
                    true, EXTENDED, END_DATE.plusDays(30), probationExtended(30)),
            new Case("owner: upper-case PASS", NEW_OWNER, IN_PROGRESS, END_DATE, "PASS",
                    true, PASSED, END_DATE, PROBATION_PASSED),
            new Case("owner: unknown decision code", NEW_OWNER, IN_PROGRESS, END_DATE, "maybe",
                    false, IN_PROGRESS, END_DATE, null),
            new Case("unknown client id", null, null, null, "pass",
                    false, null, null, null));

    @Test
    void onlyNewOwnersCanBeDecided() {
        SoftAssertions softly = new SoftAssertions();
        for (Case each : CASES) {
            ClientRepository repository = mock(ClientRepository.class);
            Client client = each.role() == null ? null : clientOf(each);
            when(repository.findById(CLIENT_ID)).thenReturn(Optional.ofNullable(client));
            VolunteerService service = new VolunteerService(repository);

            Optional<VolunteerService.DecisionResult> decision = service.decide(CLIENT_ID, each.decisionCode());

            softly.assertThat(decision.isPresent()).as("%s: decided", each.id()).isEqualTo(each.expectDecided());
            softly.assertThat(wasSaved(repository)).as("%s: saved", each.id()).isEqualTo(each.expectDecided());
            decision.ifPresent(result -> {
                softly.assertThat(result.targetChatId()).as("%s: target chat", each.id()).isEqualTo(CHAT_ID);
                softly.assertThat(result.message()).as("%s: message", each.id()).isEqualTo(each.expectedMessage());
            });
            if (client != null) {
                softly.assertThat(client.getProbationStatus()).as("%s: status", each.id())
                        .isEqualTo(each.expectedStatus());
                softly.assertThat(client.getProbationEndDate()).as("%s: end date", each.id())
                        .isEqualTo(each.expectedEndDate());
            }
        }
        softly.assertAll();
    }

    private static Client clientOf(Case each) {
        Client client = new Client();
        client.setId(CLIENT_ID);
        client.setChatId(CHAT_ID);
        client.setRole(each.role());
        client.setProbationStatus(each.status());
        client.setProbationEndDate(each.probationEndDate());
        return client;
    }

    private static boolean wasSaved(ClientRepository repository) {
        return mockingDetails(repository).getInvocations().stream()
                .anyMatch(invocation -> invocation.getMethod().getName().equals("save"));
    }
}
