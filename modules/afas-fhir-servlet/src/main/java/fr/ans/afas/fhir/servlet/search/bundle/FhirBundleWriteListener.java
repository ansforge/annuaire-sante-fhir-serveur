/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPageIterator;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.data.PagingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;


/**
 * Base class to write fhir bundle with nio servlet
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public abstract class FhirBundleWriteListener<T> implements WriteListener {

    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    final String serverUrl;
    ServletOutputStream output;
    AsyncContext context;
    FhirStoreService<T> fhirStoreService;
    ExpressionFactory<T> expressionFactory;
    SearchConfig searchConfig;
    FhirPageIterator fhirPageIterator;
    FhirBundleBuilder fhirBundleBuilder = new FhirBundleBuilder();
    NextUrlManager<T> nextUrlManager;
    int index = 0;
    SelectExpression<T> selectExpression;
    RenderingState state = RenderingState.HEADER;

    protected FhirBundleWriteListener(FhirStoreService<T> fhirStoreService, ExpressionFactory<T> expressionFactory, SearchConfig searchConfig, NextUrlManager<T> nextUrlManager,
                                      ServletOutputStream sos, AsyncContext c, SelectExpression<T> selectExpression, String serverUrl) {
        this.context = c;
        this.output = sos;
        this.fhirStoreService = fhirStoreService;
        this.expressionFactory = expressionFactory;
        this.searchConfig = searchConfig;
        this.nextUrlManager = nextUrlManager;
        this.selectExpression = selectExpression;
        this.serverUrl = serverUrl;
    }

    @Override
    public void onWritePossible() {
        try {
            while (output.isReady()) {
                switch (state) {
                    case HEADER:
                        writeHeader();
                        break;
                    case ENTRIES:
                        writeEntries();
                        break;
                    case FOOTER:
                        writeFooter();
                        break;
                }
            }

        } catch (Exception e) {
            logger.debug("Error writing the request", e);
            context.complete();
        }
    }

    private void writeFooter() throws IOException {
        if (fhirPageIterator.hasNext()) {
            var id = nextUrlManager.store(PagingData.<T>builder()
                    .pageSize(selectExpression.getCount())
                    .size(CountResult.builder().total(fhirPageIterator.searchContext().getTotal()).build())
                    .type(selectExpression.getFhirResource())
                    .selectExpression(selectExpression)
                    .uuid(UUID.randomUUID().toString())
                    .timestamp(fhirPageIterator.searchContext().getRevision())
                    .lastId(fhirPageIterator.searchContext().getFirstId())
                    .build());
            output.write(this.fhirBundleBuilder.getFooter(serverUrl, id).getBytes(Charset.defaultCharset()));
            context.complete();
        } else {
            output.write(this.fhirBundleBuilder.getFooter(null, null).getBytes(Charset.defaultCharset()));
            context.complete();
        }
    }

    private void writeEntries() throws IOException {
        if (fhirPageIterator.hasNext() && index < selectExpression.getCount()) {
            var entry = fhirPageIterator.next();
            var resourceAsByte = "";
            if (index++ > 0) {
                resourceAsByte += ",";
            }
            resourceAsByte += FhirBundleBuilder.wrapBundleEntry(entry);
            output.write(resourceAsByte.getBytes(Charset.defaultCharset()));
        } else {
            state = RenderingState.FOOTER;
        }
    }

    private void writeHeader() throws IOException {
        output.write(this.fhirBundleBuilder.getHeader(fhirPageIterator.searchContext().getTotal()).getBytes(Charset.defaultCharset()));
        state = RenderingState.ENTRIES;
    }


    @Override
    public void onError(Throwable throwable) {
        logger.debug("Error reading the request", throwable);
        context.complete();
    }


    enum RenderingState {
        HEADER,
        ENTRIES,
        FOOTER
    }
}
