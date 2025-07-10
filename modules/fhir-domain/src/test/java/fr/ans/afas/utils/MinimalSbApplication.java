/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils;

import fr.ans.afas.fhirserver.hook.exception.BadHookConfiguration;
import fr.ans.afas.fhirserver.hook.service.HookService;
import fr.ans.afas.fhirserver.service.audit.DefaultReadAuditService;
import fr.ans.afas.fhirserver.service.audit.DefaultWriteAuditService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import javax.inject.Inject;


/**
 * A minimal spring boot application used for tests. This configuration import a basic FHIR SearchConfiguration
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@SpringBootApplication
public class MinimalSbApplication {

    @Bean
    SampleHookService sampleHookService() {
        return new SampleHookService();
    }


    @Bean
    public DefaultWriteAuditService defaultWriteAuditService() {
        return new DefaultWriteAuditService();
    }

    @Bean
    public DefaultReadAuditService defaultReadAuditService() {
        return new DefaultReadAuditService();
    }

    @Inject
    @Bean
    public HookService hookService(ApplicationContext applicationContext) throws BadHookConfiguration {
        return new HookService(applicationContext);
    }
}
