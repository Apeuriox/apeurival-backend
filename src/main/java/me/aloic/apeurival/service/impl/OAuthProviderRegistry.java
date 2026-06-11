package me.aloic.apeurival.service.impl;

import me.aloic.apeurival.service.OAuthProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuthProviderRegistry {

    private final Map<String, OAuthProvider> providers;

    public OAuthProviderRegistry(List<OAuthProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        OAuthProvider::getProviderName,
                        Function.identity()));
    }

    public OAuthProvider get(String provider) {
        OAuthProvider p = providers.get(provider);
        if (p == null) {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + provider);
        }
        return p;
    }
}
