package com.example.priutbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One Telegram chat/user. Tracks which shelter they're talking about, where they are
 * in the conversation (see {@link BotStage}), and — once a volunteer registers them
 * as a new owner — their probation period for the adopted animal (Этап 3).
 */
@Entity
@Table(name = "client", uniqueConstraints = @UniqueConstraint(columnNames = "chat_id"))
public class Client {

    public static final int CONTACT_INFO_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Enumerated(EnumType.STRING)
    private ShelterType shelterType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientRole role = ClientRole.GUEST;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotStage stage = BotStage.SHELTER_SELECTION;

    @Column(name = "contact_info", length = CONTACT_INFO_MAX_LENGTH)
    private String contactInfo;

    @Column(name = "probation_start_date")
    private LocalDate probationStartDate;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "probation_status")
    private ProbationStatus probationStatus;

    @Column(name = "last_report_date")
    private LocalDate lastReportDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getTelegramUsername() {
        return telegramUsername;
    }

    public void setTelegramUsername(String telegramUsername) {
        this.telegramUsername = telegramUsername;
    }

    public ShelterType getShelterType() {
        return shelterType;
    }

    public void setShelterType(ShelterType shelterType) {
        this.shelterType = shelterType;
    }

    public ClientRole getRole() {
        return role;
    }

    public void setRole(ClientRole role) {
        this.role = role;
    }

    public BotStage getStage() {
        return stage;
    }

    public void setStage(BotStage stage) {
        this.stage = stage;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public LocalDate getProbationStartDate() {
        return probationStartDate;
    }

    public void setProbationStartDate(LocalDate probationStartDate) {
        this.probationStartDate = probationStartDate;
    }

    public LocalDate getProbationEndDate() {
        return probationEndDate;
    }

    public void setProbationEndDate(LocalDate probationEndDate) {
        this.probationEndDate = probationEndDate;
    }

    public ProbationStatus getProbationStatus() {
        return probationStatus;
    }

    public void setProbationStatus(ProbationStatus probationStatus) {
        this.probationStatus = probationStatus;
    }

    public LocalDate getLastReportDate() {
        return lastReportDate;
    }

    public void setLastReportDate(LocalDate lastReportDate) {
        this.lastReportDate = lastReportDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
