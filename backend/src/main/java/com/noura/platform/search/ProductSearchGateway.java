package com.noura.platform.search;

import java.util.List;
import java.util.UUID;

public interface ProductSearchGateway {
    /**
     * Executes search product ids.
     *
     * @param q The q value.
     * @param limit The limit value.
     * @return A list of matching items.
     */
    List<UUID> searchProductIds(String q, int limit);
}
