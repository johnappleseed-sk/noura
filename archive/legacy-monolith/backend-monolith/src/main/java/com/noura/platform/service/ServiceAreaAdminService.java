package com.noura.platform.service;

import com.noura.platform.domain.enums.ServiceAreaStatus;
import com.noura.platform.domain.enums.ServiceAreaType;
import com.noura.platform.dto.location.ServiceAreaDto;
import com.noura.platform.dto.location.ServiceAreaRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Defines admin operations for governed service-area management.
 */
public interface ServiceAreaAdminService {
    /**
     * Lists governed service areas.
     *
     * @param query The optional free-text filter.
     * @param status The optional status filter.
     * @param type The optional type filter.
     * @param pageable The pagination configuration.
     * @return A paginated service-area result set.
     */
    Page<ServiceAreaDto> list(String query, ServiceAreaStatus status, ServiceAreaType type, Pageable pageable);

    /**
     * Retrieves a single governed service area.
     *
     * @param serviceAreaId The service-area identifier.
     * @return The mapped service-area DTO.
     */
    ServiceAreaDto get(UUID serviceAreaId);

    /**
     * Creates a governed service area.
     *
     * @param request The request payload.
     * @param actor The authenticated actor name.
     * @return The mapped service-area DTO.
     */
    ServiceAreaDto create(ServiceAreaRequest request, String actor);

    /**
     * Updates a governed service area.
     *
     * @param serviceAreaId The service-area identifier.
     * @param request The request payload.
     * @param actor The authenticated actor name.
     * @return The mapped service-area DTO.
     */
    ServiceAreaDto update(UUID serviceAreaId, ServiceAreaRequest request, String actor);

    /**
     * Moves a governed service area to trash.
     *
     * @param serviceAreaId The service-area identifier.
     * @param actor The authenticated actor name.
     */
    void delete(UUID serviceAreaId, String actor);

    /**
     * Restores a governed service area to the active lifecycle state.
     *
     * @param serviceAreaId The service-area identifier.
     * @param actor The authenticated actor name.
     * @return The mapped service-area DTO.
     */
    ServiceAreaDto activate(UUID serviceAreaId, String actor);

    /**
     * Moves a governed service area to the inactive lifecycle state.
     *
     * @param serviceAreaId The service-area identifier.
     * @param actor The authenticated actor name.
     * @return The mapped service-area DTO.
     */
    ServiceAreaDto deactivate(UUID serviceAreaId, String actor);
}
