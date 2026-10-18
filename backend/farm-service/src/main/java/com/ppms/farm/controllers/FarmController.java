package com.ppms.farm.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import com.ppms.farm.dtos.CoreDtos.*;
import com.ppms.farm.services.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@PreAuthorize("@scope.canRead('FARM_MANAGER')")
@RestController
@RequestMapping("/api/v1/farm")
@RequiredArgsConstructor
public class FarmController {
 private final FarmService farms; private final HouseService houses; private final SiloService silos; private final LotService lots;
 private final DailySheetService sheets; private final FarmDeliveryService deliveries;

 @GetMapping("/farms") public List<FarmResponse> farms(){return farms.list();}
 @GetMapping("/farms/{farmId}") public FarmResponse farm(@PathVariable UUID farmId){return farms.get(farmId);}
 @PreAuthorize("@scope.canWrite('FARM_MANAGER')")
 @PostMapping("/farms") @ResponseStatus(HttpStatus.CREATED) public FarmResponse createFarm(@Valid @RequestBody FarmRequest request){return farms.create(request);}

 @GetMapping("/farms/{farmId}/houses") public List<HouseResponse> houses(@PathVariable UUID farmId){return houses.list(farmId);}
 @PreAuthorize("@scope.canWrite('FARM_MANAGER')")
 @PostMapping("/farms/{farmId}/houses") @ResponseStatus(HttpStatus.CREATED) public HouseResponse createHouse(@PathVariable UUID farmId,@Valid @RequestBody HouseRequest request){return houses.create(farmId,request);}

 @GetMapping("/farms/{farmId}/silos") public List<SiloResponse> silos(@PathVariable UUID farmId){return silos.list(farmId);}
 @PreAuthorize("@scope.canWrite('FARM_MANAGER')")
 @PostMapping("/farms/{farmId}/silos") @ResponseStatus(HttpStatus.CREATED) public SiloResponse createSilo(@PathVariable UUID farmId,@Valid @RequestBody SiloRequest request){return silos.create(farmId,request);}

 @GetMapping("/farms/{farmId}/lots") public List<LotResponse> lots(@PathVariable UUID farmId){return lots.list(farmId);}
 @PreAuthorize("@scope.canWrite('FARM_MANAGER')")
 @PostMapping("/farms/{farmId}/lots") @ResponseStatus(HttpStatus.CREATED) public LotResponse createLot(@PathVariable UUID farmId,@Valid @RequestBody LotRequest request){return lots.create(farmId,request);}
 @PreAuthorize("@scope.canWrite('FARM_MANAGER')")
 @PostMapping("/lots/{lotId}/close") public LotResponse closeLot(@PathVariable UUID lotId){return lots.close(lotId);}

 @GetMapping("/lots/{lotId}/daily-sheets") public List<DailySheetResponse> sheets(@PathVariable UUID lotId){return sheets.list(lotId);}
 @PreAuthorize("@scope.canWrite('FARM_MANAGER')")
 @PostMapping("/lots/{lotId}/daily-sheets") @ResponseStatus(HttpStatus.CREATED) public DailySheetResponse recordSheet(@PathVariable UUID lotId,@Valid @RequestBody DailySheetRequest request){return sheets.record(lotId,request);}

 @GetMapping("/lots/{lotId}/deliveries") public List<FarmDeliveryResponse> deliveries(@PathVariable UUID lotId){return deliveries.list(lotId);}
 @PreAuthorize("@scope.canWrite('FARM_MANAGER')")
 @PostMapping("/lots/{lotId}/deliveries") @ResponseStatus(HttpStatus.CREATED) public FarmDeliveryResponse recordDelivery(@PathVariable UUID lotId,@Valid @RequestBody FarmDeliveryRequest request){return deliveries.record(lotId,request);}
}
