/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.metadata;

import fr.ans.afas.fhirserver.search.config.SearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
@RequiredArgsConstructor
public class CapabilityStatementReadListener implements ReadListener {

    /**
     * The servlet response
     */
    private final HttpServletResponse response;

    /**
     * The context
     */
    private final AsyncContext asyncContext;
    /**
     * The search configuration
     */
    private final SearchConfig searchConfig;

    @Override
    public void onDataAvailable() {
        // no data for the capability statement
    }

    @Override
    public void onAllDataRead() throws IOException {
        var output = response.getOutputStream();
        var writeListener = new CapabilityStatementWriteListener(output, asyncContext, searchConfig);
        output.setWriteListener(writeListener);
    }

    @Override
    public void onError(Throwable throwable) {
        log.debug("Error reading the request", throwable);
        asyncContext.complete();
    }
}
