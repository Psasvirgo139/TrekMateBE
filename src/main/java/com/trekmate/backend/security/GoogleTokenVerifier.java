package com.trekmate.backend.security;

import com.trekmate.backend.exception.AppException;
import com.trekmate.backend.exception.ErrorCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Component
@Slf4j
public class GoogleTokenVerifier {

    private final String clientId;

    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
    }

    public GoogleUserInfo verify(String idToken) {
        if (!StringUtils.hasText(clientId)) {
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED, "Google sign-in is not configured");
        }
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED, "Invalid Google token");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            return new GoogleUserInfo(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name")
            );
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Google token verification failed", ex);
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    public record GoogleUserInfo(String googleId, String email, String displayName) {}
}
