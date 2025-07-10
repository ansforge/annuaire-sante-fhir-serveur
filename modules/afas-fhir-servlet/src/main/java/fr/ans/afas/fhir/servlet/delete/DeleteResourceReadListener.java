/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.delete;

import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhir.servlet.servletutils.DefaultReadListener;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;

import java.io.IOException;

/**
 * @param <T>
 * @author aelqadim
 */
@Slf4j
public class DeleteResourceReadListener<T> extends DefaultReadListener {

    private final HttpServletResponse response;
    private final AsyncContext asyncContext;
    private final DeleteParams deleteParams;
    private final FhirServerContext<T> fhirServerContext;

    public DeleteResourceReadListener(FhirServerContext<T> fhirServerContext, ServletInputStream input, HttpServletResponse response, AsyncContext asyncContext,
                                      DeleteParams deleteParams) {
        super(asyncContext, input);
        this.response = response;
        this.asyncContext = asyncContext;
        this.fhirServerContext = fhirServerContext;
        this.deleteParams = deleteParams;
    }

    @Override
    public void onAllDataReadInTenant() throws IOException {
        try {
            // Convert the resource ID to IIdType
            IIdType idType = new IdType(deleteParams.getId());

            // Perform the delete operation
            boolean deleted = fhirServerContext.getFhirStoreService().businessDelete(deleteParams.getResource(), idType);

            // Set the HTTP status code based on the delete operation result
            if (deleted) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 No Content
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404 Not Found
            }

            // Complete the async context
            asyncContext.complete();

        } catch (Exception e) {
            log.error("Error deleting resource", e);
            if (e instanceof MethodNotAllowedException) {
                ErrorWriter.writeError(e, asyncContext, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            } else {
                ErrorWriter.writeError(e, asyncContext, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            asyncContext.complete();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("Error in async delete request", throwable);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        asyncContext.complete();
    }
}
