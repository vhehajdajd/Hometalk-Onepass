package com.hometalk.onepass.inquiry.repository;

import com.hometalk.onepass.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findByUserId(Long userId);

    @Query("select i from Inquiry i left join fetch i.attachments where i.id = :id")
    Optional<Inquiry> findByIdWithAttachments(@Param("id") Long id);
}
