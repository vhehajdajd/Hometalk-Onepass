package com.hometalk.onepass.inquiry.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.hometalk.onepass.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findByUserId(Long userId, Pageable pageable);

    @Query("select i from Inquiry i left join fetch i.attachments where i.id = :id")
    Optional<Inquiry> findByIdWithAttachments(@Param("id") Long id);

    // userId + 상태 검색
    Page<Inquiry> findByStatus(String status, Pageable pageable);
    Page<Inquiry> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    // 최신 순 고정 조회
    List<Inquiry> findTop10ByOrderByCreatedAtDesc();

    Optional<Inquiry> findFirstByUserIdOrderByIdDesc(Long userId);
}
