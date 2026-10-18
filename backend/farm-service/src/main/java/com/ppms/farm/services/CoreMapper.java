package com.ppms.farm.services;
import com.ppms.farm.domain.*;
import com.ppms.farm.dtos.CoreDtos.*;

final class CoreMapper {
 private CoreMapper() {}
 static FarmResponse farm(Farm x){return new FarmResponse(x.getId(),x.getCode(),x.getName(),x.getAddress(),x.getCounty(),x.isActive(),x.getCreatedAt(),x.getUpdatedAt());}
 static HouseResponse house(House x){return new HouseResponse(x.getId(),x.getFarmId(),x.getCode(),x.getName(),x.getCapacity(),x.getAreaM2(),x.isActive());}
 static SiloResponse silo(Silo x){return new SiloResponse(x.getId(),x.getFarmId(),x.getCode(),x.getName(),x.getCapacityKg());}
 static LotResponse lot(Lot x){return new LotResponse(x.getId(),x.getFarmId(),x.getHouseId(),x.getBreedCode(),x.getChickBatchId(),x.getArrivalDate(),x.getFemaleCount(),x.getMaleCount(),x.getStatus(),x.getExpectedCatchDate());}
 static DailySheetResponse sheet(DailySheet x){return new DailySheetResponse(x.getId(),x.getLotId(),x.getDate(),x.getMortality(),x.getFeedKg(),x.getWaterLiters(),x.getAvgWeightG(),x.getTemperatureMin(),x.getTemperatureMax(),x.getHumidityPct(),x.getComments());}
 static FarmDeliveryResponse delivery(FarmDelivery x){return new FarmDeliveryResponse(x.getId(),x.getLotId(),x.getDispatchDate(),x.getQuantity(),x.getAvgWeightKg(),x.getFciRef());}
}
