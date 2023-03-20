/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.servlet;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import fr.ans.afas.AfasServerConfigurerAdapter;
import fr.ans.afas.fhir.GlobalProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.annotation.WebServlet;
import java.util.List;

/**
 * The fhir (hapi) servlet that expose FHIR api.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@WebServlet(urlPatterns = {"/fhir/*", "/fhir/v1/*"})
public class FhirServlet extends ca.uhn.fhir.rest.server.RestfulServer {


    /**
     * Build the servlet
     *
     * @param ctx             the fhir context
     * @param providers       list of Hapi FHIR providers
     * @param serverBaseUrl   the public url of the service
     * @param maxPageSize     the max page size
     * @param defaultPageSize the default page size
     */
    @Autowired
    public FhirServlet(FhirContext ctx,
                       List<IResourceProvider> providers,
                       IPagingProvider pagingProvider,
                       @Value("${afas.publicUrl}") String serverBaseUrl,
                       @Value("${afas.fhir.max-page-size:2500}") int maxPageSize,
                       @Value("${afas.fhir.default-page-size:50}") int defaultPageSize,
                       AfasServerConfigurerAdapter afasConfigurerAdapter,
                       @Autowired(required = false) GlobalProvider globalProvider) {
        super(ctx);
        this.setDefaultResponseEncoding(EncodingEnum.JSON);
        this.setServerAddressStrategy(new HardcodedServerAddressStrategy(serverBaseUrl));
        registerProviders(providers);
        if (globalProvider != null) {
            registerProvider(globalProvider);
        }

        setServerName("Afas Fhir server");
        setServerVersion("V1-R4");
        setImplementationDescription("Afas data");
        setPagingProvider(pagingProvider);

        afasConfigurerAdapter.configureHapiServlet(this);
    }


}