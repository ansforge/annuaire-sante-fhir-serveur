/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import fr.ans.afas.fhir.GlobalProvider;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import java.util.List;

/**
 * A simple Hapi servlet to launch the server
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@WebServlet(urlPatterns = {"/fhir/*", "/fhir/v1/*"})
public class FhirServlet extends ca.uhn.fhir.rest.server.RestfulServer {

    @Inject
    public FhirServlet(FhirContext ctx,
                       List<IResourceProvider> providers,
                       GlobalProvider globalProvider) {
        super(ctx);
        this.setDefaultResponseEncoding(EncodingEnum.JSON);
        registerProviders(providers);
        registerProvider(globalProvider);
    }
}
