/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.transaction;

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
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.context.MessageSource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Slf4j
public class TransactionReadListener<T> extends DefaultReadListener {


    /**
     * The servlet response
     */
    private final HttpServletResponse response;

    /**
     * The context
     */
    private final AsyncContext asyncContext;

    private final String publicUrl;

    private final MessageSource messageSource;

    private final FhirServerContext<T> fhirServerContext;

    public TransactionReadListener(FhirServerContext<T> fhirServerContext, ServletInputStream inputStream, HttpServletResponse response, AsyncContext asyncContext,
                                   String publicUrl, MessageSource messageSource) {
        super(asyncContext, inputStream);
        this.fhirServerContext = fhirServerContext;
        this.response = response;
        this.asyncContext = asyncContext;
        this.publicUrl = publicUrl;
        this.messageSource = messageSource;
    }


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
            var fhirBundle = (Bundle) parser.parseResource(requestContent);

            TransactionalResourceProvider<T> transactionalResourceProvider = new TransactionalResourceProvider<>(fhirServerContext.getFhirStoreService());
            Bundle bundle = transactionalResourceProvider.transaction(fhirBundle);

            // Set HTTP headers:
            response.addHeader("Location", publicUrl + HttpUtils.SERVLET_API_PATH + "/");
            response.addHeader("Last-Modified", lastModified);
            response.setStatus(HttpServletResponse.SC_OK);
            log.info("{} resources traited", bundle.getEntry().size());
            output.write(parser.encodeResourceToString(bundle).getBytes(Charset.defaultCharset()));
            asyncContext.complete();
        } catch (Exception e) {
            if (e instanceof DataFormatException || e instanceof ConfigurationException || e instanceof UnprocessableEntityException) { // Supón que UnprocessableEntityException es lanzada para errores de reglas de negocio
                ErrorWriter.writeError(e, asyncContext, CustomHttpServletResponse.SC_UNPROCESSABLE_ENTITY);
            } else if (e instanceof DataFormatFhirException) {
                ErrorWriter.writeError(e, asyncContext, HttpServletResponse.SC_BAD_REQUEST);
            } else {
                ErrorWriter.writeError(e, asyncContext, HttpServletResponse.SC_NOT_FOUND);
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
