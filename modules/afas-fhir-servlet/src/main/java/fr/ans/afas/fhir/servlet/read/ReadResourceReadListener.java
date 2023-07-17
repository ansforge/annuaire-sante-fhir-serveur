/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.read;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Async reader for the Fhir read operation
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ReadResourceReadListener<T> implements ReadListener {

    /**
     * The servlet response
     */
    private final HttpServletResponse response;

    /**
     * The context
     */
    private final AsyncContext asyncContext;
    /**
     * The fhir store service
     */
    private final FhirStoreService<T> fhirStoreService;

    /**
     * Parameters of the read operation
     */
    private final ReadSearchParams readSearchParams;

    /**
     * The fhir context
     */
    private final FhirContext fhirContext;

    @Override
    public void onDataAvailable() {
        // no data for the read operation
    }

    @Override
    public void onAllDataRead() throws IOException {
        var output = response.getOutputStream();
        var writeListener = new ReadResourceWriteListener<T>(output, asyncContext, fhirStoreService, readSearchParams, fhirContext);
        output.setWriteListener(writeListener);
    }

    @Override
    public void onError(Throwable throwable) {
        log.debug("Error reading the request", throwable);
        asyncContext.complete();
    }
}
