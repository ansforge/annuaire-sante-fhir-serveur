/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.config;

import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.filter.TenantFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(value = "afas.fhir.tenant.mode", havingValue = "path", matchIfMissing = true)
@Configuration
public class PathFilterConfig {


    @Bean
    public FilterRegistrationBean<TenantFilter> pathFilter(ServerSearchConfig serverSearchConfig) {
        FilterRegistrationBean<TenantFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantFilter(serverSearchConfig));
        registrationBean.addUrlPatterns("/fhir/*");
        return registrationBean;
    }
}
