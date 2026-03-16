package com.noura.platform.commerce.b2b.application;

import com.noura.platform.commerce.b2b.domain.*;
import com.noura.platform.commerce.b2b.infrastructure.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for B2B company and pricing operations.
 */
@Service
@Transactional
public class B2BService {
    private final CompanyRepo companyRepo;
    private final PriceListRepo priceListRepo;
    private final PriceListItemRepo priceListItemRepo;
    private final B2BPurchaseOrderRepo purchaseOrderRepo;

    public B2BService(
            CompanyRepo companyRepo,
            PriceListRepo priceListRepo,
            PriceListItemRepo priceListItemRepo,
            B2BPurchaseOrderRepo purchaseOrderRepo) {
        this.companyRepo = companyRepo;
        this.priceListRepo = priceListRepo;
        this.priceListItemRepo = priceListItemRepo;
        this.purchaseOrderRepo = purchaseOrderRepo;
    }

    // === Company Management ===

    public Company createCompany(Company company) {
        company.setStatus(CompanyStatus.PENDING_APPROVAL);
        return companyRepo.save(company);
    }

    public Company approveCompany(Long companyId) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        company.setStatus(CompanyStatus.ACTIVE);
        company.setApprovedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        return companyRepo.save(company);
    }

    public Company suspendCompany(Long companyId) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        company.setStatus(CompanyStatus.SUSPENDED);
        company.setUpdatedAt(LocalDateTime.now());
        return companyRepo.save(company);
    }

    @Transactional(readOnly = true)
    public Optional<Company> findCompanyById(Long id) {
        return companyRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<Company> findCompaniesByStatus(CompanyStatus status, Pageable pageable) {
        return companyRepo.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Company> searchCompanies(String query, Pageable pageable) {
        return companyRepo.searchByName(query, pageable);
    }

    // === Pricing ===

    /**
     * Get the effective price for a product for a given company.
     */
    @Transactional(readOnly = true)
    public BigDecimal getEffectivePrice(Long companyId, Long productId, Long variantId, int quantity, BigDecimal standardPrice) {
        Company company = companyRepo.findById(companyId).orElse(null);
        if (company == null) {
            return standardPrice;
        }

        // Check company's assigned price list
        if (company.getPriceList() != null && company.getPriceList().isValid()) {
            BigDecimal listPrice = getPriceFromList(company.getPriceList().getId(), productId, variantId, quantity, standardPrice);
            if (listPrice != null) {
                return applyCompanyDiscount(listPrice, company);
            }
        }

        return applyCompanyDiscount(standardPrice, company);
    }

    private BigDecimal getPriceFromList(Long priceListId, Long productId, Long variantId, int quantity, BigDecimal standardPrice) {
        List<PriceListItem> items = priceListItemRepo.findApplicableItems(priceListId, productId, variantId, quantity);

        if (items.isEmpty()) {
            return null;
        }

        PriceListItem item = items.get(0); // Best match (highest min quantity that applies)

        return switch (item.getPriceType()) {
            case FIXED -> item.getFixedPrice();
            case DISCOUNT_PERCENT -> standardPrice.multiply(
                    BigDecimal.ONE.subtract(item.getDiscountPercent().divide(BigDecimal.valueOf(100)))
            );
            case DISCOUNT_AMOUNT -> standardPrice.subtract(item.getFixedPrice());
        };
    }

    private BigDecimal applyCompanyDiscount(BigDecimal price, Company company) {
        if (company.getDiscountPercent() != null && company.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            return price.multiply(
                    BigDecimal.ONE.subtract(company.getDiscountPercent().divide(BigDecimal.valueOf(100)))
            );
        }
        return price;
    }

    // === Purchase Orders ===

    public PurchaseOrder createDraftOrder(Long companyId) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        if (!company.canPlaceOrder()) {
            throw new IllegalStateException("Company cannot place orders: " + company.getStatus());
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(generatePoNumber());
        po.setCompany(company);
        po.setStatus(POStatus.DRAFT);

        return purchaseOrderRepo.save(po);
    }

    public PurchaseOrder submitOrder(Long orderId) {
        PurchaseOrder po = purchaseOrderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (po.getStatus() != POStatus.DRAFT) {
            throw new IllegalStateException("Order cannot be submitted: " + po.getStatus());
        }

        // Check credit
        Company company = po.getCompany();
        if (!company.hasAvailableCredit(po.getTotalAmount())) {
            throw new IllegalStateException("Insufficient credit limit");
        }

        po.setStatus(POStatus.PENDING_APPROVAL);
        po.setSubmittedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());

        return purchaseOrderRepo.save(po);
    }

    public PurchaseOrder approveOrder(Long orderId, Long approverContactId) {
        PurchaseOrder po = purchaseOrderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (po.getStatus() != POStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Order cannot be approved: " + po.getStatus());
        }

        po.setStatus(POStatus.APPROVED);
        po.setApprovedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());

        // Set payment due date based on terms
        Company company = po.getCompany();
        int terms = company.getPaymentTermsDays() != null ? company.getPaymentTermsDays() : 30;
        po.setPaymentDueDate(LocalDate.now().plusDays(terms));

        // Update company balance
        company.setCurrentBalance(company.getCurrentBalance().add(po.getTotalAmount()));
        companyRepo.save(company);

        return purchaseOrderRepo.save(po);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrder> findOrdersByCompany(Long companyId, Pageable pageable) {
        return purchaseOrderRepo.findByCompanyId(companyId, pageable);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> findOverdueOrders() {
        return purchaseOrderRepo.findOverdueOrders();
    }

    private String generatePoNumber() {
        return "PO-" + System.currentTimeMillis();
    }

    // === Additional methods for controller ===

    public Company updateCompany(Company company) {
        Company existing = companyRepo.findById(company.getId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        company.setUpdatedAt(LocalDateTime.now());
        return companyRepo.save(company);
    }

    @Transactional(readOnly = true)
    public Page<Company> findCompanies(CompanyStatus status, Pageable pageable) {
        if (status != null) {
            return companyRepo.findByStatus(status, pageable);
        }
        return companyRepo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<CompanyContact> getCompanyContacts(Long companyId) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        return company.getContacts();
    }

    public CompanyContact addContact(Long companyId, CompanyContact contact) {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        contact.setCompany(company);
        company.getContacts().add(contact);
        companyRepo.save(company);
        return contact;
    }

    @Transactional(readOnly = true)
    public List<PriceList> findPriceLists(boolean activeOnly) {
        if (activeOnly) {
            return priceListRepo.findByActiveTrue();
        }
        return priceListRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<PriceList> findPriceListById(Long id) {
        return priceListRepo.findById(id);
    }

    public PriceList createPriceList(PriceList priceList) {
        return priceListRepo.save(priceList);
    }

    public PriceListItem addPriceListItem(Long priceListId, PriceListItem item) {
        PriceList priceList = priceListRepo.findById(priceListId)
                .orElseThrow(() -> new IllegalArgumentException("Price list not found"));
        item.setPriceList(priceList);
        return priceListItemRepo.save(item);
    }

    /**
     * Get effective price (uses standard catalog price lookup).
     */
    @Transactional(readOnly = true)
    public BigDecimal getEffectivePrice(Long companyId, Long productId, Long variantId, int quantity) {
        // Default to 0 if no standard price lookup available
        // In real implementation, fetch from Product entity
        BigDecimal standardPrice = BigDecimal.valueOf(100); // Placeholder
        return getEffectivePrice(companyId, productId, variantId, quantity, standardPrice);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrder> findPurchaseOrders(Long companyId, POStatus status, Pageable pageable) {
        if (companyId != null && status != null) {
            return purchaseOrderRepo.findByCompanyIdAndStatus(companyId, status, pageable);
        } else if (companyId != null) {
            return purchaseOrderRepo.findByCompanyId(companyId, pageable);
        } else if (status != null) {
            return purchaseOrderRepo.findByStatus(status, pageable);
        }
        return purchaseOrderRepo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<PurchaseOrder> findPurchaseOrderById(Long id) {
        return purchaseOrderRepo.findById(id);
    }

    public PurchaseOrder createDraftOrder(Long companyId, String customerPoNumber) {
        PurchaseOrder po = createDraftOrder(companyId);
        po.setCustomerReference(customerPoNumber);
        return purchaseOrderRepo.save(po);
    }

    public PurchaseOrderItem addOrderItem(Long orderId, Long productId, Long variantId, int quantity) {
        PurchaseOrder po = purchaseOrderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (po.getStatus() != POStatus.DRAFT) {
            throw new IllegalStateException("Cannot add items to non-draft order");
        }

        BigDecimal unitPrice = getEffectivePrice(po.getCompany().getId(), productId, variantId, quantity);

        PurchaseOrderItem item = new PurchaseOrderItem();
        item.setPurchaseOrder(po);
        item.setProductId(productId);
        item.setVariantId(variantId);
        item.setQuantity(BigDecimal.valueOf(quantity));
        item.setUnitPrice(unitPrice);
        item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        item.setProductName("Product " + productId); // Placeholder - fetch from Product entity

        po.getItems().add(item);
        recalculateOrderTotals(po);
        purchaseOrderRepo.save(po);

        return item;
    }

    public PurchaseOrder approveOrder(Long orderId, String approvedBy) {
        PurchaseOrder po = purchaseOrderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (po.getStatus() != POStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Order cannot be approved: " + po.getStatus());
        }

        po.setStatus(POStatus.APPROVED);
        po.setApprovedAt(LocalDateTime.now());
        po.setInternalNotes("Approved by: " + approvedBy);
        po.setUpdatedAt(LocalDateTime.now());

        Company company = po.getCompany();
        int terms = company.getPaymentTermsDays() != null ? company.getPaymentTermsDays() : 30;
        po.setPaymentDueDate(LocalDate.now().plusDays(terms));

        company.setCurrentBalance(company.getCurrentBalance().add(po.getTotalAmount()));
        companyRepo.save(company);

        return purchaseOrderRepo.save(po);
    }

    public PurchaseOrder rejectOrder(Long orderId, String reason) {
        PurchaseOrder po = purchaseOrderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (po.getStatus() != POStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Order cannot be rejected: " + po.getStatus());
        }

        po.setStatus(POStatus.REJECTED);
        po.setNotes(reason);
        po.setUpdatedAt(LocalDateTime.now());

        return purchaseOrderRepo.save(po);
    }

    public PurchaseOrder cancelOrder(Long orderId) {
        PurchaseOrder po = purchaseOrderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (po.getStatus() == POStatus.SHIPPED
            || po.getStatus() == POStatus.CLOSED
            || po.getStatus() == POStatus.CANCELLED) {
            throw new IllegalStateException("Order cannot be cancelled: " + po.getStatus());
        }

        po.setStatus(POStatus.CANCELLED);
        po.setUpdatedAt(LocalDateTime.now());

        return purchaseOrderRepo.save(po);
    }

    private void recalculateOrderTotals(PurchaseOrder po) {
        BigDecimal subtotal = po.getItems().stream()
                .map(PurchaseOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        po.setSubtotal(subtotal);
        po.setTotalAmount(subtotal.add(po.getTaxAmount() != null ? po.getTaxAmount() : BigDecimal.ZERO)
                .add(po.getShippingAmount() != null ? po.getShippingAmount() : BigDecimal.ZERO));
    }
}
