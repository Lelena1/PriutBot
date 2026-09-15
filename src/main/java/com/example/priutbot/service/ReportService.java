package com.example.priutbot.service;

import com.example.priutbot.entity.BotStage;
import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.PetReport;
import com.example.priutbot.repository.ClientRepository;
import com.example.priutbot.repository.PetReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ReportService {

    public enum Outcome {COMPLETED, WAITING_FOR_TEXT, WAITING_FOR_PHOTO, ALREADY_SUBMITTED, TEXT_TOO_LONG}

    private final PetReportRepository petReportRepository;
    private final ClientRepository clientRepository;

    public ReportService(PetReportRepository petReportRepository, ClientRepository clientRepository) {
        this.petReportRepository = petReportRepository;
        this.clientRepository = clientRepository;
    }

    public boolean hasSubmittedToday(Client client) {
        return petReportRepository.findByClientAndReportDate(client, LocalDate.now())
                .filter(r -> r.getPhotoFileId() != null && r.getDetails() != null)
                .isPresent();
    }

    private PetReport getOrCreateTodayDraft(Client client) {
        return petReportRepository.findByClientAndReportDate(client, LocalDate.now())
                .orElseGet(() -> {
                    PetReport report = new PetReport();
                    report.setClient(client);
                    report.setReportDate(LocalDate.now());
                    return report;
                });
    }

    private boolean isComplete(PetReport report) {
        return report.getPhotoFileId() != null && report.getDetails() != null;
    }

    @Transactional
    public Outcome receivePhoto(Client client, String fileId, String captionOrNull) {
        PetReport draft = getOrCreateTodayDraft(client);
        if (isComplete(draft)) {
            return Outcome.ALREADY_SUBMITTED;
        }
        if (captionOrNull != null && !captionOrNull.isBlank()) {
            String details = detailsWith(draft, captionOrNull);
            if (isTooLong(details)) {
                return Outcome.TEXT_TOO_LONG;
            }
            draft.setDetails(details);
        }
        draft.setPhotoFileId(fileId);
        petReportRepository.save(draft);
        return finishOrWait(client, draft, Outcome.WAITING_FOR_TEXT);
    }

    @Transactional
    public Outcome receiveText(Client client, String text) {
        PetReport draft = getOrCreateTodayDraft(client);
        if (isComplete(draft)) {
            return Outcome.ALREADY_SUBMITTED;
        }
        String details = detailsWith(draft, text);
        if (isTooLong(details)) {
            return Outcome.TEXT_TOO_LONG;
        }
        draft.setDetails(details);
        petReportRepository.save(draft);
        return finishOrWait(client, draft, Outcome.WAITING_FOR_PHOTO);
    }

    private String detailsWith(PetReport draft, String text) {
        return draft.getDetails() == null ? text : draft.getDetails() + "\n" + text;
    }

    private boolean isTooLong(String details) {
        return details.length() > PetReport.DETAILS_MAX_LENGTH;
    }

    private Outcome finishOrWait(Client client, PetReport draft, Outcome waitingOutcome) {
        if (isComplete(draft)) {
            client.setLastReportDate(LocalDate.now());
            client.setStage(BotStage.MAIN_MENU);
            clientRepository.save(client);
            return Outcome.COMPLETED;
        }
        return waitingOutcome;
    }

    public Optional<PetReport> latestReport(Client client) {
        return petReportRepository.findByClientOrderByReportDateDesc(client).stream().findFirst();
    }

    /** Волонтёр отмечает последний отчёт как заполненный плохо (для стандартного предупреждения). */
    @Transactional
    public boolean markLatestReportPoor(Client client) {
        Optional<PetReport> latest = latestReport(client);
        latest.ifPresent(report -> {
            report.setFlaggedPoor(true);
            petReportRepository.save(report);
        });
        return latest.isPresent();
    }
}
