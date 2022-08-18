/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhir;

import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IPagingProvider;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionDeserializer;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.search.expression.serialization.SerializeUrlEncrypter;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;

/**
 * The paging provider. This paging provider use url to store the request and context params.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AfasPagingProvider implements IPagingProvider {

    /**
     * The expression factory
     */
    @Autowired
    ExpressionFactory<?> expressionFactory;

    /**
     * The store service
     */
    @Autowired
    FhirStoreService<?> fhirStoreService;

    /**
     * The expression serializer
     */
    @Autowired
    ExpressionSerializer expressionSerializer;

    /**
     * The expression deserializer
     */
    @Autowired
    ExpressionDeserializer<?> expressionDeserializer;

    /**
     * The url encrypter
     */
    @Autowired
    SerializeUrlEncrypter serializeUrlEncrypter;

    @Override
    public int getDefaultPageSize() {
        return 50;
    }

    @Override
    public int getMaximumPageSize() {
        return 2000;
    }

    @Override
    public boolean canStoreSearchResults() {
        return true;
    }


    @Override
    public IBundleProvider retrieveResultList(@Nullable RequestDetails theRequestDetails, @NotNull String theSearchIdEncrypted) {
        return null;
    }

    @Override
    public IBundleProvider retrieveResultList(@Nullable RequestDetails theRequestDetails, @NotNull String theSearchIdEncoded, String thePageId) {
        var theSearchId = serializeUrlEncrypter.decrypt(thePageId);
        var parts = theSearchId.split("_", 7);
        var pageSize = parts[0];
        var size = parts[1];
        var uuid = parts[2];
        var timestamp = parts[3];
        var type = parts[4];
        var contextAsString = parts[5];
        var exp = parts[6];

        var context = new HashMap<String, Object>();
        var keyVals = contextAsString.split(",");
        for (var keyVal : keyVals) {
            var p = keyVal.split("=");
            context.put(p[0], p[1]);
        }

        var expression = expressionDeserializer.deserialize(exp);
        return new AfasBundleProvider<>(Integer.parseInt(pageSize), Integer.parseInt(size), uuid, new DateDt(new Date(Long.parseLong(timestamp))), fhirStoreService, type, (SelectExpression) expression, context, expressionSerializer, serializeUrlEncrypter);
    }

    @Override
    public String storeResultList(@Nullable RequestDetails theRequestDetails, IBundleProvider theList) {
        return null;
    }
}
