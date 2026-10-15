package com.example.priutbot.repository;

import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.ClientRole;
import com.example.priutbot.entity.PetReport;
import com.example.priutbot.entity.ProbationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against an in-memory H2 database whose schema is created by the Liquibase changelog.
 * Hibernate is in {@code validate} mode, so a mismatch between migrations and entities fails the test.
 */
@DataJpaTest
class RepositoryIntegrationTest {

    private static final List<ProbationStatus> ACTIVE = List.of(ProbationStatus.IN_PROGRESS, ProbationStatus.EXTENDED);

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PetReportRepository petReportRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibaseAppliedAllChangeSets() {
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT id FROM databasechangelog ORDER BY orderexecuted", String.class);

        assertThat(ids).containsExactly("001-create-client", "002-create-pet-report");
    }

    @Test
    void findsClientByChatId() {
        clientRepository.save(client(100L, ClientRole.GUEST, null, null));

        assertThat(clientRepository.findByChatId(100L)).isPresent();
        assertThat(clientRepository.findByChatId(200L)).isEmpty();
    }

    @Test
    void chatIdMustBeUnique() {
        clientRepository.saveAndFlush(client(100L, ClientRole.GUEST, null, null));

        assertThatThrownBy(() -> clientRepository.saveAndFlush(client(100L, ClientRole.GUEST, null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void selectsOwnersWhoseProbationHasEnded() {
        LocalDate today = LocalDate.now();
        Client ended = clientRepository.save(client(1L, ClientRole.NEW_OWNER, ProbationStatus.IN_PROGRESS, today));
        Client extendedAndEnded = clientRepository.save(
                client(2L, ClientRole.NEW_OWNER, ProbationStatus.EXTENDED, today.minusDays(1)));
        clientRepository.save(client(3L, ClientRole.NEW_OWNER, ProbationStatus.IN_PROGRESS, today.plusDays(5)));
        clientRepository.save(client(4L, ClientRole.NEW_OWNER, ProbationStatus.PASSED, today.minusDays(1)));
        clientRepository.save(client(5L, ClientRole.GUEST, null, null));

        List<Client> result = clientRepository.findByRoleAndProbationStatusInAndProbationEndDateLessThanEqual(
                ClientRole.NEW_OWNER, ACTIVE, today);

        assertThat(result).containsExactlyInAnyOrder(ended, extendedAndEnded);
        assertThat(clientRepository.findByRoleAndProbationStatusIn(ClientRole.NEW_OWNER, ACTIVE)).hasSize(3);
    }

    @Test
    void reportsAreFoundByDateAndOrderedNewestFirst() {
        LocalDate today = LocalDate.now();
        Client owner = clientRepository.save(client(1L, ClientRole.NEW_OWNER, ProbationStatus.IN_PROGRESS, today));
        petReportRepository.save(report(owner, today.minusDays(2)));
        PetReport latest = petReportRepository.save(report(owner, today));
        petReportRepository.save(report(owner, today.minusDays(1)));

        assertThat(petReportRepository.findByClientAndReportDate(owner, today)).contains(latest);
        assertThat(petReportRepository.findByClientOrderByReportDateDesc(owner))
                .extracting(PetReport::getReportDate)
                .containsExactly(today, today.minusDays(1), today.minusDays(2));
    }

    private Client client(Long chatId, ClientRole role, ProbationStatus status, LocalDate probationEnd) {
        Client client = new Client();
        client.setChatId(chatId);
        client.setRole(role);
        client.setProbationStatus(status);
        client.setProbationEndDate(probationEnd);
        return client;
    }

    private PetReport report(Client owner, LocalDate date) {
        PetReport report = new PetReport();
        report.setClient(owner);
        report.setReportDate(date);
        report.setPhotoFileId("photo");
        report.setDetails("ok");
        return report;
    }
}
