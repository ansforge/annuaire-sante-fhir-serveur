/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.servletutils;


import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * @author Anouar EL Qadim
 * @since 1.0.0
 */
public class CustomHttpServletResponse extends HttpServletResponseWrapper {

    public static final int SC_UNPROCESSABLE_ENTITY = 422;

    public CustomHttpServletResponse(HttpServletResponse response) {
        super(response);
    }

}