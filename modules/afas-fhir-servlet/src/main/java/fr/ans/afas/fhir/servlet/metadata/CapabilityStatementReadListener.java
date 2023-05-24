/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.metadata;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Async reader for the capability statement handler (/metadata).
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class CapabilityStatementReadListener implements ReadListener {

    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The servlet response
     */
    final HttpServletResponse response;
    /**
     * The fhir context
     */
    final FhirContext context;
    /**
     * The context
     */
    final AsyncContext asyncContext;
    /**
     * The search configuration
     */
    final SearchConfig searchConfig;


    /**
     * Construct the read listener for the capability statement
     *
     * @param r            the response
     * @param asyncContext the async context
     * @param context      the fhir context
     * @param searchConfig the search config
     */
    public CapabilityStatementReadListener(HttpServletResponse r, AsyncContext asyncContext, FhirContext context, SearchConfig searchConfig) {
        this.response = r;
        this.context = context;
        this.asyncContext = asyncContext;
        this.searchConfig = searchConfig;
    }

    @Override
    public void onDataAvailable() {
        // no data for the capability statement
    }

    @Override
    public void onAllDataRead() throws IOException {
        var output = response.getOutputStream();
        var writeListener = new CapabilityStatementWriteListener(output, asyncContext, context, searchConfig);
        output.setWriteListener(writeListener);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.debug("Error reading the request", throwable);
        asyncContext.complete();
    }
}
