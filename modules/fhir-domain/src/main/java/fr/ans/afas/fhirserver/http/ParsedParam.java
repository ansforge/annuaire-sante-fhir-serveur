/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.http;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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
