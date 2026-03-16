package com.noura.platform.service.impl.productgen;

import com.noura.platform.common.exception.NotFoundException;
import com.noura.platform.inventory.domain.Product;
import com.noura.platform.inventory.repository.InventoryProductRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "inventory", name = "enabled", havingValue = "true")
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${inventory.datasource.url:}')")
public class ProductInventoryMirrorService {

    private final InventoryProductRepository inventoryProductRepository;

    @Transactional(transactionManager = "inventoryTransactionManager")
    public void mirror(String inventoryProductId, String description, String barcode, String qrCode) {
        Product inventoryProduct = inventoryProductRepository.findByIdAndDeletedAtIsNull(inventoryProductId)
                .orElseThrow(() -> new NotFoundException("INVENTORY_PRODUCT_NOT_FOUND", "Inventory product not found"));

        if (description != null) {
            inventoryProduct.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        }
        if (barcode != null) {
            inventoryProduct.setBarcodeValue(StringUtils.hasText(barcode) ? barcode.trim() : null);
        }
        if (qrCode != null) {
            inventoryProduct.setQrCodeValue(StringUtils.hasText(qrCode) ? qrCode.trim() : null);
        }
        inventoryProductRepository.save(inventoryProduct);
    }
}
