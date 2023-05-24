/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.http;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * Store a fhir parameter
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class ParsedParam {

    String paramName;
    List<String> paramValues;
    String modifier;


    @Builder
    public ParsedParam(String paramName, List<String> paramValues, String modifier) {
        this.paramName = paramName;
        this.paramValues = paramValues;
        this.modifier = modifier;
    }
}
