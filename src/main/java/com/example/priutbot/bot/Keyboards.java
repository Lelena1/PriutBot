package com.example.priutbot.bot;

import com.example.priutbot.content.AdoptionTopic;
import com.example.priutbot.content.InfoTopic;
import com.example.priutbot.entity.ShelterType;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class Keyboards {

    public static final String SHELTER_CAT = "SHELTER:CAT";
    public static final String SHELTER_DOG = "SHELTER:DOG";

    public static final String MENU_INFO = "MENU:INFO";
    public static final String MENU_ADOPTION = "MENU:ADOPTION";
    public static final String MENU_REPORT = "MENU:REPORT";
    public static final String MENU_VOLUNTEER = "MENU:VOLUNTEER";

    public static final String INFO_PREFIX = "INFO:";
    public static final String ADOPT_PREFIX = "ADOPT:";

    public static final String BACK_MAIN = "BACK:MAIN";

    private InlineKeyboardButton button(String label, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(label);
        button.setCallbackData(callbackData);
        return button;
    }

    private InlineKeyboardMarkup markup(List<List<InlineKeyboardButton>> rows) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    private List<InlineKeyboardButton> row(InlineKeyboardButton... buttons) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (InlineKeyboardButton b : buttons) {
            row.add(b);
        }
        return row;
    }

    public InlineKeyboardMarkup shelterSelection() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row(button("🐱 " + ShelterType.CAT.getDisplayName(), SHELTER_CAT)));
        rows.add(row(button("🐶 " + ShelterType.DOG.getDisplayName(), SHELTER_DOG)));
        return markup(rows);
    }

    public InlineKeyboardMarkup mainMenu() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row(button("ℹ️ Узнать информацию о приюте", MENU_INFO)));
        rows.add(row(button("🏠 Как взять животное из приюта", MENU_ADOPTION)));
        rows.add(row(button("📋 Прислать отчёт о питомце", MENU_REPORT)));
        rows.add(row(button("🙋 Позвать волонтёра", MENU_VOLUNTEER)));
        return markup(rows);
    }

    public static final String INFO_CONTACT = "INFO:CONTACT";
    public static final String ADOPT_CONTACT = "ADOPT:CONTACT";

    public InlineKeyboardMarkup infoMenu() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (InfoTopic topic : InfoTopic.values()) {
            rows.add(row(button(topic.getButtonLabel(), INFO_PREFIX + topic.name())));
        }
        rows.add(row(button("✍️ Оставить контакты для связи", INFO_CONTACT)));
        rows.add(row(button("🙋 Позвать волонтёра", MENU_VOLUNTEER)));
        rows.add(row(button("⬅️ Назад в меню", BACK_MAIN)));
        return markup(rows);
    }

    public InlineKeyboardMarkup adoptionMenu(ShelterType shelterType) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (AdoptionTopic topic : AdoptionTopic.values()) {
            boolean dogOnly = topic == AdoptionTopic.CYNOLOGIST_ADVICE || topic == AdoptionTopic.CYNOLOGIST_LIST;
            if (dogOnly && shelterType != ShelterType.DOG) {
                continue;
            }
            rows.add(row(button(topic.getButtonLabel(), ADOPT_PREFIX + topic.name())));
        }
        rows.add(row(button("✍️ Оставить контакты для связи", ADOPT_CONTACT)));
        rows.add(row(button("🙋 Позвать волонтёра", MENU_VOLUNTEER)));
        rows.add(row(button("⬅️ Назад в меню", BACK_MAIN)));
        return markup(rows);
    }

    public InlineKeyboardMarkup backToMainOnly() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row(button("⬅️ Назад в меню", BACK_MAIN)));
        return markup(rows);
    }
}
