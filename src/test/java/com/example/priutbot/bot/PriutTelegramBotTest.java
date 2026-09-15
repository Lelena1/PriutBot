package com.example.priutbot.bot;

import com.example.priutbot.config.BotProperties;
import com.example.priutbot.content.ContentProvider;
import com.example.priutbot.content.MessageTemplates;
import com.example.priutbot.entity.BotStage;
import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.ClientRole;
import com.example.priutbot.entity.ShelterType;
import com.example.priutbot.service.ClientService;
import com.example.priutbot.service.ReportService;
import com.example.priutbot.service.VolunteerService;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

class PriutTelegramBotTest {

    static final long VISITOR_CHAT = 700001L;
    static final long VOLUNTEER_CHAT = 900001L;
    static final long VOLUNTEER_GROUP = -100500L;
    static final long TARGET_CLIENT_ID = 5L;
    static final String CONTACT_OF_255 = "к".repeat(255);
    static final String CONTACT_OF_256 = "к".repeat(256);

    record Collaborators(ClientService clientService, ReportService reportService, VolunteerService volunteerService) {
    }

    record Expected(String userReplyContains, String firstButtonCallback, BotStage lastStageSet,
                    int volunteerMessages, String volunteerMessageContains, boolean contactSaved) {
    }

    record Case(String id, long chatId, ClientRole role, BotStage stage, ShelterType shelter, Update update,
                Consumer<Collaborators> arrange, Expected expected) {
    }

    static final List<Case> CASES = List.of(
            new Case("BACK:MAIN while awaiting contact details ends the input",
                    VISITOR_CHAT, ClientRole.GUEST, BotStage.AWAITING_CONTACT_INFO, ShelterType.CAT,
                    callback("BACK:MAIN"), null,
                    new Expected("Главное меню:", "MENU:INFO", BotStage.MAIN_MENU, 0, null, false)),
            new Case("MENU:INFO while awaiting adoption contact details ends the input",
                    VISITOR_CHAT, ClientRole.GUEST, BotStage.AWAITING_CONTACT_ADOPTION, ShelterType.DOG,
                    callback("MENU:INFO"), null,
                    new Expected("Что рассказать о приюте?", "INFO:ABOUT", BotStage.MAIN_MENU, 0, null, false)),
            new Case("MENU:VOLUNTEER while awaiting contact details ends the input and alerts volunteers",
                    VISITOR_CHAT, ClientRole.GUEST, BotStage.AWAITING_CONTACT_INFO, ShelterType.CAT,
                    callback("MENU:VOLUNTEER"), null,
                    new Expected("Хорошо, волонтёр скоро с вами свяжется!", "MENU:INFO", BotStage.MAIN_MENU,
                            2, "просит позвать волонтёра", false)),
            new Case("a topic button while awaiting the report ends the input",
                    VISITOR_CHAT, ClientRole.NEW_OWNER, BotStage.AWAITING_REPORT, ShelterType.CAT,
                    callback("INFO:SCHEDULE"), null,
                    new Expected("Режим работы", "INFO:ABOUT", BotStage.MAIN_MENU, 0, null, false)),
            new Case("INFO:CONTACT restarts the contact input",
                    VISITOR_CHAT, ClientRole.GUEST, BotStage.AWAITING_CONTACT_ADOPTION, ShelterType.CAT,
                    callback("INFO:CONTACT"), null,
                    new Expected("контактные данные", null, BotStage.AWAITING_CONTACT_INFO, 0, null, false)),
            new Case("MENU:REPORT for an owner starts the report input",
                    VISITOR_CHAT, ClientRole.NEW_OWNER, BotStage.MAIN_MENU, ShelterType.DOG,
                    callback("MENU:REPORT"), null,
                    new Expected("Пришлите, пожалуйста, фото питомца", null, BotStage.AWAITING_REPORT, 0, null, false)),
            new Case("a button pressed in the main menu sets no stage",
                    VISITOR_CHAT, ClientRole.GUEST, BotStage.MAIN_MENU, ShelterType.CAT,
                    callback("BACK:MAIN"), null,
                    new Expected("Главное меню:", "MENU:INFO", null, 0, null, false)),
            new Case("an info topic without a chosen shelter asks to choose one",
                    VISITOR_CHAT, ClientRole.NEW_OWNER, BotStage.SHELTER_SELECTION, null,
                    callback("INFO:ABOUT"), null,
                    new Expected(MessageTemplates.CHOOSE_SHELTER_FIRST, "SHELTER:CAT", null, 0, null, false)),
            new Case("an adoption topic without a chosen shelter asks to choose one",
                    VISITOR_CHAT, ClientRole.NEW_OWNER, BotStage.SHELTER_SELECTION, null,
                    callback("ADOPT:DOCUMENTS"), null,
                    new Expected(MessageTemplates.CHOOSE_SHELTER_FIRST, "SHELTER:CAT", null, 0, null, false)),
            new Case("an info topic with a chosen shelter shows its text",
                    VISITOR_CHAT, ClientRole.GUEST, BotStage.MAIN_MENU, ShelterType.CAT,
                    callback("INFO:ABOUT"), null,
                    new Expected("приют для кошек", "INFO:ABOUT", null, 0, null, false)),
            new Case("contact details within the limit are saved and forwarded to volunteers",
                    VISITOR_CHAT, ClientRole.GUEST, BotStage.AWAITING_CONTACT_INFO, ShelterType.CAT,
                    text(VISITOR_CHAT, CONTACT_OF_255), null,
                    new Expected("Спасибо! Ваши контакты сохранены", "MENU:INFO", null,
                            2, "@tester (Приют для кошек) оставил(а) контакты для связи:\n" + CONTACT_OF_255, true)),
            new Case("contact details over the limit are refused with an explanation",
                    VISITOR_CHAT, ClientRole.GUEST, BotStage.AWAITING_CONTACT_INFO, ShelterType.CAT,
                    text(VISITOR_CHAT, CONTACT_OF_256), null,
                    new Expected("не длиннее 255 символов", null, null, 0, null, false)),
            new Case("/help for a volunteer describes /pending as README does",
                    VOLUNTEER_CHAT, ClientRole.VOLUNTEER, BotStage.MAIN_MENU, null,
                    text(VOLUNTEER_CHAT, "/help"), null,
                    new Expected("/pending — список активных новых хозяев: статус, дата окончания срока, "
                            + "дата последнего отчёта", null, null, 0, null, false)),
            new Case("/warn on a guest is refused and nothing is sent to the guest",
                    VOLUNTEER_CHAT, ClientRole.VOLUNTEER, BotStage.MAIN_MENU, null,
                    text(VOLUNTEER_CHAT, "/warn " + TARGET_CLIENT_ID),
                    collaborators -> when(collaborators.clientService().findById(TARGET_CLIENT_ID))
                            .thenReturn(Optional.of(clientOf(VISITOR_CHAT, ClientRole.GUEST, BotStage.MAIN_MENU, ShelterType.CAT))),
                    new Expected("Не найден хозяин с таким clientId.", null, null, 0, null, false)),
            new Case("/decide on a non-owner is explained",
                    VOLUNTEER_CHAT, ClientRole.VOLUNTEER, BotStage.MAIN_MENU, null,
                    text(VOLUNTEER_CHAT, "/decide " + TARGET_CLIENT_ID + " pass"), null,
                    new Expected("Не найден новый хозяин с таким clientId или неверное решение.", null, null,
                            0, null, false)),
            new Case("a report text over the limit gets an explanation",
                    VISITOR_CHAT, ClientRole.NEW_OWNER, BotStage.AWAITING_REPORT, ShelterType.CAT,
                    text(VISITOR_CHAT, "т".repeat(4001)),
                    collaborators -> when(collaborators.reportService().receiveText(any(), anyString()))
                            .thenReturn(ReportService.Outcome.TEXT_TOO_LONG),
                    new Expected("не больше 4000 символов", null, null, 0, null, false)));

    @Test
    void repliesAndStageChangesMatchTheCases() {
        SoftAssertions softly = new SoftAssertions();
        for (Case each : CASES) {
            Client client = clientOf(each.chatId(), each.role(), each.stage(), each.shelter());
            Collaborators collaborators = collaboratorsFor(client);
            if (each.arrange() != null) {
                each.arrange().accept(collaborators);
            }
            CapturingBot bot = new CapturingBot(collaborators);

            bot.onUpdateReceived(each.update());

            Expected expected = each.expected();
            List<SendMessage> userReplies = bot.sentTo(each.chatId());
            List<SendMessage> volunteerMessages = new ArrayList<>(bot.sentTo(VOLUNTEER_CHAT));
            volunteerMessages.addAll(bot.sentTo(VOLUNTEER_GROUP));
            if (each.chatId() == VOLUNTEER_CHAT) {
                volunteerMessages.clear();
            }
            softly.assertThat(userReplies).as("%s: one reply to the chat", each.id()).hasSize(1);
            if (userReplies.size() == 1) {
                SendMessage reply = userReplies.get(0);
                softly.assertThat(reply.getText()).as("%s: reply text", each.id()).contains(expected.userReplyContains());
                softly.assertThat(firstButtonCallback(reply)).as("%s: keyboard", each.id())
                        .isEqualTo(expected.firstButtonCallback());
            }
            softly.assertThat(lastStageSet(collaborators.clientService())).as("%s: last stage set", each.id())
                    .isEqualTo(expected.lastStageSet());
            softly.assertThat(volunteerMessages).as("%s: volunteer messages", each.id())
                    .hasSize(expected.volunteerMessages());
            if (expected.volunteerMessageContains() != null) {
                softly.assertThat(volunteerMessages).as("%s: volunteer message text", each.id())
                        .allSatisfy(message -> softly.assertThat(message.getText())
                                .contains(expected.volunteerMessageContains()));
            }
            softly.assertThat(contactSaved(collaborators.clientService())).as("%s: contact saved", each.id())
                    .isEqualTo(expected.contactSaved());
            softly.assertThat(mockingDetails(collaborators.reportService()).getInvocations().stream()
                            .anyMatch(invocation -> invocation.getMethod().getName().equals("markLatestReportPoor")))
                    .as("%s: report flagged", each.id()).isFalse();
        }
        softly.assertAll();
    }

    private static Collaborators collaboratorsFor(Client client) {
        ClientService clientService = mock(ClientService.class);
        when(clientService.getOrCreate(anyLong(), any())).thenReturn(client);
        when(clientService.label(any())).thenReturn("@tester");
        return new Collaborators(clientService, mock(ReportService.class), mock(VolunteerService.class));
    }

    private static Client clientOf(long chatId, ClientRole role, BotStage stage, ShelterType shelter) {
        Client client = new Client();
        client.setId(1L);
        client.setChatId(chatId);
        client.setTelegramUsername("tester");
        client.setRole(role);
        client.setStage(stage);
        client.setShelterType(shelter);
        return client;
    }

    private static BotStage lastStageSet(ClientService clientService) {
        return mockingDetails(clientService).getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("setStage"))
                .map(invocation -> (BotStage) invocation.getArgument(1))
                .reduce((first, last) -> last)
                .orElse(null);
    }

    private static boolean contactSaved(ClientService clientService) {
        return mockingDetails(clientService).getInvocations().stream()
                .anyMatch(invocation -> invocation.getMethod().getName().equals("saveContactInfo"));
    }

    private static String firstButtonCallback(SendMessage message) {
        if (!(message.getReplyMarkup() instanceof InlineKeyboardMarkup markup)) {
            return null;
        }
        return markup.getKeyboard().get(0).get(0).getCallbackData();
    }

    private static Update text(long chatId, String text) {
        Message message = messageIn(chatId);
        message.setText(text);
        Update update = new Update();
        update.setUpdateId(1);
        update.setMessage(message);
        return update;
    }

    private static Update callback(String data) {
        CallbackQuery query = new CallbackQuery();
        query.setId("callback-1");
        query.setFrom(user(VISITOR_CHAT));
        query.setChatInstance("test");
        query.setData(data);
        query.setMessage(messageIn(VISITOR_CHAT));
        Update update = new Update();
        update.setUpdateId(1);
        update.setCallbackQuery(query);
        return update;
    }

    private static Message messageIn(long chatId) {
        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setType("private");
        Message message = new Message();
        message.setMessageId(1);
        message.setChat(chat);
        message.setFrom(user(chatId));
        return message;
    }

    private static User user(long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setIsBot(false);
        user.setUserName("tester");
        return user;
    }

    static class CapturingBot extends PriutTelegramBot {
        final List<SendMessage> sent = new ArrayList<>();

        CapturingBot(Collaborators collaborators) {
            super(properties(), collaborators.clientService(), new ContentProvider(), new Keyboards(),
                    collaborators.reportService(), collaborators.volunteerService());
        }

        @Override
        public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) {
            if (method instanceof SendMessage message) {
                sent.add(message);
            }
            return null;
        }

        List<SendMessage> sentTo(long chatId) {
            return sent.stream().filter(message -> message.getChatId().equals(String.valueOf(chatId))).toList();
        }

        private static BotProperties properties() {
            BotProperties properties = mock(BotProperties.class);
            when(properties.getToken()).thenReturn("000000:TEST-NOT-A-REAL-TOKEN");
            when(properties.getUsername()).thenReturn("priut_test_bot");
            when(properties.getVolunteerChatIds()).thenReturn(List.of(VOLUNTEER_CHAT, VOLUNTEER_GROUP));
            when(properties.isVolunteer(VOLUNTEER_CHAT)).thenReturn(true);
            return properties;
        }
    }
}
