/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */

package fr.ans.afas.filter;

import fr.ans.afas.exception.TenantNotFoundException;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;
import fr.ans.afas.utils.TenantUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

public class TenantFilter implements Filter {

    private final ServerSearchConfig serverSearchConfig;
    Pattern pattern = Pattern.compile("/fhir/v\\d*/(.+?)(/.*)?");

    public TenantFilter(ServerSearchConfig serverSearchConfig) {
        this.serverSearchConfig = serverSearchConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        var requestUri = ((HttpServletRequest) request).getRequestURI();

        var matcherTenant = pattern.matcher(requestUri);

        if (!matcherTenant.matches()) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        var tenantUri = matcherTenant.group(1);

        Optional<TenantSearchConfig> tenant = serverSearchConfig.getConfigs().values().stream()
                .filter(t -> t.getTenantConfig().getName().equals(tenantUri)).findFirst();

        if (tenant.isEmpty()) {
            throw new TenantNotFoundException(requestUri);
        }

        // Si c'est une requête POST avec form-urlencoded, on capture les paramètres
        if (request instanceof HttpServletRequest httpRequest &&
                "POST".equalsIgnoreCase(httpRequest.getMethod()) &&
                httpRequest.getContentType() != null &&
                httpRequest.getContentType().contains("application/x-www-form-urlencoded")) {

            var parameterMap = httpRequest.getParameterMap();
            var queryString = parameterMap.entrySet().stream()
                    .flatMap(entry -> java.util.Arrays.stream(entry.getValue())
                            .map(value -> entry.getKey() + "=" + value))
                    .collect(java.util.stream.Collectors.joining("&"));

            // On stocke les paramètres dans l'attribut de requête
            request.setAttribute("cachedPostQueryString", queryString);
        }

        setCurrentTenantAndForward(tenant.get().getTenantConfig().getName(), tenant.get().getTenantConfig().getPath(), request, response);

    }

    private void forward(HttpServletRequest req, String tenantPath, ServletRequest request, ServletResponse response) throws IOException, ServletException {
        req.getRequestDispatcher(req.getRequestURI().replace(tenantPath, "")).forward(request, response);
    }

    private void setCurrentTenantAndForward(String tenant, String tenantPath, ServletRequest request, ServletResponse response) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        TenantUtil.setCurrentTenant(tenant);
        forward(req, tenantPath, request, response);
    }
}
