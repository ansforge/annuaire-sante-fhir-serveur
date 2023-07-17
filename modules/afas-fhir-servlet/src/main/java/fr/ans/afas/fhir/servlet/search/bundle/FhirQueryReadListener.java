/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public abstract class FhirQueryReadListener<T> implements ReadListener {

    protected final FhirStoreService<T> fhirStoreService;
    protected final ExpressionFactory<T> expressionFactory;
    protected final SearchConfig searchConfig;

    protected final NextUrlManager<T> nextUrlManager;
    protected final AsyncContext ac;
    private final ServletInputStream input;
    protected final Queue<String> queue = new LinkedBlockingQueue<>();

    public void onDataAvailable() throws IOException {

        var sb = new StringBuilder();
        int len;
        var b = new byte[1024];
        while (input.isReady() && (len = input.read(b)) != -1) {
            var data = new String(b, 0, len);
            sb.append(data);
        }
        queue.add(sb.toString());
    }


    @Override
    public void onError(Throwable throwable) {
        log.debug("Error reading the request", throwable);
        ac.complete();
    }

}
