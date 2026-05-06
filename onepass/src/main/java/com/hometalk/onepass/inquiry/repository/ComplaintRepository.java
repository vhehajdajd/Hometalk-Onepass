package com.hometalk.onepass.inquiry.repository;

import com.hometalk.onepass.inquiry.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
}
