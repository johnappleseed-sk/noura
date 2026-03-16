package com.noura.platform.service.impl;

import com.noura.platform.domain.entity.Promotion;
import com.noura.platform.domain.enums.PromotionType;
import com.noura.platform.dto.pricing.PromotionEvaluationDto;
import com.noura.platform.dto.pricing.PromotionEvaluationItemRequest;
import com.noura.platform.repository.PromotionApplicationRepository;
import com.noura.platform.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionRuleEngineServiceImplTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionApplicationRepository promotionApplicationRepository;

    @InjectMocks
    private PromotionRuleEngineServiceImpl service;

    @Test
    void appliesThresholdDiscountAndFreeShipping() {
        Promotion threshold = new Promotion();
        threshold.setId(UUID.randomUUID());
        threshold.setName("Threshold 10");
        threshold.setCode("SAVE10");
        threshold.setType(PromotionType.CART_THRESHOLD_DISCOUNT);
        threshold.setActive(true);
        threshold.setStackable(true);
        threshold.setPriority(100);
        threshold.setConditions(new LinkedHashMap<>() {{
            put("threshold", 100);
            put("percent", 10);
        }});

        Promotion freeShipping = new Promotion();
        freeShipping.setId(UUID.randomUUID());
        freeShipping.setName("Ship Free");
        freeShipping.setType(PromotionType.FREE_SHIPPING);
        freeShipping.setActive(true);
        freeShipping.setStackable(true);
        freeShipping.setPriority(90);
        freeShipping.setConditions(new LinkedHashMap<>() {{
            put("threshold", 100);
        }});

        when(promotionRepository.findByActiveTrueAndArchivedFalse()).thenReturn(List.of(threshold, freeShipping));
        when(promotionApplicationRepository.findByPromotionId(threshold.getId())).thenReturn(List.of());
        when(promotionApplicationRepository.findByPromotionId(freeShipping.getId())).thenReturn(List.of());

        PromotionEvaluationDto result = service.evaluate(
                new BigDecimal("150.00"),
                null,
                null,
                List.of(new PromotionEvaluationItemRequest(UUID.randomUUID(), null, 1, new BigDecimal("150.00")))
        );

        assertThat(result.discountAmount()).isEqualByComparingTo("15.00");
        assertThat(result.freeShipping()).isTrue();
        assertThat(result.appliedPromotionCodes()).containsExactly("SAVE10", "Ship Free");
    }
}
