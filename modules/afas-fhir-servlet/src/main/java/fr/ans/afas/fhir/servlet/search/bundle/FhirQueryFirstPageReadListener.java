/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.exception.ResourceNotFoundException;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhirserver.http.FhirRequestParser;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirQueryFirstPageReadListener<T> extends FhirQueryReadListener<T> {

    final String fhirPath;
    private final HttpServletResponse res;

    private final AfasConfiguration afasConfiguration;

    public FhirQueryFirstPageReadListener(FhirServerContext<T> fhirServerContext, ServletInputStream in, HttpServletResponse r, AsyncContext c, String fhirPath, AfasConfiguration afasConfiguration) {
        super(fhirServerContext, in, c);
        res = r;
        this.fhirPath = fhirPath;
        this.afasConfiguration = afasConfiguration;
    }


    public void onAllDataReadInTenant() {
        try {
            String fPath = this.queue.isEmpty() ? fhirPath : String.format("%s?%s", fhirPath, this.queue.stream().map(param -> param)
                    .collect(Collectors.joining("&")));
            var query = FhirRequestParser.parseSelectExpression(fPath, this.fhirServerContext.getExpressionFactory(), this.fhirServerContext.getSearchConfigService());

            // now all data are read, set up a WriteListener to write
            var output = res.getOutputStream();
            var writeListener = new FhirBundleFirstPageWriteListener<>(this.fhirServerContext, output, ac, query, afasConfiguration);

            output.setWriteListener(writeListener);

        }
        catch (Exception e) {
            ErrorWriter.writeError(e, ac, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ac.complete();
        }

    }


}
