package com.ppms.admin.api.catalog;

import org.springframework.security.access.prepost.PreAuthorize;
import com.ppms.admin.service.UnitOfMeasureService;
import com.ppms.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/units")
public class UnitOfMeasureController {

    private final UnitOfMeasureService unitService;

    public UnitOfMeasureController(UnitOfMeasureService unitService) {
        this.unitService = unitService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UnitOfMeasureDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(unitService.list(page, size, sort, activeOnly));
    }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping
    public ResponseEntity<UnitOfMeasureDto> create(@Valid @RequestBody UnitOfMeasureRequest request) {
        UnitOfMeasureDto created = unitService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/units/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitOfMeasureDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(unitService.get(id));
    }

    @PreAuthorize("@scope.canWrite()")
    @PutMapping("/{id}")
    public ResponseEntity<UnitOfMeasureDto> update(@PathVariable UUID id, @Valid @RequestBody UnitOfMeasureRequest request) {
        return ResponseEntity.ok(unitService.update(id, request));
    }

    @PreAuthorize("@scope.canWrite()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        unitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
