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

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        store.values().removeIf(e -> now - e.createdAt > 300_000); // 5 min
    }

    private record StateEntry(String value, long createdAt) {}
}
