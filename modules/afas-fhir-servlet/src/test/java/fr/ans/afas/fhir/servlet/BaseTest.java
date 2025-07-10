/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhir.servlet.service.FhirOperationFactory;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.SecurityService;
import fr.ans.afas.service.TestMultitenantService;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;

/**
 * A test class with standard services
 */
public abstract class BaseTest {


    FhirServerContext<Object> fhirServerContext;


    AfasConfiguration afasConfiguration;

    MessageSource messageSource;

    FhirOperationFactory fhirOperationFactory;

    protected void setup() {
        this.fhirServerContext = Mockito.mock(FhirServerContext.class);
        this.afasConfiguration = Mockito.mock(AfasConfiguration.class);
        this.messageSource = Mockito.mock(MessageSource.class);
        this.fhirOperationFactory = Mockito.mock(FhirOperationFactory.class);
        Mockito.when(afasConfiguration.getPublicUrl()).thenAnswer((p) -> "http://a/fhir/");
        Mockito.when(fhirServerContext.getMultiTenantService()).thenReturn(new TestMultitenantService());
        Mockito.when(fhirServerContext.getFhirStoreService()).thenReturn(Mockito.mock(FhirStoreService.class));
        Mockito.when(fhirServerContext.getNextUrlManager()).thenReturn(Mockito.mock(NextUrlManager.class));
        Mockito.when(fhirServerContext.getFhirContext()).thenReturn(FhirContext.forR4());
        Mockito.when(fhirServerContext.getExpressionFactory()).thenReturn(Mockito.mock(ExpressionFactory.class));
        Mockito.when(fhirServerContext.getSearchConfigService()).thenReturn(Mockito.mock(SearchConfigService.class));
        Mockito.when(fhirServerContext.getSecurityService()).thenReturn(Mockito.mock(SecurityService.class));

    }

}
