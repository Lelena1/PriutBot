package com.example.priutbot.service;

import com.example.priutbot.entity.BotStage;
import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.PetReport;
import com.example.priutbot.repository.ClientRepository;
import com.example.priutbot.repository.PetReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private PetReportRepository petReportRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ReportService reportService;

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setChatId(100L);
        client.setStage(BotStage.AWAITING_REPORT);
    }

    @Test
    void photoWithoutCaptionWaitsForText() {
        when(petReportRepository.findByClientAndReportDate(client, LocalDate.now())).thenReturn(Optional.empty());

        ReportService.Outcome outcome = reportService.receivePhoto(client, "photo-1", null);

        assertThat(outcome).isEqualTo(ReportService.Outcome.WAITING_FOR_TEXT);
        ArgumentCaptor<PetReport> saved = ArgumentCaptor.forClass(PetReport.class);
        verify(petReportRepository).save(saved.capture());
        assertThat(saved.getValue().getPhotoFileId()).isEqualTo("photo-1");
        assertThat(saved.getValue().getDetails()).isNull();
        assertThat(saved.getValue().getReportDate()).isEqualTo(LocalDate.now());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void photoWithCaptionCompletesReport() {
        when(petReportRepository.findByClientAndReportDate(client, LocalDate.now())).thenReturn(Optional.empty());

        ReportService.Outcome outcome = reportService.receivePhoto(client, "photo-1", "Ест хорошо, спит много");

        assertThat(outcome).isEqualTo(ReportService.Outcome.COMPLETED);
        assertThat(client.getLastReportDate()).isEqualTo(LocalDate.now());
        assertThat(client.getStage()).isEqualTo(BotStage.MAIN_MENU);
        verify(clientRepository).save(client);
    }

    @Test
    void textBeforePhotoWaitsForPhoto() {
        when(petReportRepository.findByClientAndReportDate(client, LocalDate.now())).thenReturn(Optional.empty());

        ReportService.Outcome outcome = reportService.receiveText(client, "Рацион: сухой корм");

        assertThat(outcome).isEqualTo(ReportService.Outcome.WAITING_FOR_PHOTO);
        assertThat(client.getLastReportDate()).isNull();
    }

    @Test
    void textAfterPhotoCompletesReport() {
        PetReport draft = report("photo-1", null);
        when(petReportRepository.findByClientAndReportDate(client, LocalDate.now())).thenReturn(Optional.of(draft));

        ReportService.Outcome outcome = reportService.receiveText(client, "Всё хорошо");

        assertThat(outcome).isEqualTo(ReportService.Outcome.COMPLETED);
        assertThat(draft.getDetails()).isEqualTo("Всё хорошо");
        verify(clientRepository).save(client);
    }

    @Test
    void secondReportOnTheSameDayIsRejected() {
        PetReport complete = report("photo-1", "Всё хорошо");
        when(petReportRepository.findByClientAndReportDate(client, LocalDate.now())).thenReturn(Optional.of(complete));

        ReportService.Outcome outcome = reportService.receivePhoto(client, "photo-2", "Ещё один");

        assertThat(outcome).isEqualTo(ReportService.Outcome.ALREADY_SUBMITTED);
        assertThat(complete.getPhotoFileId()).isEqualTo("photo-1");
        verify(petReportRepository, never()).save(any());
    }

    @Test
    void markLatestReportPoorFlagsNewestReport() {
        PetReport latest = report("photo-1", "Кратко");
        when(petReportRepository.findByClientOrderByReportDateDesc(client)).thenReturn(List.of(latest));

        assertThat(reportService.markLatestReportPoor(client)).isTrue();
        assertThat(latest.isFlaggedPoor()).isTrue();
        verify(petReportRepository).save(latest);
    }

    @Test
    void markLatestReportPoorWithoutReportsReturnsFalse() {
        when(petReportRepository.findByClientOrderByReportDateDesc(client)).thenReturn(List.of());

        assertThat(reportService.markLatestReportPoor(client)).isFalse();
        verify(petReportRepository, never()).save(any());
    }

    private PetReport report(String photoFileId, String details) {
        PetReport report = new PetReport();
        report.setClient(client);
        report.setReportDate(LocalDate.now());
        report.setPhotoFileId(photoFileId);
        report.setDetails(details);
        return report;
    }
}
