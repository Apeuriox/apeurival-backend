package me.aloic.apeurival.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OAuthStateStore {

    private final Map<String, StateEntry> store = new ConcurrentHashMap<>();

    public String generate() {
        cleanExpired();
        String state = UUID.randomUUID().toString().replace("-", "");
        store.put(state, new StateEntry(state, System.currentTimeMillis()));
        log.info("Generated Oauth, state {}",state);
        return state;
    }

    public boolean validate(String state) {
        cleanExpired();
        StateEntry entry = store.remove(state);
        log.info("Validating Oauth, state {}",state);
        return entry != null;
    }

    public String storeToken(String jwt) {
        cleanExpired();
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        store.put(code, new StateEntry(jwt, System.currentTimeMillis()));
        log.info("Generated Oauth token stored, code {}",code);
        return code;
    }

    public String retrieveToken(String code) {
        cleanExpired();
        StateEntry entry = store.remove(code);
        log.info("Retrieved Oauth token removed, code {}",code);
        return entry != null ? entry.value : null;
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        store.values().removeIf(e -> now - e.createdAt > 300_000); // 5 min
        log.info("Expired Oauth token removed");
    }

    private record StateEntry(String value, long createdAt) {}
}
