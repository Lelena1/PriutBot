package com.example.priutbot.entity;

/**
 * Where in the conversation a given chat currently is. Persisted on {@link Client}
 * so the bot can resume correctly after a restart, since Telegram updates carry no
 * session state of their own.
 */
public enum BotStage {
    /** Этап 0: выбор приюта (кошки/собаки). Every fresh conversation starts here. */
    SHELTER_SELECTION,
    /** Main menu after a shelter is chosen: info / adoption / report / call volunteer. */
    MAIN_MENU,
    /** Этап 1: waiting for free-text contact details after a "consult me" request. */
    AWAITING_CONTACT_INFO,
    /** Этап 2: waiting for free-text contact details from a potential adopter. */
    AWAITING_CONTACT_ADOPTION,
    /**
     * Этап 3: actively collecting today's report. Photo and text can arrive in either
     * order (each message is saved into today's draft); the report is complete once
     * the draft has both a photo and a text description.
     */
    AWAITING_REPORT
}
