/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.config.yaml;

import fr.ans.afas.exception.CantLoadConfigurationException;
import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MultiConfigLoader {
    private static final Yaml yaml = new Yaml(new Constructor(YamlFhirWrapper.class, new LoaderOptions()));
    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static boolean isYamlFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".yml") || fileName.endsWith(".yaml");
    }

    private static YamlFhirWrapper loadFile(Path path) {
        try {
            return yaml.load(Files.readString(path, Charset.defaultCharset()));
        } catch (IOException e) {
            throw new CantLoadConfigurationException("Error loading the file: " + path.getFileName() + ". " + e.getMessage());
        }
    }

    @SuppressWarnings("java:S3864")// peek is used for debug
    public List<TenantSearchConfig> loadConfigs(String path) throws URISyntaxException, IOException {
        var url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url != null) {
            var folder = Paths.get(url.toURI());
            try (var lst = Files.list(folder)) {
                return lst.filter(Files::isRegularFile)
                        .filter(MultiConfigLoader::isYamlFile)
                        .sorted()
                        // used for debug in production:
                        .peek(p -> logger.info("Loading file: {}", p.getFileName()))
                        .map(MultiConfigLoader::loadFile)
                        .map(YamlFhirWrapper::getFhir)
                        .toList();
            }

        } else {
            logger.debug("Dossier non trouvé dans le classpath");
        }
        return List.of();
    }


}
