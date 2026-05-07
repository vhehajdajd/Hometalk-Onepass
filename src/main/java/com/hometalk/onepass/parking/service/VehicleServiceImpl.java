package com.hometalk.onepass.parking.service;

import com.hometalk.onepass.auth.entity.Household;
import com.hometalk.onepass.auth.entity.User;
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

import java.util.List;
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
    private final ParkingLogRepository parkingLogRepository;
    private final NotificationPublisher notificationPublisher;

    @Override
    public VehicleResponse register(Long userId, VehicleRegisterRequest request,
                                    List<MultipartFile> documents) {
        User user = userRepository.findById(1L) // TODO: Security 연동 후 userId로 변경
                .orElseThrow(() -> new ParkingException("사용자를 찾을 수 없습니다."));
        Household household = user.getHousehold();

        String vehicleNumber = request.getVehicleNumber().replace(" ", "");
        if (vehicleRepository.existsByVehicleNumber(vehicleNumber)) {
            throw new ParkingException("이미 등록된 차량 번호입니다.");
        }

        Vehicle vehicle = new Vehicle(household, user, vehicleNumber,
                request.getModel(), request.getVehicleType());
        vehicleRepository.save(vehicle);

        List<String> documentPaths = fileStorageService.saveDocuments(documents);
        if (documentPaths.isEmpty()) {
            throw new ParkingException("첨부 서류는 필수입니다.");
        }

        vehicleApprovalRepository.save(
                new VehicleApproval(vehicle, String.join(",", documentPaths)));

        return new VehicleResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getHouseholdVehicles(Long householdId) {
        User user = userRepository.findById(1L) // TODO: Security 연동 후 변경
                .orElseThrow(() -> new ParkingException("사용자를 찾을 수 없습니다."));
        Household household = user.getHousehold();

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

        List<String> documentPaths = fileStorageService.saveDocuments(documents);
        if (documentPaths.isEmpty()) {
            throw new ParkingException("첨부 서류는 필수입니다.");
        }

        vehicleApprovalRepository.save(
                new VehicleApproval(vehicle, String.join(",", documentPaths)));
        vehicle.pending();

        return new VehicleResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleApprovalResponse> getApprovalList(Vehicle.VehicleStatus status) {
        return vehicleRepository.findByStatus(status)
                .stream()
                .map(vehicle -> vehicleApprovalRepository
                        .findTopByVehicleOrderByApprovalIdDesc(vehicle))
                .filter(Optional::isPresent)
                .map(opt -> new VehicleApprovalResponse(opt.get()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleApprovalResponse> getApprovalList(Vehicle.VehicleStatus status,
                                                         int page, int size) {
        VehicleApproval.ApprovalStatus approvalStatus =
                VehicleApproval.ApprovalStatus.valueOf(status.name());
        Pageable pageable = PageRequest.of(page, size, Sort.by("approvalId").descending());
        return vehicleApprovalRepository.findByStatus(approvalStatus, pageable)
                .map(VehicleApprovalResponse::new);
    }

    @Override
    public void approve(Long userId, Long approvalId) {
        User user = null; // TODO: Security 연동 후 추출

        VehicleApproval approval = vehicleApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new ParkingException("승인 이력을 찾을 수 없습니다."));

        approval.approve(user);
        approval.getVehicle().approve();

        // ─── 알림: 차량 승인 → 입주자에게 ───
        Long ownerId = approval.getVehicle().getUser().getId();
        notificationPublisher.publish(
                ownerId,
                NotificationTargetRole.RESIDENT,
                NotificationType.VEHICLE_APPROVED,
                "차량 승인 완료",
                approval.getVehicle().getVehicleNumber() + " 차량이 승인되었습니다.",
                "/parking/vehicle",
                approval.getVehicle().getVehicleId()
        );
    }

    @Override
    public void reject(Long userId, Long approvalId, String rejectReason) {
        User user = null; // TODO: Security 연동 후 추출

        VehicleApproval approval = vehicleApprovalRepository.findById(approvalId)
                .orElseThrow(() -> new ParkingException("승인 이력을 찾을 수 없습니다."));

        approval.reject(user, rejectReason);
        approval.getVehicle().reject();

        // ─── 알림: 차량 반려 → 입주자에게 ───
        Long ownerId = approval.getVehicle().getUser().getId();
        notificationPublisher.publish(
                ownerId,
                NotificationTargetRole.RESIDENT,
                NotificationType.VEHICLE_REJECTED,
                "차량 반려",
                approval.getVehicle().getVehicleNumber() + " 차량이 반려되었습니다. 사유: " + rejectReason,
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
}