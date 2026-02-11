package com.newproject.shipping.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newproject.shipping.service.ShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {
    private static final Logger logger = LoggerFactory.getLogger(PaymentEventListener.class);

    private final ObjectMapper objectMapper;
    private final ShipmentService shipmentService;

    public PaymentEventListener(ObjectMapper objectMapper, ShipmentService shipmentService) {
        this.objectMapper = objectMapper;
        this.shipmentService = shipmentService;
    }

    @KafkaListener(topics = "${shipping.payment-events.topic:payment.events}", groupId = "${spring.application.name}-payment-events")
    public void onPaymentEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("eventType").asText("");
            if (!"PAYMENT_CREATED".equals(eventType) && !"PAYMENT_UPDATED".equals(eventType)) {
                return;
            }

            JsonNode paymentPayload = root.path("payload");
            Long orderId = asLong(paymentPayload.path("orderId"));
            if (orderId == null) {
                logger.warn("Skipping payment event with missing orderId: {}", payload);
                return;
            }

            String paymentStatus = paymentPayload.path("status").asText(null);
            shipmentService.upsertFromPaymentEvent(orderId, paymentStatus);
        } catch (Exception ex) {
            logger.warn("Unable to process payment event: {}", ex.getMessage());
        }
    }

    private Long asLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
