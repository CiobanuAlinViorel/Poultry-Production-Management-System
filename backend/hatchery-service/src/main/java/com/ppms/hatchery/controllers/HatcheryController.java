package com.ppms.hatchery.controllers;

import com.ppms.hatchery.domain.*;
import com.ppms.hatchery.dtos.*;
import com.ppms.hatchery.services.EggReceptionService;
import com.ppms.hatchery.services.HatcheryCoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hatchery")
@RequiredArgsConstructor
public class HatcheryController {

    private final HatcheryCoreService hatcheryCoreService;
    private final EggReceptionService eggReceptionService;

    @PostMapping("/sites")
    public ResponseEntity<Hatchery> createHatchery(@RequestBody @Valid HatcheryRequest request) {
        return ResponseEntity.ok(hatcheryCoreService.createHatchery(request));
    }

    @GetMapping("/sites")
    public ResponseEntity<List<Hatchery>> listHatcheries() {
        return ResponseEntity.ok(hatcheryCoreService.listHatcheries());
    }

    @PostMapping("/sites/{hatcheryId}/incubators")
    public ResponseEntity<Incubator> createIncubator(@PathVariable UUID hatcheryId, @RequestBody @Valid IncubatorRequest request) {
        return ResponseEntity.ok(hatcheryCoreService.createIncubator(request));
    }

    @GetMapping("/sites/{hatcheryId}/incubators")
    public ResponseEntity<List<Incubator>> listIncubators(@PathVariable UUID hatcheryId) {
        return ResponseEntity.ok(hatcheryCoreService.listIncubators(hatcheryId));
    }

    @PostMapping("/egg-batches")
    public ResponseEntity<EggBatch> createEggBatch(@RequestBody @Valid EggBatchRequest request) {
        return ResponseEntity.ok(eggReceptionService.receive(request));
    }

    @GetMapping("/sites/{hatcheryId}/egg-batches")
    public ResponseEntity<List<EggBatch>> listEggBatches(@PathVariable UUID hatcheryId) {
        return ResponseEntity.ok(hatcheryCoreService.listEggBatches(hatcheryId));
    }

    @PostMapping("/incubation-batches")
    public ResponseEntity<IncubationBatch> createIncubationBatch(@RequestBody @Valid IncubationBatchRequest request) {
        return ResponseEntity.ok(hatcheryCoreService.createIncubationBatch(request));
    }

    @GetMapping("/sites/{hatcheryId}/incubation-batches")
    public ResponseEntity<List<IncubationBatch>> listIncubationBatches(@PathVariable UUID hatcheryId) {
        return ResponseEntity.ok(hatcheryCoreService.listIncubationBatches(hatcheryId));
    }

    @PostMapping("/chick-batches")
    public ResponseEntity<ChickBatch> createChickBatch(@RequestBody @Valid ChickBatchRequest request) {
        return ResponseEntity.ok(hatcheryCoreService.createChickBatch(request));
    }

    @GetMapping("/sites/{hatcheryId}/chick-batches")
    public ResponseEntity<List<ChickBatch>> listChickBatches(@PathVariable UUID hatcheryId) {
        return ResponseEntity.ok(hatcheryCoreService.listChickBatches(hatcheryId));
    }
}
