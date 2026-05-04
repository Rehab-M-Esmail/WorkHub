package com.example.WorkHub.models;

import com.example.WorkHub.config.multitenancy.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
public abstract class TenantAwareEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @PrePersist
    public void prePersist() {
        if (this.tenantId == null) {
          this.tenantId = Long.valueOf(TenantContext.getTenantId());
        }
    }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}