/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir;

import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;

/**
 * Capability statement implementation.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AfasConformanceProvider extends ServerCapabilityStatementProvider {

    /**
     * Construct the conformance provider
     *
     * @param theServer the hapi server
     */
    public AfasConformanceProvider(RestfulServer theServer) {
        super(theServer);
    }
}