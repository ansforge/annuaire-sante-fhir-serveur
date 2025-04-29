/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas;

import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * Sample application to test the test classes
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@SpringBootApplication
public class SampleApp {

    public static void main(String[] args) {
        SpringApplication.run(SampleApp.class, args);
    }

    @Bean
    ServerSearchConfig ServerSearchConfig() {
        return ServerSearchConfig.builder()
                .configs(Map.of())
                .build();
    }

}
