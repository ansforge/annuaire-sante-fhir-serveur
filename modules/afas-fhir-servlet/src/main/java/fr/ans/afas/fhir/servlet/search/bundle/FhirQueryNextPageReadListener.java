/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirQueryNextPageReadListener<T> extends FhirQueryReadListener<T> {

    private final String serverUrl;
    private final String pageId;

    private final HttpServletResponse res;

    public FhirQueryNextPageReadListener(FhirStoreService<T> fhirStoreService,
                                         ExpressionFactory<T> expressionFactory,
                                         SearchConfig searchConfig,
                                         NextUrlManager<T> nextUrlManager,
                                         ServletInputStream in,
                                         HttpServletResponse r,
                                         AsyncContext c,
                                         String pageId,
                                         String serverUrl) {
        super(fhirStoreService, expressionFactory, searchConfig, nextUrlManager, c, in);
        res = r;
        this.pageId = pageId;
        this.serverUrl = serverUrl;

    }


    public void onAllDataRead() throws IOException {
        try {
            var url = nextUrlManager.find(pageId);
            if (url.isEmpty()) {
                throw new BadRequestException("Error getting the next page. Context not found");
            }
            var pagingData = PagingData.<T>builder()
                    .pageSize(url.get().getPageSize())
                    .lastId(url.get().getLastId())
                    .uuid(pageId)
                    .size(url.get().getSize())
                    .timestamp(url.get().getTimestamp())
                    .type(url.get().getType())
                    .selectExpression(url.get()
                            .getSelectExpression()).build();
            // now all data are read, set up a WriteListener to write
            var output = res.getOutputStream();
            var writeListener = new FhirBundleNextPageWriteListener<>(fhirStoreService, nextUrlManager, output, ac, pagingData, this.serverUrl);
            output.setWriteListener(writeListener);

        } catch (BadLinkException e) {
            ErrorWriter.writeError(e, ac);
            ac.complete();
        }

    }


}
