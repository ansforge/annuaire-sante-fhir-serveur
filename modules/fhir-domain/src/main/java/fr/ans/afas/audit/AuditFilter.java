/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.audit;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;


public class AuditFilter implements Filter {

    public static final String ALREADY_FILTERED = "AFAS_ALREADY_FILTERED";

    /**
     * Header used by proxy to send the ip
     */
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";


    /**
     * Find the ip, store it and chain the request
     */
    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        // ensure we filter only one time:
        if (request.getAttribute(ALREADY_FILTERED) != null) {
            chain.doFilter(request, response);
        } else {
            request.setAttribute(ALREADY_FILTERED, Boolean.TRUE);
            var req = (HttpServletRequest) request;
            var userIpAddress = req.getHeader(HEADER_X_FORWARDED_FOR);
            if (userIpAddress == null) {
                userIpAddress = request.getRemoteAddr();
            }
            AuditUtils.store(AuditInformation.builder().ip(userIpAddress).build());
            chain.doFilter(request, response);
            AuditUtils.clean();
        }
    }
}
