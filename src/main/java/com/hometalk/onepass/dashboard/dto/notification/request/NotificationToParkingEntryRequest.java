package com.hometalk.onepass.dashboard.dto.notification.request;

import com.hometalk.onepass.parking.dto.EntryType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationToParkingEntryRequest {

    @NotNull
    private Long id;

    @NotNull
    private EntryType type;
}
