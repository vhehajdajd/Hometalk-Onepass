package com.hometalk.onepass.auth.repository;

import com.hometalk.onepass.auth.entity.Household;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HouseholdRepository extends JpaRepository<Household, Long> {
    // 우편번호 + 동 + 호로 세대 조회 (입주민 본인 세대 확인용)
    Optional<Household> findByPostNumAndDongAndHo(String postNum, String dong, String ho);
    // 동 + 호로 세대 조회 (관리자 고지서 업로드용 — 아파트 전체 대상)
    Optional<Household> findByDongAndHo(String dong, String ho);
}
