/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.service;

import ca.uhn.fhir.context.FhirContext;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import fr.ans.afas.config.CleanRevisionDataConfiguration;
import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhirserver.hook.exception.BadHookConfiguration;
import fr.ans.afas.fhirserver.hook.service.HookService;
import fr.ans.afas.fhirserver.search.config.CompositeSearchConfigService;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.DefaultSerializeUrlEncrypter;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.search.expression.serialization.SerializeUrlEncrypter;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.IndexService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbExpressionFactory;
import fr.ans.afas.mdbexpression.domain.fhir.searchconfig.ASComplexSearchConfig;
import fr.ans.afas.mdbexpression.domain.fhir.serialization.MongoDbExpressionSerializer;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import fr.ans.afas.rass.service.impl.DefaultIndexService;
import fr.ans.afas.rass.service.impl.MongoDbNextUrlManager;
import fr.ans.afas.rass.service.json.FhirBaseResourceDeSerializer;
import fr.ans.afas.rass.service.json.GenericSerializer;
import fr.ans.afas.utils.TenantUtil;
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
import java.util.Map;

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

    private static final String DEMO_TENANT = "tenant-1";

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
                                          SearchConfigService searchConfigService,
                                          ApplicationContext context,
                                          MongoMultiTenantService mongoMultiTenantService
    ) throws BadHookConfiguration {
        var ms = new MongoDbFhirService(List.of(new GenericSerializer(searchConfigService, fhirContext)),
                new FhirBaseResourceDeSerializer(fhirContext),
                searchConfigService,
                fhirContext,
                new HookService(context),
                mongoMultiTenantService
        );
        TenantUtil.setCurrentTenant(DEMO_TENANT);
        return ms;
    }


    @Bean
    AfasConfiguration afasConfiguration() {
        return new AfasConfiguration();
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
     * @param searchConfigService the search config
     * @return the expression factory
     */
    @Bean
    public ExpressionFactory<Bson> expressionFactory(SearchConfigService searchConfigService) {
        return new MongoDbExpressionFactory(searchConfigService);
    }

    @Bean
    public SearchConfigService searchConfig() {
        return new CompositeSearchConfigService(List.of(new ASComplexSearchConfig()));
    }

    @Bean
    public ServerSearchConfig serverSearchConfig() {
        return new ServerSearchConfig(Map.of(DEMO_TENANT, new ASComplexSearchConfig()));
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
    ExpressionSerializer<Bson> expressionSerializer(ExpressionFactory<Bson> expressionFactory, SearchConfigService searchConfigService) {
        return new MongoDbExpressionSerializer(expressionFactory, searchConfigService);
    }

    @Bean
    @Inject
    NextUrlManager<Bson> nextUrlManager(MongoMultiTenantService mongoMultiTenantService, @Value("${afas.fhir.next-url-max-size:500}") int maxNextUrlLength, ExpressionSerializer<Bson> expressionSerializer, SerializeUrlEncrypter serializeUrlEncrypter,
                                        @Value("${spring.data.mongodb.database}") String dbName
    ) {
        return new MongoDbNextUrlManager(mongoMultiTenantService, maxNextUrlLength, expressionSerializer, serializeUrlEncrypter, dbName);
    }


    @Bean
    @Inject
    GenericSerializer genericSerializer(SearchConfigService searchConfigService, FhirContext fhirContext) {
        return new GenericSerializer(searchConfigService, fhirContext);
    }

    @Bean
    @Inject
    IndexService indexService(FhirStoreService<Bson> fhirStoreService, ExpressionFactory<Bson> expressionFactory, SearchConfigService searchConfigService, GenericSerializer genericSerializer) {
        return new DefaultIndexService(fhirStoreService, expressionFactory, searchConfigService, genericSerializer);
    }


    @Bean
    MongoMultiTenantService multiTenantService() {
        return new MongoMultiTenantService();
    }

}
