/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.audit;

import com.mongodb.client.MongoClient;
import fr.ans.afas.MongoConfig;
import fr.ans.afas.config.MongoIndexConfiguration;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.audit.DefaultReadAuditService;
import fr.ans.afas.fhirserver.service.audit.DefaultWriteAuditService;
import fr.ans.afas.servlet.FhirServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public abstract class BaseAuditTest {

    @Autowired(required = false)
    protected AuditFilter auditFilter;

    @Autowired(required = false)
    protected DefaultReadAuditService defaultReadAuditService;

    @Autowired(required = false)
    protected DefaultWriteAuditService defaultWriteAuditService;

    @MockBean
    FhirStoreService<?> fhirStoreService;

    @MockBean
    FhirServlet fhirServlet;

    @MockBean
    MongoConfig mongoConfig;

    @MockBean
    MongoClient mongoClient;

    @MockBean
    MongoIndexConfiguration mongoIndexConfiguration;
}
