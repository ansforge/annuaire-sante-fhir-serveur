/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.servletutils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class to work with http
 */
public class HttpUtils {

    /**
     * The path of the servlet v2 in url (/v2/Device...)
     */
    public static final String SERVLET_API_PATH = "v2";
    public static final String PAGING_URLS = "pagingUrls";


    private HttpUtils() {
    }

    /**
     * Generate the value of the http header Last-Modified from a data
     *
     * @param date the date
     * @return the formated date
     */
    public static String lastModifiedFromDate(ZonedDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        return date.format(formatter);
    }

    /**
     * Get the base path of the server including version and tenant
     *
     * @return the url
     */
    public static String getServerUrl(String publicUrl,String multiTenantContextPath) {
        return publicUrl
                .concat("/fhir/")
                .concat(HttpUtils.SERVLET_API_PATH)
                .concat(multiTenantContextPath);
    }

    /**
     * Get the base path of the server including version and tenant, using the request server name/port
     *
     * @return the url
     */
    public static String getServerUrl(HttpServletRequest request, String multiTenantContextPath) {
        String scheme = request.getScheme();              // http ou https
        String serverName = request.getServerName();      // nom de host
        int serverPort = request.getServerPort();         // port
        String contextPath = request.getContextPath();    // contexte appli (souvent /fhir ou vide)

        // Reconstruit la base URL (sans path spécifique FHIR)
        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);

        // Ne pas ajouter le port si c’est le port par défaut
        if (!("http".equalsIgnoreCase(scheme) && serverPort == 80)
                && !("https".equalsIgnoreCase(scheme) && serverPort == 443)) {
            baseUrl.append(":").append(serverPort);
        }

        if (contextPath != null && !contextPath.isEmpty()) {
            baseUrl.append(contextPath);
        }

        return baseUrl
                .append("/fhir/")
                .append(HttpUtils.SERVLET_API_PATH)
                .append(multiTenantContextPath)
                .toString();
    }

    public static HttpSession getSession(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        }
        return session;
    }

    public static Map<Integer, String> getPagingUrls(HttpServletRequest request) {
        HttpSession session = getSession(request);
        if (session == null) {
            return new HashMap<>();
        }
        Map<Integer, String> pagingUrls = (Map<Integer, String>) session.getAttribute(PAGING_URLS);
        if (pagingUrls == null) {
            session.setAttribute(PAGING_URLS, new HashMap<Integer, String>());
            return (Map<Integer, String>) session.getAttribute(PAGING_URLS);
        }
        return pagingUrls;
    }
}
