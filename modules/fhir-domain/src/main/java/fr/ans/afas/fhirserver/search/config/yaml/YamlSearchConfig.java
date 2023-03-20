/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config.yaml;

import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;


/**
 * Configure the search config for a usage with yaml.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fhir")
@PropertySource(value = "classpath:indexes.yml", factory = YamlPropertySourceFactory.class)
public class YamlSearchConfig extends ServerSearchConfig {
}
