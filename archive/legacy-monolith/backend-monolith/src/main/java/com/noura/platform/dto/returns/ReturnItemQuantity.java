package com.noura.platform.dto.returns;

public record ReturnItemQuantity(
        Long returnItemId,
        Integer quantityReceived,
        String conditionNotes
) {
}
