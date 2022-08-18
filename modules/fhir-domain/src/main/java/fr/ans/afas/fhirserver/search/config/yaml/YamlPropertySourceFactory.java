/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config.yaml;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Properties;

/**
 * Implementation of {@link PropertySourceFactory} that read yaml properties
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    /**
     * Create the property source from the yaml resource
     *
     * @return the new PropertySource (never null)
     * @throws IOException
     */
    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource encodedResource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(encodedResource.getResource());
        Properties properties = factory.getObject();
        return new PropertiesPropertySource(encodedResource.getResource().getFilename(), properties);
    }
}