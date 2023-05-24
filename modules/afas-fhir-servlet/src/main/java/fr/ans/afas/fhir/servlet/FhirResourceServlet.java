/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhir.servlet.metadata.CapabilityStatementReadListener;
import fr.ans.afas.fhir.servlet.search.bundle.FhirQueryFirstPageReadListener;
import fr.ans.afas.fhir.servlet.search.bundle.FhirQueryNextPageReadListener;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet that handle fhir api calls
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@WebServlet(urlPatterns = "/fhir/v2-alpha/*", asyncSupported = true)
public class FhirResourceServlet<T> extends HttpServlet {


    private static final String FHIR_CONTENT_TYPE = "application/fhir+json;charset=UTF-8";
    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    final FhirStoreService<T> fhirStoreService;

    final ExpressionFactory<T> expressionFactory;

    final SearchConfig searchConfig;

    final NextUrlManager<T> nextUrlManager;

    final FhirContext fhirContext;

    final String serverUrl;


    //    @Autowired
    @Inject
    public FhirResourceServlet(FhirStoreService<T> fhirStoreService,
                               ExpressionFactory<T> expressionFactory,
                               SearchConfig searchConfig,
                               NextUrlManager<T> nextUrlManager,
                               FhirContext fhirContext,
                               @Value("${afas.publicUrl}") String serverUrl) {
        this.fhirStoreService = fhirStoreService;
        this.expressionFactory = expressionFactory;
        this.searchConfig = searchConfig;
        this.nextUrlManager = nextUrlManager;
        this.fhirContext = fhirContext;
        this.serverUrl = serverUrl;
    }

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
     * @throws IOException if something was wrong with io
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AsyncContext context = null;
        try {
            context = request.startAsync();
            context.setTimeout(300000);
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
            }
            // capability statement:
            else if (fhirPath.startsWith("metadata")) {
                getCapabilityStatement(response, context, input);
            }
            // first page:
            else {
                searchFirstPage(response, context, input, fullPath);
            }
        } catch (Exception e) {
            if (context != null) {
                try {
                    ErrorWriter.writeError(e, context);
                    context.complete();
                } catch (IOException ioException) {
                    logger.debug("Error writing the error");
                }
            }
        }
    }

    /**
     * Process the capability statement query
     *
     * @param response the response
     * @param context  the async context
     * @param input    the servlet input stream
     */
    private void getCapabilityStatement(HttpServletResponse response, AsyncContext context, ServletInputStream input) {
        var readListener = new CapabilityStatementReadListener(response, context, fhirContext, searchConfig);
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
        var readListener = new FhirQueryNextPageReadListener<>(fhirStoreService, expressionFactory, searchConfig, nextUrlManager, input, response, context, pageId, serverUrl);
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
        var readListener = new FhirQueryFirstPageReadListener<>(fhirStoreService, expressionFactory, searchConfig, nextUrlManager, input, response, context, fhirPath, serverUrl);
        input.setReadListener(readListener);
    }


}
