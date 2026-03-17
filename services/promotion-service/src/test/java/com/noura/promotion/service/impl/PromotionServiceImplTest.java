package com.noura.promotion.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.promotion.domain.entity.PromotionApplicationRecord;
import com.noura.promotion.domain.entity.PromotionRecord;
import com.noura.promotion.domain.enums.PromotionApplicableEntityType;
import com.noura.promotion.domain.enums.PromotionType;
import com.noura.promotion.dto.promotion.PromotionApplicationItemRequest;
import com.noura.promotion.dto.promotion.PromotionEvaluationItemRequest;
import com.noura.promotion.dto.promotion.PromotionEvaluationRequest;
import com.noura.promotion.dto.promotion.PromotionEvaluationResponse;
import com.noura.promotion.dto.promotion.PromotionResponse;
import com.noura.promotion.dto.promotion.PromotionUpsertRequest;
import com.noura.promotion.dto.promotion.PromotionValidationRequest;
import com.noura.promotion.dto.promotion.PromotionValidationResponse;
import com.noura.promotion.exception.PromotionOperationException;
import com.noura.promotion.repository.PromotionApplicationRecordRepository;
import com.noura.promotion.repository.PromotionRecordRepository;
import com.noura.promotion.service.model.PromotionRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PromotionServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock
    private PromotionRecordRepository promotionRecordRepository;

    @Mock
    private PromotionApplicationRecordRepository promotionApplicationRecordRepository;

    private PromotionServiceImpl promotionService;

    /**
     * Initializes the service under test before each test case.
     */
    @BeforeEach
    void setUp() {
        promotionService = new PromotionServiceImpl(
                promotionRecordRepository,
                promotionApplicationRecordRepository,
                new ObjectMapper()
        );
    }

    /**
     * Verifies promotion creation persists records and application mappings for authorized actors.
     */
    @Test
    void shouldCreatePromotionForAuthorizedActor() {
        UUID promotionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID productId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        when(promotionRecordRepository.findAll()).thenReturn(List.of());
        when(promotionRecordRepository.save(any(PromotionRecord.class))).thenAnswer(invocation -> {
            PromotionRecord promotion = invocation.getArgument(0, PromotionRecord.class);
            promotion.setId(promotionId);
            promotion.setCreatedAt(Instant.now());
            promotion.setUpdatedAt(Instant.now());
            return promotion;
        });
        when(promotionApplicationRecordRepository.save(any(PromotionApplicationRecord.class))).thenAnswer(invocation -> {
            PromotionApplicationRecord application = invocation.getArgument(0, PromotionApplicationRecord.class);
            application.setId(UUID.randomUUID());
            application.setCreatedAt(Instant.now());
            application.setUpdatedAt(Instant.now());
            return application;
        });

        PromotionResponse response = promotionService.createPromotion(
                new PromotionRequestContext("admin-user", null, Set.of("ADMIN"), false),
                new PromotionUpsertRequest(
                        "Spring Sale",
                        PromotionType.PERCENTAGE,
                        "SPRING-SALE",
                        "Ten percent off selected items",
                        "SAVE10",
                        Map.of("percent", 10),
                        Instant.parse("2026-03-17T00:00:00Z"),
                        Instant.parse("2026-03-31T23:59:59Z"),
                        true,
                        true,
                        50,
                        1000,
                        null,
                        "vip",
                        false,
                        List.of(new PromotionApplicationItemRequest(PromotionApplicableEntityType.PRODUCT, productId))
                )
        );

        Assertions.assertEquals(promotionId, response.id());
        Assertions.assertEquals("SPRING-SALE", response.code());
        Assertions.assertEquals("SAVE10", response.couponCode());
        Assertions.assertEquals(new BigDecimal("10.00"), response.discountPercent());
        Assertions.assertEquals(1, response.applications().size());
    }

    /**
     * Verifies unauthorized actors cannot mutate promotions.
     */
    @Test
    void shouldRejectPromotionMutationWhenActorCannotManage() {
        PromotionOperationException exception = Assertions.assertThrows(
                PromotionOperationException.class,
                () -> promotionService.createPromotion(
                        new PromotionRequestContext("customer-1", null, Set.of("CUSTOMER"), false),
                        new PromotionUpsertRequest(
                                "Promo",
                                PromotionType.FIXED,
                                "PROMO",
                                null,
                                "PROMO",
                                Map.of("amount", 5),
                                null,
                                null,
                                true,
                                true,
                                0,
                                null,
                                null,
                                null,
                                false,
                                List.of()
                        )
                )
        );

        Assertions.assertEquals("PROMOTION_FORBIDDEN", exception.getCode());
        verify(promotionRecordRepository, never()).save(any(PromotionRecord.class));
    }

    /**
     * Verifies evaluation applies automatic and promo-code promotions deterministically in priority order.
     */
    @Test
    void shouldEvaluateAutomaticAndCodeBasedPromotions() {
        PromotionRecord automaticPromotion = promotion(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Automatic VIP Discount",
                null,
                null,
                PromotionType.PERCENTAGE,
                Map.of("percent", 10),
                10
        );
        automaticPromotion.setCustomerSegment("vip");

        PromotionRecord couponPromotion = promotion(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "Coupon Discount",
                "COUPON-5",
                "SAVE5",
                PromotionType.FIXED,
                Map.of("amount", 5),
                20
        );

        when(promotionRecordRepository.findByActiveTrueAndArchivedFalse()).thenReturn(List.of(automaticPromotion, couponPromotion));
        when(promotionApplicationRecordRepository.findByPromotionIdIn(any())).thenReturn(List.of());

        PromotionEvaluationResponse response = promotionService.evaluateCartDiscount(
                new PromotionEvaluationRequest(
                        new BigDecimal("50.00"),
                        "SAVE5",
                        "VIP",
                        List.of(new PromotionEvaluationItemRequest(null, null, null, 1, new BigDecimal("50.00")))
                )
        );

        Assertions.assertEquals(new BigDecimal("10.00"), response.discountAmount());
        Assertions.assertEquals(new BigDecimal("40.00"), response.discountedSubtotal());
        Assertions.assertEquals(List.of(couponPromotion.getId(), automaticPromotion.getId()), response.appliedPromotionIds());
    }

    /**
     * Verifies promo-code validation surfaces ineligible future windows with a stable reason.
     */
    @Test
    void shouldExplainValidationFailureForFuturePromotion() {
        PromotionRecord futurePromotion = promotion(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "Future Discount",
                "FUTURE10",
                "FUTURE10",
                PromotionType.PERCENTAGE,
                Map.of("percent", 10),
                5
        );
        futurePromotion.setStartDate(Instant.now().plusSeconds(3600));

        when(promotionRecordRepository.findAll()).thenReturn(List.of(futurePromotion));
        when(promotionApplicationRecordRepository.findByPromotionIdIn(List.of(futurePromotion.getId()))).thenReturn(List.of());

        PromotionValidationResponse response = promotionService.validatePromoCode(
                new PromotionValidationRequest(
                        "future10",
                        new BigDecimal("50.00"),
                        null,
                        List.of(new PromotionEvaluationItemRequest(null, null, null, 1, new BigDecimal("50.00")))
                )
        );

        Assertions.assertTrue(response.valid());
        Assertions.assertFalse(response.eligible());
        Assertions.assertEquals("PROMOTION_NOT_STARTED", response.reasonCode());
        Assertions.assertNull(response.evaluation());
    }

    /**
     * Verifies update requests reject ambiguous code reuse across code and coupon namespaces.
     */
    @Test
    void shouldRejectDuplicateIdentifierSpace() {
        UUID existingId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        UUID updatingId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        PromotionRecord existing = promotion(existingId, "Existing", null, "SAVE10", PromotionType.FIXED, Map.of("amount", 5), 0);
        PromotionRecord updating = promotion(updatingId, "Updating", "OLD", null, PromotionType.FIXED, Map.of("amount", 5), 0);

        when(promotionRecordRepository.findById(updatingId)).thenReturn(Optional.of(updating));
        when(promotionRecordRepository.findAll()).thenReturn(List.of(existing, updating));

        PromotionOperationException exception = Assertions.assertThrows(
                PromotionOperationException.class,
                () -> promotionService.updatePromotion(
                        new PromotionRequestContext("admin-user", null, Set.of("ADMIN"), false),
                        updatingId,
                        new PromotionUpsertRequest(
                                "Updating",
                                PromotionType.FIXED,
                                "SAVE10",
                                null,
                                null,
                                Map.of("amount", 5),
                                null,
                                null,
                                true,
                                true,
                                0,
                                null,
                                null,
                                null,
                                false,
                                List.of()
                        )
                )
        );

        Assertions.assertEquals("PROMOTION_CODE_EXISTS", exception.getCode());
        verify(promotionRecordRepository, never()).save(any(PromotionRecord.class));
    }

    /**
     * Builds a promotion aggregate with deterministic condition JSON for testing.
     *
     * @param id promotion identifier
     * @param name promotion name
     * @param code business code
     * @param couponCode coupon code
     * @param type promotion type
     * @param conditions condition map
     * @param priority priority
     * @return promotion aggregate
     */
    private PromotionRecord promotion(
            UUID id,
            String name,
            String code,
            String couponCode,
            PromotionType type,
            Map<String, Object> conditions,
            int priority
    ) {
        PromotionRecord promotion = new PromotionRecord();
        promotion.setId(id);
        promotion.setName(name);
        promotion.setCode(code);
        promotion.setCouponCode(couponCode);
        promotion.setType(type);
        promotion.setConditionsJson(writeJson(conditions));
        promotion.setPriority(priority);
        promotion.setActive(true);
        promotion.setArchived(false);
        promotion.setStackable(true);
        promotion.setCreatedAt(Instant.now());
        promotion.setUpdatedAt(Instant.now());
        return promotion;
    }

    /**
     * Serializes condition JSON for promotion test records.
     *
     * @param value source condition map
     * @return JSON string
     */
    private String writeJson(Map<String, Object> value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
