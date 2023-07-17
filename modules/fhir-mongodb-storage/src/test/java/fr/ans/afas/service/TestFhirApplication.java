/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import ca.uhn.fhir.context.FhirContext;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import fr.ans.afas.config.CleanRevisionDataConfiguration;
import fr.ans.afas.fhirserver.hook.exception.BadHookConfiguration;
import fr.ans.afas.fhirserver.hook.service.HookService;
import fr.ans.afas.fhirserver.search.config.CompositeSearchConfig;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.DefaultSerializeUrlEncrypter;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.search.expression.serialization.SerializeUrlEncrypter;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbExpressionFactory;
import fr.ans.afas.mdbexpression.domain.fhir.searchconfig.ASComplexSearchConfig;
import fr.ans.afas.mdbexpression.domain.fhir.serialization.MongoDbExpressionSerializer;
import fr.ans.afas.rass.service.DatabaseService;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.impl.MongoDbNextUrlManager;
import fr.ans.afas.rass.service.impl.SimpleDatabaseService;
import fr.ans.afas.rass.service.json.FhirBaseResourceDeSerializer;
import fr.ans.afas.rass.service.json.GenericSerializer;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.inject.Inject;
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

    @Inject
    @Bean
    public MongoDbFhirService fhirService(FhirContext fhirContext,
                                          MongoClient mongoClient,
                                          SearchConfig searchConfig,
                                          ApplicationContext context,
                                          @Value("${afas.fhir.max-include-size:5000}") int maxIncludePageSize,
                                          DatabaseService databaseService
    ) throws BadHookConfiguration {
        return new MongoDbFhirService(List.of(new GenericSerializer(searchConfig, fhirContext)),
                new FhirBaseResourceDeSerializer(fhirContext),
                mongoClient,
                searchConfig,
                fhirContext,
                new HookService(context),
                maxIncludePageSize,
                databaseService
        );
    }


    /**
     * The name of the mongodb database
     */
    @Bean
    DatabaseService databaseService(@Value("${afas.mongodb.dbname}") String name) {
        return new SimpleDatabaseService(name);
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
    public ExpressionFactory<Bson> expressionFactory(SearchConfig searchConfig) {
        return new MongoDbExpressionFactory(searchConfig);
    }

    @Bean
    public SearchConfig searchConfig() {
        return new CompositeSearchConfig(List.of(new ASComplexSearchConfig()));
    }

    /**
     * Create the url serializer
     *
     * @return the url serializer
     */
    @Bean
    SerializeUrlEncrypter serializeUrlEncrypter(@Value("afas.fhir.next-url-encryption-key") String key) {
        return new DefaultSerializeUrlEncrypter(key);
    }

    /**
     * Create the expression serializer for mongodb
     *
     * @return the expression serializer
     */
    @Bean
    @Inject
    ExpressionSerializer<Bson> expressionSerializer(ExpressionFactory<Bson> expressionFactory, SearchConfig searchConfig) {
        return new MongoDbExpressionSerializer(expressionFactory, searchConfig);
    }

    @Bean
    @Inject
    NextUrlManager<Bson> nextUrlManager(MongoClient mongoClient, @Value("${afas.fhir.next-url-max-size:500}") int maxNextUrlLength, ExpressionSerializer<Bson> expressionSerializer, SerializeUrlEncrypter serializeUrlEncrypter,
                                        @Value("${spring.data.mongodb.database}") String dbName
    ) {
        return new MongoDbNextUrlManager(mongoClient, maxNextUrlLength, expressionSerializer, serializeUrlEncrypter, dbName);
    }

}
