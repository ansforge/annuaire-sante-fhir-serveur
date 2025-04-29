/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.servlet;

import fr.ans.afas.fhirserver.http.FhirRequestParser;
import fr.ans.afas.service.TestMultitenantService;
import fr.ans.afas.utils.TenantUtil;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Utility class used to make tests on async servlet
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class ServletTestUtil {

    public static final String SERVER_URL = "http://localhost:8080";

    private ServletTestUtil() {
    }


    /**
     * Call an async servlet and return the result as a writer.
     *
     * @param servlet     the servlet to call
     * @param method      the http method (upper case). Supported values are: GET, POST, PUT, DELETE
     * @param path        the full path to call
     * @param contextPath the context path of the servlet
     * @return the response as a string writer
     */
    // We suppress the warning because this is class that is only used in tests.
    @SuppressWarnings("java:S3011")
    @NotNull
    public static StringWriter callAsyncServlet(HttpServlet servlet, String method, String path, String contextPath, String body, String contentType) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return callAsyncServletWithResponse(servlet, method, path, contextPath, body, contentType).writer;
    }

    @NotNull
    public static StringWriter callAsyncServlet(HttpServlet servlet, String method, String path, String contextPath, String body) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return callAsyncServletWithResponse(servlet, method, path, contextPath, body).writer;
    }

    @NotNull
    public static ResponseAndWriter callAsyncServletWithResponse(HttpServlet servlet, String method, String path, String contextPath, String body) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return callAsyncServletWithResponse(servlet, method, path, contextPath, body, null);
    }

    /**
     * Call an async servlet and return the result with the writer and the servlet response
     *
     * @param servlet     the servlet to call
     * @param method      the http method (upper case). Supported values are: GET, POST, PUT, DELETE
     * @param path        the full path to call
     * @param contextPath the context path of the servlet
     * @return the HttpServletResponse and the writter
     */
    // We suppress the warning because this is class that is only used in tests.
    @SuppressWarnings("java:S3011")
    @NotNull
    public static ResponseAndWriter callAsyncServletWithResponse(HttpServlet servlet, String method, String path, String contextPath, String body, String contentType) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        var complete = new AtomicBoolean(Boolean.FALSE);
        var out = new StringWriter();
        var asyncContext = Mockito.mock(AsyncContext.class);

        TenantUtil.setCurrentTenant(TestMultitenantService.TEST_TENANT1);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(asyncContext.getRequest()).thenReturn(request);
        Mockito.when(request.getAttribute("afas_tenant")).thenReturn(TestMultitenantService.TEST_TENANT1);
        Mockito.when(request.getContentType()).thenReturn(contentType != null ? contentType : "application/json");


        HttpServletResponse response;
        try (var servletOutputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return !complete.get();
            }

            @Override
            public void setWriteListener(WriteListener listener) {
                try {
                    listener.onWritePossible();
                } catch (IOException e) {
                    // nothing to do in test mode
                }
            }

            @Override
            public void write(int b) {
                out.write(b);
            }

        }) {
            response = Mockito.mock(HttpServletResponse.class);
            Mockito.when(response.getOutputStream()).thenReturn(servletOutputStream);

            final int[] status = {0};

            Mockito.doAnswer(invocation -> {
                Object[] args = invocation.getArguments();
                status[0] = (int) args[0];
                return null;
            }).when(response).setStatus(ArgumentMatchers.anyInt());
            Mockito.when(response.getStatus()).thenAnswer(i -> status[0]);


            var headers = new HashMap<String, String>();
            Mockito.doAnswer(invocation -> {
                Object[] args = invocation.getArguments();
                headers.put((String) args[0], (String) args[1]);
                return null;
            }).when(response).addHeader(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
            Mockito.when(response.getHeader(ArgumentMatchers.anyString())).thenAnswer(i -> headers.get(i.getArguments()[0]));

        }
        Mockito.when(asyncContext.getResponse()).thenReturn(response);
        Mockito.doAnswer(invocation -> {
            complete.set(true);
            return null;
        }).when(asyncContext).complete();
        Mockito.when(request.getAsyncContext()).thenReturn(asyncContext);
        Mockito.when(request.getMethod()).thenReturn(method);
        Mockito.when(request.startAsync()).thenReturn(asyncContext);
        Mockito.when(request.getRequestURI()).thenReturn(path);
        Mockito.when(request.getParameter(ArgumentMatchers.anyString())).then(a -> {
            var url = new URL("http://aa/" + path);
            var parsed = FhirRequestParser.parseParameters(url.getQuery());
            for (var p : parsed) {
                if (p.getParamName().endsWith(a.getArgument(0))) {
                    return p.getParamValues().get(0);
                }
            }
            return null;
        });
        Mockito.when(request.getServletPath()).thenReturn(contextPath);
        Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer().append(SERVER_URL).append(contextPath));

        var b = new byte[0];
        if (StringUtils.hasLength(body)) {
            b = body.getBytes(Charset.defaultCharset());
        }
        Mockito.when(request.getInputStream()).thenReturn(new MockedServletInputStream(new ByteArrayInputStream(b)));
        PrintWriter writer = new PrintWriter(out);
        Mockito.when(response.getWriter()).thenReturn(writer);


        // for runnable :
        Mockito.doAnswer(invocation -> {
            var runnable = (Runnable) invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(asyncContext).start(ArgumentMatchers.any(Runnable.class));


        // Choose the method based on the parameter
        Method m;
        switch (method) {
            case "GET":
                m = servlet.getClass().getDeclaredMethod("doGet", HttpServletRequest.class, HttpServletResponse.class);
                break;
            case "POST":
                m = servlet.getClass().getDeclaredMethod("doPost", HttpServletRequest.class, HttpServletResponse.class);
                break;
            case "PUT":
                m = servlet.getClass().getDeclaredMethod("doPut", HttpServletRequest.class, HttpServletResponse.class);
                break;
            case "DELETE":
                m = servlet.getClass().getDeclaredMethod("doDelete", HttpServletRequest.class, HttpServletResponse.class);
                break;
            default:
                throw new UnsupportedOperationException("Only GET, POST, PUT ans DELETE are supported");

        }

        m.setAccessible(true);
        m.invoke(servlet, request, response);

        request.startAsync();
        request.getInputStream().available();
        request.getInputStream().readAllBytes();
        response.getOutputStream().flush();
        return new ResponseAndWriter(out, response);
    }

    @Getter
    public static class ResponseAndWriter {
        StringWriter writer;
        HttpServletResponse servletResponse;

        protected ResponseAndWriter(StringWriter writer, HttpServletResponse servletResponse) {
            this.writer = writer;
            this.servletResponse = servletResponse;
        }
    }

}
