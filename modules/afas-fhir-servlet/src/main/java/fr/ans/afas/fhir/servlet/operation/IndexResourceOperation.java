/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.operation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.param.DateParam;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhirserver.search.exception.BadParametersException;
import fr.ans.afas.fhirserver.service.IndexService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import java.io.PrintWriter;

/**
 * Fhir operation that launch the refresh of indexes
 */
@Slf4j
public class IndexResourceOperation implements Runnable{

    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final AsyncContext context;

    private final IndexService indexService;

    /**
     * Construct the index resource fhir operation
     * @param context
     * @param indexService
     */
    public IndexResourceOperation(AsyncContext context, IndexService indexService){
        this.context = context;
        this.indexService = indexService;
    }

    /**
     * Launch the fhir resource
     */
    @Override
    public void run() {
        try (var out = context.getResponse().getOutputStream();
             var writer = new PrintWriter(out)) {

            var paramDate = context.getRequest().getParameter("fromDate");
            if (paramDate == null) {
                throw new BadParametersException("The parameter fromDate is required. This is a date with the fhir format. Ex: fromDate=2023-07-25");
            }

            var p = new DateParam();
            p.setValueAsString(paramDate);

            indexService.refreshIndexes(p.getValue().getTime());
            var operationOutcome = new OperationOutcome();
            var operationOutcomeIssueComponent = operationOutcome.addIssue();
            operationOutcomeIssueComponent.setCode(OperationOutcome.IssueType.PROCESSING);
            operationOutcomeIssueComponent.setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
            FhirContext.forR4().newJsonParser().encodeResourceToWriter(operationOutcome, writer);
        } catch (Exception e) {
            logger.debug("Error rendering the operation response", e);
            ErrorWriter.writeError(e, context);
        } finally {
            context.complete();
        }
    }
}
