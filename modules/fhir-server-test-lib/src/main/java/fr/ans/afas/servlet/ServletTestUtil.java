/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.servlet;

import fr.ans.afas.fhirserver.http.FhirRequestParser;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.util.StringUtils;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Utility class used to make tests on async servlet
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class ServletTestUtil {

    private ServletTestUtil() {
    }


    /**
     * Call an async servlet and return the result as a writer.
     *
     * @param servlet     the servlet to call
     * @param method      the http method (upper case). Supported values are: GET, POST, PUT, DELETE
     * @param path        the full path to call
     * @param contextPath the context path of the servlet
     * @return the writer with the response
     */
    // We suppress the warning because this is class that is only used in tests.
    @SuppressWarnings("java:S3011")
    @NotNull
    public static StringWriter callAsyncServlet(HttpServlet servlet, String method, String path, String contextPath, String body) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        var complete = new AtomicBoolean(Boolean.FALSE);
        var out = new StringWriter();
        var asyncContext = Mockito.mock(AsyncContext.class);


        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(asyncContext.getRequest()).thenReturn(request);

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
        }
        Mockito.when(asyncContext.getResponse()).thenReturn(response);
        Mockito.doAnswer(invocation -> {
            complete.set(true);
            return null;
        }).when(asyncContext).complete();
        Mockito.when(request.getAsyncContext()).thenReturn(asyncContext);
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

        var b = new byte[0];
        if (StringUtils.hasLength(body)) {
            b = body.getBytes(Charset.defaultCharset());
        }
        Mockito.when(request.getInputStream()).thenReturn(new MockedServletInputStream(new ByteArrayInputStream(b)));


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
                throw new UnsupportedOperationException("Only GET are supported");

        }

        m.setAccessible(true);
        m.invoke(servlet, request, response);

        request.startAsync();
        request.getInputStream().available();
        request.getInputStream().readAllBytes();
        response.getOutputStream().flush();
        return out;
    }

}
