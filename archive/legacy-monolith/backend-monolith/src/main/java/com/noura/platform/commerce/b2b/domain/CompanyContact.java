package com.noura.platform.commerce.b2b.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Contact person at a B2B company.
 * Multiple contacts can exist per company with different roles.
 */
@Entity
@Table(name = "b2b_company_contacts")
public class CompanyContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Can link to customer account for self-service
    @Column(name = "customer_account_id")
    private Long customerAccountId;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 100)
    private String jobTitle;

    @Column(length = 100)
    private String department;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String mobilePhone;

    // Role flags
    @Column(nullable = false)
    private boolean isPrimary = false;

    @Column(nullable = false)
    private boolean canPlaceOrders = true;

    @Column(nullable = false)
    private boolean canViewInvoices = true;

    @Column(nullable = false)
    private boolean canApproveOrders = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // === Computed ===

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // === Getters and Setters ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Long getCustomerAccountId() {
        return customerAccountId;
    }

    public void setCustomerAccountId(Long customerAccountId) {
        this.customerAccountId = customerAccountId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public boolean isCanPlaceOrders() {
        return canPlaceOrders;
    }

    public void setCanPlaceOrders(boolean canPlaceOrders) {
        this.canPlaceOrders = canPlaceOrders;
    }

    public boolean isCanViewInvoices() {
        return canViewInvoices;
    }

    public void setCanViewInvoices(boolean canViewInvoices) {
        this.canViewInvoices = canViewInvoices;
    }

    public boolean isCanApproveOrders() {
        return canApproveOrders;
    }

    public void setCanApproveOrders(boolean canApproveOrders) {
        this.canApproveOrders = canApproveOrders;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
