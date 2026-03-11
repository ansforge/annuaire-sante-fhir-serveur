/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.operation;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.domain.ResourceAndSubResources;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;

/**
 * Fhir operation that show the status of indexing process
 */
@Slf4j
public class StoreWithDependenciesOperation implements Runnable {

    private static final FhirContext fhirContext = FhirContext.forR4();

    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());


    private final AsyncContext context;

    private final FhirStoreService fhirStoreService;

    private final String body;

    /**
     *
     * @param context
     * @param fhirStoreService
     */
    public StoreWithDependenciesOperation(AsyncContext context, FhirStoreService fhirStoreService, String requestBody) {
        this.context = context;
        this.fhirStoreService = fhirStoreService;
        this.body = requestBody;
    }

    /**
     * Get the status and generate an operation outcome
     */
    @Override
    public void run() {
        try (var out = context.getResponse().getOutputStream(); var writer = new PrintWriter(out)) {

            // Parse request
            // On passe par un Parameters pour être conforme à la spec FHIR pour les opérations
            var fhirBundle = (Bundle) ((Parameters) fhirContext.newJsonParser().parseResource(this.body)).getParameterFirstRep().getResource();

            // On mappe chaque sous-objet contenu dans contained comme un élément de la liste des sous-ressources (links);
            var resourceAndSubResources = fhirBundle.getEntry().stream().map(r -> {
                DomainResource dr = (DomainResource) r.getResource();
                List<DomainResource> subResources = dr.getContained().stream().map(sr -> (DomainResource) sr).toList();
                dr.setContained(null);
                return new ResourceAndSubResources(dr, subResources);
            }).toList();

            // process request
            this.fhirStoreService.storeWithDependencies(resourceAndSubResources, false, true);


            // Return status
            var operationOutcome = new OperationOutcome();
            var operationOutcomeIssueComponent = operationOutcome.addIssue();
            operationOutcomeIssueComponent.setCode(OperationOutcome.IssueType.VALUE);
            operationOutcomeIssueComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
            operationOutcomeIssueComponent.setDiagnostics("Done");
            FhirContext.forR4().newJsonParser().encodeResourceToWriter(operationOutcome, writer);
        } catch (Exception e) {
            logger.debug("Error rendering the operation response", e);
            ErrorWriter.writeError(e, context, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            context.complete();
        }
    }
}
