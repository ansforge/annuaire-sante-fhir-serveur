/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IPagingProvider;
import com.mongodb.client.MongoClient;
import fr.ans.afas.audit.AuditFilter;
import fr.ans.afas.config.MongoIndexConfiguration;
import fr.ans.afas.domain.ResourceAndSubResources;
import fr.ans.afas.fhir.AfasPagingProvider;
import fr.ans.afas.fhir.TransactionalResourceProvider;
import fr.ans.afas.fhirserver.hook.exception.BadHookConfiguration;
import fr.ans.afas.fhirserver.hook.service.HookService;
import fr.ans.afas.fhirserver.search.config.CompositeSearchConfig;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.search.config.yaml.YamlSearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.DefaultSerializeUrlEncrypter;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.search.expression.serialization.SerializeUrlEncrypter;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.audit.DefaultReadAuditService;
import fr.ans.afas.fhirserver.service.audit.DefaultWriteAuditService;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbExpressionFactory;
import fr.ans.afas.mdbexpression.domain.fhir.serialization.MongoDbExpressionSerializer;
import fr.ans.afas.rass.service.DatabaseService;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.impl.MongoDbNextUrlManager;
import fr.ans.afas.rass.service.impl.SimpleDatabaseService;
import fr.ans.afas.rass.service.json.FhirBaseResourceDeSerializer;
import fr.ans.afas.rass.service.json.FhirBaseResourceSerializer;
import fr.ans.afas.rass.service.json.GenericSerializer;
import fr.ans.afas.subscription.SubscriptionConfiguration;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.inject.Inject;
import java.util.List;

/**
 * Autoconfiguration of the fhir server.
 * This will set up a fhir server R4 without any resources. The storage is Mongodb.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Configuration
@Import({MongoConfig.class, YamlSearchConfig.class, SubscriptionConfiguration.class})
@ServletComponentScan
public class AfasServerAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public FhirContext context() {
        return FhirContext.forR4();
    }


    /***
     * The expression factory for MongoDB
     * @param searchConfig the search config
     * @return the expression factory
     */
    @ConditionalOnMissingBean
    @Bean
    @Inject
    public ExpressionFactory<Bson> expressionFactory(SearchConfig searchConfig) {
        return new MongoDbExpressionFactory(searchConfig);
    }


    /**
     * Create the url serializer
     *
     * @return the url serializer
     */
    @ConditionalOnMissingBean
    @Bean
    SerializeUrlEncrypter serializeUrlEncrypter(@Value("afas.fhir.next-url-encryption-key") String key) {
        return new DefaultSerializeUrlEncrypter(key);
    }

    /**
     * Create the expression serializer for mongodb
     *
     * @return the expression serializer
     */
    @ConditionalOnMissingBean
    @Bean
    @Inject
    ExpressionSerializer<Bson> expressionSerializer(ExpressionFactory<Bson> expressionFactory, SearchConfig searchConfig) {
        return new MongoDbExpressionSerializer(expressionFactory, searchConfig);
    }

    /**
     * Create the expression serializer for mongodb
     *
     * @return the expression serializer
     */
    @ConditionalOnMissingBean
    @Bean
    @Inject
    NextUrlManager<Bson> nextUrlManager(MongoClient mongoClient, @Value("${afas.fhir.next-url-max-size:500}") int maxNextUrlLength, ExpressionSerializer<Bson> expressionSerializer, SerializeUrlEncrypter serializeUrlEncrypter, @Value("${spring.data.mongodb.database}") String dbName) {
        return new MongoDbNextUrlManager(mongoClient, maxNextUrlLength, expressionSerializer, serializeUrlEncrypter, dbName);

    }

    /**
     * Create the paging provider for the hapi framework
     *
     * @return the paging provider
     */
    @ConditionalOnMissingBean
    @Bean
    IPagingProvider afasPagingProvider() {
        return new AfasPagingProvider<Bson>();
    }


    @ConditionalOnMissingBean
    @Bean
    @Inject
    FhirBaseResourceDeSerializer fhirBaseResourceDeSerializer(FhirContext fhirContext) {
        return new FhirBaseResourceDeSerializer(fhirContext);
    }

    /**
     * Create the storage service on mongodb
     *
     * @return the storage service
     */
    @ConditionalOnMissingBean(FhirStoreService.class)
    @Bean
    @Inject
    FhirStoreService<Bson> fhirStoreService(
            List<FhirBaseResourceSerializer<ResourceAndSubResources>> serializers,
            FhirBaseResourceDeSerializer fhirBaseResourceDeSerializer,
            MongoClient mongoClient,
            SearchConfig searchConfig,
            FhirContext fhirContext,
            ApplicationContext context,
            @Value("${afas.fhir.max-include-size:5000}")
                    int maxIncludePageSize,
            DatabaseService databaseService) throws BadHookConfiguration {
        return new MongoDbFhirService(
                serializers,
                fhirBaseResourceDeSerializer,
                mongoClient,
                searchConfig,
                fhirContext,
                new HookService(context),
                maxIncludePageSize,
                databaseService
        );
    }


    @ConditionalOnMissingBean
    @Bean
    DatabaseService databaseService(@Value("${afas.mongodb.dbname}") String dbName) {
        return new SimpleDatabaseService(dbName);
    }


    @ConditionalOnMissingBean
    @Bean
    AfasServerConfigurerAdapter afasServerConfigurerAdapter() {
        return new AfasServerConfigurerAdapter();
    }

    @ConditionalOnMissingBean
    @Bean
    @Inject
    public SearchConfig searchConfigService(List<ServerSearchConfig> searchConfigs) {
        return new CompositeSearchConfig(searchConfigs);
    }


    /**
     * Create mongodb indexes
     *
     * @return the service that create indexes on mongodb
     */
    @ConditionalOnMissingBean
    @Bean
    public MongoIndexConfiguration mongoIndexConfiguration() {
        return new MongoIndexConfiguration();
    }


    /**
     * Create the serializer of fhir resources
     *
     * @return the serializer
     */
    @ConditionalOnMissingBean
    @Inject
    @Bean
    public GenericSerializer genericSerializer(SearchConfig searchConfig, FhirContext fhirContext) {
        return new GenericSerializer(searchConfig, fhirContext);
    }


    /**
     * Create the fhir hapi transaction provider
     *
     * @return the transaction provider
     */
    @ConditionalOnMissingBean
    @Inject
    @Bean
    public TransactionalResourceProvider<Bson> transactionalResourceProvider(FhirStoreService<Bson> fhirStoreService) {
        return new TransactionalResourceProvider<>(fhirStoreService);
    }


    // Audit

    /**
     * The audit filter
     *
     * @return audit filter
     */
    @ConditionalOnProperty(value = "afas.fhir.audit.enabled", havingValue = "true")
    @Bean
    public AuditFilter auditFilter() {
        return new AuditFilter();
    }


    /**
     * Default audit listener for read operations
     */
    @ConditionalOnExpression("${afas.fhir.audit.enabled:false} && ${afas.fhir.audit.read:false}")
    @Bean
    DefaultReadAuditService defaultReadAuditService() {
        return new DefaultReadAuditService();
    }

    /**
     * Default audit listener for write operations
     */
    @ConditionalOnExpression("${afas.fhir.audit.enabled:false} && ${afas.fhir.audit.write:true}")
    @Bean
    DefaultWriteAuditService defaultWriteAuditService() {
        return new DefaultWriteAuditService();
    }

    /**
     * Default audit listener for write operations

     @ConditionalOnMissingBean
     @Inject
     @Bean DefaultIndexService defaultIndexService(FhirStoreService<Bson> fhirStoreService, ExpressionFactory<Bson> expressionFactory, SearchConfig searchConfig, GenericSerializer genericSerializer) {
     return new DefaultIndexService(fhirStoreService, expressionFactory, searchConfig, genericSerializer);
     }  */

}
