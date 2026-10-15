package com.example.priutbot.service;

import com.example.priutbot.content.MessageTemplates;
import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.ClientRole;
import com.example.priutbot.entity.ProbationStatus;
import com.example.priutbot.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private VolunteerService volunteerService;

    @Test
    void registerNewOwnerStartsProbation() {
        Client client = new Client();
        client.setChatId(42L);
        when(clientRepository.findByChatId(42L)).thenReturn(Optional.of(client));
        when(clientRepository.save(client)).thenReturn(client);

        Optional<Client> result = volunteerService.registerNewOwner(42L);

        assertThat(result).contains(client);
        assertThat(client.getRole()).isEqualTo(ClientRole.NEW_OWNER);
        assertThat(client.getProbationStatus()).isEqualTo(ProbationStatus.IN_PROGRESS);
        assertThat(client.getProbationStartDate()).isEqualTo(LocalDate.now());
        assertThat(client.getProbationEndDate())
                .isEqualTo(LocalDate.now().plusDays(VolunteerService.DEFAULT_PROBATION_DAYS));
    }

    @Test
    void registerUnknownChatDoesNothing() {
        when(clientRepository.findByChatId(42L)).thenReturn(Optional.empty());

        assertThat(volunteerService.registerNewOwner(42L)).isEmpty();
        verify(clientRepository, never()).save(any());
    }

    @Test
    void passDecisionNotifiesOwner() {
        Client client = ownerOnProbation();
        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));

        Optional<VolunteerService.DecisionResult> result = volunteerService.decide(7L, "PASS");

        assertThat(result).contains(new VolunteerService.DecisionResult(42L, MessageTemplates.PROBATION_PASSED));
        assertThat(client.getProbationStatus()).isEqualTo(ProbationStatus.PASSED);
        verify(clientRepository).save(client);
    }

    @Test
    void failDecisionNotifiesOwner() {
        Client client = ownerOnProbation();
        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));

        Optional<VolunteerService.DecisionResult> result = volunteerService.decide(7L, "fail");

        assertThat(result).map(VolunteerService.DecisionResult::message).contains(MessageTemplates.PROBATION_FAILED);
        assertThat(client.getProbationStatus()).isEqualTo(ProbationStatus.FAILED);
    }

    @ParameterizedTest
    @CsvSource({"extend14, 14", "extend30, 30"})
    void extendDecisionMovesEndDate(String code, int days) {
        Client client = ownerOnProbation();
        LocalDate originalEnd = client.getProbationEndDate();
        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));

        Optional<VolunteerService.DecisionResult> result = volunteerService.decide(7L, code);

        assertThat(client.getProbationStatus()).isEqualTo(ProbationStatus.EXTENDED);
        assertThat(client.getProbationEndDate()).isEqualTo(originalEnd.plusDays(days));
        assertThat(result).map(VolunteerService.DecisionResult::message)
                .contains(MessageTemplates.probationExtended(days));
    }

    @Test
    void unknownDecisionCodeChangesNothing() {
        Client client = ownerOnProbation();
        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));

        assertThat(volunteerService.decide(7L, "maybe")).isEmpty();
        assertThat(client.getProbationStatus()).isEqualTo(ProbationStatus.IN_PROGRESS);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void decisionForUnknownClientIsEmpty() {
        when(clientRepository.findById(7L)).thenReturn(Optional.empty());

        assertThat(volunteerService.decide(7L, "pass")).isEmpty();
    }

    private Client ownerOnProbation() {
        Client client = new Client();
        client.setId(7L);
        client.setChatId(42L);
        client.setRole(ClientRole.NEW_OWNER);
        client.setProbationStatus(ProbationStatus.IN_PROGRESS);
        client.setProbationStartDate(LocalDate.now().minusDays(30));
        client.setProbationEndDate(LocalDate.now());
        return client;
    }
}
