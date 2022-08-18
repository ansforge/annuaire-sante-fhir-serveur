package fr.ans.afas.utils;

import fr.ans.afas.fhirserver.search.config.yaml.YamlSearchConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(YamlSearchConfig.class)
public class MinimalSbApplication {
}
