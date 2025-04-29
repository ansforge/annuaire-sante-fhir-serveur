/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;

import java.io.IOException;
import java.util.Locale;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirQueryNextPageReadListener<T> extends FhirQueryReadListener<T> {

    private final String pageId;

    private final HttpServletResponse res;

    private final AfasConfiguration afasConfiguration;

    private final MessageSource messageSource;

    public FhirQueryNextPageReadListener(FhirServerContext<T> fhirServerContext,
                                         ServletInputStream in,
                                         HttpServletResponse r,
                                         AsyncContext c,
                                         String pageId,
                                         AfasConfiguration afasConfiguration,
                                         MessageSource messageSource) {
        super(fhirServerContext, in, c);
        res = r;
        this.pageId = pageId;
        this.afasConfiguration = afasConfiguration;
        this.messageSource = messageSource;
    }


    public void onAllDataReadInTenant() throws IOException {
        try {
            var url = fhirServerContext.getNextUrlManager().find(pageId);
            if (url.isEmpty()) {
                throw new BadRequestException(messageSource.getMessage("error.next.page.notFound", null, Locale.getDefault()));
            }
            var pagingData = PagingData.<T>builder()
                    .pageSize(url.get().getPageSize())
                    .lastId(url.get().getLastId())
                    .uuid(pageId)
                    .size(url.get().getSize())
                    .timestamp(url.get().getTimestamp())
                    .type(url.get().getType())
                    .elements(url.get().getElements())
                    .selectExpression(url.get().getSelectExpression())
                    .build();
            // now all data are read, set up a WriteListener to write
            var output = res.getOutputStream();
            var writeListener = new FhirBundleNextPageWriteListener<>(fhirServerContext, output, ac, pagingData, afasConfiguration);
            output.setWriteListener(writeListener);

        } catch (BadLinkException e) {
            ErrorWriter.writeError(e, ac, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ac.complete();
        }

    }


}
