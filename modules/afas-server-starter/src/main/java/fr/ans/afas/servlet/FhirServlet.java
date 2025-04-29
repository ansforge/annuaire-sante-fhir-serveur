/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.servlet;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import fr.ans.afas.AfasServerConfigurerAdapter;
import fr.ans.afas.fhirserver.service.MultiTenantService;
import jakarta.servlet.annotation.WebServlet;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import java.util.List;

/**
 * The fhir (hapi) servlet that expose FHIR api.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@WebServlet(urlPatterns = {"/fhir/v1/*", "/fhir/*/v1/*"})
public class FhirServlet extends ca.uhn.fhir.rest.server.RestfulServer {

    /**
     * Build the servlet
     *
     * @param ctx                   the fhir context
     * @param providers             list of Hapi FHIR providers
     * @param serverBaseUrl         the public url of the service
     * @param afasConfigurerAdapter the configurer adapter of AfasServer
     * @param multiTenantService    service to access tenants
     */
    @Inject
    public FhirServlet(FhirContext ctx,
                       List<IResourceProvider> providers,
                       IPagingProvider pagingProvider,
                       @Value("${afas.publicUrl}") String serverBaseUrl,
                       AfasServerConfigurerAdapter afasConfigurerAdapter,
                       MultiTenantService multiTenantService) {
        super(ctx);
        this.setDefaultResponseEncoding(EncodingEnum.JSON);
        this.setServerAddressStrategy(new MultitenantServerAddressStrategy(serverBaseUrl, multiTenantService));
        registerProviders(providers);


        setServerName("Afas Fhir server");
        setServerVersion("V1-R4");
        setImplementationDescription("Afas data");
        setPagingProvider(pagingProvider);

        afasConfigurerAdapter.configureHapiServlet(this);
    }


}