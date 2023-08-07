/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.fhir.servlet.exception.UnknownErrorWritingResponse;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPageIterator;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.CantWriteFhirResource;
import fr.ans.afas.utils.ConditionMatching;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.DomainResource;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;


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
    private final AfasConfiguration afasConfiguration;
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

    static IParser parser = FhirContext.forR4().newJsonParser();

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
            log.error("Error writing the request", e);
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
            output.write(this.fhirBundleBuilder.getFooter(afasConfiguration.getPublicUrl(), id).getBytes(Charset.defaultCharset()));
            context.complete();
            return;
        }
        output.write(this.fhirBundleBuilder.getFooter(null, null).getBytes(Charset.defaultCharset()));
        context.complete();
    }

    private void writeEntries() throws UnknownErrorWritingResponse {
        try {
            boolean match =
                ConditionMatching.ifExecute(fhirPageIterator::hasNext, x -> writeIteratorEntries())
                .elseIfExecute(() -> this.toInclude != null && !this.toInclude.isEmpty(), x -> state = RenderingState.INCLUDES)
                .elseIfExecute(() -> this.toRevInclude != null && !this.toRevInclude.isEmpty(), x -> this.addRevIncludes(1))
                .matches();

            if(!match) {
                fhirPageIterator.close();
                state = RenderingState.FOOTER;
            }
        } catch (Exception e) {
            log.error("Error writing fhir response", e);
            throw new UnknownErrorWritingResponse(e.getMessage());
        }
    }

    private void writeIteratorEntries() {
        try {
            var entry = fhirPageIterator.next();
            var resourceAsByte = "";
            if (index++ > 0) {
                resourceAsByte += ",";
            }
            resourceAsByte += FhirBundleBuilder.wrapBundleEntry(entry);
            output.write(resourceAsByte.getBytes(Charset.defaultCharset()));
            toInclude = fhirPageIterator.getIncludesTypeReference();
            toRevInclude = fhirPageIterator.getRevIncludeIds();
            this.addRevIncludes(afasConfiguration.getFhir().getIncludes().getBufferSize());
        } catch (Exception e) {
            log.error("Error writing fhir response", e);
            throw new CantWriteFhirResource(e.getMessage());
        }
    }

    private void addRevIncludes(int includeSize) {
        if(fhirPageIterator.getRevIncludeIds().size() >= includeSize) {
            state = RenderingState.REVINCLUDES;
        }
    }

    private void writeIncludes() throws IOException {
        if (toInclude != null && !toInclude.isEmpty()) {
            // initialize the cursor:
            if (includeCursor == null) {
                var resource = toInclude.entrySet().stream().findAny();
                resource.ifPresent(res ->
                        includeCursor = fhirStoreService.findByIds(fhirPageIterator.searchContext().getRevision(), res.getKey(), res.getValue())
                );
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
                fhirPageIterator.clearIncludesTypeReference();
                state = RenderingState.REVINCLUDES;
            }
        } else {
            state = RenderingState.REVINCLUDES;
        }
    }

    private void writeRevIncludes() throws IOException {
        if(toRevInclude.isEmpty()) {
            state = RenderingState.ENTRIES;
        }
        else {
            List<DomainResource> revIncludes = fhirStoreService.findRevIncludes(fhirPageIterator.searchContext().getRevision(), toRevInclude, selectExpression.getRevincludes());
            StringBuilder resourceAsByte = revIncludes.isEmpty() ? new StringBuilder() : new StringBuilder(",");
            for(DomainResource rev: revIncludes) {
                var bundleEntry = new FhirBundleBuilder.BundleEntry(rev.fhirType(), rev.getId(), parser.encodeResourceToString(rev));
                resourceAsByte.append(FhirBundleBuilder.wrapBundleEntry(bundleEntry));
            }
            output.write(resourceAsByte.toString().getBytes(Charset.defaultCharset()));
            fhirPageIterator.clearRevIncludeIds();
            state = RenderingState.ENTRIES;
        }
    }


    private void writeHeader() throws IOException {
        output.write(this.fhirBundleBuilder.getHeader(fhirPageIterator.searchContext().getTotal()).getBytes(Charset.defaultCharset()));
        state = RenderingState.ENTRIES;

    }


    @Override
    public void onError(Throwable throwable) {
        log.error("Error reading the request", throwable);
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
