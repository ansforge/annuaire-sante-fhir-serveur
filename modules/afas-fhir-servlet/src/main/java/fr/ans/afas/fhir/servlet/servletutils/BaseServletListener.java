/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.servletutils;

import fr.ans.afas.exception.TenantNotFoundException;
import fr.ans.afas.fhir.servlet.FhirResourceServlet;
import fr.ans.afas.utils.TenantUtil;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletRequest;
import org.springframework.util.StringUtils;


public abstract class BaseServletListener {
    protected final AsyncContext context;

    protected BaseServletListener(AsyncContext context) {
        this.context = context;
    }

    protected String getCurrentTenantInAsyncReq(ServletRequest req) {
        var tenant = (String) req.getAttribute(FhirResourceServlet.REQUEST_AFAS_TENANT_ATTRIBUTE);
        if (!StringUtils.hasLength(tenant)) {
            throw new TenantNotFoundException(tenant);
        }
        return tenant;
    }

    protected void setTenant() {
        TenantUtil.setCurrentTenant(getCurrentTenantInAsyncReq(this.context.getRequest()));
    }
}
