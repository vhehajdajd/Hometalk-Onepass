package com.hometalk.onepass.parking.dto.request;

import com.hometalk.onepass.parking.dto.EntryType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EntryRequest {

    @NotNull
    private Long id;

    @NotNull
    private EntryType type;
}
