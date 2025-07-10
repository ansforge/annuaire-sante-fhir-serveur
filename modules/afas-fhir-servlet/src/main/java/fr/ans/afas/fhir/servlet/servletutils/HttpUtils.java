/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.servletutils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utility class to work with http
 */
public class HttpUtils {

    /**
     * The path of the servlet v2 in url (/v2/Device...)
     */
    public static final String SERVLET_API_PATH = "v2";


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
}
