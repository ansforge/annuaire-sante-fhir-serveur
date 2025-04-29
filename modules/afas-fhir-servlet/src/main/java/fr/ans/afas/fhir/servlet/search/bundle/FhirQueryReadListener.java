/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.fhir.servlet.servletutils.DefaultReadListener;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletInputStream;
import lombok.extern.slf4j.Slf4j;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Slf4j
public abstract class FhirQueryReadListener<T> extends DefaultReadListener {

    protected final FhirServerContext<T> fhirServerContext;
    protected final AsyncContext ac;

    /**
     * Construct a read listener for the search operation
     *
     * @param inputStream       the input stream of the request
     * @param fhirServerContext context of the server (services)
     * @param ac                the context
     */
    protected FhirQueryReadListener(FhirServerContext<T> fhirServerContext, ServletInputStream inputStream,
                                    AsyncContext ac) {
        super(ac, inputStream);
        this.fhirServerContext = fhirServerContext;
        this.ac = ac;
    }


    @Override
    public void onError(Throwable throwable) {
        log.debug("Error reading the request", throwable);
        ac.complete();
    }

}
