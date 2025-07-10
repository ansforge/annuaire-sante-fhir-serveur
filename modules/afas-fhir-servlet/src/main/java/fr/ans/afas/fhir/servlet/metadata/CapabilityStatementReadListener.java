/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.metadata;

import fr.ans.afas.fhir.servlet.exception.BadRequestException;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ReadListener;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;


/**
 * Async reader for the capability statement handler (/metadata).
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
// TODO, refactor, ici on est pas lié au tenant
@Slf4j
@RequiredArgsConstructor
public class CapabilityStatementReadListener<T> implements ReadListener {

    private final FhirServerContext<T> fhirServerContext;

    /**
     * The servlet response
     */
    private final HttpServletResponse response;

    /**
     * The context
     */
    private final AsyncContext asyncContext;


    @Override
    public void onDataAvailable() {
        // no data for the capability statement
        throw new BadRequestException("This endpoint doens't support body");
    }

    @Override
    public void onAllDataRead() throws IOException {
        var output = response.getOutputStream();
        var writeListener = new CapabilityStatementWriteListener<>(fhirServerContext, output, asyncContext);
        output.setWriteListener(writeListener);
    }

    @Override
    public void onError(Throwable throwable) {
        log.debug("Error reading the request", throwable);
        asyncContext.complete();
    }
}
