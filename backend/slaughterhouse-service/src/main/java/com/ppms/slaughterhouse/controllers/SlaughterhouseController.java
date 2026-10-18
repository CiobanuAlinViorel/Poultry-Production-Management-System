package com.ppms.slaughterhouse.controllers;

import com.ppms.slaughterhouse.domain.*;
import com.ppms.slaughterhouse.dtos.CoreDtos.*;
import com.ppms.slaughterhouse.services.SlaughterhouseCoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/slaughterhouse")
@RequiredArgsConstructor
public class SlaughterhouseController {

    private final SlaughterhouseCoreService service;

    @PostMapping("/sites")
    public ResponseEntity<Slaughterhouse> createSite(@RequestBody @Valid SlaughterhouseRequest request) {
        return ResponseEntity.ok(service.createSlaughterhouse(request));
    }

    @GetMapping("/sites")
    public ResponseEntity<List<Slaughterhouse>> listSites() {
        return ResponseEntity.ok(service.listSlaughterhouses());
    }

    @PostMapping("/sites/{id}/lines")
    public ResponseEntity<Line> createLine(@PathVariable UUID id, @RequestBody @Valid LineRequest request) {
        return ResponseEntity.ok(service.createLine(request));
    }

    @GetMapping("/sites/{id}/lines")
    public ResponseEntity<List<Line>> listLines(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listLines(id));
    }

    @PostMapping("/sessions")
    public ResponseEntity<SlaughterSession> createSession(@RequestBody @Valid SlaughterSessionRequest request) {
        return ResponseEntity.ok(service.createSession(request));
    }

    @GetMapping("/sites/{id}/sessions")
    public ResponseEntity<List<SlaughterSession>> listSessions(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listSessions(id));
    }

    @PostMapping("/customers")
    public ResponseEntity<Customer> createCustomer(@RequestBody @Valid CustomerRequest request) {
        return ResponseEntity.ok(service.createCustomer(request));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<Customer>> listCustomers() {
        return ResponseEntity.ok(service.listCustomers());
    }

    @PostMapping("/sales-orders")
    public ResponseEntity<SalesOrder> createSalesOrder(@RequestBody @Valid SalesOrderRequest request) {
        return ResponseEntity.ok(service.createSalesOrder(request));
    }

    @GetMapping("/sales-orders")
    public ResponseEntity<List<SalesOrder>> listSalesOrders() {
        return ResponseEntity.ok(service.listSalesOrders());
    }

    @PostMapping("/dispatch-orders")
    public ResponseEntity<DispatchOrder> createDispatchOrder(@RequestBody @Valid DispatchOrderRequest request) {
        return ResponseEntity.ok(service.createDispatchOrder(request));
    }

    @GetMapping("/dispatch-orders")
    public ResponseEntity<List<DispatchOrder>> listDispatchOrders() {
        return ResponseEntity.ok(service.listDispatchOrders());
    }
}
