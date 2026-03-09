package com.noura.platform.dto.returns;

import java.util.List;

public record CreateReturnRequest(
        Long orderId,
        String reason,
        String reasonDetails,
        String customerNotes,
        List<CreateReturnItemRequest> items
) {
}
