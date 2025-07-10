/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.create;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhir.servlet.servletutils.CustomHttpServletResponse;
import fr.ans.afas.fhir.servlet.servletutils.DefaultReadListener;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import fr.ans.afas.fhirserver.service.exception.DataFormatFhirException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.DomainResource;
import org.springframework.context.MessageSource;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Async reader for the Fhir put operation
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Slf4j
public class PostResourceReadListener<T> extends DefaultReadListener {

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
    private final PostParams postParams;

    private final FhirServerContext<T> fhirServerContext;

    private final String serverBaseUrl;

    private final MessageSource messageSource;


    /**
     * Construct a read listener for the put operation
     *
     * @param fhirServerContext the context of the server (with services)
     * @param inputStream       the input stream of the request
     * @param response          the response
     * @param asyncContext      the context
     * @param postParams        parameters of the operation
     * @param serverBaseUrl     the base url of the server
     */
    public PostResourceReadListener(FhirServerContext<T> fhirServerContext, ServletInputStream inputStream, HttpServletResponse response,
                                    AsyncContext asyncContext, PostParams postParams,
                                    String serverBaseUrl, MessageSource messageSource) {
        super(asyncContext, inputStream);
        this.response = response;
        this.asyncContext = asyncContext;
        this.fhirServerContext = fhirServerContext;
        this.postParams = postParams;
        this.serverBaseUrl = serverBaseUrl;
        this.messageSource = messageSource;
    }

    @SneakyThrows
    @Override
    public void onAllDataReadInTenant() throws IOException {
        try {
            if (queue.isEmpty()) {
                throw new DataFormatFhirException(messageSource.getMessage("error.body.empty", null, Locale.getDefault()));
            }

            var requestContent = queue.stream().collect(Collectors.joining());
            if (requestContent.isEmpty()) {
                throw new DataFormatFhirException(messageSource.getMessage("error.body.empty", null, Locale.getDefault()));
            }


            var now = ZonedDateTime.now(TimeZone.getTimeZone("GMT").toZoneId());
            var lastModified = HttpUtils.lastModifiedFromDate(now);

            var output = response.getOutputStream();
            var parser = fhirServerContext.getFhirContext().newJsonParser();
            var fhirResource = (DomainResource) parser.parseResource(requestContent);
            fhirResource.setId((UUID.randomUUID().toString()));
            fhirResource.getMeta().setLastUpdated(Date.from(now.toInstant()));


            var idTypeList = fhirServerContext.getFhirStoreService().store(List.of(fhirResource), false);
            IIdType createdIdType = idTypeList.get(0);
            response.setStatus(HttpServletResponse.SC_CREATED); // 201 Created

            response.addHeader("Location", serverBaseUrl + HttpUtils.SERVLET_API_PATH + "/" + postParams.getResource() + "/" + createdIdType.getIdPart());
            response.addHeader("ETag", "1");
            response.addHeader("Last-Modified", lastModified);

            output.setWriteListener(new WriteListener() {
                @Override
                public void onWritePossible() throws IOException {
                    asyncContext.complete();
                }

                @Override
                public void onError(Throwable throwable) {
                    asyncContext.complete();
                }
            });
        } catch (Exception e) {
            if (e instanceof DataFormatException || e instanceof ConfigurationException || e instanceof UnprocessableEntityException) { // Supón que UnprocessableEntityException es lanzada para errores de reglas de negocio
                ErrorWriter.writeError(e, context, CustomHttpServletResponse.SC_UNPROCESSABLE_ENTITY);
            } else if (e instanceof DataFormatFhirException) {
                ErrorWriter.writeError(e, context, HttpServletResponse.SC_BAD_REQUEST);
            } else {
                ErrorWriter.writeError(e, context, HttpServletResponse.SC_NOT_FOUND);
            }
            asyncContext.complete();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        log.debug("Error reading the request", throwable);
        asyncContext.complete();
    }
}
