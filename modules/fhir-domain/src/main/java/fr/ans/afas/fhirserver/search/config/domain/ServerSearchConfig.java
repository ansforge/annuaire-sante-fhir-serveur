/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The search config of the server
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Setter
public class ServerSearchConfig {
    private String validationMode;
    private List<FhirResourceSearchConfig> resources = new ArrayList<>();
}
