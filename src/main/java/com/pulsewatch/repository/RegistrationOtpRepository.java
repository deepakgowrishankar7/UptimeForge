package com.pulsewatch.repository;

import com.pulsewatch.entity.RegistrationOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistrationOtpRepository extends JpaRepository<RegistrationOtp, Long> {

    Optional<RegistrationOtp> findFirstByEmailAndUsedAtIsNullOrderByCreatedAtDesc(String email);

    void deleteAllByEmail(String email);
}
