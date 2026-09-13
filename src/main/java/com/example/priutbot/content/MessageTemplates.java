package com.example.priutbot.content;

/** Standard message texts specified by the shelter (Этап 3 outcomes and warnings). */
public final class MessageTemplates {

    private MessageTemplates() {
    }

    public static final String POOR_REPORT_WARNING =
            "Дорогой хозяин, мы заметили, что ты заполняешь отчет не так подробно, как необходимо. "
                    + "Пожалуйста, подойди ответственнее к этому занятию. В противном случае волонтеры "
                    + "приюта будут обязаны самолично проверять условия содержания животного.";

    public static final String PROBATION_PASSED =
            "Поздравляем! Вы успешно прошли испытательный срок. Спасибо, что заботитесь о своём питомце "
                    + "и присылали ежедневные отчёты — дальнейшие отчёты присылать не нужно. "
                    + "Если возникнут вопросы, всегда можно позвать волонтёра.";

    public static final String PROBATION_FAILED =
            "К сожалению, по итогам испытательного срока волонтёры приняли решение не оставлять "
                    + "животное у вас. Пожалуйста, свяжитесь с волонтёром приюта через кнопку "
                    + "«Позвать волонтёра», чтобы обсудить дальнейшие шаги по возврату животного в приют.";

    public static String probationExtended(int additionalDays) {
        return "Волонтёры приняли решение продлить испытательный срок ещё на " + additionalDays
                + " дней. Пожалуйста, продолжайте присылать ежедневные отчёты о питомце.";
    }

    public static String missingReportReminderForOwner() {
        return "Не забудьте прислать сегодняшний отчёт о питомце: фото, рацион, самочувствие "
                + "и изменения в поведении.";
    }

    public static String overdueReportAlertForVolunteers(String ownerLabel, String shelterName, long daysSinceLastReport) {
        return "⚠ Хозяин " + ownerLabel + " (" + shelterName + ") не присылал отчёт о питомце "
                + daysSinceLastReport + " дн. Пожалуйста, свяжитесь с ним.";
    }

    public static String probationEndedAlertForVolunteers(String ownerLabel, String shelterName) {
        return "📋 У хозяина " + ownerLabel + " (" + shelterName + ") закончился испытательный срок. "
                + "Нужно принять решение: /decide <id> pass|extend14|extend30|fail";
    }

    public static String volunteerCallAlert(String requesterLabel, String shelterName, String context) {
        return "🔔 Пользователь " + requesterLabel + " (" + shelterName + ") просит позвать волонтёра.\n"
                + "Контекст: " + context;
    }
}
