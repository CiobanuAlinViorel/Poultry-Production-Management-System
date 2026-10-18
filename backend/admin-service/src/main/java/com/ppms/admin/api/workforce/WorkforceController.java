package com.ppms.admin.api.workforce;

import com.ppms.admin.api.workforce.WorkforceDtos.*;
import com.ppms.admin.service.*;
import com.ppms.shared.security.CurrentUser;
import com.ppms.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkforceController {
    private final PositionService positions;
    private final EmployeeService employees;
    private final TeamService teams;
    private final ShiftTemplateService shifts;
    private final RosterService rosters;
    private final CertificationService certifications;
    private final AbsenceService absences;

    @GetMapping("/positions") public List<PositionDto> positions(){return positions.list();}
    @PostMapping("/positions") public ResponseEntity<PositionDto> createPosition(@Valid @RequestBody PositionRequest r){PositionDto d=positions.create(r);return ResponseEntity.created(URI.create("/api/v1/positions/"+d.id())).body(d);}
    @PatchMapping("/positions/{id}") public PositionDto updatePosition(@PathVariable UUID id,@Valid @RequestBody PositionRequest r){return positions.update(id,r);}

    @GetMapping("/employees") public List<EmployeeDto> employees(){return employees.list();}
    @PostMapping("/employees") public ResponseEntity<EmployeeDto> createEmployee(@Valid @RequestBody EmployeeRequest r){EmployeeDto d=employees.create(r);return ResponseEntity.created(URI.create("/api/v1/employees/"+d.id())).body(d);}
    @GetMapping("/employees/{id}") public EmployeeDto employee(@PathVariable UUID id){return employees.get(id);}
    @PatchMapping("/employees/{id}") public EmployeeDto updateEmployee(@PathVariable UUID id,@Valid @RequestBody EmployeeUpdate r){return employees.update(id,r);}
    @PostMapping("/employees/{id}/terminate") public EmployeeDto terminate(@PathVariable UUID id,@Valid @RequestBody TerminateRequest r){return employees.terminate(id,r);}
    @PostMapping("/employees/{id}/link-user") public EmployeeDto linkUser(@PathVariable UUID id,@Valid @RequestBody LinkUserRequest r){return employees.linkUser(id,r);}

    @GetMapping("/teams") public List<TeamDto> teams(@RequestParam(required=false) UUID siteId){return teams.list(siteId);}
    @PostMapping("/teams") public ResponseEntity<TeamDto> createTeam(@Valid @RequestBody TeamRequest r){TeamDto d=teams.create(r);return ResponseEntity.created(URI.create("/api/v1/teams/"+d.id())).body(d);}
    @PostMapping("/teams/{id}/members") public TeamDto addMember(@PathVariable UUID id,@Valid @RequestBody TeamMemberRequest r){return teams.addMember(id,r);}
    @DeleteMapping("/teams/{id}/members/{employeeId}") public ResponseEntity<Void> removeMember(@PathVariable UUID id,@PathVariable UUID employeeId){teams.removeMember(id,employeeId);return ResponseEntity.noContent().build();}

    @GetMapping("/shift-templates") public List<ShiftTemplateDto> shifts(@RequestParam(required=false) UUID siteId){return shifts.list(siteId);}
    @PostMapping("/shift-templates") public ResponseEntity<ShiftTemplateDto> createShift(@Valid @RequestBody ShiftTemplateRequest r){ShiftTemplateDto d=shifts.create(r);return ResponseEntity.created(URI.create("/api/v1/shift-templates/"+d.id())).body(d);}

    @GetMapping("/rosters") public List<RosterDto> rosters(@RequestParam(required=false) UUID siteId,@RequestParam(required=false) LocalDate weekStart){return rosters.list(siteId,weekStart);}
    @PostMapping("/rosters") public ResponseEntity<RosterDto> createRoster(@Valid @RequestBody RosterRequest r){RosterDto d=rosters.createDraft(r);return ResponseEntity.created(URI.create("/api/v1/rosters/"+d.id())).body(d);}
    @PostMapping("/rosters/{id}/publish") public RosterDto publish(@PathVariable UUID id,@CurrentUser UserPrincipal user){return rosters.publish(id,user==null?null:user.getUserId());}
    @PostMapping("/rosters/{id}/copy-from-previous-week") public RosterDto copy(@PathVariable UUID id){return rosters.copyFromPreviousWeek(id);}
    @PutMapping("/rosters/{id}/assignments") public RosterDto assignments(@PathVariable UUID id,@Valid @RequestBody List<@Valid DutyAssignmentRequest> r){return rosters.bulkAssignments(id,r);}

    @GetMapping("/employees/{id}/certifications") public List<CertificationDto> certifications(@PathVariable UUID id){return certifications.list(id);}
    @PostMapping("/employees/{id}/certifications") public CertificationDto createCertification(@PathVariable UUID id,@Valid @RequestBody CertificationRequest r){return certifications.create(id,r);}
    @PutMapping("/employees/{id}/certifications/{certificationId}") public CertificationDto updateCertification(@PathVariable UUID id,@PathVariable UUID certificationId,@Valid @RequestBody CertificationRequest r){return certifications.update(id,certificationId,r);}
    @DeleteMapping("/employees/{id}/certifications/{certificationId}") public ResponseEntity<Void> deleteCertification(@PathVariable UUID id,@PathVariable UUID certificationId){certifications.delete(id,certificationId);return ResponseEntity.noContent().build();}

    @GetMapping("/employees/{id}/absences") public List<AbsenceDto> absences(@PathVariable UUID id){return absences.list(id);}
    @PostMapping("/employees/{id}/absences") public AbsenceDto createAbsence(@PathVariable UUID id,@Valid @RequestBody AbsenceRequest r){return absences.create(id,r);}
    @PutMapping("/employees/{id}/absences/{absenceId}") public AbsenceDto updateAbsence(@PathVariable UUID id,@PathVariable UUID absenceId,@Valid @RequestBody AbsenceRequest r){return absences.update(id,absenceId,r);}
    @DeleteMapping("/employees/{id}/absences/{absenceId}") public ResponseEntity<Void> deleteAbsence(@PathVariable UUID id,@PathVariable UUID absenceId){absences.delete(id,absenceId);return ResponseEntity.noContent().build();}
}
