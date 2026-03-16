package com.noura.platform.dto.returns;

public record CreateReturnItemRequest(
        Long orderItemId,
        Integer quantity
) {
}
