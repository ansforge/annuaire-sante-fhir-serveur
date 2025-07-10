package fr.ans.afas.rass.service.impl;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.CountOptions;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.HasCondition;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbIncludeExpression;
import fr.ans.afas.rass.service.CloseableWrapper;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoQueryUtilsTest {

    @Mock
    private SearchConfigService searchConfigService;

    @Mock
    private MongoCollection<Document> collection;

    @Mock
    private MongoMultiTenantService mongoMultiTenantService;

    @Mock
    private SelectExpression<Bson> selectExpression;

    @Mock
    private CountOptions countOptions;

    @Mock
    private SearchContext searchContext;

    @InjectMocks
    private MongoQueryUtils mongoQueryUtils;

    MockedStatic<AggregationUtils> aggregationUtils;

    @BeforeEach
    void setUp() {
        aggregationUtils = mockStatic(AggregationUtils.class);
    }

    @AfterEach
    public void afterEach() {
        aggregationUtils.close();
    }

    @Test
    void testCount_WithAggregation() {
        List<Document> documents = new ArrayList<>();
        documents.add(new Document());
        when(selectExpression.getHasConditions()).thenReturn(Collections.singletonList(mock(HasCondition.class)));
        aggregationUtils.when(() -> AggregationUtils.generateAggregation(any(), any(), anyLong(), any(), any())).thenReturn(documents);
        AggregateIterable aggregateIterable = mock(AggregateIterable.class);
        MongoCursor<Document> mongoCursor = mock(MongoCursor.class);
        when(mongoCursor.next()).thenReturn(new Document("c", 5));
        when(mongoCursor.hasNext()).thenReturn(true);
        when(collection.aggregate(any())).thenReturn(aggregateIterable);
        when(collection.aggregate(any()).maxTime(anyLong(), any())).thenReturn(aggregateIterable);
        when(collection.aggregate(any()).iterator()).thenReturn(mongoCursor);


        CountResult result = MongoQueryUtils.count(searchConfigService, collection, selectExpression, countOptions, mongoMultiTenantService);

        assertNotNull(result);
        assertEquals(5, result.getTotal());
    }

    @Test
    void testCount_WithoutAggregation() {
        when(selectExpression.getHasConditions()).thenReturn(Collections.emptyList());
        when(selectExpression.interpreter()).thenReturn(mock(Bson.class));
        when(collection.countDocuments(any(Bson.class), any())).thenReturn(5L);

        CountResult result = MongoQueryUtils.count(searchConfigService, collection, selectExpression, countOptions, mongoMultiTenantService);

        assertNotNull(result);
        assertEquals(5L, result.getTotal());
        verify(collection).countDocuments(any(Bson.class), any());
    }

    @Test
    void testCount_WithMongoExecutionTimeoutException() {
        List<Document> documents = new ArrayList<>();
        documents.add(new Document());
        when(selectExpression.getHasConditions()).thenReturn(Collections.singletonList(mock(HasCondition.class)));
        aggregationUtils.when(() -> AggregationUtils.generateAggregation(any(), any(), anyLong(), any(), any())).thenReturn(documents);
        when(collection.aggregate(any())).thenThrow(new MongoExecutionTimeoutException(1, "Timeout"));

        CountResult result = MongoQueryUtils.count(searchConfigService, collection, selectExpression, countOptions, mongoMultiTenantService);

        assertNotNull(result);
        assertNull(result.getTotal());
    }

    @Test
    void testCount_WithMongoCommandException() {
        List<Document> documents = new ArrayList<>();
        documents.add(new Document());
        when(selectExpression.getHasConditions()).thenReturn(Collections.singletonList(mock(HasCondition.class)));
        aggregationUtils.when(() -> AggregationUtils.generateAggregation(any(), any(), anyLong(), any(), any())).thenReturn(documents);
        when(collection.aggregate(any())).thenThrow(new MongoCommandException(new BsonDocument(), new ServerAddress()));

        CountResult result = MongoQueryUtils.count(searchConfigService, collection, selectExpression, countOptions, mongoMultiTenantService);

        assertNotNull(result);
        assertNull(result.getTotal());
    }

    @Test
    void testSearchFirstPage_WithAggregation() {
        List<Document> documents = new ArrayList<>();
        documents.add(new Document());
        when(selectExpression.getHasConditions()).thenReturn(Collections.singletonList(mock(HasCondition.class)));
        aggregationUtils.when(() -> AggregationUtils.generateAggregation(any(), any(), anyLong(), any(), any())).thenReturn(documents);
        when(collection.aggregate(any())).thenReturn(mock(AggregateIterable.class));

        CloseableWrapper<MongoCursor<Document>> result = MongoQueryUtils.searchFirstPage(searchConfigService, 10, selectExpression, collection, 1L, mongoMultiTenantService);

        assertNotNull(result);
        verify(collection).aggregate(any());
    }

    @Test
    void testSearchFirstPage_WithoutAggregation() {
        FindIterable<Document> findIterable = mock(FindIterable.class);
        FindIterable<Document> projectionIterable = mock(FindIterable.class);
        FindIterable<Document> sortIterable = mock(FindIterable.class);
        when(selectExpression.getHasConditions()).thenReturn(Collections.emptyList());
        when(selectExpression.interpreter()).thenReturn(mock(Bson.class));
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.projection(null)).thenReturn(projectionIterable);
        when(projectionIterable.sort(any(Bson.class))).thenReturn(sortIterable);
        when(sortIterable.limit(anyInt())).thenReturn(mock(FindIterable.class));

        CloseableWrapper<MongoCursor<Document>> result = MongoQueryUtils.searchFirstPage(searchConfigService, 10, selectExpression, collection, 1L, mongoMultiTenantService);

        assertNotNull(result);
        verify(collection).find(any(Bson.class));
    }

    @Test
    void testSearchNextPage_WithAggregation() {
        List<Document> documents = new ArrayList<>();
        documents.add(new Document());
        when(selectExpression.getHasConditions()).thenReturn(Collections.singletonList(mock(HasCondition.class)));
        aggregationUtils.when(() -> AggregationUtils.generateAggregation(any(), any(), anyLong(), any(), any())).thenReturn(documents);
        when(collection.aggregate(any())).thenReturn(mock(AggregateIterable.class));

        CloseableWrapper<MongoCursor<Document>> result = MongoQueryUtils.searchNextPage(searchConfigService, 10, searchContext, selectExpression, collection, "someId", mongoMultiTenantService);

        assertNotNull(result);
        verify(collection).aggregate(any());
    }

    @Test
    void testSearchNextPage_WithoutAggregation() {
        Document document = new Document();
        document.append(MongoQueryUtils.VALID_FROM_ATTRIBUTE, "123465");
        document.append(MongoQueryUtils.VALID_TO_ATTRIBUTE, "78965412");
        FindIterable<Document> findIterable = mock(FindIterable.class);
        FindIterable<Document> projectionIterable = mock(FindIterable.class);
        FindIterable<Document> sortIterable = mock(FindIterable.class);

        when(selectExpression.getHasConditions()).thenReturn(Collections.emptyList());
        when(selectExpression.interpreter()).thenReturn(document);
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.projection(null)).thenReturn(projectionIterable);
        when(projectionIterable.sort(any(Bson.class))).thenReturn(sortIterable);
        when(sortIterable.limit(anyInt())).thenReturn(mock(FindIterable.class));

        CloseableWrapper<MongoCursor<Document>> result = MongoQueryUtils.searchNextPage(searchConfigService, 10, searchContext, selectExpression, collection, "66e452c6e7cab2642a6134cc", mongoMultiTenantService);

        assertNotNull(result);
        verify(collection).find(any(Bson.class));
    }

    @Test
    void testWrapQueryWithRevisionDate() {
        Bson query = mock(Bson.class);
        Bson result = MongoQueryUtils.wrapQueryWithRevisionDate(System.currentTimeMillis(), query);

        assertNotNull(result);
    }

    @Test
    void testAddSinceParam_WithSince() {
        when(selectExpression.getSince()).thenReturn(mock(Date.class));
        Bson bson = mock(Bson.class);

        Bson result = MongoQueryUtils.addSinceParam(selectExpression, bson);

        assertNotNull(result);
    }

    @Test
    void testAddSinceParam_WithoutSince() {
        when(selectExpression.getSince()).thenReturn(null);
        Bson bson = mock(Bson.class);

        Bson result = MongoQueryUtils.addSinceParam(selectExpression, bson);

        assertEquals(bson, result);
    }

    @Test
    void testExtractIncludeReferences_Success() {
        String referenceExpected = "Practitioner/12345";
        Map<String, Set<String>> includes = new HashMap<>();
        SearchParamConfig searchParamConfig = mock(SearchParamConfig.class);
        Document document = new Document("practitioner-reference", List.of(referenceExpected));
        when(searchParamConfig.getIndexName()).thenReturn("practitioner");
        when(selectExpression.getIncludes()).thenReturn(Collections.singleton(new MongoDbIncludeExpression(searchConfigService, "type", "name")));
        when(searchConfigService.getSearchConfigByResourceAndParamName(anyString(), anyString())).thenReturn(Optional.of(searchParamConfig));

        MongoQueryUtils.extractIncludeReferences(searchConfigService, "someType", selectExpression, includes, document);

        // Add assertions to verify that the references are extracted correctly
        assertEquals(1, includes.size());
        assertTrue(includes.get("Practitioner").contains(referenceExpected));
    }

    @Test
    void testExtractIncludeReferences_BadConfigurationException() {
        when(selectExpression.getIncludes()).thenReturn(Collections.singleton(new MongoDbIncludeExpression(searchConfigService, "type", "name")));
        when(searchConfigService.getSearchConfigByResourceAndParamName(anyString(), anyString())).thenReturn(Optional.empty());
        Document document = new Document();

        Exception exception = assertThrows(BadConfigurationException.class, () ->
                MongoQueryUtils.extractIncludeReferences(searchConfigService, "someType", selectExpression, null, document));

        assertEquals("Search not supported on path: someType.name", exception.getMessage());
    }

    @Test
    void testGenerateProjectionWithElements() {
        // Arrange
        SearchParamConfig searchParamConfigActive = SearchParamConfig.builder().name("active").isCompulsoryOrModifierElementsParam(true).build();
        FindIterable<Document> findIterable = mock(FindIterable.class);
        FindIterable<Document> projectionIterable = mock(FindIterable.class);
        FindIterable<Document> sortIterable = mock(FindIterable.class);
        FindIterable<Document> limit = mock(FindIterable.class);
        Document projectionExpected = new Document();
        projectionExpected.append("_id", 1);
        projectionExpected.append("_hash", 1);
        projectionExpected.append("_revision", 1);
        projectionExpected.append("_validFrom", 1);
        projectionExpected.append("_validTo", 1);
        projectionExpected.append("_lastWriteDate", 1);
        projectionExpected.append("fhir.resourceType", 1);
        projectionExpected.append("fhir.id", 1);
        projectionExpected.append("fhir.meta", 1);
        projectionExpected.append("t_profile", 1);
        projectionExpected.append("fhir.active", 1);
        projectionExpected.append("fhir.identifier", 1);

        when(selectExpression.getElements()).thenReturn(Set.of("identifier"));
        when(selectExpression.getFhirResource()).thenReturn("Device");
        when(searchConfigService.getAllByFhirResource(anyString())).thenReturn(List.of(searchParamConfigActive));
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.projection(projectionExpected)).thenReturn(projectionIterable);
        when(projectionIterable.sort(any(Bson.class))).thenReturn(sortIterable);
        when(sortIterable.limit(anyInt())).thenReturn(limit);

        // Act
        CloseableWrapper<MongoCursor<Document>> result = MongoQueryUtils.searchFirstPage(searchConfigService, 10, selectExpression, collection, 1L, mongoMultiTenantService);

        // Assert
        assertNotNull(result);
    }
}