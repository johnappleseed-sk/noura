package com.noura.platform.dto.superinventory;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ApproveProductSubmissionRequest(
        UUID targetProductId,
        @Size(max = 1000) String notes
) {
}
