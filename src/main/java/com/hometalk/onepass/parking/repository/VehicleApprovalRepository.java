package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.parking.entity.VehicleApproval;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleApprovalRepository extends JpaRepository<VehicleApproval, Long> {

    // 차량별 최신 승인 이력 조회
    Optional<VehicleApproval> findTopByVehicleOrderByApprovalIdDesc(Vehicle vehicle);

    // 승인 상태별 조회 (soft delete된 차량 제외)
    @Query("SELECT va FROM VehicleApproval va JOIN va.vehicle v WHERE va.status = :status AND v.deletedAt IS NULL")
    List<VehicleApproval> findByStatus(@Param("status") VehicleApproval.ApprovalStatus status);

    // 승인 상태별 조회 - 페이지네이션 (soft delete된 차량 제외)
    @Query(value = "SELECT va FROM VehicleApproval va JOIN va.vehicle v WHERE va.status = :status AND v.deletedAt IS NULL",
            countQuery = "SELECT COUNT(va) FROM VehicleApproval va JOIN va.vehicle v WHERE va.status = :status AND v.deletedAt IS NULL")
    Page<VehicleApproval> findByStatus(@Param("status") VehicleApproval.ApprovalStatus status, Pageable pageable);

    // 차량별 전체 승인 이력 조회
    List<VehicleApproval> findByVehicleOrderByApprovalIdDesc(Vehicle vehicle);
}