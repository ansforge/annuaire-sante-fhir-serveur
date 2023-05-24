/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.http;

import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * Store a value and a prefix (used for date, quantity in fhir)
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Setter
public class PrefixAndValue {
    ParamPrefixEnum prefix;
    String value;
}
