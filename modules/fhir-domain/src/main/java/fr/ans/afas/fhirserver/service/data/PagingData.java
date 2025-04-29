/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service.data;

import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import java.util.Set;


@Getter
@Builder
@AllArgsConstructor
public class PagingData<T> {

    @NonNull
    private final String type;
    @NonNull
    private final SelectExpression<T> selectExpression;
    private final int pageSize;
    @NonNull
    private final CountResult size;
    @NonNull
    private final String uuid;
    @NonNull
    private final String lastId;
    private final long timestamp;
    private final Set<String> elements;
}
