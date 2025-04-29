/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.service;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import com.mongodb.client.MongoClient;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbDateRangeExpression;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import fr.ans.afas.rass.service.impl.MongoDbNextUrlManager;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Test the paging features
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})

public class PagingTestIT {


    private static final String LAST_ID_1 = "1";
    private static final String UUID_1 = UUID.randomUUID().toString();
    private static final String UUID_2 = UUID.randomUUID().toString();
    private static final long TOTAL_1 = 3L;
    private static final int SIZE_1 = 2;
    private static final String TYPE = "Device";

    /**
     * The test db name
     */
    @Value("${spring.data.mongodb.database}")
    String dbName;

    /**
     * The expression factory
     */
    @Inject
    ExpressionFactory<Bson> expressionFactory;


    @Inject
    SearchConfigService searchConfigService;
    /**
     * Service to access fhir data
     */
    @Inject
    MongoDbFhirService mongoDbFhirService;

    /**
     * The mongodb client
     */
    @Inject
    MongoClient mongoClient;


    @Inject
    MongoDbNextUrlManager mongoDbNextUrlManager;

    @Inject
    MongoMultiTenantService multiTenantService;

    @Before
    public void init() {
        mongoDbFhirService.deleteAll();
    }

    /**
     *
     */
    @Test
    public void testSaveNextUrl() throws BadLinkException {

        for (var i : new Integer[]{/* store in db*/1, /**/ 10000}) {

            this.mongoDbNextUrlManager.cleanOldPagingData(new Date().getTime());
            this.mongoDbNextUrlManager.setMaxNextUrlLength(i);

            var selectExpression = new SelectExpression<>(TYPE, expressionFactory);
            selectExpression.getExpression().addExpression(new MongoDbDateRangeExpression(searchConfigService, FhirSearchPath.builder().path("_lastUpdated").resource(TYPE).build(), new Date(), TemporalPrecisionEnum.YEAR, ParamPrefixEnum.GREATERTHAN_OR_EQUALS));
            selectExpression.setCount(2);


            var date1 = new Date().getTime();

            var stored = mongoDbNextUrlManager.store(pd(selectExpression, date1));
            var found = mongoDbNextUrlManager.find(stored);

            Assert.assertEquals(TYPE, found.get().getType());
            Assert.assertEquals(SIZE_1, found.get().getPageSize());
            Assert.assertEquals(LAST_ID_1, found.get().getLastId());
            Assert.assertEquals(TOTAL_1, (long) found.get().getSize().getTotal());
            Assert.assertEquals(SIZE_1, (long) found.get().getSelectExpression().getCount());
            Assert.assertEquals(date1, found.get().getTimestamp());
        }

    }


    @Test
    public void testNextUrlClean() throws BadLinkException {

        // db storage:
        this.mongoDbNextUrlManager.setMaxNextUrlLength(Integer.MIN_VALUE);

        clearMongoDb();
        var selectExpression = new SelectExpression<>(TYPE, expressionFactory);
        selectExpression.getExpression().addExpression(new MongoDbDateRangeExpression(searchConfigService, FhirSearchPath.builder().path("_lastUpdated").resource(TYPE).build(), new Date(), TemporalPrecisionEnum.YEAR, ParamPrefixEnum.GREATERTHAN_OR_EQUALS));
        selectExpression.setCount(2);


        var date1 = new Date().getTime();
        var stored1 = mongoDbNextUrlManager.store(pd(selectExpression, date1));
        // check that we can get it again
        mongoDbNextUrlManager.find(stored1);

        await().atLeast(10, TimeUnit.MILLISECONDS).until(() -> System.currentTimeMillis() - date1 > 1);

        var dateBetween = new Date().getTime();


        await().atLeast(10, TimeUnit.MILLISECONDS).until(() -> System.currentTimeMillis() - dateBetween > 1);

        var date2 = new Date().getTime();
        var stored2 = mongoDbNextUrlManager.store(pd(selectExpression, date2, UUID_2));


        mongoDbNextUrlManager.cleanOldPagingData(dateBetween);

        mongoDbNextUrlManager.find(stored2);
        Assert.assertThrows(BadLinkException.class, () -> mongoDbNextUrlManager.find(stored1));


    }


    @Test
    public void testDbOrSelfContained() throws BadLinkException {
        var colNextUrls = multiTenantService.getCollection(MongoDbNextUrlManager.MONGO_COLLECTION_NAME);
        clearMongoDb();

        var selectExpression = new SelectExpression<>(TYPE, expressionFactory);
        selectExpression.getExpression().addExpression(new MongoDbDateRangeExpression(searchConfigService, FhirSearchPath.builder().path("_lastUpdated").resource(TYPE).build(), new Date(), TemporalPrecisionEnum.YEAR, ParamPrefixEnum.GREATERTHAN_OR_EQUALS));
        selectExpression.setCount(2);


        var date1 = new Date().getTime();


        // url storage:
        this.mongoDbNextUrlManager.setMaxNextUrlLength(Integer.MAX_VALUE);
        var stored = mongoDbNextUrlManager.store(pd(selectExpression, date1));
        mongoDbNextUrlManager.find(stored);
        Assert.assertTrue(stored.startsWith(MongoDbNextUrlManager.FIRST_CHAR_IN_URL));

        Assert.assertEquals(0, colNextUrls.countDocuments());

        // db storage:
        this.mongoDbNextUrlManager.setMaxNextUrlLength(Integer.MIN_VALUE);
        stored = mongoDbNextUrlManager.store(pd(selectExpression, date1));
        mongoDbNextUrlManager.find(stored);
        Assert.assertTrue(stored.startsWith(MongoDbNextUrlManager.FIRST_CHAR_IN_DB));

        Assert.assertEquals(1, colNextUrls.countDocuments());

    }


    private PagingData<Bson> pd(SelectExpression<Bson> selectExpression, long timestamp) {
        return this.pd(selectExpression, timestamp, UUID_1);
    }

    private PagingData<Bson> pd(SelectExpression<Bson> selectExpression, long timestamp, String uuid) {
        return PagingData.<Bson>builder()
                .lastId(LAST_ID_1)
                .timestamp(timestamp)
                .selectExpression(selectExpression)
                .type(TYPE)
                .uuid(uuid)
                .pageSize(SIZE_1)
                .size(CountResult.builder()
                        .total(TOTAL_1)
                        .build()).build();
    }

    private void clearMongoDb() {
        var colNextUrls = mongoClient.getDatabase(dbName).getCollection(MongoDbNextUrlManager.MONGO_COLLECTION_NAME);
        colNextUrls.deleteMany(new BsonDocument());
    }

}
