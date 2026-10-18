package com.ppms.admin.api.catalog;

import org.springframework.security.access.prepost.PreAuthorize;
import com.ppms.admin.service.BreedService;
import com.ppms.shared.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/breeds")
public class BreedController {

    private final BreedService breedService;

    public BreedController(BreedService breedService) {
        this.breedService = breedService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<BreedDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(breedService.list(page, size, sort, activeOnly));
    }

    @PreAuthorize("@scope.canWrite()")
    @PostMapping
    public ResponseEntity<BreedDto> create(@Valid @RequestBody BreedRequest request) {
        BreedDto created = breedService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/breeds/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BreedDto> get(@PathVariable UUID id) {
        return ResponseEntity.ok(breedService.get(id));
    }

    @PreAuthorize("@scope.canWrite()")
    @PutMapping("/{id}")
    public ResponseEntity<BreedDto> update(@PathVariable UUID id, @Valid @RequestBody BreedRequest request) {
        return ResponseEntity.ok(breedService.update(id, request));
    }

    @PreAuthorize("@scope.canWrite()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        breedService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
