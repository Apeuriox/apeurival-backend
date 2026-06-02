package me.aloic.apeurival.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthStateStore {

    private final Map<String, StateEntry> store = new ConcurrentHashMap<>();

    public String generate() {
        cleanExpired();
        String state = UUID.randomUUID().toString().replace("-", "");
        store.put(state, new StateEntry(state, System.currentTimeMillis()));
        return state;
    }

    public boolean validate(String state) {
        cleanExpired();
        StateEntry entry = store.remove(state);
        return entry != null;
    }

    public String storeToken(String jwt) {
        cleanExpired();
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        store.put(code, new StateEntry(jwt, System.currentTimeMillis()));
        return code;
    }

    public String retrieveToken(String code) {
        cleanExpired();
        StateEntry entry = store.remove(code);
        return entry != null ? entry.value : null;
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        store.values().removeIf(e -> now - e.createdAt > 300_000); // 5 min
    }

    private record StateEntry(String value, long createdAt) {}
}
