/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirBundleFirstPageWriteListener<T> extends AbstractFhirBundleWriteListener<T> {


    public FhirBundleFirstPageWriteListener(FhirServerContext<T> fhirServerContext,
                                            ServletOutputStream sos,
                                            AsyncContext c,
                                            SelectExpression<T> selectExpression,
                                            AfasConfiguration afasConfiguration) {
        super(fhirServerContext, afasConfiguration, sos, c, selectExpression, fhirServerContext.getFhirStoreService().iterate(null, selectExpression));
    }

}
