package com.example.slaughterhouse.application.service;

import com.example.slaughterhouse.domain.entities.DeliveryNotice;
import com.example.slaughterhouse.domain.entities.SlaughterLot;
import com.example.slaughterhouse.domain.entities.SlaughterhouseUser;
import com.example.slaughterhouse.domain.enums.ReceptionStatus;
import com.example.slaughterhouse.infrastructure.persistance.repository.DeliveryNoticeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DeliveryNoticeService {

    private final DeliveryNoticeRepository repository;

    public DeliveryNoticeService(DeliveryNoticeRepository repository) {
        this.repository = repository;
    }

    public DeliveryNotice createDeliveryNotice(String externalSystemId,
                                               String farmOrigin,
                                               String lotNumberFromFarm,
                                               LocalDate scheduledDeliveryDate,
                                               Integer estimatedQuantity,
                                               Float averageWeight,
                                               String breed,
                                               Integer averageAgeInDays,
                                               String transportDetails,
                                               String vehiclePlate,
                                               String driverInfo) {

        DeliveryNotice notice = new DeliveryNotice();
        notice.setExternalSystemId(externalSystemId);
        notice.setFarmOrigin(farmOrigin);
        notice.setLotNumberFromFarm(lotNumberFromFarm);
        notice.setScheduledDeliveryDate(scheduledDeliveryDate);
        notice.setEstimatedQuantity(estimatedQuantity);
        notice.setAverageWeight(averageWeight);
        notice.setBreed(breed);
        notice.setAverageAgeInDays(averageAgeInDays);
        notice.setTransportDetails(transportDetails);
        notice.setVehiclePlate(vehiclePlate);
        notice.setDriverInfo(driverInfo);
        notice.setReceptionStatus(ReceptionStatus.PENDING);
        notice.setIsActive(true);

        return repository.save(notice);
    }

    /**
     * Marks all delivery notices as received for a given lot and calculates total received quantity.
     */
    public int receiveDeliveryNotices(List<DeliveryNotice> notices, SlaughterhouseUser employee) {
        int totalReceived = 0;

        for (DeliveryNotice notice : notices) {
            if (!notice.isReceived()) {
                notice.markAsReceived(employee);
                repository.save(notice);
                totalReceived += notice.getEstimatedQuantity();
            }
        }

        return totalReceived;
    }
}
