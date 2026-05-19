package com.hometalk.onepass.inquiry.repository;

import com.hometalk.onepass.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @EntityGraph(attributePaths = {"user"})
    Page<Inquiry> findByUserId(Long userId, Pageable pageable);

    @Query("""
        select distinct i
        from Inquiry i
        left join fetch i.user
        left join fetch i.attachments
        where i.id = :id
        """)
    Optional<Inquiry> findByIdWithAttachments(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user"})
    Page<Inquiry> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Inquiry> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<Inquiry> findTop10ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user"})
    Optional<Inquiry> findFirstByUser_IdOrderByIdDesc(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<Inquiry> findTop5ByUser_IdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndStatus(Long userId, String status);
}