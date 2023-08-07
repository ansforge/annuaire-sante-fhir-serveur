/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.operation;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhirserver.service.IndexService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import java.io.PrintWriter;

/**
 * Fhir operation that show the status of indexing process
 */
@Slf4j
public class IndexResourceStatusOperation implements Runnable {

    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AsyncContext context;

    private final IndexService indexService;

    /**
     * Construct the index status fhir operation
     *
     * @param context
     * @param indexService
     */
    public IndexResourceStatusOperation(AsyncContext context, IndexService indexService) {
        this.context = context;
        this.indexService = indexService;
    }

    /**
     * Get the status and generate an operation outcome
     */
    @Override
    public void run() {
        try (var out = context.getResponse().getOutputStream();
             var writer = new PrintWriter(out)) {
            var operationOutcome = new OperationOutcome();
            var operationOutcomeIssueComponent = operationOutcome.addIssue();
            operationOutcomeIssueComponent.setCode(OperationOutcome.IssueType.VALUE);
            operationOutcomeIssueComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
            if (indexService.isRunning()) {
                operationOutcomeIssueComponent.setDiagnostics("Running");
            } else {
                operationOutcomeIssueComponent.setDiagnostics("Not running");
            }
            FhirContext.forR4().newJsonParser().encodeResourceToWriter(operationOutcome, writer);
        } catch (Exception e) {
            logger.debug("Error rendering the operation response", e);
            ErrorWriter.writeError(e, context);
        } finally {
            context.complete();
        }
    }
}
