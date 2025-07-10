/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.config.domain;


import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;

@Setter
@Getter
public class TenantSearchConfig {


    protected String validationMode;
    protected String implementationGuideUrl;
    protected String copyright;
    @NotNull
    protected Collection<FhirResourceSearchConfig> resources;
    @NotNull
    protected Tenant tenantConfig;


    public TenantSearchConfig() {
        this.resources = new ArrayList<>();
    }

    @Builder
    public TenantSearchConfig(String validationMode, @NotNull @NonNull Collection<FhirResourceSearchConfig> resources, @NotNull @NonNull Tenant tenantConfig) {
        this.validationMode = validationMode;
        this.resources = resources;
        this.tenantConfig = tenantConfig;
    }

}
