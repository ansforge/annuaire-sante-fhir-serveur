/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.search.bundle;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.fhir.servlet.exception.UnknownErrorWritingResponse;
import fr.ans.afas.fhir.servlet.servletutils.DefaultWriteListener;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPageIterator;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.CantWriteFhirResource;
import fr.ans.afas.utils.ConditionMatching;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
public abstract class AbstractFhirBundleWriteListener<T> extends DefaultWriteListener {

    static IParser parser = FhirContext.forR4().newJsonParser();
    private final AfasConfiguration afasConfiguration;
    private final ServletOutputStream output;

    private final SelectExpression<T> selectExpression;
    private final FhirBundleBuilder fhirBundleBuilder = new FhirBundleBuilder();
    private final FhirPageIterator fhirPageIterator;
    FhirServerContext<T> fhirServerContext;
    Iterator<FhirBundleBuilder.BundleEntry> includeCursor;
    private RenderingState state = RenderingState.HEADER;
    private int index = 0;
    private Map<String, Set<String>> toInclude;
    private Set<String> toRevInclude;

    protected AbstractFhirBundleWriteListener(FhirServerContext<T> fhirServerContext, AfasConfiguration afasConfiguration, ServletOutputStream output, AsyncContext context, SelectExpression<T> selectExpression, FhirPageIterator fhirPageIterator) {
        super(context);
        this.fhirServerContext = fhirServerContext;
        this.afasConfiguration = afasConfiguration;
        this.output = output;
        this.selectExpression = selectExpression;
        this.fhirPageIterator = fhirPageIterator;
    }

    @Override
    public void onWritePossibleInTenant() {
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

    private String addIfHasParams(String text) {
        return text == null ? "" : "?" + text;
    }

    void writeFooter() throws IOException {

        var req = this.context.getRequest();
        var httpReq = (HttpServletRequest) req;
        //TODO temporal solution to remove tenant before generating the next url because the tenant don't have to appear in next url but we can remove this solution at the moment that HAPI would be deleted
        var currentUrl = afasConfiguration.getPublicUrl().concat(httpReq.getRequestURI().replaceAll("(/0.x)|(/1.x)", "")).concat(addIfHasParams(httpReq.getQueryString()));

        if (fhirPageIterator.hasNextPage()) {
            //TODO temporal solution to remove tenant before generating the next url because the tenant don't have to appear in next url but we can remove this solution at the moment that HAPI would be deleted
            var nextUrl = HttpUtils.getServerUrl(afasConfiguration.getPublicUrl(), "").replace(selectExpression.getFhirResource(), "");

            var id = fhirServerContext.getNextUrlManager().store(PagingData.<T>builder()
                    .pageSize(selectExpression.getCount())
                    .size(CountResult.builder().total(fhirPageIterator.searchContext().getTotal()).build())
                    .type(selectExpression.getFhirResource())
                    .selectExpression(selectExpression)
                    .elements(fhirPageIterator.getElements())
                    .uuid(UUID.randomUUID().toString())
                    .timestamp(fhirPageIterator.searchContext().getRevision())
                    .lastId(fhirPageIterator.searchContext().getFirstId())
                    .build());


            output.write(this.fhirBundleBuilder.getFooter(nextUrl, currentUrl, id).getBytes(Charset.defaultCharset()));
            context.complete();
            return;
        }
        output.write(this.fhirBundleBuilder.getFooter(afasConfiguration.getPublicUrl(), currentUrl, null).getBytes(Charset.defaultCharset()));
        context.complete();
    }

    void writeEntries() throws UnknownErrorWritingResponse {
        try {
            boolean match =
                    ConditionMatching.ifExecute(fhirPageIterator::hasNext, x -> writeIteratorEntries())
                            .elseIfExecute(() -> this.toInclude != null && !this.toInclude.isEmpty(), x -> state = RenderingState.INCLUDES)
                            .elseIfExecute(() -> this.toRevInclude != null && !this.toRevInclude.isEmpty(), x -> this.addRevIncludes(1))
                            .matches();

            if (!match) {
                fhirPageIterator.close();
                state = RenderingState.FOOTER;
            }
        } catch (Exception e) {
            log.error("Error writing fhir response", e);
            context.complete();
            throw new UnknownErrorWritingResponse(e.getMessage());
        }
    }

    void writeIteratorEntries() {
        try {
            var entry = fhirPageIterator.next();
            var resourceAsByte = "";
            if (index++ > 0) {
                resourceAsByte += ",";
            }
            //TODO temporal solution to remove tenant before generating the next url because the tenant don't have to appear in next url but we can remove this solution at the moment that HAPI would be deleted
            resourceAsByte += FhirBundleBuilder.wrapBundleEntry(HttpUtils.getServerUrl(afasConfiguration.getPublicUrl(), ""), entry);
            output.write(resourceAsByte.getBytes(Charset.defaultCharset()));
            toInclude = fhirPageIterator.getIncludesTypeReference();
            toRevInclude = fhirPageIterator.getRevIncludeIds();
            this.addRevIncludes(afasConfiguration.getFhir().getIncludes().getBufferSize());
        } catch (Exception e) {
            log.error("Error writing fhir response", e);
            context.complete();
            throw new CantWriteFhirResource(e.getMessage());
        }
    }

    private void addRevIncludes(int includeSize) {
        if (fhirPageIterator.getRevIncludeIds().size() >= includeSize) {
            state = RenderingState.REVINCLUDES;
        }
    }

    private void writeIncludes() throws IOException {
        if (toInclude != null && !toInclude.isEmpty()) {
            // initialize the cursor:
            if (includeCursor == null) {
                var resource = toInclude.entrySet().stream().map(res ->
                        fhirServerContext.getFhirStoreService().findByIds(fhirPageIterator.searchContext().getRevision(), res.getKey(), res.getValue())).toList();
                includeCursor = new CombinedCursor(resource);
            }// or write the response:
            if (includeCursor.hasNext()) {
                var entry = includeCursor.next();
                var resourceAsByte = ",";
                //TODO temporal solution to remove tenant before generating the next url because the tenant don't have to appear in next url but we can remove this solution at the moment that HAPI would be deleted
                resourceAsByte += FhirBundleBuilder.wrapBundleEntry(HttpUtils.getServerUrl(afasConfiguration.getPublicUrl(), ""), entry);
                output.write(resourceAsByte.getBytes(Charset.defaultCharset()));
            } else {
                includeCursor = null;
                fhirPageIterator.clearIncludesTypeReference();
                state = RenderingState.REVINCLUDES;
            }
        } else {
            state = RenderingState.REVINCLUDES;
        }
    }

    private void writeRevIncludes() throws IOException {
        if (toRevInclude.isEmpty()) {
            state = RenderingState.ENTRIES;
        } else {
            var revIncludes = fhirServerContext.getFhirStoreService().findRevIncludes(fhirPageIterator.searchContext().getRevision(), toRevInclude, selectExpression.getRevincludes());
            var resourceAsByte = new StringBuilder();
            for (FhirBundleBuilder.BundleEntry entry : revIncludes) {
                resourceAsByte.append(",");
                //TODO temporal solution to remove tenant before generating the next url because the tenant don't have to appear in next url but we can remove this solution at the moment that HAPI would be deleted
                resourceAsByte.append(FhirBundleBuilder.wrapBundleEntry(HttpUtils.getServerUrl(afasConfiguration.getPublicUrl(), ""), entry));
            }
            output.write(resourceAsByte.toString().getBytes(Charset.defaultCharset()));
            fhirPageIterator.clearRevIncludeIds();
            state = RenderingState.ENTRIES;
        }
    }


    private void writeHeader() throws IOException {
        var bundleId = UUID.randomUUID().toString();
        output.write(this.fhirBundleBuilder.getHeader(bundleId, fhirPageIterator.searchContext().getTotal()).getBytes(Charset.defaultCharset()));
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
