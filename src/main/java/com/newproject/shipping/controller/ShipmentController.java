package com.newproject.shipping.controller;

import com.newproject.shipping.dto.ShipmentRequest;
import com.newproject.shipping.dto.ShipmentResponse;
import com.newproject.shipping.service.ShipmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {
    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public List<ShipmentResponse> list(@RequestParam(value = "orderId", required = false) Long orderId) {
        return shipmentService.list(orderId);
    }

    @GetMapping("/{id}")
    public ShipmentResponse get(@PathVariable Long id) {
        return shipmentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse create(@Valid @RequestBody ShipmentRequest request) {
        return shipmentService.create(request);
    }

    @PutMapping("/{id}")
    public ShipmentResponse update(@PathVariable Long id, @Valid @RequestBody ShipmentRequest request) {
        return shipmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        shipmentService.delete(id);
    }
}
