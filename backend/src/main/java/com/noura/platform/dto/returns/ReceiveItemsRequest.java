package com.noura.platform.dto.returns;

import java.util.List;

public record ReceiveItemsRequest(
        List<ReturnItemQuantity> itemQuantities
) {
}
