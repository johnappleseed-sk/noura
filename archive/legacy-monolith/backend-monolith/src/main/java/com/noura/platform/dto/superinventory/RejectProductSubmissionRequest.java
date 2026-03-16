package com.noura.platform.dto.superinventory;

import jakarta.validation.constraints.Size;

public record RejectProductSubmissionRequest(
        @Size(max = 1000) String notes
) {
}
