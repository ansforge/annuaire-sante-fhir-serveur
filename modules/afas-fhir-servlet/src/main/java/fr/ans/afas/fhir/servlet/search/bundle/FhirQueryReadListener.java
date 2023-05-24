/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public abstract class FhirQueryReadListener<T> implements ReadListener {


    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final Queue<String> queue = new LinkedBlockingQueue<>();
    protected final ServletInputStream input;
    protected final AsyncContext ac;
    FhirStoreService<T> fhirStoreService;
    ExpressionFactory<T> expressionFactory;
    SearchConfig searchConfig;
    NextUrlManager<T> nextUrlManager;

    protected FhirQueryReadListener(FhirStoreService<T> fhirStoreService, ExpressionFactory<T> expressionFactory, SearchConfig searchConfig, NextUrlManager<T> nextUrlManager, ServletInputStream in, AsyncContext c) {
        input = in;
        ac = c;
        this.fhirStoreService = fhirStoreService;
        this.expressionFactory = expressionFactory;
        this.searchConfig = searchConfig;
        this.nextUrlManager = nextUrlManager;

    }

    public void onDataAvailable() throws IOException {

        var sb = new StringBuilder();
        int len = -1;
        var b = new byte[1024];
        while (input.isReady() && (len = input.read(b)) != -1) {
            var data = new String(b, 0, len);
            sb.append(data);
        }
        queue.add(sb.toString());
    }


    @Override
    public void onError(Throwable throwable) {
        logger.debug("Error reading the request", throwable);
        ac.complete();
    }

}
