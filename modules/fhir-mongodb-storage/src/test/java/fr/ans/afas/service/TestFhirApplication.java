/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import ca.uhn.fhir.context.FhirContext;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import fr.ans.afas.config.CleanRevisionDataConfiguration;
import fr.ans.afas.fhirserver.search.config.CompositeSearchConfig;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbExpressionFactory;
import fr.ans.afas.mdbexpression.domain.fhir.searchconfig.ASComplexSearchConfig;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.json.FhirBaseResourceDeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * Base of the spring boot test application
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@SpringBootApplication(scanBasePackages = "fr.ans.afas", exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@Import(CleanRevisionDataConfiguration.class)
public class TestFhirApplication {
    /**
     * Launch the service
     *
     * @param args command line args (not used)
     */
    public static void main(String[] args) {
        // Enable MongoDB logging in general
        System.setProperty("DEBUG.MONGO", "false");
        System.setProperty("DB.TRACE", "false");
        SpringApplication.run(TestFhirApplication.class, args);
    }


    /**
     * Create a mongodb client
     *
     * @param mongoUri Uri of the mongodb server
     * @return the client
     */
    @Bean
    public MongoClient mongoClient(@Value("${afas.mongodb.uri}") String mongoUri) {
        return MongoClients.create(mongoUri);
    }

    @Autowired
    @Bean
    public MongoDbFhirService fhirService(FhirContext fhirContext,
                                          MongoClient mongoClient,
                                          SearchConfig searchConfig,
                                          @Value("${afas.fhir.max-include-size:5000}") int maxIncludePageSize,
                                          @Value("${afas.mongodb.dbname}") String dbName
                                          ) {
        return new MongoDbFhirService(List.of(
                new DefaultDeviceSerializer(fhirContext)),
                new FhirBaseResourceDeSerializer(fhirContext),
                mongoClient,
                searchConfig,
                fhirContext,
                maxIncludePageSize,
                dbName
        );
    }

    /**
     * The fhir context
     *
     * @return the fhir context
     */
    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    /**
     * The expression factory
     *
     * @param searchConfig the search config
     * @return the expression factory
     */
    @Bean
    public ExpressionFactory expressionFactory(SearchConfig searchConfig) {
        return new MongoDbExpressionFactory(searchConfig);
    }

    @Bean
    public SearchConfig searchConfig() {
        return new CompositeSearchConfig(List.of(new ASComplexSearchConfig()));
    }
}
