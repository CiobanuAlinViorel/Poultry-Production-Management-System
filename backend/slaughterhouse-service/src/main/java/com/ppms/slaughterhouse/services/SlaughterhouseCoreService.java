package com.ppms.slaughterhouse.services;

import com.ppms.slaughterhouse.domain.*;
import com.ppms.slaughterhouse.dtos.CoreDtos.*;
import com.ppms.slaughterhouse.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SlaughterhouseCoreService {

    private final SlaughterhouseRepository slaughterhouseRepository;
    private final LineRepository lineRepository;
    private final SlaughterSessionRepository slaughterSessionRepository;
    private final CustomerRepository customerRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final DispatchOrderRepository dispatchOrderRepository;

    public Slaughterhouse createSlaughterhouse(SlaughterhouseRequest request) {
        Slaughterhouse s = new Slaughterhouse();
        s.setId(UUID.randomUUID());
        s.setCode(request.code());
        s.setName(request.name());
        s.setAddress(request.address());
        s.setCounty(request.county());
        s.setDailyCapacityBirds(request.dailyCapacityBirds());
        s.setActive(true);
        s.setCreatedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        return slaughterhouseRepository.save(s);
    }

    public List<Slaughterhouse> listSlaughterhouses() {
        return slaughterhouseRepository.findAll();
    }

    public Line createLine(LineRequest request) {
        Line l = new Line();
        l.setId(UUID.randomUUID());
        l.setSlaughterhouseId(request.slaughterhouseId());
        l.setCode(request.code());
        l.setName(request.name());
        l.setSpeedBph(request.speedBph());
        l.setActive(true);
        l.setCreatedAt(Instant.now());
        l.setUpdatedAt(Instant.now());
        return lineRepository.save(l);
    }

    public List<Line> listLines(UUID slaughterhouseId) {
        return lineRepository.findBySlaughterhouseId(slaughterhouseId);
    }

    public SlaughterSession createSession(SlaughterSessionRequest request) {
        SlaughterSession ss = new SlaughterSession();
        ss.setId(UUID.randomUUID());
        ss.setSlaughterhouseId(request.slaughterhouseId());
        ss.setLineId(request.lineId());
        ss.setSessionCode(request.sessionCode());
        ss.setStartedAt(request.startedAt());
        ss.setEndedAt(request.expectedEndAt());
        ss.setBirdsPlanned(request.birdsPlanned());
        ss.setStatus(SlaughterSession.Status.PLANNED);
        ss.setCreatedAt(Instant.now());
        ss.setUpdatedAt(Instant.now());
        return slaughterSessionRepository.save(ss);
    }

    public List<SlaughterSession> listSessions(UUID slaughterhouseId) {
        return slaughterSessionRepository.findBySlaughterhouseId(slaughterhouseId);
    }

    public Customer createCustomer(CustomerRequest request) {
        Customer c = new Customer();
        c.setId(UUID.randomUUID());
        c.setCode(request.code());
        c.setName(request.name());
        c.setVatNo(request.vatNo());
        c.setAddress(request.address());
        c.setActive(true);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return customerRepository.save(c);
    }

    public List<Customer> listCustomers() {
        return customerRepository.findAll();
    }

    public SalesOrder createSalesOrder(SalesOrderRequest request) {
        SalesOrder so = new SalesOrder();
        so.setId(UUID.randomUUID());
        so.setOrderNo(request.orderNo());
        so.setCustomerId(request.customerId());
        so.setOrderDate(request.orderDate());
        so.setRequestedDate(request.requestedDate());
        so.setStatus(SalesOrder.Status.DRAFT);
        so.setCreatedAt(Instant.now());
        so.setUpdatedAt(Instant.now());
        return salesOrderRepository.save(so);
    }

    public List<SalesOrder> listSalesOrders() {
        return salesOrderRepository.findAll();
    }

    public DispatchOrder createDispatchOrder(DispatchOrderRequest request) {
        DispatchOrder d = new DispatchOrder();
        d.setId(UUID.randomUUID());
        d.setOrderNo(request.orderNo());
        d.setSalesOrderId(request.salesOrderId());
        d.setCustomerId(request.customerId());
        d.setDispatchDate(request.dispatchDate());
        d.setQuantity(request.quantity());
        d.setStatus(DispatchOrder.Status.PLANNED);
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return dispatchOrderRepository.save(d);
    }

    public List<DispatchOrder> listDispatchOrders() {
        return dispatchOrderRepository.findAll();
    }
}
