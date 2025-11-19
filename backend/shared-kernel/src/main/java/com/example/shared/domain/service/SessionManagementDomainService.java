package com.example.shared.domain.service;

import com.example.shared.domain.entity.Session;
import com.example.shared.domain.entity.User;
import com.example.shared.domain.exception.SessionException;
import com.example.shared.domain.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionManagementDomainService {

    private final SessionRepository sessionRepository;

//    public Session createUserSession(User user, String deviceInfo, String ipAddress) {
//        invalidateExistingSessions(user);
//
//        Session session = Session.createNew(user, deviceInfo, ipAddress);
//        return sessionRepository.save(session);
//    }

    public void invalidateUserSession(String sessionToken) {
        Session session = sessionRepository.findByToken(sessionToken)
                .orElseThrow(() -> new SessionException("Session not found"));

        session.invalidate();
        sessionRepository.save(session);
    }

//    public void invalidateAllUserSessions(User user) {
//        List<Session> activeSessions = sessionRepository.findActiveSessionsByUser(user);
//        activeSessions.forEach(Session::invalidate);
//        sessionRepository.saveAll(activeSessions);
//    }

    public void refreshSession(String sessionToken) {
        Session session = sessionRepository.findByToken(sessionToken)
                .orElseThrow(() -> new SessionException("Session not found"));

        session.refresh();
        sessionRepository.save(session);
    }

    public boolean validateSessionActive(String sessionToken) {
        return sessionRepository.findByToken(sessionToken)
                .map(Session::isActive)
                .orElse(false);
    }

//    private void invalidateExistingSessions(User user) {
//        List<Session> existingSessions = sessionRepository.findActiveSessionsByUser(user);
//        if (!existingSessions.isEmpty()) {
//            existingSessions.forEach(Session::invalidate);
//            sessionRepository.saveAll(existingSessions);
//        }
//    }
}
