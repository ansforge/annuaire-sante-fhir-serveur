/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.read;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhir.servlet.exception.ResourceNotFoundException;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.IdType;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.nio.charset.Charset;


/**
 * Write a resource
 *
 * @param <T>
 */
@Slf4j
@RequiredArgsConstructor
public class ReadResourceWriteListener<T> implements WriteListener {

    /**
     * The servlet output stream
     */
    private final ServletOutputStream sos;

    /**
     * The async context
     */
    private final AsyncContext context;

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
    public void onWritePossible() {
        try {
            var found = fhirStoreService.findById(this.readSearchParams.getResource(), new IdType(this.readSearchParams.getId()));
            if (found == null) {
                throw new ResourceNotFoundException("Resource not found with id: " + this.readSearchParams.getId());
            }
            sos.write(fhirContext.newJsonParser().encodeResourceToString(found).getBytes(Charset.defaultCharset()));
            context.complete();
        } catch (ResourceNotFoundException resourceNotFoundException) {
            ErrorWriter.writeError(resourceNotFoundException, context);
            context.complete();
        } catch (Exception e) {
            log.debug("Error writing the request", e);
            context.complete();
        }
    }


    @Override
    public void onError(Throwable throwable) {
        log.debug("Error writing the request", throwable);
        context.complete();
    }
}
