package com.example.priutbot.bot;

import com.example.priutbot.config.BotProperties;
import com.example.priutbot.content.AdoptionTopic;
import com.example.priutbot.content.ContentProvider;
import com.example.priutbot.content.InfoTopic;
import com.example.priutbot.content.MessageTemplates;
import com.example.priutbot.content.ShelterContent;
import com.example.priutbot.entity.BotStage;
import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.ClientRole;
import com.example.priutbot.entity.PetReport;
import com.example.priutbot.entity.ShelterType;
import com.example.priutbot.service.ClientService;
import com.example.priutbot.service.ReportService;
import com.example.priutbot.service.VolunteerService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Optional;

/** Routes every incoming Telegram update to the right stage handler based on {@link Client#getStage()}. */
@Component
public class PriutTelegramBot extends TelegramLongPollingBot {

    private final BotProperties botProperties;
    private final ClientService clientService;
    private final ContentProvider contentProvider;
    private final Keyboards keyboards;
    private final ReportService reportService;
    private final VolunteerService volunteerService;

    public PriutTelegramBot(BotProperties botProperties,
                             ClientService clientService,
                             ContentProvider contentProvider,
                             Keyboards keyboards,
                             ReportService reportService,
                             VolunteerService volunteerService) {
        super(botProperties.getToken());
        this.botProperties = botProperties;
        this.clientService = clientService;
        this.contentProvider = contentProvider;
        this.keyboards = keyboards;
        this.reportService = reportService;
        this.volunteerService = volunteerService;
    }

    @Override
    public String getBotUsername() {
        return botProperties.getUsername();
    }

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update);
            } else if (update.hasMessage()) {
                handleMessage(update.getMessage());
            }
        } catch (Exception e) {
            // A single bad update must not bring the whole bot down.
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------- messages

    private void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String username = message.getFrom() != null ? message.getFrom().getUserName() : null;
        Client client = clientService.getOrCreate(chatId, username);
        clientService.markAsVolunteerIfNeeded(client, botProperties.isVolunteer(chatId));

        if (message.hasText() && message.getText().startsWith("/")) {
            handleCommand(client, message.getText().trim());
            return;
        }

        switch (client.getStage()) {
            case AWAITING_CONTACT_INFO, AWAITING_CONTACT_ADOPTION -> handleContactInput(client, message);
            case AWAITING_REPORT -> handleReportInput(client, message);
            default -> sendCurrentMenu(client, "Пожалуйста, воспользуйтесь кнопками ниже.");
        }
    }

    private void handleContactInput(Client client, Message message) {
        if (!message.hasText()) {
            sendText(client.getChatId(), "Пожалуйста, пришлите контактные данные текстом.", null);
            return;
        }
        String contactInfo = message.getText().trim();
        if (contactInfo.length() > Client.CONTACT_INFO_MAX_LENGTH) {
            sendText(client.getChatId(), MessageTemplates.contactInfoTooLong(Client.CONTACT_INFO_MAX_LENGTH), null);
            return;
        }
        clientService.saveContactInfo(client, contactInfo);
        notifyVolunteers(MessageTemplates.contactDetailsAlertForVolunteers(
                clientService.label(client), shelterName(client), contactInfo));
        sendText(client.getChatId(), "Спасибо! Ваши контакты сохранены, волонтёр свяжется с вами.",
                keyboards.mainMenu());
    }

    private void notifyVolunteers(String alert) {
        for (Long volunteerChatId : botProperties.getVolunteerChatIds()) {
            sendText(volunteerChatId, alert, null);
        }
    }

    private String shelterName(Client client) {
        return client.getShelterType() != null ? client.getShelterType().getDisplayName() : "приют не выбран";
    }

    private void handleReportInput(Client client, Message message) {
        ReportService.Outcome outcome;
        if (message.hasPhoto()) {
            String fileId = largestPhotoFileId(message.getPhoto());
            outcome = reportService.receivePhoto(client, fileId, message.getCaption());
        } else if (message.hasText()) {
            outcome = reportService.receiveText(client, message.getText().trim());
        } else {
            sendText(client.getChatId(), "Пришлите, пожалуйста, фото питомца или текстовое описание.", null);
            return;
        }

        switch (outcome) {
            case COMPLETED -> sendText(client.getChatId(),
                    "Спасибо, отчёт за сегодня принят! Так держать 🐾", keyboards.mainMenu());
            case WAITING_FOR_TEXT -> sendText(client.getChatId(),
                    "Фото получено! Теперь опишите одним сообщением рацион, самочувствие "
                            + "и изменения в поведении питомца.", null);
            case WAITING_FOR_PHOTO -> sendText(client.getChatId(),
                    "Текст получен! Теперь пришлите, пожалуйста, фото питомца.", null);
            case ALREADY_SUBMITTED -> sendText(client.getChatId(),
                    "Отчёт за сегодня уже принят, спасибо! Возвращайтесь завтра.", keyboards.mainMenu());
            case TEXT_TOO_LONG -> sendText(client.getChatId(),
                    MessageTemplates.reportTextTooLong(PetReport.DETAILS_MAX_LENGTH), null);
        }
    }

    private String largestPhotoFileId(List<PhotoSize> sizes) {
        return sizes.get(sizes.size() - 1).getFileId();
    }

    // ---------------------------------------------------------------- commands

    private void handleCommand(Client client, String text) {
        String[] parts = text.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/start" -> {
                clientService.restart(client);
                sendText(client.getChatId(),
                        "Привет! Я бот приюта для животных 🐾 Помогу узнать о приюте, "
                                + "подскажу, как взять животное, и приму отчёт о питомце. "
                                + "Для начала выберите приют:",
                        keyboards.shelterSelection());
            }
            case "/help" -> sendHelp(client);
            case "/newowner" -> handleNewOwnerCommand(client, parts);
            case "/decide" -> handleDecideCommand(client, parts);
            case "/pending" -> handlePendingCommand(client);
            case "/warn" -> handleWarnCommand(client, parts);
            default -> sendText(client.getChatId(), "Неизвестная команда. /help — список команд.", null);
        }
    }

    private void sendHelp(Client client) {
        StringBuilder sb = new StringBuilder("/start — начать заново (выбрать приют)\n");
        if (botProperties.isVolunteer(client.getChatId())) {
            sb.append("\nКоманды волонтёра:\n")
                    .append("/newowner <chatId> — зарегистрировать нового хозяина (испытательный срок 30 дней)\n")
                    .append("/pending — список активных новых хозяев: статус, дата окончания срока, дата последнего отчёта\n")
                    .append("/decide <clientId> pass|fail|extend14|extend30 — решение по испытательному сроку\n")
                    .append("/warn <clientId> — отправить хозяину стандартное предупреждение о плохом отчёте\n");
        }
        sendText(client.getChatId(), sb.toString(), null);
    }

    private void handleNewOwnerCommand(Client client, String[] parts) {
        if (!botProperties.isVolunteer(client.getChatId())) {
            sendText(client.getChatId(), "Эта команда доступна только волонтёрам.", null);
            return;
        }
        if (parts.length < 2) {
            sendText(client.getChatId(), "Использование: /newowner <chatId>", null);
            return;
        }
        Long targetChatId;
        try {
            targetChatId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            sendText(client.getChatId(), "chatId должен быть числом.", null);
            return;
        }
        Optional<Client> registered = volunteerService.registerNewOwner(targetChatId);
        if (registered.isEmpty()) {
            sendText(client.getChatId(),
                    "Пользователь с таким chatId ещё не писал боту (нет записи). Попросите его сначала "
                            + "отправить боту /start.", null);
            return;
        }
        sendText(client.getChatId(), "Готово! Пользователь зарегистрирован как новый хозяин, "
                + "испытательный срок начат.", null);
        sendText(targetChatId, "Поздравляем с новым питомцем! 🎉 Теперь, пожалуйста, присылайте "
                + "ежедневный отчёт о нём (фото + рацион/самочувствие/поведение) в течение 30 дней — "
                + "воспользуйтесь кнопкой «Прислать отчёт о питомце» в главном меню.", keyboards.mainMenu());
    }

    private void handleDecideCommand(Client client, String[] parts) {
        if (!botProperties.isVolunteer(client.getChatId())) {
            sendText(client.getChatId(), "Эта команда доступна только волонтёрам.", null);
            return;
        }
        if (parts.length < 3) {
            sendText(client.getChatId(), "Использование: /decide <clientId> pass|fail|extend14|extend30", null);
            return;
        }
        Long clientId;
        try {
            clientId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            sendText(client.getChatId(), "clientId должен быть числом.", null);
            return;
        }
        Optional<VolunteerService.DecisionResult> result = volunteerService.decide(clientId, parts[2]);
        if (result.isEmpty()) {
            sendText(client.getChatId(), "Не найден новый хозяин с таким clientId или неверное решение.", null);
            return;
        }
        sendText(client.getChatId(), "Решение отправлено хозяину.", null);
        sendText(result.get().targetChatId(), result.get().message(), keyboards.mainMenu());
    }

    private void handlePendingCommand(Client client) {
        if (!botProperties.isVolunteer(client.getChatId())) {
            sendText(client.getChatId(), "Эта команда доступна только волонтёрам.", null);
            return;
        }
        List<Client> active = volunteerService.activeNewOwners();
        if (active.isEmpty()) {
            sendText(client.getChatId(), "Активных новых хозяев пока нет.", null);
            return;
        }
        StringBuilder sb = new StringBuilder("Активные новые хозяева:\n");
        for (Client c : active) {
            sb.append("id=").append(c.getId())
                    .append(" chatId=").append(c.getChatId())
                    .append(" ").append(clientService.label(c))
                    .append(" (").append(c.getShelterType() != null ? c.getShelterType().getDisplayName() : "?")
                    .append(") статус=").append(c.getProbationStatus())
                    .append(" окончание=").append(c.getProbationEndDate())
                    .append(" последний отчёт=").append(c.getLastReportDate() != null ? c.getLastReportDate() : "нет")
                    .append("\n");
        }
        sendText(client.getChatId(), sb.toString(), null);
    }

    private void handleWarnCommand(Client client, String[] parts) {
        if (!botProperties.isVolunteer(client.getChatId())) {
            sendText(client.getChatId(), "Эта команда доступна только волонтёрам.", null);
            return;
        }
        if (parts.length < 2) {
            sendText(client.getChatId(), "Использование: /warn <clientId>", null);
            return;
        }
        Long targetId;
        try {
            targetId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            sendText(client.getChatId(), "clientId должен быть числом.", null);
            return;
        }
        Optional<Client> target = clientService.findById(targetId)
                .filter(candidate -> candidate.getRole() == ClientRole.NEW_OWNER);
        if (target.isEmpty()) {
            sendText(client.getChatId(), "Не найден хозяин с таким clientId.", null);
            return;
        }
        reportService.markLatestReportPoor(target.get());
        sendText(target.get().getChatId(), MessageTemplates.POOR_REPORT_WARNING, null);
        sendText(client.getChatId(), "Предупреждение отправлено хозяину.", null);
    }

    // ---------------------------------------------------------------- callbacks

    private void handleCallback(Update update) {
        try {
            execute(new AnswerCallbackQuery(update.getCallbackQuery().getId()));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        String username = update.getCallbackQuery().getFrom().getUserName();
        String data = update.getCallbackQuery().getData();

        Client client = clientService.getOrCreate(chatId, username);
        clientService.markAsVolunteerIfNeeded(client, botProperties.isVolunteer(chatId));
        leavePendingInput(client);

        switch (data) {
            case Keyboards.SHELTER_CAT -> chooseShelterAndGreet(client, ShelterType.CAT);
            case Keyboards.SHELTER_DOG -> chooseShelterAndGreet(client, ShelterType.DOG);
            case Keyboards.MENU_INFO -> sendText(chatId, "Что рассказать о приюте?", keyboards.infoMenu());
            case Keyboards.MENU_ADOPTION -> sendText(chatId, "Что вас интересует по поводу усыновления?",
                    keyboards.adoptionMenu(client.getShelterType()));
            case Keyboards.MENU_REPORT -> handleReportMenu(client);
            case Keyboards.MENU_VOLUNTEER -> callVolunteer(client, "нажал(а) «Позвать волонтёра»");
            case Keyboards.BACK_MAIN -> sendText(chatId, "Главное меню:", keyboards.mainMenu());
            case Keyboards.INFO_CONTACT -> {
                clientService.setStage(client, BotStage.AWAITING_CONTACT_INFO);
                sendText(chatId, "Напишите, пожалуйста, ваши контактные данные для связи (телефон и/или имя).", null);
            }
            case Keyboards.ADOPT_CONTACT -> {
                clientService.setStage(client, BotStage.AWAITING_CONTACT_ADOPTION);
                sendText(chatId, "Напишите, пожалуйста, ваши контактные данные для связи (телефон и/или имя).", null);
            }
            default -> {
                if (client.getShelterType() == null) {
                    askToChooseShelter(client);
                } else if (data.startsWith(Keyboards.INFO_PREFIX)) {
                    handleInfoTopic(client, data.substring(Keyboards.INFO_PREFIX.length()));
                } else if (data.startsWith(Keyboards.ADOPT_PREFIX)) {
                    handleAdoptionTopic(client, data.substring(Keyboards.ADOPT_PREFIX.length()));
                }
            }
        }
    }

    private void leavePendingInput(Client client) {
        if (isAwaitingInput(client.getStage())) {
            clientService.setStage(client, BotStage.MAIN_MENU);
        }
    }

    private boolean isAwaitingInput(BotStage stage) {
        return stage == BotStage.AWAITING_CONTACT_INFO
                || stage == BotStage.AWAITING_CONTACT_ADOPTION
                || stage == BotStage.AWAITING_REPORT;
    }

    private void chooseShelterAndGreet(Client client, ShelterType shelterType) {
        clientService.chooseShelter(client, shelterType);
        sendText(client.getChatId(), "Вы выбрали: " + shelterType.getDisplayName() + ". Чем могу помочь?",
                keyboards.mainMenu());
    }

    private void handleReportMenu(Client client) {
        if (client.getRole() != ClientRole.NEW_OWNER) {
            sendText(client.getChatId(),
                    "Похоже, вы ещё не зарегистрированы как новый хозяин животного — это оформляет "
                            + "волонтёр после подписания договора. Если вы уже забрали животное, "
                            + "позовите волонтёра, чтобы вас зарегистрировали.",
                    keyboards.backToMainOnly());
            return;
        }
        if (reportService.hasSubmittedToday(client)) {
            sendText(client.getChatId(), "Вы уже прислали отчёт за сегодня, спасибо! Возвращайтесь завтра.",
                    keyboards.backToMainOnly());
            return;
        }
        clientService.setStage(client, BotStage.AWAITING_REPORT);
        sendText(client.getChatId(),
                "Пришлите, пожалуйста, фото питомца и отдельным сообщением опишите рацион, самочувствие "
                        + "и изменения в поведении (можно в любом порядке).", null);
    }

    private void handleInfoTopic(Client client, String topicName) {
        try {
            InfoTopic topic = InfoTopic.valueOf(topicName);
            ShelterContent content = contentProvider.get(client.getShelterType());
            sendText(client.getChatId(), topic.resolveText(content), keyboards.infoMenu());
        } catch (IllegalArgumentException e) {
            sendText(client.getChatId(), "Не удалось найти эту информацию.", keyboards.infoMenu());
        }
    }

    private void handleAdoptionTopic(Client client, String topicName) {
        try {
            AdoptionTopic topic = AdoptionTopic.valueOf(topicName);
            ShelterContent content = contentProvider.get(client.getShelterType());
            String text = topic.resolveText(content);
            if (text == null) {
                text = "Этот раздел неприменим для выбранного приюта.";
            }
            sendText(client.getChatId(), text, keyboards.adoptionMenu(client.getShelterType()));
        } catch (IllegalArgumentException e) {
            sendText(client.getChatId(), "Не удалось найти эту информацию.",
                    keyboards.adoptionMenu(client.getShelterType()));
        }
    }

    private void askToChooseShelter(Client client) {
        sendText(client.getChatId(), MessageTemplates.CHOOSE_SHELTER_FIRST, keyboards.shelterSelection());
    }

    private void callVolunteer(Client client, String context) {
        String shelterName = client.getShelterType() != null
                ? client.getShelterType().getDisplayName() : "приют не выбран";
        String alert = MessageTemplates.volunteerCallAlert(clientService.label(client), shelterName, context);
        for (Long volunteerChatId : botProperties.getVolunteerChatIds()) {
            sendText(volunteerChatId, alert, null);
        }
        sendText(client.getChatId(), "Хорошо, волонтёр скоро с вами свяжется!", keyboards.mainMenu());
    }

    private void sendCurrentMenu(Client client, String prefixMessage) {
        InlineKeyboardMarkup markup = client.getStage() == BotStage.SHELTER_SELECTION
                ? keyboards.shelterSelection()
                : keyboards.mainMenu();
        sendText(client.getChatId(), prefixMessage, markup);
    }

    // ---------------------------------------------------------------- low-level send

    public void sendText(Long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        if (markup != null) {
            message.setReplyMarkup(markup);
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
