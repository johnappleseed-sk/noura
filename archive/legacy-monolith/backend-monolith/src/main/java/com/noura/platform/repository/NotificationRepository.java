package com.noura.platform.repository;

import com.noura.platform.domain.entity.NotificationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationMessage, UUID>, JpaSpecificationExecutor<NotificationMessage> {
}
