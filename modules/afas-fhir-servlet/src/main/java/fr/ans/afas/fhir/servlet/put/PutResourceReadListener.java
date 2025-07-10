/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.put;

import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhir.servlet.exception.IdDoesntMatchException;
import fr.ans.afas.fhir.servlet.servletutils.DefaultReadListener;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.DomainResource;
import org.springframework.context.MessageSource;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;


/**
 * Async reader for the Fhir put operation
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Slf4j
public class PutResourceReadListener<T> extends DefaultReadListener {

    private final FhirServerContext<T> fhirServerContext;
    /**
     * The servlet response
     */
    private final HttpServletResponse response;
    /**
     * The context
     */
    private final AsyncContext asyncContext;
    /**
     * Parameters of the operation
     */
    private final PutParams putParams;
    private final String serverBaseUrl;
    private final MessageSource messageSource;


    /**
     * Construct a read listener for the put operation
     *
     * @param fhirServerContext context with services of the fhir server
     * @param inputStream       the input stream of the request
     * @param response          the response
     * @param asyncContext      the context
     * @param putParams         parameters of the operation
     * @param serverBaseUrl     the base url of the server
     */
    public PutResourceReadListener(FhirServerContext<T> fhirServerContext, ServletInputStream inputStream, HttpServletResponse response, AsyncContext asyncContext,
                                   PutParams putParams, String serverBaseUrl, MessageSource messageSource) {
        super(asyncContext, inputStream);
        this.response = response;
        this.asyncContext = asyncContext;
        this.fhirServerContext = fhirServerContext;
        this.putParams = putParams;
        this.serverBaseUrl = serverBaseUrl;
        this.messageSource = messageSource;
    }

    @Override
    public void onAllDataReadInTenant() throws IOException {
        try {
            if (queue.isEmpty()) return;// REMOVE

            var requestContent = queue.stream().collect(Collectors.joining());

            var output = response.getOutputStream();
            var parser = fhirServerContext.getFhirContext().newJsonParser();
            var fhirResource = (DomainResource) parser.parseResource(requestContent);


            var idWithResource = putParams.getId();
            if (!idWithResource.equals(fhirResource.getIdPart())) {
                throw new IdDoesntMatchException(messageSource.getMessage("error.id.doesnt.match", new Object[]{idWithResource, fhirResource.getId()}, Locale.getDefault()));
            }


            var now = ZonedDateTime.now(TimeZone.getTimeZone("GMT").toZoneId());
            var lastModified = HttpUtils.lastModifiedFromDate(now);
            fhirResource.getMeta().setLastUpdated(Date.from(now.toInstant()));


            // Store the resource:
            var ret = this.fhirServerContext.getFhirStoreService().store(List.of(fhirResource), false);
            if (ret.get(0).getVersionIdPart() != null && ret.get(0).getVersionIdPartAsLong() == 1) {
                response.setStatus(HttpServletResponse.SC_CREATED);
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
            }


            // Set HTTP headers:
            response.addHeader("Location", serverBaseUrl + HttpUtils.SERVLET_API_PATH + "/" + ret.get(0).getResourceType() + "/" + ret.get(0).getIdPart());
            response.addHeader("ETag", ret.get(0).getVersionIdPart());
            response.addHeader("Last-Modified", lastModified);

            output.setWriteListener(new WriteListener() {
                @Override
                public void onWritePossible() {
                    asyncContext.complete();
                }

                @Override
                public void onError(Throwable throwable) {
                    asyncContext.complete();
                }
            });
        } catch (IdDoesntMatchException e) {
            ErrorWriter.writeError(e, asyncContext, HttpServletResponse.SC_BAD_REQUEST);
            asyncContext.complete();
        } catch (Exception e) {
            ErrorWriter.writeError(e, asyncContext, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            asyncContext.complete();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        log.debug("Error reading the request", throwable);
        asyncContext.complete();
    }
}
