package com.noura.platform.inventory.audit;

import com.noura.platform.inventory.domain.AuditLog;
import com.noura.platform.inventory.domain.BaseUuidEntity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;

public class InventoryAuditEntityListener {

    @PostLoad
    public void afterLoad(Object entity) {
        if (entity instanceof AuditLog || !(entity instanceof BaseUuidEntity auditable)) {
            return;
        }
        auditable.setAuditSnapshot(auditService().serializeEntityState(entity));
    }

    @PostPersist
    public void afterCreate(Object entity) {
        if (entity instanceof AuditLog || !(entity instanceof BaseUuidEntity auditable)) {
            return;
        }
        String afterState = auditService().serializeEntityState(entity);
        auditService().recordEntityChange("CREATED", entity, null, afterState);
        auditable.setAuditSnapshot(afterState);
    }

    @PostUpdate
    public void afterUpdate(Object entity) {
        if (entity instanceof AuditLog || !(entity instanceof BaseUuidEntity auditable)) {
            return;
        }
        String afterState = auditService().serializeEntityState(entity);
        auditService().recordEntityChange("UPDATED", entity, auditable.getAuditSnapshot(), afterState);
        auditable.setAuditSnapshot(afterState);
    }

    @PreRemove
    public void beforeDelete(Object entity) {
        if (entity instanceof AuditLog || !(entity instanceof BaseUuidEntity auditable)) {
            return;
        }
        auditable.setAuditSnapshot(auditService().serializeEntityState(entity));
    }

    @PostRemove
    public void afterDelete(Object entity) {
        if (entity instanceof AuditLog || !(entity instanceof BaseUuidEntity auditable)) {
            return;
        }
        auditService().recordEntityChange("DELETED", entity, auditable.getAuditSnapshot(), null);
    }

    private InventoryAuditService auditService() {
        return InventoryApplicationContextProvider.getBean(InventoryAuditService.class);
    }
}
