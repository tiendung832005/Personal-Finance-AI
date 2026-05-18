package com.data.personalfinanceinsightai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public String buildInvitationAcceptLink(String token) {
        return frontendUrl + "/invitations/accept?token=" + token;
    }

    public void sendGroupInvitation(String email, String token, String groupName) {
        String link = buildInvitationAcceptLink(token);

        log.info("=== INVITATION LINK ===");
        log.info("To: {}", email);
        log.info("Group: {}", groupName);
        log.info("Link: {}", link);
        log.info("======================");
    }
}
