package com.noura.platform.service;

import com.noura.platform.dto.pricing.PromotionEvaluationDto;
import com.noura.platform.dto.pricing.PromotionEvaluationItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionRuleEngineService {
    PromotionEvaluationDto evaluate(BigDecimal subtotal,
                                    String couponCode,
                                    String customerSegment,
                                    List<PromotionEvaluationItemRequest> items);
}
