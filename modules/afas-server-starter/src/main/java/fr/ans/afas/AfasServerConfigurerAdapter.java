/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas;

import fr.ans.afas.servlet.FhirServlet;

/**
 * The configurer or the fhir server. This adapter allows to override or customize some configurations like the Hapi Servlet.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AfasServerConfigurerAdapter {

    /**
     * Configure the hapi servlet
     *
     * @param fhirServlet the hapi servlet
     */
    public void configureHapiServlet(FhirServlet fhirServlet) {
        // Configure the servlet
    }

}
