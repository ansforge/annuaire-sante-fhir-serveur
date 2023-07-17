/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.config.yaml;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import javax.annotation.Nullable;


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
     */
    @NotNull
    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource encodedResource) {
        var factory = new YamlPropertiesFactoryBean();
        factory.setResources(encodedResource.getResource());
        var properties = factory.getObject();

        if (properties == null || encodedResource.getResource().getFilename() == null) {
            throw new NullPointerException();
        }

        return new PropertiesPropertySource(encodedResource.getResource().getFilename(), properties);
    }
}