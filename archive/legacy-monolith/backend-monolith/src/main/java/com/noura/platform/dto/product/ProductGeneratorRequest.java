package com.noura.platform.dto.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductGeneratorRequest {

    private String name;

    private String category;

    private String brand;

    private String targetAudience;
}
