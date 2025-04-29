/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.error;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import fr.ans.afas.fhirserver.service.exception.PublicException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.OperationOutcome;

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
     * Write an OperationOutcome to an async context from a message.
     *
     * @param message the message of the operation outcome
     * @param context the async context
     */
    public static void writeError(String message, AsyncContext context, int status) {
        var operationOutcome = new OperationOutcome();
        var operationOutcomeIssueComponent = operationOutcome.addIssue();
        operationOutcomeIssueComponent.setCode(OperationOutcome.IssueType.EXCEPTION);
        operationOutcomeIssueComponent.setSeverity(OperationOutcome.IssueSeverity.ERROR);
        operationOutcomeIssueComponent.setDiagnostics(message);
        HttpServletResponse response = (HttpServletResponse) context.getResponse();
        response.setStatus(status);

        try {
            var writer = new PrintWriter(response.getOutputStream());
            FhirContext.forR4().newJsonParser().encodeResourceToWriter(operationOutcome, writer);
        } catch (IOException ex) {
            log.debug("Error writing the error", ex);
        }
    }

    /**
     * Write an error as an OperationOutcome to an async context.
     *
     * @param e       the exception
     * @param context the async context
     */
    public static void writeError(Exception e, AsyncContext context, int status) {
        if (e instanceof PublicException || e instanceof DataFormatException || e instanceof ConfigurationException) {
            ErrorWriter.writeError(e.getMessage(), context, status);
        } else {
            ErrorWriter.writeError("Unknown error", context, status);
        }
    }
}
