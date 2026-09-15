package com.example.priutbot.service;

import com.example.priutbot.content.MessageTemplates;
import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.ClientRole;
import com.example.priutbot.entity.ProbationStatus;
import com.example.priutbot.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Volunteer-only actions: registering new owners and deciding probation outcomes (Этап 3). */
@Service
public class VolunteerService {

    public static final int DEFAULT_PROBATION_DAYS = 30;
    private static final List<ProbationStatus> ACTIVE_STATUSES = List.of(ProbationStatus.IN_PROGRESS, ProbationStatus.EXTENDED);

    public record DecisionResult(Long targetChatId, String message) {
    }

    private final ClientRepository clientRepository;

    public VolunteerService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /** The person must have messaged the bot at least once (so we know their chat ID) before a volunteer can register them. */
    @Transactional
    public Optional<Client> registerNewOwner(Long chatId) {
        return clientRepository.findByChatId(chatId).map(client -> {
            client.setRole(ClientRole.NEW_OWNER);
            client.setProbationStartDate(LocalDate.now());
            client.setProbationEndDate(LocalDate.now().plusDays(DEFAULT_PROBATION_DAYS));
            client.setProbationStatus(ProbationStatus.IN_PROGRESS);
            return clientRepository.save(client);
        });
    }

    @Transactional
    public Optional<DecisionResult> decide(Long clientId, String decisionCode) {
        Optional<Client> maybeClient = clientRepository.findById(clientId)
                .filter(candidate -> candidate.getRole() == ClientRole.NEW_OWNER);
        if (maybeClient.isEmpty()) {
            return Optional.empty();
        }
        Client client = maybeClient.get();
        String message;
        switch (decisionCode.toLowerCase()) {
            case "pass" -> {
                client.setProbationStatus(ProbationStatus.PASSED);
                message = MessageTemplates.PROBATION_PASSED;
            }
            case "fail" -> {
                client.setProbationStatus(ProbationStatus.FAILED);
                message = MessageTemplates.PROBATION_FAILED;
            }
            case "extend14" -> {
                client.setProbationStatus(ProbationStatus.EXTENDED);
                client.setProbationEndDate(client.getProbationEndDate().plusDays(14));
                message = MessageTemplates.probationExtended(14);
            }
            case "extend30" -> {
                client.setProbationStatus(ProbationStatus.EXTENDED);
                client.setProbationEndDate(client.getProbationEndDate().plusDays(30));
                message = MessageTemplates.probationExtended(30);
            }
            default -> {
                return Optional.empty();
            }
        }
        clientRepository.save(client);
        return Optional.of(new DecisionResult(client.getChatId(), message));
    }

    public List<Client> activeNewOwners() {
        return clientRepository.findByRoleAndProbationStatusIn(ClientRole.NEW_OWNER, ACTIVE_STATUSES);
    }

    public List<Client> ownersWithProbationEndedToday() {
        return clientRepository.findByRoleAndProbationStatusInAndProbationEndDateLessThanEqual(
                ClientRole.NEW_OWNER, ACTIVE_STATUSES, LocalDate.now());
    }
}
