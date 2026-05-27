package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.HouseholdRepository;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.publisher.NotificationPublisher;
import com.hometalk.onepass.parking.dto.request.VehicleRegisterRequest;
import com.hometalk.onepass.parking.dto.response.VehicleApprovalResponse;
import com.hometalk.onepass.parking.dto.response.VehicleResponse;
import com.hometalk.onepass.parking.entity.ParkingLog;
import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.parking.entity.VehicleApproval;
import com.hometalk.onepass.parking.exception.ParkingException;
import com.hometalk.onepass.parking.repository.ParkingLogRepository;
import com.hometalk.onepass.parking.repository.VehicleApprovalRepository;
import com.hometalk.onepass.parking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleApprovalRepository vehicleApprovalRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final ParkingLogRepository parkingLogRepository;
    private final NotificationPublisher notificationPublisher;

    @Override
    public VehicleResponse register(Long userId, VehicleRegisterRequest request,
                                    List<MultipartFile> documents) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ParkingException("사용자를 찾을 수 없습니다."));
        Household household = user.getHousehold();

        String vehicleNumber = request.getVehicleNumber().replace(" ", "");
        if (vehicleRepository.existsByVehicleNumberAndDeletedAtIsNull(vehicleNumber)) {
            throw new ParkingException("이미 등록된 차량 번호입니다.");
        }

        if (documents == null || documents.isEmpty() ||
                documents.stream().allMatch(MultipartFile::isEmpty)) {
            throw new ParkingException("첨부 서류는 필수입니다.");
        }

        // 파일 저장
        List<String> documentPaths = fileStorageService.saveDocuments(documents);
        if (documentPaths.isEmpty()) {
            throw new ParkingException("서류 저장에 실패했습니다.");
        }

        // DB 저장 실패 시 파일 롤백 (1번 수정)
        try {
            Vehicle vehicle = new Vehicle(household, user, vehicleNumber,
                    request.getModel(), request.getVehicleType());
            vehicleRepository.save(vehicle);
            vehicleApprovalRepository.save(
                    new VehicleApproval(vehicle, String.join(",", documentPaths)));
            return new VehicleResponse(vehicle);
        } catch (Exception e) {
            documentPaths.forEach(fileStorageService::deleteFile);
            throw new ParkingException("차량 등록 중 오류가 발생했습니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getHouseholdVehicles(Long householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ParkingException("세대를 찾을 수 없습니다."));

        return vehicleRepository.findByHousehold(household)
                .stream()
                .map(vehicle -> {
                    Optional<ParkingLog> parkingLog = parkingLogRepository
                            .findByVehicleNumberAndStatus(
                                    vehicle.getVehicleNumber(), ParkingLog.ParkingStatus.PARKED);
                    return new VehicleResponse(vehicle, parkingLog.orElse(null));
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public String getRejectReason(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ParkingException("차량을 찾을 수 없습니다."));
        return vehicleApprovalRepository.findTopByVehicleOrderByApprovalIdDesc(vehicle)
                .map(VehicleApproval::getRejectReason)
                .orElse(null);
    }

    @Override
    public VehicleResponse reapply(Long vehicleId, List<MultipartFile> documents) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ParkingException("차량을 찾을 수 없습니다."));

        // 기존 서류 파일 삭제 (3번 수정)
        vehicleApprovalRepository.findTopByVehicleOrderByApprovalIdDesc(vehicle)
                .ifPresent(existing -> {
                    if (existing.getDocumentPath() != null) {
                        Arrays.stream(existing.getDocumentPath().split(","))
                                .forEach(fileStorageService::deleteFile);
                    }
                });

        List<String> documentPaths = fileStorageService.saveDocuments(documents);
        if (documentPaths.isEmpty()) {
            throw new ParkingException("첨부 서류는 필수입니다.");
        }

        vehicleApprovalRepository.save(
                new VehicleApproval(vehicle, String.join(",", documentPaths)));
        vehicle.pending();

        return new VehicleResponse(vehicle);
    }

    // 2번 수정 - N+1 해결: 세대별 승인 차량 수를 한 번에 집계
    @Override
    @Transactional(readOnly = true)
    public List<VehicleApprovalResponse> getApprovalList(Vehicle.VehicleStatus status) {
        List<VehicleApproval> approvals = vehicleRepository.findByStatus(status)
                .stream()
                .map(vehicle -> vehicleApprovalRepository
                        .findTopByVehicleOrderByApprovalIdDesc(vehicle))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        // 세대 ID 목록 추출 후 한 번에 집계
        List<Long> householdIds = approvals.stream()
                .map(a -> a.getVehicle().getHousehold().getId())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Integer> approvedCountMap = vehicleRepository
                .countApprovedByHouseholdIds(householdIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));

        return approvals.stream()
                .map(approval -> {
                    VehicleApprovalResponse response = new VehicleApprovalResponse(approval);
                    response.setApprovedCount(
                            approvedCountMap.getOrDefault(
                                    approval.getVehicle().getHousehold().getId(), 0));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleApprovalResponse> getApprovalList(Vehicle.VehicleStatus status,
                                                         int page, int size) {
        VehicleApproval.ApprovalStatus approvalStatus =
                VehicleApproval.ApprovalStatus.valueOf(status.name());
        Pageable pageable = PageRequest.of(page, size, Sort.by("approvalId").descending());

        Page<VehicleApproval> approvalPage = vehicleApprovalRepository
                .findByStatus(approvalStatus, pageable);

        // 세대 ID 목록 추출 후 한 번에 집계 (페이지네이션도 N+1 해결)
        List<Long> householdIds = approvalPage.getContent().stream()
                .map(a -> a.getVehicle().getHousehold().getId())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Integer> approvedCountMap = vehicleRepository
                .countApprovedByHouseholdIds(householdIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Long) row[1]).intValue()
                ));

        return approvalPage.map(approval -> {
            VehicleApprovalResponse response = new VehicleApprovalResponse(approval);
            response.setApprovedCount(
                    approvedCountMap.getOrDefault(
                            approval.getVehicle().getHousehold().getId(), 0));
            return response;
        });
    }

    @Override
    public void approve(Long userId, Long approvalId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ParkingException("사용자를 찾을 수 없습니다."));

        VehicleApproval approval = vehicleApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new ParkingException("승인 이력을 찾을 수 없습니다."));

        String vehicleNumber = approval.getVehicle().getVehicleNumber();
        vehicleRepository.findByVehicleNumber(vehicleNumber)
                .ifPresent(v -> {
                    if (!v.getVehicleId().equals(approval.getVehicle().getVehicleId())
                            && v.getStatus() == Vehicle.VehicleStatus.APPROVED) {
                        throw new ParkingException("이미 승인된 동일 차량 번호가 존재합니다.");
                    }
                });

        approval.approve(user);
        approval.getVehicle().approve();

        notificationPublisher.publish(
                approval.getVehicle().getUser().getId(),
                NotificationTargetRole.RESIDENT,
                NotificationType.VEHICLE_APPROVED,
                "차량 등록 완료",
                "차량등록이 완료되었습니다.",
                "/parking/vehicle",
                approval.getVehicle().getVehicleId()
        );
    }

    @Override
    public void reject(Long userId, Long approvalId, String rejectReason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ParkingException("사용자를 찾을 수 없습니다."));

        VehicleApproval approval = vehicleApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new ParkingException("승인 이력을 찾을 수 없습니다."));

        approval.reject(user, rejectReason);
        approval.getVehicle().reject();

        notificationPublisher.publish(
                approval.getVehicle().getUser().getId(),
                NotificationTargetRole.RESIDENT,
                NotificationType.VEHICLE_REJECTED,
                "차량 등록 반려",
                "차량 등록이 반려되었습니다. 사유를 확인 후 다시 등록해 주세요.",
                "/parking/vehicle",
                approval.getVehicle().getVehicleId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ParkingException("차량을 찾을 수 없습니다."));
        return new VehicleResponse(vehicle);
    }

    @Override
    public void delete(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ParkingException("차량을 찾을 수 없습니다."));
        parkingLogRepository.findByVehicleNumberAndStatus(
                        vehicle.getVehicleNumber(), ParkingLog.ParkingStatus.PARKED)
                .ifPresent(l -> { throw new ParkingException("주차 중인 차량은 삭제할 수 없습니다."); });
        vehicle.softDelete();
    }

    @Override
    public void adminDelete(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ParkingException("차량을 찾을 수 없습니다."));
        vehicle.softDelete();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByVehicleNumber(String vehicleNumber) {
        return vehicleRepository.existsByVehicleNumber(vehicleNumber);
    }
}