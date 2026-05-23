package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.auth.entity.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    List<Vehicle> findByHousehold(Household household);

    List<Vehicle> findByVehicleNumberEndingWith(String suffix);

    List<Vehicle> findByStatus(Vehicle.VehicleStatus status);

    boolean existsByVehicleNumber(String vehicleNumber);

    // 퀵서치 - 공백 제거 후 끝 4자리 비교 (APPROVED만)
    @Query("""
        SELECT v FROM Vehicle v
        JOIN FETCH v.household h
        JOIN FETCH v.user u
        WHERE v.status = 'APPROVED'
          AND v.deletedAt IS NULL
          AND RIGHT(REPLACE(v.vehicleNumber, ' ', ''), 4) = :last4
        """)
    List<Vehicle> findApprovedByLast4(@Param("last4") String last4);

    // 입주자 차량 목록 전체 (APPROVED)
    List<Vehicle> findAllByStatusAndDeletedAtIsNull(Vehicle.VehicleStatus status);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.household JOIN FETCH v.user WHERE v.status = :status AND v.deletedAt IS NULL")
    List<Vehicle> findAllByStatusWithHousehold(@Param("status") Vehicle.VehicleStatus status);
}