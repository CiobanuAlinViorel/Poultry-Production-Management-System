package com.ppms.admin.service;
import com.ppms.admin.api.workforce.WorkforceDtos.*;
import com.ppms.admin.domain.workforce.Employee;
import com.ppms.admin.repositories.workforce.*;
import com.ppms.shared.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class EmployeeService {
 private final EmployeeRepository repository; private final PositionRepository positions;
 @Transactional(readOnly=true) public List<EmployeeDto> list(){return repository.findAll().stream().map(this::dto).toList();}
 @Transactional(readOnly=true) public EmployeeDto get(UUID id){return dto(find(id));}
 @Transactional public EmployeeDto create(EmployeeRequest r){if(repository.findByEmployeeNo(r.employeeNo()).isPresent())throw new ValidationException("Employee number already exists: "+r.employeeNo()); validatePosition(r.positionId()); Employee e=new Employee();e.setEmployeeNo(r.employeeNo());e.setFirstName(r.firstName());e.setLastName(r.lastName());e.setCnpHash(r.cnp()==null?null:hash(r.cnp()));e.setPhone(r.phone());e.setEmail(r.email());e.setHireDate(r.hireDate());e.setStatus(r.status()==null?Employee.Status.ACTIVE:r.status());e.setPositionId(r.positionId());e.setHomeSiteId(r.homeSiteId());e.setHomeSiteType(r.homeSiteType());e.setPhotoUrl(r.photoUrl());return dto(repository.save(e));}
 @Transactional public EmployeeDto update(UUID id,EmployeeUpdate r){Employee e=find(id);validatePosition(r.positionId());if(r.firstName()!=null)e.setFirstName(r.firstName());if(r.lastName()!=null)e.setLastName(r.lastName());if(r.phone()!=null)e.setPhone(r.phone());if(r.email()!=null)e.setEmail(r.email());if(r.hireDate()!=null)e.setHireDate(r.hireDate());if(r.status()!=null)e.setStatus(r.status());if(r.positionId()!=null)e.setPositionId(r.positionId());if(r.homeSiteId()!=null)e.setHomeSiteId(r.homeSiteId());if(r.homeSiteType()!=null)e.setHomeSiteType(r.homeSiteType());if(r.photoUrl()!=null)e.setPhotoUrl(r.photoUrl());return dto(e);}
 @Transactional public EmployeeDto terminate(UUID id,TerminateRequest r){Employee e=find(id);if(r.terminationDate().isBefore(e.getHireDate()))throw new ValidationException("Termination date cannot precede hire date");e.setTerminationDate(r.terminationDate());e.setStatus(Employee.Status.TERMINATED);return dto(e);}
 @Transactional public EmployeeDto linkUser(UUID id,LinkUserRequest r){repository.findByUserId(r.userId()).filter(e->!e.getId().equals(id)).ifPresent(e->{throw new ValidationException("User is already linked to another employee");});Employee e=find(id);e.setUserId(r.userId());return dto(e);}
 private Employee find(UUID id){return repository.findById(id).orElseThrow(()->new EntityNotFoundException("Employee",id));} private void validatePosition(UUID id){if(id!=null&&!positions.existsById(id))throw new EntityNotFoundException("Position",id);}
 private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private EmployeeDto dto(Employee e){return new EmployeeDto(e.getId(),e.getEmployeeNo(),e.getFirstName(),e.getLastName(),e.getPhone(),e.getEmail(),e.getHireDate(),e.getTerminationDate(),e.getStatus(),e.getPositionId(),e.getHomeSiteId(),e.getHomeSiteType(),e.getUserId(),e.getPhotoUrl());}
}
