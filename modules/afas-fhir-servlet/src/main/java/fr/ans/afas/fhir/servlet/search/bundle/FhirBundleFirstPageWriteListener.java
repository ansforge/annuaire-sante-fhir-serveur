/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirBundleFirstPageWriteListener<T> extends AbstractFhirBundleWriteListener<T> {


    public FhirBundleFirstPageWriteListener(FhirStoreService<T> fhirStoreService,
                                            NextUrlManager<T> nextUrlManager,
                                            ServletOutputStream sos,
                                            AsyncContext c,
                                            SelectExpression<T> selectExpression,
                                            String serverUrl) {
        super(fhirStoreService, serverUrl, sos, c, nextUrlManager, selectExpression, fhirStoreService.iterate(null, selectExpression));
    }

}
