/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service;

import fr.ans.afas.exception.TenantNotFoundException;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.Tenant;
import fr.ans.afas.utils.TenantUtil;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultMultiTenantService implements MultiTenantService {

    @Autowired
    private ServerSearchConfig serverSearchConfig;

    public @NotNull Tenant getTenant() {

        var ct = TenantUtil.getCurrentTenant();
        if (!serverSearchConfig.getConfigs().containsKey(ct)) {
            throw new TenantNotFoundException("Current tenant is not found: " + ct);
        }
        return serverSearchConfig.getConfigs().get(TenantUtil.getCurrentTenant()).getTenantConfig();
    }

    public @NotNull Tenant getTenantByName(String currentTenant) {

        if (!serverSearchConfig.getConfigs().containsKey(currentTenant)) {
            throw new TenantNotFoundException("Current tenant is not found");
        }
        return serverSearchConfig.getConfigs().get(currentTenant).getTenantConfig();
    }
}
