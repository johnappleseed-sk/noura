package com.noura.platform.search;

import com.noura.platform.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.search", name = "elastic-enabled", havingValue = "false", matchIfMissing = true)
public class JpaProductSearchGateway implements ProductSearchGateway {

    private final ProductRepository productRepository;

    /**
     * Executes search product ids.
     *
     * @param q The q value.
     * @param limit The limit value.
     * @return A list of matching items.
     */
    @Override
    public List<UUID> searchProductIds(String q, int limit) {
        String query = q == null ? "" : q.toLowerCase(Locale.ROOT);
        return productRepository.findAll().stream()
                .filter(product -> product.getName().toLowerCase(Locale.ROOT).contains(query))
                .limit(limit)
                .map(product -> product.getId())
                .toList();
    }
}
