package com.example.priutbot.service;

import com.example.priutbot.entity.BotStage;
import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.ClientRole;
import com.example.priutbot.entity.ShelterType;
import com.example.priutbot.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Client getOrCreate(Long chatId, String telegramUsername) {
        return clientRepository.findByChatId(chatId).orElseGet(() -> {
            Client client = new Client();
            client.setChatId(chatId);
            client.setTelegramUsername(telegramUsername);
            return clientRepository.save(client);
        });
    }

    /** Этап 0: a returning user always restarts at shelter selection. */
    @Transactional
    public void restart(Client client) {
        client.setStage(BotStage.SHELTER_SELECTION);
        clientRepository.save(client);
    }

    @Transactional
    public void chooseShelter(Client client, ShelterType shelterType) {
        client.setShelterType(shelterType);
        client.setStage(BotStage.MAIN_MENU);
        clientRepository.save(client);
    }

    @Transactional
    public void setStage(Client client, BotStage stage) {
        client.setStage(stage);
        clientRepository.save(client);
    }

    @Transactional
    public void saveContactInfo(Client client, String contactInfo) {
        client.setContactInfo(contactInfo);
        client.setStage(BotStage.MAIN_MENU);
        clientRepository.save(client);
    }

    @Transactional
    public void markAsVolunteerIfNeeded(Client client, boolean isVolunteer) {
        if (isVolunteer && client.getRole() != ClientRole.VOLUNTEER) {
            client.setRole(ClientRole.VOLUNTEER);
            clientRepository.save(client);
        }
    }

    public Optional<Client> findById(Long id) {
        return clientRepository.findById(id);
    }

    public String label(Client client) {
        if (client.getTelegramUsername() != null && !client.getTelegramUsername().isBlank()) {
            return "@" + client.getTelegramUsername();
        }
        return "id" + client.getChatId();
    }
}
