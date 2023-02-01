/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas;

import ca.uhn.fhir.context.FhirContext;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import fr.ans.afas.config.MongoIndexConfiguration;
import fr.ans.afas.configuration.HapiConfiguration;
import fr.ans.afas.configuration.SpringConfiguration;
import fr.ans.afas.configuration.SubscriptionSearchConfig;
import fr.ans.afas.fhirserver.hook.exception.BadHookConfiguration;
import fr.ans.afas.fhirserver.hook.service.HookService;
import fr.ans.afas.fhirserver.search.config.CompositeSearchConfig;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.DefaultSerializeUrlEncrypter;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.search.expression.serialization.SerializeUrlEncrypter;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbExpressionFactory;
import fr.ans.afas.mdbexpression.domain.fhir.serialization.MongoDbExpressionSerializer;
import fr.ans.afas.provider.ASComplexSearchConfig;
import fr.ans.afas.provider.DeviceProvider;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.impl.MongoDbNextUrlManager;
import fr.ans.afas.rass.service.json.FhirBaseResourceDeSerializer;
import fr.ans.afas.rass.service.json.FhirBaseResourceSerializer;
import fr.ans.afas.rass.service.json.GenericSerializer;
import fr.ans.afas.service.SignatureService;
import fr.ans.afas.service.SubscriptionManager;
import fr.ans.afas.service.impl.HMacSha256SignatureService;
import fr.ans.afas.service.impl.MongoSubscriptionManager;
import org.bson.conversions.Bson;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.util.List;


/**
 * A spring boot application with the fhir server and mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@ServletComponentScan
@SpringBootApplication(scanBasePackages = "fr.ans.afas", exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@Import({SpringConfiguration.class, HapiConfiguration.class, SubscriptionSearchConfig.class})
@Profile("full")
public class FullSpringAppWithMongo {
    /**
     * Launch the service
     *
     * @param args command line args (not used)
     */
    public static void main(String[] args) {
        SpringApplication.run(FullSpringAppWithMongo.class, args);
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

    @Bean
    DeviceProvider deviceProvider(FhirStoreService fhirStoreService, FhirContext fhirContext, ExpressionFactory expressionFactory, NextUrlManager nextUrlManager) {
        return new DeviceProvider(fhirStoreService, fhirContext, expressionFactory, nextUrlManager);
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


    /***
     * The expression factory for MongoDB
     * @param searchConfig the search config
     * @return the expression factory
     */
    @Bean
    @Autowired
    public ExpressionFactory<Bson> expressionFactory(SearchConfig searchConfig) {
        return new MongoDbExpressionFactory(searchConfig);
    }


    /**
     * Create the expression serializer for mongodb
     *
     * @return the expression serializer
     */
    @ConditionalOnMissingBean
    @Bean
    @Autowired
    NextUrlManager nextUrlManager(MongoClient mongoClient, @Value("${afas.fhir.next-url-max-size:500}") int maxNextUrlLength, ExpressionSerializer<Bson> expressionSerializer, SerializeUrlEncrypter serializeUrlEncrypter,
                                  @Value("${spring.data.mongodb.database}") String dbName
    ) {
        return new MongoDbNextUrlManager(mongoClient, maxNextUrlLength, expressionSerializer, serializeUrlEncrypter, dbName);
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
    @Autowired
    ExpressionSerializer<Bson> expressionSerializer(ExpressionFactory<Bson> expressionFactory, SearchConfig searchConfig) {
        return new MongoDbExpressionSerializer(expressionFactory, searchConfig);
    }


    @Bean
    @Autowired
    FhirBaseResourceDeSerializer fhirBaseResourceDeSerializer(FhirContext fhirContext) {
        return new FhirBaseResourceDeSerializer(fhirContext);
    }

    /**
     * Create the storage service on mongodb
     *
     * @return the storage service
     */
    @Bean
    @Autowired
    FhirStoreService<Bson> fhirStoreService(
            List<FhirBaseResourceSerializer> serializers,
            FhirBaseResourceDeSerializer fhirBaseResourceDeSerializer,
            MongoClient mongoClient,
            SearchConfig searchConfig,
            ApplicationContext context,
            FhirContext fhirContext,
            @Value("${afas.fhir.max-include-size:5000}")
                    int maxIncludePageSize,
            @Value("${afas.mongodb.dbname}")
                    String dbName) throws BadHookConfiguration {
        return new MongoDbFhirService(
                serializers,
                fhirBaseResourceDeSerializer,
                mongoClient,
                searchConfig,
                fhirContext,
                new HookService(context),
                maxIncludePageSize,
                dbName
        );
    }


    /**
     * Create mongodb indexes
     *
     * @return the service that create indexes on mongodb
     */
    @Bean
    public MongoIndexConfiguration mongoIndexConfiguration() {
        return new MongoIndexConfiguration();
    }


    /**
     * Create the serializer of fhir resources
     *
     * @return the serializer
     */
    @Autowired
    @Bean
    public GenericSerializer genericSerializer(SearchConfig searchConfig, FhirContext fhirContext) {
        return new GenericSerializer(searchConfig, fhirContext);
    }


    @Bean
    SignatureService signatureService() {
        return new HMacSha256SignatureService("1234");
    }


    @Bean
    ASComplexSearchConfig asComplexSearchConfig() {
        return new ASComplexSearchConfig();
    }

    @Bean
    @Autowired
    public SearchConfig searchConfigService(List<ServerSearchConfig> searchConfigs) {
        return new CompositeSearchConfig(searchConfigs);
    }


    @Bean
    SubscriptionManager subscriptionManagerMocked() {
        var subscriptionManager = Mockito.mock(MongoSubscriptionManager.class);
        Mockito.doNothing().when(subscriptionManager).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());
        return subscriptionManager;
    }

}
