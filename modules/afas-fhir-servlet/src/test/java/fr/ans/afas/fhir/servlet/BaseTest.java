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
import org.springframework.context.MessageSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A test class with standard services
 */
public abstract class BaseTest {
    FhirServerContext<Object> fhirServerContext;

    AfasConfiguration afasConfiguration;

    MessageSource messageSource;

    FhirOperationFactory fhirOperationFactory;

    FhirStoreService fhirStoreServiceMock;

    protected void setup() {
        this.fhirServerContext = mock(FhirServerContext.class);
        this.afasConfiguration = mock(AfasConfiguration.class);
        this.messageSource = mock(MessageSource.class);
        this.fhirOperationFactory = mock(FhirOperationFactory.class);
        this.fhirStoreServiceMock = mock(FhirStoreService.class);
        when(afasConfiguration.getPublicUrl()).thenAnswer((p) -> "http://a/fhir/");
        when(fhirServerContext.getMultiTenantService()).thenReturn(new TestMultitenantService());
        when(fhirServerContext.getFhirStoreService()).thenReturn(fhirStoreServiceMock);
        when(fhirServerContext.getNextUrlManager()).thenReturn(mock(NextUrlManager.class));
        when(fhirServerContext.getFhirContext()).thenReturn(FhirContext.forR4());
        when(fhirServerContext.getExpressionFactory()).thenReturn(mock(ExpressionFactory.class));
        when(fhirServerContext.getSearchConfigService()).thenReturn(mock(SearchConfigService.class));
        when(fhirServerContext.getSecurityService()).thenReturn(mock(SecurityService.class));

    }

}
