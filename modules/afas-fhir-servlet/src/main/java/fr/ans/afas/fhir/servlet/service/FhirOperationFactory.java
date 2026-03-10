/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.service;

import fr.ans.afas.fhir.servlet.operation.IndexResourceOperation;
import fr.ans.afas.fhir.servlet.operation.IndexResourceStatusOperation;
import fr.ans.afas.fhir.servlet.operation.StoreWithDependenciesOperation;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.IndexService;
import jakarta.servlet.AsyncContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * A factory to get an operation with its name
 */
@Component
public class FhirOperationFactory {

    /**
     * Index service for operations that use indexes
     */
    @Autowired
    IndexService indexService;

    @Autowired
    FhirStoreService fhirStoreService;

    public Runnable findOperationByName(String name, AsyncContext context) {
        return switch (name) {
            case "$index" -> new IndexResourceOperation(context, indexService);
            case "$index-status" -> new IndexResourceStatusOperation(context, indexService);
            default -> throw new UnsupportedOperationException("Fhir operation not supported");
        };
    }

    public Runnable findWriteOperationByName(String name, AsyncContext context, String body) {
        return switch (name) {
            case "$store-with-deps" -> new StoreWithDependenciesOperation(context, fhirStoreService, body);
            default -> throw new UnsupportedOperationException("Fhir write operation not supported");
        };
    }
}
