/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.error;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhirserver.service.exception.PublicException;
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
public class ErrorWriter {


    private ErrorWriter() {
    }

    /**
     * Write an error as an OperationOutcome to an async context.
     *
     * @param e       the exception
     * @param context the async context
     * @throws IOException if an error occur when we write the response
     */
    public static void writeError(Exception e, AsyncContext context) throws IOException {
        var parser = FhirContext.forR4().newJsonParser();
        var operationOutcome = new OperationOutcome();
        var operationOutcomeIssueComponent = operationOutcome.addIssue();
        operationOutcomeIssueComponent.setCode(OperationOutcome.IssueType.EXCEPTION);
        operationOutcomeIssueComponent.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        if (e instanceof PublicException) {
            operationOutcomeIssueComponent.setDiagnostics(e.getMessage());
        } else {
            operationOutcomeIssueComponent.setDiagnostics("Unknown error");
        }
        var writer = new PrintWriter(context.getResponse().getOutputStream());
        parser.encodeResourceToWriter(operationOutcome, writer);
    }
}
