/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhirserver.http.FhirRequestParser;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;

import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirQueryFirstPageReadListener<T> extends FhirQueryReadListener<T> {

    final String fhirPath;
    private final HttpServletResponse res;

    private final AfasConfiguration afasConfiguration;

    public FhirQueryFirstPageReadListener(FhirStoreService<T> fhirStoreService, ExpressionFactory<T> expressionFactory, SearchConfig searchConfig, NextUrlManager<T> nextUrlManager, ServletInputStream in, HttpServletResponse r, AsyncContext c, String fhirPath, AfasConfiguration afasConfiguration) {
        super(fhirStoreService, expressionFactory, searchConfig, nextUrlManager, c, in);
        res = r;
        this.fhirPath = fhirPath;
        this.afasConfiguration = afasConfiguration;
    }


    public void onAllDataRead() {
        try {
            String fPath = this.queue.isEmpty() ? fhirPath : String.format("%s?%s", fhirPath, this.queue.stream().map(param -> param)
                    .collect(Collectors.joining("&")));
            var query = FhirRequestParser.parseSelectExpression(fPath, this.expressionFactory, this.searchConfig);

            // now all data are read, set up a WriteListener to write
            var output = res.getOutputStream();
            var writeListener = new FhirBundleFirstPageWriteListener<>(fhirStoreService, nextUrlManager, output, ac, query, afasConfiguration);

            output.setWriteListener(writeListener);

        } catch (Exception e) {
            ErrorWriter.writeError(e, ac);
            ac.complete();
        }

    }


}
