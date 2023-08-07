/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "afas")
@Setter
@Getter
public class AfasConfiguration {

    private Fhir fhir = new Fhir();

    private String publicUrl;

    private int servletTimeout = 600000;

    @Setter
    @Getter
    public static class Fhir {
        private Includes includes = new Includes();
    }

    @Setter
    @Getter
    public static class Includes {
        private int bufferSize = 1000;
    }
}
