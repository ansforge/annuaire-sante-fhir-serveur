/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhirserver.http.FhirRequestParser;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;

import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirQueryFirstPageReadListener<T> extends FhirQueryReadListener<T> {

    private final HttpServletResponse res;
    String fhirPath;
    String serverUrl;

    public FhirQueryFirstPageReadListener(FhirStoreService<T> fhirStoreService, ExpressionFactory<T> expressionFactory, SearchConfig searchConfig, NextUrlManager<T> nextUrlManager, ServletInputStream in, HttpServletResponse r, AsyncContext c, String fhirPath, String serverUrl) {
        super(fhirStoreService, expressionFactory, searchConfig, nextUrlManager, in, c);
        res = r;
        this.fhirPath = fhirPath;
        this.serverUrl = serverUrl;
    }


    public void onAllDataRead() throws IOException {
        try {
            var query = FhirRequestParser.parseSelectExpression(fhirPath, this.expressionFactory, this.searchConfig);

            // now all data are read, set up a WriteListener to write
            var output = res.getOutputStream();
            var writeListener = new FhirBundleFirstPageWriteListener<>(fhirStoreService, expressionFactory, searchConfig, nextUrlManager, output, ac, query, serverUrl);

            output.setWriteListener(writeListener);

        } catch (Exception e) {
            ErrorWriter.writeError(e, ac);
            ac.complete();
        }

    }


}
