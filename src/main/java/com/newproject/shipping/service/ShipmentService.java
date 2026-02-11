package com.newproject.shipping.service;

import com.newproject.shipping.domain.Shipment;
import com.newproject.shipping.dto.ShipmentRequest;
import com.newproject.shipping.dto.ShipmentResponse;
import com.newproject.shipping.events.EventPublisher;
import com.newproject.shipping.exception.NotFoundException;
import com.newproject.shipping.repository.ShipmentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final EventPublisher eventPublisher;

    public ShipmentService(ShipmentRepository shipmentRepository, EventPublisher eventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ShipmentResponse create(ShipmentRequest request) {
        Shipment shipment = new Shipment();
        applyRequest(shipment, request);
        OffsetDateTime now = OffsetDateTime.now();
        shipment.setCreatedAt(now);
        shipment.setUpdatedAt(now);
        if (shipment.getStatus() == null) {
            shipment.setStatus("CREATED");
        }

        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish("SHIPMENT_CREATED", "shipment", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public ShipmentResponse update(Long id, ShipmentRequest request) {
        Shipment shipment = shipmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Shipment not found"));

        applyRequest(shipment, request);
        shipment.setUpdatedAt(OffsetDateTime.now());

        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish("SHIPMENT_UPDATED", "shipment", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public ShipmentResponse upsertFromPaymentEvent(Long orderId, String paymentStatus) {
        Shipment shipment = shipmentRepository.findFirstByOrderIdOrderByIdAsc(orderId)
            .orElseGet(Shipment::new);

        boolean created = shipment.getId() == null;
        OffsetDateTime now = OffsetDateTime.now();

        if (created) {
            shipment.setOrderId(orderId);
            shipment.setCarrier("AUTO-KAFKA");
            shipment.setTrackingNumber("PENDING-" + orderId);
            shipment.setCreatedAt(now);
        }

        shipment.setStatus(resolveShipmentStatus(paymentStatus));
        shipment.setUpdatedAt(now);

        Shipment saved = shipmentRepository.save(shipment);
        eventPublisher.publish(created ? "SHIPMENT_CREATED" : "SHIPMENT_UPDATED", "shipment", saved.getId().toString(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ShipmentResponse get(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Shipment not found"));
        return toResponse(shipment);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> list(Long orderId) {
        if (orderId != null) {
            return shipmentRepository.findByOrderId(orderId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        }
        return shipmentRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        Shipment shipment = shipmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Shipment not found"));
        shipmentRepository.delete(shipment);
        eventPublisher.publish("SHIPMENT_CANCELLED", "shipment", id.toString(), null);
    }

    private void applyRequest(Shipment shipment, ShipmentRequest request) {
        shipment.setOrderId(request.getOrderId());
        shipment.setCarrier(request.getCarrier());
        shipment.setTrackingNumber(request.getTrackingNumber());
        if (request.getStatus() != null) {
            shipment.setStatus(request.getStatus());
        }
    }

    private String resolveShipmentStatus(String paymentStatus) {
        if (paymentStatus == null) {
            return "PENDING_PAYMENT";
        }

        if ("PAID".equalsIgnoreCase(paymentStatus) || "SETTLED".equalsIgnoreCase(paymentStatus)) {
            return "READY_TO_SHIP";
        }

        if ("FAILED".equalsIgnoreCase(paymentStatus) || "DECLINED".equalsIgnoreCase(paymentStatus)) {
            return "ON_HOLD";
        }

        return "PENDING_PAYMENT";
    }

    private ShipmentResponse toResponse(Shipment shipment) {
        ShipmentResponse response = new ShipmentResponse();
        response.setId(shipment.getId());
        response.setOrderId(shipment.getOrderId());
        response.setCarrier(shipment.getCarrier());
        response.setTrackingNumber(shipment.getTrackingNumber());
        response.setStatus(shipment.getStatus());
        response.setCreatedAt(shipment.getCreatedAt());
        response.setUpdatedAt(shipment.getUpdatedAt());
        return response;
    }
}
