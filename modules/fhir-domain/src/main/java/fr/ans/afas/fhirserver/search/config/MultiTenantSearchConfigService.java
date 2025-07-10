/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.config;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.domain.JoinPath;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;
import fr.ans.afas.fhirserver.search.exception.NotFoundTenantException;
import fr.ans.afas.utils.TenantUtil;

import java.util.*;

/**
 * Access search config based on a tenant.
 */
public class MultiTenantSearchConfigService implements SearchConfigService {

    Map<String, BaseSearchConfigService> tenantConfigServices = new HashMap<>();

    /**
     * Construct the search config
     *
     * @param serverSearchConfig the configuration
     */
    public MultiTenantSearchConfigService(ServerSearchConfig serverSearchConfig) {
        for (var ssc : serverSearchConfig.getConfigs().entrySet()) {
            tenantConfigServices.put(ssc.getKey(), new CompositeSearchConfigService(List.of(ssc.getValue())));
        }
    }

    @Override
    public TenantSearchConfig getServerSearchConfig() {
        return currentServerSearchConfig().getServerSearchConfig();
    }

    @Override
    public List<SearchParamConfig> getAllByFhirResource(String fhirResource) {
        return currentServerSearchConfig().getAllByFhirResource(fhirResource);
    }

    @Override
    public Optional<SearchParamConfig> getSearchConfigByPath(FhirSearchPath path) {
        return currentServerSearchConfig().getSearchConfigByPath(path);
    }

    @Override
    public Optional<SearchParamConfig> getSearchConfigByResourceAndParamName(String resourceType, String paramName) {
        return currentServerSearchConfig().getSearchConfigByResourceAndParamName(resourceType, paramName);
    }

    @Override
    public List<JoinPath> getJoinsByFhirResource(String fhirResource) {
        return currentServerSearchConfig().getJoinsByFhirResource(fhirResource);
    }

    @Override
    public Set<String> getResources() {
        return currentServerSearchConfig().getResources();
    }

    @Override
    public Set<String> getIndexesByFhirResource(String fhirResource) {
        return currentServerSearchConfig().getIndexesByFhirResource(fhirResource);
    }


    private BaseSearchConfigService currentServerSearchConfig() {
        var tenant = TenantUtil.getCurrentTenant();
        if (!tenantConfigServices.containsKey(tenant)) {
            throw new NotFoundTenantException("Tenant not found: " + tenant);
        }
        return this.tenantConfigServices.get(tenant);
    }


}
