package com.hometalk.onepass.complaint.repository;

import com.hometalk.onepass.complaint.entity.ComplaintAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintAnswerRepository
        extends JpaRepository<ComplaintAnswer, Long> {

    List<ComplaintAnswer> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}