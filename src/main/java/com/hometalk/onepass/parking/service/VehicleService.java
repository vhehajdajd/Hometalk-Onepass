package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.parking.dto.request.VehicleRegisterRequest;
import com.hometalk.onepass.parking.dto.request.VehicleApprovalRequest;
import com.hometalk.onepass.parking.dto.response.VehicleApprovalResponse;
import com.hometalk.onepass.parking.dto.response.VehicleResponse;
import com.hometalk.onepass.parking.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VehicleService {

    // 차량 등록
    VehicleResponse register(Long userId, VehicleRegisterRequest request, List<MultipartFile> documents);

    // 세대별 차량 목록 조회
    List<VehicleResponse> getHouseholdVehicles(Long householdId);

    // 차량 단건 조회
    VehicleResponse getVehicle(Long vehicleId);

    // 반려 사유 조회
    String getRejectReason(Long vehicleId);

    // 차량 재신청
    VehicleResponse reapply(Long vehicleId, List<MultipartFile> documents);

    // 관리자 - 차량 목록 조회 (상태별)
    List<VehicleApprovalResponse> getApprovalList(Vehicle.VehicleStatus status);

    // 관리자 - 차량 승인
    void approve(Long userId, Long approvalId);

    // 관리자 - 차량 반려
    void reject(Long userId, Long approvalId, String rejectReason);

    // 입주자 차량 삭제 (소유권 검증 포함)
    // ✅ 보안 수정: householdId 추가 → 내 세대 차량인지 Impl에서 검증
    void delete(Long vehicleId, Long householdId);

    // 관리자 차량 삭제 (소유권 검증 없음 - 관리자 권한)
    void adminDelete(Long vehicleId);

    boolean existsByVehicleNumber(String vehicleNumber);

    // 페이지네이션
    Page<VehicleApprovalResponse> getApprovalList(Vehicle.VehicleStatus status, int page, int size);
}