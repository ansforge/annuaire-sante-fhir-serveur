/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.error;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhirserver.service.exception.PublicException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.OperationOutcome;

import javax.servlet.AsyncContext;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Write operation outcome based on an exception
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorWriter {

    /**
     * Write an error as an OperationOutcome to an async context.
     *
     * @param e       the exception
     * @param context the async context
     */
    public static void writeError(Exception e, AsyncContext context) {
        var operationOutcome = new OperationOutcome();
        var operationOutcomeIssueComponent = operationOutcome.addIssue();
        operationOutcomeIssueComponent.setCode(OperationOutcome.IssueType.EXCEPTION);
        operationOutcomeIssueComponent.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        if (e instanceof PublicException) {
            operationOutcomeIssueComponent.setDiagnostics(e.getMessage());
        } else {
            operationOutcomeIssueComponent.setDiagnostics("Unknown error");
        }
        try {
            var writer = new PrintWriter(context.getResponse().getOutputStream());
            FhirContext.forR4().newJsonParser().encodeResourceToWriter(operationOutcome, writer);
        } catch (IOException ex) {
            log.debug("Error writing the error", e);
        }

    }
}
