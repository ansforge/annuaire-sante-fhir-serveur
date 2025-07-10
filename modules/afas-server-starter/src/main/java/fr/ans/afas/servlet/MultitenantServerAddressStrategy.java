/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.servlet;

import ca.uhn.fhir.rest.server.IServerAddressStrategy;
import fr.ans.afas.exception.TenantNotFoundException;
import fr.ans.afas.fhirserver.search.config.domain.Tenant;
import fr.ans.afas.fhirserver.service.MultiTenantService;
import fr.ans.afas.utils.TenantUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to found the server address in a multi tenant context.
 */
public class MultitenantServerAddressStrategy implements IServerAddressStrategy {

    private final Map<String, Tenant> cachedTenants = new HashMap<>();
    private final MultiTenantService multiTenantService;
    private String baseUrl;

    public MultitenantServerAddressStrategy(String baseUrl, MultiTenantService multiTenantService) {
        Validate.notBlank(baseUrl, "baseUrl must not be null or empty");
        this.baseUrl = baseUrl;
        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.multiTenantService = multiTenantService;
    }

    @Override
    public String determineServerBase(ServletContext servletContext, HttpServletRequest httpServletRequest) {
        var currentTenant = TenantUtil.getCurrentTenant();
        cachedTenants.computeIfAbsent(currentTenant, a -> {
            var t = multiTenantService.getTenantByName(currentTenant);
            if (t == null) {
                throw new TenantNotFoundException(currentTenant);
            }
            return t;
        });
        return baseUrl + "/fhir/v1";
    }
}
