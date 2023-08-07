/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhir.servlet.metadata.CapabilityStatementReadListener;
import fr.ans.afas.fhir.servlet.read.ReadResourceReadListener;
import fr.ans.afas.fhir.servlet.read.ReadSearchParams;
import fr.ans.afas.fhir.servlet.search.bundle.FhirQueryFirstPageReadListener;
import fr.ans.afas.fhir.servlet.search.bundle.FhirQueryNextPageReadListener;
import fr.ans.afas.fhir.servlet.service.FhirOperationFactory;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * Servlet that handle fhir api calls
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
@WebServlet(urlPatterns = "/fhir/v2-alpha/*", asyncSupported = true)
public class FhirResourceServlet<T> extends HttpServlet {

    private static final String FHIR_CONTENT_TYPE = "application/fhir+json;charset=UTF-8";

    private final FhirStoreService<T> fhirStoreService;

    private final ExpressionFactory<T> expressionFactory;

    private final SearchConfig searchConfig;

    private final NextUrlManager<T> nextUrlManager;

    private final AfasConfiguration afasConfiguration;
    /**
     * The Fhir context
     */
    private static final FhirContext fhirContext = FhirContext.forR4();


    private final FhirOperationFactory fhirOperationFactory;

    /**
     * Handle the get (SEARCH/READ/CAPABILITY STATEMENT) of the fhir server.
     * This method detect the queried function (search, metadata...) and response with the good content.
     *
     * @param request  an {@link HttpServletRequest} object that
     *                 contains the request the client has made
     *                 of the servlet
     * @param response an {@link HttpServletResponse} object that
     *                 contains the response the servlet sends
     *                 to the client
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        AsyncContext context = null;
        try {
            context = request.startAsync();
            context.setTimeout(afasConfiguration.getServletTimeout());
            var input = request.getInputStream();
            response.setContentType(FHIR_CONTENT_TYPE);

            var fhirPath = request.getRequestURI().replaceAll(request.getServletPath(), "");
            if (fhirPath.startsWith("/")) {
                fhirPath = fhirPath.substring(1);
            }

            var params = request.getQueryString();
            var fullPath = fhirPath;
            if (StringUtils.isNotBlank(params)) {
                fullPath = fullPath + "?" + params;
            }

            // next page:
            if (fhirPath.startsWith("_page")) {
                searchNextPage(request, response, context, input);
                return;
            }

            // capability statement:
            if (fhirPath.startsWith("metadata")) {
                getCapabilityStatement(response, context, input);
                return;
            }

            // operations:
            if (fhirPath.startsWith("$")) {
                var operation = fhirOperationFactory.findOperationByName(fhirPath, context);
                context.start(operation);
                return;
            }

            // read:
            var parts = fhirPath.split("/");
            if (parts.length == 2 && !parts[1].startsWith("_")) {
                read(response, context, input, ReadSearchParams.builder().resource(parts[0]).id(parts[1]).build());
                return;
            }

            // first page:
            searchFirstPage(response, context, input, fullPath);

        } catch (Exception e) {
            log.error("doGet error : ", e);
            Optional.ofNullable(context)
                    .ifPresent(c -> {
                        ErrorWriter.writeError(e, c);
                        c.complete();
                    });
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        doGet(req, resp);
    }

    /**
     * Process the capability statement query
     *
     * @param response the response
     * @param context  the async context
     * @param input    the servlet input stream
     */
    private void getCapabilityStatement(HttpServletResponse response, AsyncContext context, ServletInputStream input) {
        var readListener = new CapabilityStatementReadListener(response, context, searchConfig);
        input.setReadListener(readListener);
    }



    /**
     * Process the read operation
     *
     * @param response
     * @param context
     * @param build
     */
    private void read(HttpServletResponse response, AsyncContext context, ServletInputStream input, ReadSearchParams build) {
        var readListener = new ReadResourceReadListener<T>(response, context, fhirStoreService, build, fhirContext);
        input.setReadListener(readListener);
    }

    /**
     * Process get of a "next page" (search with next link)
     *
     * @param request  the request
     * @param response the response
     * @param context  the async context
     * @param input    the servlet input stream
     */
    private void searchNextPage(HttpServletRequest request, HttpServletResponse response, AsyncContext context, ServletInputStream input) throws BadSelectExpression {
        //
        var pageId = request.getParameter("id");
        if (StringUtils.isBlank(pageId)) {
            throw new BadSelectExpression("Parameter id required to fetch the next page");
        }
        var readListener = new FhirQueryNextPageReadListener<>(fhirStoreService, expressionFactory, searchConfig, nextUrlManager, input, response, context, pageId, afasConfiguration);
        input.setReadListener(readListener);
    }

    /**
     * Process the first page of a search
     *
     * @param response the response
     * @param context  the async context
     * @param input    the servlet input stream
     * @param fhirPath the full path of the query
     */
    private void searchFirstPage(HttpServletResponse response, AsyncContext context, ServletInputStream input, String fhirPath) {
        var readListener = new FhirQueryFirstPageReadListener<>(fhirStoreService, expressionFactory, searchConfig, nextUrlManager, input, response, context, fhirPath, afasConfiguration);
        input.setReadListener(readListener);
    }


}
