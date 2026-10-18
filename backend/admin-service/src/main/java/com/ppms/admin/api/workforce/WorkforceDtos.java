package com.ppms.admin.api.workforce;

import com.ppms.admin.domain.workforce.*;
import com.ppms.shared.domain.ScopeType;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class WorkforceDtos {
    private WorkforceDtos() {}
    public record PositionRequest(@NotBlank String code, @NotBlank String nameRo, String nameEn, String module, UUID[] defaultRoleIds) {}
    public record PositionDto(UUID id, String code, String nameRo, String nameEn, String module, UUID[] defaultRoleIds) {}
    public record EmployeeRequest(@NotBlank String employeeNo, @NotBlank String firstName, @NotBlank String lastName, @Pattern(regexp="\\d{13}") String cnp, String phone, @Email String email, @NotNull LocalDate hireDate, Employee.Status status, UUID positionId, UUID homeSiteId, ScopeType homeSiteType, String photoUrl) {}
    public record EmployeeUpdate(String firstName, String lastName, String phone, @Email String email, LocalDate hireDate, Employee.Status status, UUID positionId, UUID homeSiteId, ScopeType homeSiteType, String photoUrl) {}
    public record EmployeeDto(UUID id, String employeeNo, String firstName, String lastName, String phone, String email, LocalDate hireDate, LocalDate terminationDate, Employee.Status status, UUID positionId, UUID homeSiteId, ScopeType homeSiteType, UUID userId, String photoUrl) {}
    public record TerminateRequest(@NotNull LocalDate terminationDate) {}
    public record LinkUserRequest(@NotNull UUID userId) {}
    public record TeamRequest(@NotNull UUID siteId, @NotBlank String name, UUID leaderEmployeeId) {}
    public record TeamDto(UUID id, UUID siteId, String name, UUID leaderEmployeeId, List<TeamMemberDto> members) {}
    public record TeamMemberRequest(@NotNull UUID employeeId, @NotNull LocalDate fromDate) {}
    public record TeamMemberDto(UUID id, UUID employeeId, LocalDate fromDate, LocalDate toDate) {}
    public record ShiftTemplateRequest(UUID siteId, @NotBlank String code, @NotNull LocalTime startTime, @NotNull LocalTime endTime, @PositiveOrZero int breakMinutes, String color) {}
    public record ShiftTemplateDto(UUID id, UUID siteId, String code, LocalTime startTime, LocalTime endTime, int breakMinutes, String color) {}
    public record RosterRequest(@NotNull UUID siteId, @NotNull LocalDate weekStart) {}
    public record RosterDto(UUID id, UUID siteId, LocalDate weekStart, Roster.Status status, UUID publishedBy, Instant publishedAt, List<DutyAssignmentDto> assignments) {}
    public record DutyAssignmentRequest(@NotNull UUID employeeId, @NotNull LocalDate date, @NotNull UUID shiftTemplateId, UUID subUnitId, String roleHint, String note) {}
    public record DutyAssignmentDto(UUID id, UUID employeeId, LocalDate date, UUID shiftTemplateId, UUID subUnitId, String roleHint, String note) {}
    public record CertificationRequest(@NotNull Certification.Type type, String number, LocalDate issuedAt, LocalDate expiresAt, String documentRef) {}
    public record CertificationDto(UUID id, UUID employeeId, Certification.Type type, String number, LocalDate issuedAt, LocalDate expiresAt, String documentRef) {}
    public record AbsenceRequest(@NotNull LocalDate fromDate, @NotNull LocalDate toDate, @NotNull Absence.Type type, UUID approvedBy) {}
    public record AbsenceDto(UUID id, UUID employeeId, LocalDate fromDate, LocalDate toDate, Absence.Type type, UUID approvedBy) {}
}
