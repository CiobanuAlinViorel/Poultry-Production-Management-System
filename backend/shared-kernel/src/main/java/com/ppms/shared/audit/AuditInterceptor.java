package com.ppms.shared.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ppms.shared.events.OutboxPublisher;
import com.ppms.shared.events.Topics;
import com.ppms.shared.persistence.UuidV7Generator;
import com.ppms.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditInterceptor {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.ppms.shared.audit.Audited)")
    @Transactional
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audited audited = method.getAnnotation(Audited.class);

        AuditLogEntity log = new AuditLogEntity();
        log.setId(UuidV7Generator.generate());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            log.setActorId(principal.getUserId());
            log.setActorName(principal.getDisplayName());
        }
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            log.setIpAddress(req.getHeader("X-Forwarded-For"));
            log.setUserAgent(req.getHeader("User-Agent"));
        }
        log.setAction(audited.action());
        log.setEntityType(audited.entity());
        log.setOccurredAt(Instant.now());

        try {
            log.setRequestPayload(maskPayload(joinPoint.getArgs()));
            Object result = joinPoint.proceed();
            log.setOutcome("SUCCESS");
            auditLogRepository.save(log);
            return result;
        } catch (Exception e) {
            log.setOutcome("FAILURE");
            log.setErrorCode(e instanceof com.ppms.shared.exception.DomainException de ? de.getCode() : e.getClass().getSimpleName());
            auditLogRepository.save(log);
            throw e;
        }
    }

    private String maskPayload(Object[] args) {
        try {
            return objectMapper.writeValueAsString(Map.of("args", args));
        } catch (Exception e) {
            return "{}";
        }
    }
}
