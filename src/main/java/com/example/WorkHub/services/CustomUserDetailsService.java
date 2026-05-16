package com.example.WorkHub.services;

import com.example.WorkHub.config.multitenancy.TenantContext;
import com.example.WorkHub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    /**
     * Resolves the user for the current request tenant ({@code X-Tenant-ID} via {@link TenantContext}).
     * Email alone is not unique across tenants.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new UsernameNotFoundException("Missing tenant context for user lookup");
        }
        return userRepository.findByEmailAndTenantId(username, tenantId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
