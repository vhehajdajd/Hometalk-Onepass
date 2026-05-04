package com.hometalk.onepass.inquiry.repository;

import com.hometalk.onepass.inquiry.entity.ComplaintAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintAttachmentRepository extends JpaRepository<ComplaintAttachment, Long> {
    // 여기에 save 메서드를 따로 적지 않아도 JpaRepository가 이미 가지고 있어요!
}