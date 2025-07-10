/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas;


import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;
import fr.ans.afas.fhirserver.search.config.yaml.MultiConfigLoader;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.filter.TenantFilter;
import fr.ans.afas.provider.DeviceProvider;
import fr.ans.afas.provider.OrganizationProvider;
import fr.ans.afas.servlet.FhirServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 *
 */
@SpringBootApplication
public class SimpleTestApp extends AfasServerConfigurerAdapter {
    /**
     * Launch the service
     *
     * @param args command line args (not used)
     */
    public static void main(String[] args) {
        SpringApplication.run(SimpleTestApp.class, args);
    }


    /**
     * Configure the servlet
     *
     * @param fhirServlet the servlet to configure
     */
    @Override
    public void configureHapiServlet(FhirServlet fhirServlet) {
        fhirServlet.setServerName("My FHIR server");
    }


    /**
     * The Hapi device provider
     */
    @Bean
    DeviceProvider deviceProvider(FhirStoreService fhirStoreService, FhirContext fhirContext, ExpressionFactory expressionFactory, NextUrlManager nextUrlManager) {
        return new DeviceProvider(fhirStoreService, fhirContext, expressionFactory, nextUrlManager);
    }


    /**
     * The Hapi organization provider
     */
    @Bean
    OrganizationProvider organizationProvider(FhirStoreService fhirStoreService, FhirContext fhirContext, ExpressionFactory expressionFactory, NextUrlManager nextUrlManager) {
        return new OrganizationProvider(fhirStoreService, fhirContext, expressionFactory, nextUrlManager);
    }


    @Bean
    TenantFilter tenantFilter(ServerSearchConfig serverSearchConfig) {
        return new TenantFilter(serverSearchConfig);
    }

    @Bean
    List<TenantSearchConfig> searchConfigs() throws URISyntaxException, IOException {
        var m = new MultiConfigLoader();
        return m.loadConfigs("indexes/");
    }


}
