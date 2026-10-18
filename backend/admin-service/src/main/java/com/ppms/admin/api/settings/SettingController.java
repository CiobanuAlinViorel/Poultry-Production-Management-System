package com.ppms.admin.api.settings;

import com.ppms.admin.service.SettingService;
import com.ppms.shared.domain.ScopeType;
import com.ppms.shared.security.CurrentUser;
import com.ppms.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingController {

    private final SettingService settingService;

    public SettingController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/definitions")
    public ResponseEntity<List<SettingDefinitionDto>> listDefinitions(@RequestParam(required = false) String module) {
        return ResponseEntity.ok(settingService.listDefinitions(module));
    }

    @GetMapping
    public ResponseEntity<List<EffectiveSettingDto>> listEffective(
            @RequestParam ScopeType scopeType,
            @RequestParam(required = false) UUID scopeId) {
        return ResponseEntity.ok(settingService.listEffectiveValues(scopeType, scopeId));
    }

    @GetMapping(value = "/{key}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getEffectiveValue(
            @PathVariable String key,
            @RequestParam ScopeType scopeType,
            @RequestParam(required = false) UUID scopeId) {
        String value = settingService.getEffectiveValue(key, scopeType, scopeId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(value != null ? value : "");
    }

    @GetMapping("/{key}/history")
    public ResponseEntity<List<SettingValueDto>> history(
            @PathVariable String key,
            @RequestParam ScopeType scopeType,
            @RequestParam(required = false) UUID scopeId) {
        return ResponseEntity.ok(settingService.history(key, scopeType, scopeId));
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody SettingUpdateRequest request,
            @CurrentUser UserPrincipal principal) {
        UUID changedBy = principal != null ? principal.getUserId() : null;
        settingService.setValue(key, request.scopeType(), request.scopeId(), request, changedBy);
        return ResponseEntity.noContent().build();
    }
}
