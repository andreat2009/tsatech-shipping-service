package com.newproject.shipping.repository;

import com.newproject.shipping.domain.Shipment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByOrderId(Long orderId);
}
