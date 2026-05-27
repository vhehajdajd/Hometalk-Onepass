package com.hometalk.onepass.parking.repository;

import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.auth.entity.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    List<Vehicle> findByHousehold(Household household);

    List<Vehicle> findByVehicleNumberEndingWith(String suffix);

    List<Vehicle> findByStatus(Vehicle.VehicleStatus status);

    boolean existsByVehicleNumber(String vehicleNumber);

    boolean existsByVehicleNumberAndDeletedAtIsNull(String vehicleNumber);

    // Generated Column 사용으로 인덱스 활용
    @Query("""
        SELECT v FROM Vehicle v
        JOIN FETCH v.household h
        JOIN FETCH v.user u
        WHERE v.status = 'APPROVED'
          AND v.deletedAt IS NULL
          AND v.vehicleNumberLast4 = :last4
        """)
    List<Vehicle> findApprovedByLast4(@Param("last4") String last4);

    List<Vehicle> findAllByStatusAndDeletedAtIsNull(Vehicle.VehicleStatus status);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.household JOIN FETCH v.user WHERE v.status = :status AND v.deletedAt IS NULL")
    List<Vehicle> findAllByStatusWithHousehold(@Param("status") Vehicle.VehicleStatus status);


    // N+1 해결 - 세대별 승인 차량 수 한 번에 집계
    @Query("""
        SELECT v.household.id, COUNT(v)
        FROM Vehicle v
        WHERE v.status = 'APPROVED'
          AND v.deletedAt IS NULL
          AND v.household.id IN :householdIds
        GROUP BY v.household.id
        """)
    List<Object[]> countApprovedByHouseholdIds(@Param("householdIds") List<Long> householdIds);
}