package com.hometalk.onepass.inquiry.repository;

import com.hometalk.onepass.inquiry.entity.InquiryAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryAttachmentRepository extends JpaRepository<InquiryAttachment, Long> {
    // 첨부파일 관련 기본 CRUD 기능 제공
}