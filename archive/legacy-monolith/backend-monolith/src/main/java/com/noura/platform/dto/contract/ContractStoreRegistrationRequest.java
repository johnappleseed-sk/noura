package com.noura.platform.dto.contract;

import com.noura.platform.dto.store.StoreRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ContractStoreRegistrationRequest(
        @NotNull @Valid StoreRequest store,
        UUID primaryAdminUserId
) {
}

