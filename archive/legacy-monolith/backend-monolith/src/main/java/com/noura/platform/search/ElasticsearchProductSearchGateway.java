package com.noura.platform.search;

import com.noura.platform.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.search", name = "elastic-enabled", havingValue = "true")
public class ElasticsearchProductSearchGateway implements ProductSearchGateway {

    private final ElasticsearchOperations elasticsearchOperations;
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
        // Keep fallback until dedicated ES index/doc mapping is enabled.
        String query = q == null ? "" : q.toLowerCase(Locale.ROOT);
        return productRepository.findAll().stream()
                .filter(product -> product.getName().toLowerCase(Locale.ROOT).contains(query))
                .limit(limit)
                .map(product -> product.getId())
                .toList();
    }
}
