package com.example.priutbot.repository;

import com.example.priutbot.entity.Client;
import com.example.priutbot.entity.PetReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PetReportRepository extends JpaRepository<PetReport, Long> {

    Optional<PetReport> findByClientAndReportDate(Client client, LocalDate reportDate);

    List<PetReport> findByClientOrderByReportDateDesc(Client client);
}
