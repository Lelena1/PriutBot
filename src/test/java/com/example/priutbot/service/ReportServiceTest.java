package com.example.priutbot.service;

import com.example.priutbot.entity.BotStage;
import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.PetReport;
import com.example.priutbot.repository.ClientRepository;
import com.example.priutbot.repository.PetReportRepository;
import com.example.priutbot.service.ReportService.Outcome;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.priutbot.service.ReportService.Outcome.ALREADY_SUBMITTED;
import static com.example.priutbot.service.ReportService.Outcome.COMPLETED;
import static com.example.priutbot.service.ReportService.Outcome.TEXT_TOO_LONG;
import static com.example.priutbot.service.ReportService.Outcome.WAITING_FOR_PHOTO;
import static com.example.priutbot.service.ReportService.Outcome.WAITING_FOR_TEXT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    static final String PHOTO_FILE_ID = "photo-file-id";
    static final String TEXT_3990 = "а".repeat(3990);
    static final String TEXT_3000 = "б".repeat(3000);

    record Step(String text, String caption, boolean isPhoto) {
        static Step text(String text) {
            return new Step(text, null, false);
        }

        static Step photo(String caption) {
            return new Step(null, caption, true);
        }
    }

    record Case(String id, List<Step> steps, List<Outcome> expectedOutcomes, String expectedDetails,
                String expectedPhotoFileId) {
    }

    static final List<Case> CASES = List.of(
            new Case("two texts before the photo are both kept",
                    List.of(Step.text("Рацион: сухой корм."), Step.text("Поведение: игривая.")),
                    List.of(WAITING_FOR_PHOTO, WAITING_FOR_PHOTO),
                    "Рацион: сухой корм.\nПоведение: игривая.", null),
            new Case("a caption is appended to the stored text and completes the report",
                    List.of(Step.text("Рацион: сухой корм."), Step.photo("вот фото")),
                    List.of(WAITING_FOR_PHOTO, COMPLETED),
                    "Рацион: сухой корм.\nвот фото", PHOTO_FILE_ID),
            new Case("a captioned photo completes the report at once",
                    List.of(Step.photo("Фото и подпись"), Step.text("ещё текст")),
                    List.of(COMPLETED, ALREADY_SUBMITTED),
                    "Фото и подпись", PHOTO_FILE_ID),
            new Case("photo first, then the text",
                    List.of(Step.photo(null), Step.text("Ест хорошо.")),
                    List.of(WAITING_FOR_TEXT, COMPLETED),
                    "Ест хорошо.", PHOTO_FILE_ID),
            new Case("a text over the limit is refused and nothing is stored",
                    List.of(Step.text("т".repeat(4001))),
                    List.of(TEXT_TOO_LONG),
                    null, null),
            new Case("a second text that would overflow is refused and the first stays",
                    List.of(Step.text(TEXT_3000), Step.text("в".repeat(1000))),
                    List.of(WAITING_FOR_PHOTO, TEXT_TOO_LONG),
                    TEXT_3000, null),
            new Case("a caption that would overflow is refused together with its photo",
                    List.of(Step.text(TEXT_3990), Step.photo("б".repeat(20))),
                    List.of(WAITING_FOR_PHOTO, TEXT_TOO_LONG),
                    TEXT_3990, null),
            new Case("exactly the limit is accepted",
                    List.of(Step.text(TEXT_3990), Step.photo("б".repeat(9))),
                    List.of(WAITING_FOR_PHOTO, COMPLETED),
                    TEXT_3990 + "\n" + "б".repeat(9), PHOTO_FILE_ID));

    @Test
    void laterTextsAreAppendedAndOverLongTextsAreRefused() {
        SoftAssertions softly = new SoftAssertions();
        for (Case each : CASES) {
            AtomicReference<PetReport> storedDraft = new AtomicReference<>();
            Client client = new Client();
            client.setStage(BotStage.AWAITING_REPORT);
            ReportService service = new ReportService(repositoryHolding(storedDraft, client), savingClientRepository());

            List<Outcome> outcomes = new ArrayList<>();
            for (Step step : each.steps()) {
                outcomes.add(step.isPhoto()
                        ? service.receivePhoto(client, PHOTO_FILE_ID, step.caption())
                        : service.receiveText(client, step.text()));
            }

            PetReport draft = storedDraft.get();
            softly.assertThat(outcomes).as("%s: outcomes", each.id()).isEqualTo(each.expectedOutcomes());
            softly.assertThat(draft == null ? null : draft.getDetails()).as("%s: details", each.id())
                    .isEqualTo(each.expectedDetails());
            softly.assertThat(draft == null ? null : draft.getPhotoFileId()).as("%s: photo", each.id())
                    .isEqualTo(each.expectedPhotoFileId());
            boolean completed = outcomes.contains(COMPLETED);
            softly.assertThat(client.getStage()).as("%s: stage", each.id())
                    .isEqualTo(completed ? BotStage.MAIN_MENU : BotStage.AWAITING_REPORT);
            softly.assertThat(client.getLastReportDate()).as("%s: last report date", each.id())
                    .isEqualTo(completed ? LocalDate.now() : null);
        }
        softly.assertAll();
    }

    private static PetReportRepository repositoryHolding(AtomicReference<PetReport> storedDraft, Client client) {
        PetReportRepository repository = mock(PetReportRepository.class);
        when(repository.findByClientAndReportDate(eq(client), any()))
                .thenAnswer(invocation -> Optional.ofNullable(storedDraft.get()));
        when(repository.save(any())).thenAnswer(invocation -> {
            storedDraft.set(invocation.getArgument(0));
            return storedDraft.get();
        });
        return repository;
    }

    private static ClientRepository savingClientRepository() {
        ClientRepository repository = mock(ClientRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return repository;
    }
}
