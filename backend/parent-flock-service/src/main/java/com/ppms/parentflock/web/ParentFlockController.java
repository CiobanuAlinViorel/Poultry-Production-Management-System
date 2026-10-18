package com.ppms.parentflock.web;

import org.springframework.security.access.prepost.PreAuthorize;
import com.ppms.parentflock.dtos.BreederSiteResponse;
import com.ppms.parentflock.dtos.CreateBreederSiteRequest;
import com.ppms.parentflock.dtos.CreateDailySheetRequest;
import com.ppms.parentflock.dtos.CreateEggBatchRequest;
import com.ppms.parentflock.dtos.CreateFlockRequest;
import com.ppms.parentflock.dtos.CreateHouseRequest;
import com.ppms.parentflock.dtos.DailyProductionSheetResponse;
import com.ppms.parentflock.dtos.EggBatchResponse;
import com.ppms.parentflock.dtos.FlockResponse;
import com.ppms.parentflock.dtos.HouseResponse;
import com.ppms.parentflock.service.BreederSiteService;
import com.ppms.parentflock.service.DailyProductionService;
import com.ppms.parentflock.service.EggBatchService;
import com.ppms.parentflock.service.FlockService;
import com.ppms.parentflock.service.HouseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@PreAuthorize("@scope.canRead('FLOCK_SUPERVISOR')")
@RestController
@RequestMapping("/api/v1/parent-flock")
public class ParentFlockController {

    private final BreederSiteService breederSiteService;
    private final HouseService houseService;
    private final FlockService flockService;
    private final DailyProductionService dailyProductionService;
    private final EggBatchService eggBatchService;

    public ParentFlockController(BreederSiteService breederSiteService,
                                 HouseService houseService,
                                 FlockService flockService,
                                 DailyProductionService dailyProductionService,
                                 EggBatchService eggBatchService) {
        this.breederSiteService = breederSiteService;
        this.houseService = houseService;
        this.flockService = flockService;
        this.dailyProductionService = dailyProductionService;
        this.eggBatchService = eggBatchService;
    }

    @PreAuthorize("@scope.canWrite('FLOCK_SUPERVISOR')")
    @PostMapping("/sites")
    @ResponseStatus(HttpStatus.CREATED)
    public BreederSiteResponse createSite(@Valid @RequestBody CreateBreederSiteRequest request) {
        return breederSiteService.create(request);
    }

    @GetMapping("/sites")
    public List<BreederSiteResponse> listSites() {
        return breederSiteService.list();
    }

    @GetMapping("/sites/{siteId}")
    public BreederSiteResponse getSite(@PathVariable UUID siteId) {
        return breederSiteService.get(siteId);
    }

    @PreAuthorize("@scope.canWrite('FLOCK_SUPERVISOR')")
    @PutMapping("/sites/{siteId}")
    public BreederSiteResponse updateSite(@PathVariable UUID siteId,
                                          @Valid @RequestBody CreateBreederSiteRequest request) {
        return breederSiteService.update(siteId, request);
    }

    @PreAuthorize("@scope.canWrite('FLOCK_SUPERVISOR')")
    @PostMapping("/sites/{siteId}/houses")
    @ResponseStatus(HttpStatus.CREATED)
    public HouseResponse createHouse(@PathVariable UUID siteId,
                                      @Valid @RequestBody CreateHouseRequest request) {
        return houseService.create(siteId, request);
    }

    @GetMapping("/sites/{siteId}/houses")
    public List<HouseResponse> listHouses(@PathVariable UUID siteId) {
        return houseService.listBySite(siteId);
    }

    @PreAuthorize("@scope.canWrite('FLOCK_SUPERVISOR')")
    @PostMapping("/sites/{siteId}/flocks")
    @ResponseStatus(HttpStatus.CREATED)
    public FlockResponse createFlock(@PathVariable UUID siteId,
                                      @Valid @RequestBody CreateFlockRequest request) {
        return flockService.create(siteId, request);
    }

    @GetMapping("/sites/{siteId}/flocks")
    public List<FlockResponse> listFlocks(@PathVariable UUID siteId,
                                         @RequestParam(required = false) UUID houseId) {
        if (houseId != null) {
            return flockService.listBySiteAndHouse(siteId, houseId);
        }
        return flockService.listBySite(siteId);
    }

    @PreAuthorize("@scope.canWrite('FLOCK_SUPERVISOR')")
    @PostMapping("/flocks/{flockId}/daily-sheets")
    @ResponseStatus(HttpStatus.CREATED)
    public DailyProductionSheetResponse recordDailySheet(@PathVariable UUID flockId,
                                                         @Valid @RequestBody CreateDailySheetRequest request) {
        return dailyProductionService.recordDailySheet(flockId, request);
    }

    @GetMapping("/flocks/{flockId}/daily-sheets")
    public List<DailyProductionSheetResponse> listDailySheets(@PathVariable UUID flockId) {
        return dailyProductionService.listByFlock(flockId);
    }

    @PreAuthorize("@scope.canWrite('FLOCK_SUPERVISOR')")
    @PostMapping("/egg-batches")
    @ResponseStatus(HttpStatus.CREATED)
    public EggBatchResponse createEggBatch(@Valid @RequestBody CreateEggBatchRequest request) {
        return eggBatchService.create(request);
    }

    @GetMapping("/egg-batches")
    public List<EggBatchResponse> listEggBatches() {
        return eggBatchService.list();
    }
}
