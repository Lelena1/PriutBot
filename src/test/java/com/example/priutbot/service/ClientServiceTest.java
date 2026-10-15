package com.example.priutbot.service;

import com.example.priutbot.entity.BotStage;
import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.ClientRole;
import com.example.priutbot.entity.ShelterType;
import com.example.priutbot.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    void getOrCreateReturnsExistingClient() {
        Client existing = new Client();
        existing.setChatId(1L);
        when(clientRepository.findByChatId(1L)).thenReturn(Optional.of(existing));

        assertThat(clientService.getOrCreate(1L, "anna")).isSameAs(existing);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void getOrCreateRegistersNewGuest() {
        when(clientRepository.findByChatId(1L)).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Client created = clientService.getOrCreate(1L, "anna");

        assertThat(created.getChatId()).isEqualTo(1L);
        assertThat(created.getTelegramUsername()).isEqualTo("anna");
        assertThat(created.getRole()).isEqualTo(ClientRole.GUEST);
        assertThat(created.getStage()).isEqualTo(BotStage.SHELTER_SELECTION);
    }

    @Test
    void chooseShelterOpensMainMenu() {
        Client client = new Client();

        clientService.chooseShelter(client, ShelterType.DOG);

        ArgumentCaptor<Client> saved = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(saved.capture());
        assertThat(saved.getValue().getShelterType()).isEqualTo(ShelterType.DOG);
        assertThat(saved.getValue().getStage()).isEqualTo(BotStage.MAIN_MENU);
    }

    @Test
    void volunteerRoleIsAssignedOnlyOnce() {
        Client volunteer = new Client();
        volunteer.setRole(ClientRole.VOLUNTEER);

        clientService.markAsVolunteerIfNeeded(volunteer, true);

        verify(clientRepository, never()).save(any());
    }

    @Test
    void labelPrefersUsernameOverChatId() {
        Client withUsername = new Client();
        withUsername.setChatId(5L);
        withUsername.setTelegramUsername("anna");
        Client withoutUsername = new Client();
        withoutUsername.setChatId(5L);
        withoutUsername.setTelegramUsername(" ");

        assertThat(clientService.label(withUsername)).isEqualTo("@anna");
        assertThat(clientService.label(withoutUsername)).isEqualTo("id5");
    }
}
