package com.hometalk.onepass.complaint.repository;

import com.hometalk.onepass.complaint.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Page<Complaint> findByUser_Id(Long userId, Pageable pageable);

    Page<Complaint> findAllBySecretFalse(Pageable pageable);

    Optional<Complaint> findFirstByUser_IdOrderByIdDesc(Long userId);

    List<Complaint> findTop5ByUser_IdOrderByCreatedAtDesc(Long userId);

    List<Complaint> findTop10ByOrderByCreatedAtDesc();

    @Query("select distinct c from Complaint c " +
            "left join fetch c.attachments " +
            "where c.id = :id")
    Optional<Complaint> findByIdWithFiles(@Param("id") Long id);
}