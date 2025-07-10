/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;

import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.exception.ForbiddenException;
import fr.ans.afas.fhir.servlet.create.PostParams;
import fr.ans.afas.fhir.servlet.create.PostResourceReadListener;
import fr.ans.afas.fhir.servlet.delete.DeleteParams;
import fr.ans.afas.fhir.servlet.delete.DeleteResourceReadListener;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhir.servlet.metadata.CapabilityStatementReadListener;
import fr.ans.afas.fhir.servlet.put.PutParams;
import fr.ans.afas.fhir.servlet.put.PutResourceReadListener;
import fr.ans.afas.fhir.servlet.read.ReadResourceReadListener;
import fr.ans.afas.fhir.servlet.read.ReadSearchParams;
import fr.ans.afas.fhir.servlet.search.bundle.FhirQueryFirstPageReadListener;
import fr.ans.afas.fhir.servlet.search.bundle.FhirQueryNextPageReadListener;
import fr.ans.afas.fhir.servlet.service.FhirOperationFactory;
import fr.ans.afas.fhir.servlet.transaction.TransactionReadListener;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import fr.ans.afas.utils.TenantUtil;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Optional;

/**
 * @author Anouar EL Qadim
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
@WebServlet(urlPatterns = "/fhir/v2/*", asyncSupported = true)
public class FhirResourceServlet<T> extends HttpServlet {

    public static final String REQUEST_AFAS_TENANT_ATTRIBUTE = "afas_tenant";
    private static final String FHIR_CONTENT_TYPE = "application/fhir+json;charset=UTF-8";
    public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

    private final FhirServerContext<T> fhirServerContext;
    private final AfasConfiguration afasConfiguration;
    private final FhirOperationFactory fhirOperationFactory;
    private final MessageSource messageSource;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response, HttpMethod.GET);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        processRequest(req, resp, HttpMethod.POST);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        processRequest(req, resp, HttpMethod.PUT);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        processRequest(req, resp, HttpMethod.DELETE);
    }

    /**
     * @param request
     * @param response
     * @param method
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response, HttpMethod method) {
        AsyncContext context = null;
        try {
            context = initAsyncContext(request);
            ServletInputStream input = request.getInputStream();
            response.setContentType(FHIR_CONTENT_TYPE);
            String fhirPath = extractFhirPath(request);
            String fullPath = getFullPath(request, fhirPath);

            switch (method) {
                case GET:
                    handleGetRequest(request, response, context, input, fhirPath, fullPath);
                    break;
                case POST:
                    handlePostRequest(request, response, context, input, fhirPath);
                    break;
                case PUT:
                    fhirServerContext.getSecurityService().canWriteResource(request);
                    handlePutRequest(response, context, input, fhirPath);
                    break;
                case DELETE:
                    fhirServerContext.getSecurityService().canWriteResource(request);
                    handleDeleteRequest(response, context, input, fhirPath);
                    break;
                default:
                    throw new UnsupportedOperationException(messageSource.getMessage("error.http.not.supported", null, Locale.getDefault()));
            }

        } catch (ForbiddenException e1){
            handleError(context, e1, HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            handleError(context, e, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * @param request
     * @param response
     * @param context
     * @param input
     * @param fhirPath
     * @param fullPath
     * @throws BadSelectExpression
     */
    private void handleGetRequest(HttpServletRequest request, HttpServletResponse response, AsyncContext context, ServletInputStream input, String fhirPath, String fullPath) throws BadSelectExpression {
        if (fhirPath.startsWith("_page")) {
            searchNextPage(request, response, context, input);
        } else if (fhirPath.startsWith("metadata")) {
            getCapabilityStatement(response, context, input);
        } else if (fhirPath.startsWith("$")) {
            startOperation(fhirPath, context);
        } else {
            String[] parts = fhirPath.split("/");
            if (parts.length == 2 && !parts[1].startsWith("_")) {
                read(response, context, input, ReadSearchParams.builder().resource(parts[0]).id(parts[1]).build());
            } else {
                searchFirstPage(response, context, input, fullPath);
            }
        }
    }

    /**
     * @param request
     * @param response
     * @param context
     * @param input
     * @param fhirPath
     */
    private void handlePostRequest(HttpServletRequest request, HttpServletResponse response, AsyncContext context, ServletInputStream input, String fhirPath) {
        try {
            String[] parts = fhirPath.split("/");
            if(APPLICATION_X_WWW_FORM_URLENCODED.equals(request.getContentType())) {
                handleGetRequest(request, response, context, input, fhirPath, fhirPath);
            }
            else if (parts.length == 1 && !parts[0].isEmpty()) {
                fhirServerContext.getSecurityService().canWriteResource(request);
                PostParams params = PostParams.builder().resource(parts[0]).build();
                write(response, context, input, params);
            }
            else {
                fhirServerContext.getSecurityService().canWriteResource(request);
                bundleTransaction(response, context, input);
            }
        } catch (ForbiddenException e1){
            handleError(context, e1, HttpServletResponse.SC_FORBIDDEN);
        } catch (Exception e) {
            handleError(context, e, HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * @param response
     * @param context
     * @param input
     * @param fhirPath
     */
    private void handlePutRequest(HttpServletResponse response, AsyncContext context, ServletInputStream input, String fhirPath) {
        String[] parts = fhirPath.split("/");
        if (parts.length == 2 && !parts[1].startsWith("_")) {
            writeWithId(response, context, input, PutParams.builder().resource(parts[0]).id(parts[1]).build());
        }
        else {
            handleError(context, new BadSelectExpression(messageSource.getMessage("error.invalid.put.path", null, Locale.getDefault())), HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * @param response
     * @param context
     * @param input
     * @param fhirPath
     */
    private void handleDeleteRequest(HttpServletResponse response, AsyncContext context, ServletInputStream input, String fhirPath) {

        String[] parts = fhirPath.split("/");
        if (parts.length == 2 && !parts[1].startsWith("_")) {
            DeleteParams params = DeleteParams.builder().resource(parts[0]).id(parts[1]).build();
            delete(response, context, input, params);
        } else {
            handleError(context, new BadSelectExpression(messageSource.getMessage("error.invalid.delete.path", null, Locale.getDefault())), HttpServletResponse.SC_NOT_FOUND);
        }

    }

    /**
     * @param request
     * @return
     */
    private AsyncContext initAsyncContext(HttpServletRequest request) {
        AsyncContext context = request.startAsync();
        context.getRequest().setAttribute(REQUEST_AFAS_TENANT_ATTRIBUTE, TenantUtil.getCurrentTenant());
        context.setTimeout(afasConfiguration.getServletTimeout());
        return context;
    }

    /**
     * @param request
     * @return
     */
    private String extractFhirPath(HttpServletRequest request) {
        String fhirPath = request.getRequestURI().replaceAll(request.getServletPath(), "");
        return fhirPath.startsWith("/") ? fhirPath.substring(1) : fhirPath;
    }

    /**
     * @param request
     * @param fhirPath
     * @return
     */
    private String getFullPath(HttpServletRequest request, String fhirPath) {
        String params = request.getQueryString();
        return StringUtils.isNotBlank(params) ? fhirPath + "?" + params : fhirPath;
    }

    private void handleError(AsyncContext context, Exception e, int status) {
        log.error("Request processing error: ", e);
        Optional.ofNullable(context).ifPresent(c -> {
            ErrorWriter.writeError(e, c, status);
            c.complete();
        });
    }

    /**
     * @param response
     * @param context
     * @param input
     */
    private void getCapabilityStatement(HttpServletResponse response, AsyncContext context, ServletInputStream input) {
        var readListener = new CapabilityStatementReadListener<>(fhirServerContext, response, context);
        input.setReadListener(readListener);
    }

    /**
     * @param response
     * @param context
     * @param input
     * @param build
     */
    private void read(HttpServletResponse response, AsyncContext context, ServletInputStream input, ReadSearchParams build) {
        var readListener = new ReadResourceReadListener<>(fhirServerContext, response, context, build);
        input.setReadListener(readListener);
    }

    /**
     * @param response
     * @param context
     * @param input
     * @param build
     */
    private void writeWithId(HttpServletResponse response, AsyncContext context, ServletInputStream input, PutParams build) {
        var readListener = new PutResourceReadListener<T>(fhirServerContext, input, response, context, build, afasConfiguration.getPublicUrl(), messageSource);
        input.setReadListener(readListener);
    }

    /**
     * @param response
     * @param context
     * @param input
     * @param build
     */
    private void write(HttpServletResponse response, AsyncContext context, ServletInputStream input, PostParams build) {
        var readListener = new PostResourceReadListener<T>(fhirServerContext, input, response, context, build, afasConfiguration.getPublicUrl(), messageSource);
        input.setReadListener(readListener);
    }

    private void bundleTransaction(HttpServletResponse response, AsyncContext context, ServletInputStream input) {
        var readListener = new TransactionReadListener<>(fhirServerContext, input, response, context, afasConfiguration.getPublicUrl(), messageSource);
        input.setReadListener(readListener);
    }

    /**
     * @param response
     * @param context
     * @param input
     * @param params
     */
    private void delete(HttpServletResponse response, AsyncContext context, ServletInputStream input, DeleteParams params) {
        var readListener = new DeleteResourceReadListener<>(fhirServerContext, input, response, context, params);
        input.setReadListener(readListener);
    }

    /**
     * @param request
     * @param response
     * @param context
     * @param input
     * @throws BadSelectExpression
     */
    private void searchNextPage(HttpServletRequest request, HttpServletResponse response, AsyncContext context, ServletInputStream input) throws BadSelectExpression {
        String pageId = request.getParameter("id");
        if (StringUtils.isBlank(pageId)) {
            throw new BadSelectExpression(messageSource.getMessage("error.parameter.id.required", null, Locale.getDefault()));
        }
        var readListener = new FhirQueryNextPageReadListener<>(fhirServerContext, input, response, context, pageId, afasConfiguration, messageSource);
        input.setReadListener(readListener);
    }

    /**
     * @param response
     * @param context
     * @param input
     * @param fhirPath
     */
    private void searchFirstPage(HttpServletResponse response, AsyncContext context, ServletInputStream input, String fhirPath) {
        var readListener = new FhirQueryFirstPageReadListener<>(fhirServerContext, input, response, context, fhirPath, afasConfiguration);
        input.setReadListener(readListener);
    }

    /**
     * @param fhirPath
     * @param context
     */
    private void startOperation(String fhirPath, AsyncContext context) {
        var operation = fhirOperationFactory.findOperationByName(fhirPath, context);
        context.start(operation);
    }

    private enum HttpMethod {
        GET, POST, PUT, DELETE
    }
}
