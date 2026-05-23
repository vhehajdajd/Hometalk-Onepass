package com.hometalk.onepass.auth.repository;

import com.hometalk.onepass.auth.entity.Household;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HouseholdRepository extends JpaRepository<Household, Long> {
    Optional<Household> findByPostNumAndDongAndHo(String postNum, String dong, String ho);
}
