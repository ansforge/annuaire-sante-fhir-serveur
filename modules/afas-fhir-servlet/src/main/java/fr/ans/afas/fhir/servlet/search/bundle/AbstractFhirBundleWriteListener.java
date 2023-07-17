/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.fhir.servlet.exception.UnknownErrorWritingResponse;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPageIterator;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.data.PagingData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Base class to write fhir bundle with nio servlet
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractFhirBundleWriteListener<T> implements WriteListener {
    private final FhirStoreService<T> fhirStoreService;
    private final String serverUrl;
    private final ServletOutputStream output;
    private final AsyncContext context;
    private final NextUrlManager<T> nextUrlManager;
    private final SelectExpression<T> selectExpression;
    private final FhirBundleBuilder fhirBundleBuilder = new FhirBundleBuilder();
    private final FhirPageIterator fhirPageIterator;
    Iterator<FhirBundleBuilder.BundleEntry> includeCursor;
    private RenderingState state = RenderingState.HEADER;
    private int index = 0;
    private Map<String, Set<String>> toInclude;
    private Set<String> toRevInclude;

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
                    case INCLUDES:
                        writeIncludes();
                        break;
                    case REVINCLUDES:
                        writeRevIncludes();
                        break;
                    case FOOTER:
                        writeFooter();
                        break;
                }
            }
        } catch (Exception e) {
            log.debug("Error writing the request", e);
            context.complete();
        }
    }

    private void writeFooter() throws IOException {
        if (fhirPageIterator.hasNextPage()) {
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
            return;
        }
        output.write(this.fhirBundleBuilder.getFooter(null, null).getBytes(Charset.defaultCharset()));
        context.complete();
    }

    private void writeEntries() throws UnknownErrorWritingResponse {
        try {
            if (fhirPageIterator.hasNext()) {
                var entry = fhirPageIterator.next();
                var resourceAsByte = "";
                if (index++ > 0) {
                    resourceAsByte += ",";
                }
                resourceAsByte += FhirBundleBuilder.wrapBundleEntry(entry);
                output.write(resourceAsByte.getBytes(Charset.defaultCharset()));
            } else {
                fhirPageIterator.close();
                // prepare the rendering of includes:
                toInclude = fhirPageIterator.getIncludesTypeReference();
                toRevInclude = fhirPageIterator.getRevIncludeIds();
                state = RenderingState.INCLUDES;
            }
        } catch (Exception e) {
            log.debug("Error writing fhir response", e);
            throw new UnknownErrorWritingResponse(e.getMessage());
        }
    }

    private void writeIncludes() throws IOException {
        if (toInclude != null && !toInclude.isEmpty()) {
            // initialize the cursor:
            if (includeCursor == null) {
                var resource = toInclude.entrySet().stream().findAny();
                if (resource.isPresent()) {
                    includeCursor = fhirStoreService.findByIds(fhirPageIterator.searchContext().getRevision(), resource.get().getKey(), resource.get().getValue());
                }
            }// or write the response:
            if (includeCursor != null && includeCursor.hasNext()) {
                var entry = includeCursor.next();
                var resourceAsByte = ",";
                resourceAsByte += FhirBundleBuilder.wrapBundleEntry(entry);
                output.write(resourceAsByte.getBytes(Charset.defaultCharset()));
            } else {
                if (includeCursor != null) {
                    includeCursor = null;
                }
                state = RenderingState.REVINCLUDES;
            }
        } else {
            state = RenderingState.REVINCLUDES;
        }
    }

    private void writeRevIncludes() throws IOException {
        state = RenderingState.FOOTER;
    }


    private void writeHeader() throws IOException {
        output.write(this.fhirBundleBuilder.getHeader(fhirPageIterator.searchContext().getTotal()).getBytes(Charset.defaultCharset()));
        state = RenderingState.ENTRIES;

    }


    @Override
    public void onError(Throwable throwable) {
        log.debug("Error reading the request", throwable);
        context.complete();
    }


    enum RenderingState {
        HEADER,
        ENTRIES,
        INCLUDES,
        REVINCLUDES,
        FOOTER
    }
}
