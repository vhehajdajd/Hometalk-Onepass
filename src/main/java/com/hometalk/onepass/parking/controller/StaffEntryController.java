package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.parking.dto.request.EntryRequest;
import com.hometalk.onepass.parking.dto.request.ManualEntryRequest;
import com.hometalk.onepass.parking.dto.response.VehicleSearchResult;
import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.parking.entity.VisitReservation;
import com.hometalk.onepass.parking.service.StaffEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffEntryController {

    private final StaffEntryService staffEntryService;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("MM.dd HH:mm");

    // GET /staff/vehicle/search?keyword=1234
    @GetMapping("/vehicle/search")
    public ResponseEntity<List<VehicleSearchResult>> searchVehicle(
            @RequestParam String keyword) {

        if (keyword == null || keyword.strip().length() != 4) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(staffEntryService.search(keyword.strip()));
    }

    // POST /staff/vehicle/entry
    @PostMapping("/vehicle/entry")
    public ResponseEntity<Void> processEntry(
            @RequestBody @Valid EntryRequest request) {

        staffEntryService.processEntry(request);
        return ResponseEntity.ok().build();
    }

    // POST /staff/vehicle/manual-entry
    @PostMapping("/vehicle/manual-entry")
    public ResponseEntity<Void> processManualEntry(
            @RequestBody @Valid ManualEntryRequest request) {

        staffEntryService.processManualEntry(request);
        return ResponseEntity.ok().build();
    }

    // GET /staff/entry/list/visit
    @GetMapping("/entry/list/visit")
    public ResponseEntity<List<Map<String, Object>>> getTodayVisitList() {
        List<Map<String, Object>> result = staffEntryService.getTodayVisitList()
                .stream()
                .map(r -> Map.<String, Object>of(
                        "reservationId", r.getReservationId(),
                        "vehicleNumber", r.getVehicleNumber(),
                        "household",     r.getHousehold() != null
                                ? r.getHousehold().getDong() + " " + r.getHousehold().getHo()
                                : "세대 미확인",
                        "purpose",       r.getPurpose(),
                        "reservedAt",    r.getReservedAt().format(DT_FMT)
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    // GET /staff/entry/list/resident
    @GetMapping("/entry/list/resident")
    public ResponseEntity<List<Map<String, Object>>> getResidentVehicleList() {
        List<Map<String, Object>> result = staffEntryService.getResidentVehicleList()
                .stream()
                .map(v -> Map.<String, Object>of(
                        "vehicleId",     v.getVehicleId(),
                        "vehicleNumber", v.getVehicleNumber(),
                        "household",     v.getHousehold().getDong() + " " + v.getHousehold().getHo(),
                        "userName",      v.getUser().getName(),
                        "model",         v.getModel() != null ? v.getModel() : "-"
                ))
                .toList();
        return ResponseEntity.ok(result);
    }
}