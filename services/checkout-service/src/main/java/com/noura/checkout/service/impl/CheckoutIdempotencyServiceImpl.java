package com.noura.checkout.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noura.checkout.domain.entity.CheckoutRequestRecord;
import com.noura.checkout.domain.enums.CheckoutRequestStatus;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderRequest;
import com.noura.checkout.dto.checkout.CheckoutPlaceOrderResponse;
import com.noura.checkout.exception.CheckoutOperationException;
import com.noura.checkout.repository.CheckoutRequestRecordRepository;
import com.noura.checkout.service.CheckoutIdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistent idempotency manager for checkout place-order operations.
 */
@Service
@RequiredArgsConstructor
public class CheckoutIdempotencyServiceImpl implements CheckoutIdempotencyService {

    private final CheckoutRequestRecordRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<CheckoutPlaceOrderResponse> tryReplay(String customerRef, String idempotencyKey) {
        return repository.findByCustomerRefAndIdempotencyKey(customerRef, idempotencyKey)
                .filter(record -> record.getStatus() == CheckoutRequestStatus.SUCCEEDED)
                .map(CheckoutRequestRecord::getResponsePayloadJson)
                .filter(value -> value != null && !value.isBlank())
                .map(this::deserializeResponse)
                .map(this::asReplayResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UUID beginProcessing(
            String customerRef,
            String idempotencyKey,
            CheckoutPlaceOrderRequest request,
            String actor
    ) {
        CheckoutRequestRecord existing = repository.findByCustomerRefAndIdempotencyKeyForUpdate(customerRef, idempotencyKey)
                .orElse(null);

        if (existing != null) {
            if (existing.getStatus() == CheckoutRequestStatus.PROCESSING) {
                throw new CheckoutOperationException(
                        HttpStatus.CONFLICT,
                        "IDEMPOTENCY_IN_PROGRESS",
                        "Another checkout request with this idempotency key is still processing"
                );
            }
            if (existing.getStatus() == CheckoutRequestStatus.SUCCEEDED) {
                throw new CheckoutOperationException(
                        HttpStatus.CONFLICT,
                        "IDEMPOTENCY_ALREADY_COMPLETED",
                        "Checkout request with this idempotency key is already completed"
                );
            }
            return resetToProcessing(existing, request, actor).getId();
        }

        CheckoutRequestRecord created = new CheckoutRequestRecord();
        created.setCustomerRef(customerRef);
        created.setIdempotencyKey(idempotencyKey);
        created.setStatus(CheckoutRequestStatus.PROCESSING);
        created.setRequestPayloadJson(toJsonOrNull(request));
        created.setCreatedBy(actor);
        created.setUpdatedBy(actor);

        try {
            return repository.saveAndFlush(created).getId();
        } catch (DataIntegrityViolationException ex) {
            CheckoutRequestRecord concurrent = repository.findByCustomerRefAndIdempotencyKeyForUpdate(customerRef, idempotencyKey)
                    .orElseThrow(() -> ex);
            if (concurrent.getStatus() == CheckoutRequestStatus.PROCESSING) {
                throw new CheckoutOperationException(
                        HttpStatus.CONFLICT,
                        "IDEMPOTENCY_IN_PROGRESS",
                        "Another checkout request with this idempotency key is still processing"
                );
            }
            if (concurrent.getStatus() == CheckoutRequestStatus.SUCCEEDED) {
                throw new CheckoutOperationException(
                        HttpStatus.CONFLICT,
                        "IDEMPOTENCY_ALREADY_COMPLETED",
                        "Checkout request with this idempotency key is already completed"
                );
            }
            return resetToProcessing(concurrent, request, actor).getId();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void markSuccess(UUID recordId, CheckoutPlaceOrderResponse response, String actor) {
        CheckoutRequestRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found: " + recordId));
        record.setStatus(CheckoutRequestStatus.SUCCEEDED);
        record.setOrderId(response.order().orderId());
        record.setResponsePayloadJson(toJsonOrNull(response));
        record.setFailureCode(null);
        record.setFailureMessage(null);
        record.setUpdatedBy(actor);
        repository.save(record);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void markFailure(UUID recordId, String errorCode, String errorMessage, String actor) {
        CheckoutRequestRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found: " + recordId));
        record.setStatus(CheckoutRequestStatus.FAILED);
        record.setFailureCode(trimToNull(errorCode));
        record.setFailureMessage(trimToNull(errorMessage));
        record.setUpdatedBy(actor);
        repository.save(record);
    }

    /**
     * Resets an existing idempotency record into processing state for retry.
     *
     * @param record existing idempotency record
     * @param request request payload
     * @param actor actor identifier
     * @return persisted idempotency record
     */
    private CheckoutRequestRecord resetToProcessing(
            CheckoutRequestRecord record,
            CheckoutPlaceOrderRequest request,
            String actor
    ) {
        record.setStatus(CheckoutRequestStatus.PROCESSING);
        record.setOrderId(null);
        record.setRequestPayloadJson(toJsonOrNull(request));
        record.setResponsePayloadJson(null);
        record.setFailureCode(null);
        record.setFailureMessage(null);
        if (record.getCreatedBy() == null || record.getCreatedBy().isBlank()) {
            record.setCreatedBy(actor);
        }
        record.setUpdatedBy(actor);
        return repository.save(record);
    }

    /**
     * Serializes a value into JSON.
     *
     * @param value value to serialize
     * @return JSON string, or {@code null} if serialization fails
     */
    private String toJsonOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    /**
     * Deserializes checkout place-order response from JSON.
     *
     * @param payload JSON payload
     * @return deserialized response
     */
    private CheckoutPlaceOrderResponse deserializeResponse(String payload) {
        try {
            return objectMapper.readValue(payload, CheckoutPlaceOrderResponse.class);
        } catch (JsonProcessingException ex) {
            throw new CheckoutOperationException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "IDEMPOTENCY_PAYLOAD_INVALID",
                    "Stored idempotency response payload is invalid"
            );
        }
    }

    /**
     * Marks replayed idempotency responses explicitly.
     *
     * @param response original response payload
     * @return replay response payload
     */
    private CheckoutPlaceOrderResponse asReplayResponse(CheckoutPlaceOrderResponse response) {
        return new CheckoutPlaceOrderResponse(
                response.order(),
                response.payment(),
                response.reservedStock(),
                response.idempotencyKey(),
                true,
                response.placedAt(),
                response.summaryMessage()
        );
    }

    /**
     * Trims input and normalizes blanks to {@code null}.
     *
     * @param value input value
     * @return trimmed value or {@code null}
     */
    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
