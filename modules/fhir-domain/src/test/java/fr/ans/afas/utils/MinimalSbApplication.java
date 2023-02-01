/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import fr.ans.afas.fhirserver.search.config.yaml.YamlSearchConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;


/**
 * A minimal spring boot application used for tests. This configuration import a basic FHIR SearchConfiguration
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@SpringBootApplication
@Import(YamlSearchConfig.class)
public class MinimalSbApplication {
}
