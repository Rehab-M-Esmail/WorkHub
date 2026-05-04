package com.example.WorkHub.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class User extends TenantAwareEntity implements UserDetails {
    @Id
    @GeneratedValue
    private Long id;
    private String email;
    private String password;
    private String role; // should be enum as well?

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return email;
    }
}
