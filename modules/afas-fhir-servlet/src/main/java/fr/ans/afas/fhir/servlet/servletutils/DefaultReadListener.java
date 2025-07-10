/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.servletutils;


import jakarta.servlet.AsyncContext;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Base class for the ReadListener.
 * This listener parse all request and put it into oa buffer
 *
 * @author Guillaume Poulériguen
 * @since 1.25
 */
public abstract class DefaultReadListener extends BaseServletListener implements ReadListener {

    /**
     * Input stream of the request
     */
    private final ServletInputStream inputStream;

    protected Queue<String> queue = new LinkedBlockingQueue<>();

    /**
     * Construct a read listener
     *
     * @param context     the async context
     * @param inputStream the input stream of the request
     */
    protected DefaultReadListener(AsyncContext context, ServletInputStream inputStream) {
        super(context);
        this.inputStream = inputStream;
    }

    @Override
    public final void onDataAvailable() throws IOException {
        setTenant();
        var sb = new StringBuilder();
        int len;
        var b = new byte[1024];
        while (inputStream.isReady() && (len = inputStream.read(b)) != -1) {
            var data = new String(b, 0, len);
            sb.append(data);
        }
        queue.add(sb.toString());
    }


    @Override
    public final void onAllDataRead() throws IOException {
        setTenant();
        onAllDataReadInTenant();
    }

    protected abstract void onAllDataReadInTenant() throws IOException;
}
