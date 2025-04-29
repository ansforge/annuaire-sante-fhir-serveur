/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.read;

import fr.ans.afas.exception.ResourceNotFoundException;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhir.servlet.servletutils.DefaultWriteListener;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.IdType;
import java.nio.charset.Charset;

/**
 * Write a resource
 *
 * @param <T>
 */
@Slf4j
public class ReadResourceWriteListener<T> extends DefaultWriteListener {

    private final FhirServerContext<T> fhirServerContext;

    /**
     * The servlet output stream
     */
    private final ServletOutputStream sos;


    /**
     * Parameters of the read operation
     */
    private final ReadSearchParams readSearchParams;


    public ReadResourceWriteListener(FhirServerContext<T> fhirServerContext, ServletOutputStream sos, AsyncContext context, ReadSearchParams readSearchParams) {
        super(context);
        this.sos = sos;
        this.fhirServerContext = fhirServerContext;
        this.readSearchParams = readSearchParams;
    }

    @Override
    public void onWritePossibleInTenant() {
        try {
            var found = fhirServerContext.getFhirStoreService().findById(this.readSearchParams.getResource(), new IdType(this.readSearchParams.getId()));
            if (found == null) {
                throw new ResourceNotFoundException("Resource not found with id: " + this.readSearchParams.getId());
            }
            sos.write(fhirServerContext.getFhirContext().newJsonParser().encodeResourceToString(found).getBytes(Charset.defaultCharset()));
            context.complete();
        } catch (ResourceNotFoundException resourceNotFoundException) {
            ErrorWriter.writeError(resourceNotFoundException, context, HttpServletResponse.SC_NOT_FOUND);
            context.complete();
        } catch (Exception e) {
            log.debug("Error writing the request", e);
            ErrorWriter.writeError("Unexpected error", context, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            context.complete();
        }
    }


    @Override
    public void onError(Throwable throwable) {
        log.debug("Error writing the request", throwable);
        ErrorWriter.writeError("Unexpected error", context, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        context.complete();
    }
}
