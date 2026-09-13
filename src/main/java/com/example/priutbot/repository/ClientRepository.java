package com.example.priutbot.repository;

import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.ClientRole;
import com.example.priutbot.entity.ProbationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByChatId(Long chatId);

    List<Client> findByRoleAndProbationStatusIn(ClientRole role, List<ProbationStatus> statuses);

    List<Client> findByRoleAndProbationStatusInAndProbationEndDateLessThanEqual(
            ClientRole role, List<ProbationStatus> statuses, LocalDate date);
}
