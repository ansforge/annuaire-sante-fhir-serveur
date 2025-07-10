package fr.ans.afas.rass.service;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.result.DeleteResult;
import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.domain.ResourceAndSubResources;
import fr.ans.afas.exception.ResourceNotFoundException;
import fr.ans.afas.fhirserver.hook.service.HookService;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.data.TotalMode;
import fr.ans.afas.fhirserver.search.expression.IncludeExpression;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.exception.CantReadFhirResource;
import fr.ans.afas.fhirserver.service.exception.CantWriteFhirResource;
import fr.ans.afas.fhirserver.service.exception.TooManyElementToDeleteException;
import fr.ans.afas.rass.service.impl.AggregationUtils;
import fr.ans.afas.rass.service.impl.MongoQueryUtils;
import fr.ans.afas.rass.service.json.FhirBaseResourceDeSerializer;
import fr.ans.afas.rass.service.json.FhirBaseResourceSerializer;
import fr.ans.afas.rass.service.json.GenericSerializer;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
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
class MongoDbFhirServiceTest {

    @Mock
    private HookService hookService;

    @Mock
    private MongoMultiTenantService mongoMultiTenantService;

    @Mock
    private FhirBaseResourceDeSerializer fhirBaseResourceDeSerializer;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SearchConfigService searchConfigService;

    private MongoDbFhirService mongoDbFhirService;

    private List<FhirBaseResourceSerializer<ResourceAndSubResources>> serializers;

    @BeforeEach
    public void setUp() {
        serializers = List.of(new GenericSerializer(searchConfigService, FhirContext.forR4()));
        mongoDbFhirService = new MongoDbFhirService(serializers, fhirBaseResourceDeSerializer, searchConfigService, null, hookService, mongoMultiTenantService);
    }

    @Test
    void testStore_ValidCollection() {
        DomainResource resource = new Practitioner();
        resource.setId("1");
        Collection<DomainResource> resources = Collections.singletonList(resource);
        Document document = new Document("t_id", "1").append("_hash", 123465789).append("_revision", 1).append("fhir", new Document());
        MongoCollection<Document> collection = mock(MongoCollection.class);
        FindIterable<Document> findIterable = mock(FindIterable.class);
        MongoCursor<Document> mongoCursor = mock(MongoCursor.class);

        when(mongoMultiTenantService.getCollection(anyString())).thenReturn(collection);
        when(mongoCursor.next()).thenReturn(document);
        when(mongoCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(findIterable.iterator()).thenReturn(mongoCursor);
        when(collection.find(any(Bson.class))).thenReturn(findIterable);

        List<IIdType> result = mongoDbFhirService.store(resources, true);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Practitioner", result.get(0).getResourceType());
        assertEquals("1", result.get(0).getIdPart());
    }

    @Test
    void testStore_EmptyCollection() {
        Collection<DomainResource> resources = Collections.emptyList();

        List<IIdType> result = mongoDbFhirService.store(resources, true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /*@Test
    public void testFindById_ValidId() {
        DomainResource resource = new Practitioner();
        resource.setId("1");
        Document document = new Document("t_id", "1").append("fhir", new Document());
        MongoCollection<Document> collection = mock(MongoCollection.class);
        FindIterable<Document> findIterable = mock(FindIterable.class);
        FindIterable<Document> limitIterable = mock(FindIterable.class);
        when(mongoMultiTenantService.getCollection(anyString())).thenReturn(collection);
        when(searchConfigService.getResources()).thenReturn(Set.of("Practitioner"));
        when(limitIterable.first()).thenReturn(document);
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.limit(anyInt())).thenReturn(limitIterable);

        IBaseResource result = mongoDbFhirService.findById("Practitioner", new IdType("1"));

        assertNotNull(result);
        assertEquals("1", result.getIdElement().getIdPart());
    }*/

    @Test
    void testFindById_InvalidId() {
        MongoCollection<Document> collection = mock(MongoCollection.class);
        FindIterable<Document> findIterable = mock(FindIterable.class);
        FindIterable<Document> limitIterable = mock(FindIterable.class);
        when(mongoMultiTenantService.getCollection(anyString())).thenReturn(collection);
        when(searchConfigService.getResources()).thenReturn(Set.of("Practitioner"));
        when(collection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.limit(anyInt())).thenReturn(limitIterable);

        IBaseResource result = mongoDbFhirService.findById("Practitioner", new IdType("1"));

        assertNull(result);
    }

    @Test
    void testCount_ValidResourceType() {
        SelectExpression<Bson> selectExpression = mock(SelectExpression.class);
        when(searchConfigService.getResources()).thenReturn(Set.of("Practitioner"));
        when(mongoMultiTenantService.getCollection(anyString())).thenReturn(mock(MongoCollection.class));
        when(selectExpression.getFhirResource()).thenReturn("Practitioner");
        when(selectExpression.getTotalMode()).thenReturn(TotalMode.ALWAYS);
        MockedStatic<MongoQueryUtils> mongoQueryUtils = mockStatic(MongoQueryUtils.class);
        mongoQueryUtils.when(() -> MongoQueryUtils.count(any(), any(), any(), any(), any())).thenReturn(CountResult.builder().total(1L).build());

        CountResult result = mongoDbFhirService.count(selectExpression);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        mongoQueryUtils.close();
    }

    /*@Test
    public void testDelete_ValidId() {
        MongoCollection<Document> collection = mock(MongoCollection.class);
        when(mongoMultiTenantService.getCollection(anyString())).thenReturn(collection);
        when(collection.deleteOne(any())).thenReturn(mock(DeleteResult.class));

        boolean result = mongoDbFhirService.delete("DomainResource", new IdType("1"));

        assertTrue(result);
    }*/

    @Test
    void testDelete_InvalidId() {
        MongoCollection<Document> collection = mock(MongoCollection.class);
        when(mongoMultiTenantService.getCollection(anyString())).thenReturn(collection);
        when(collection.deleteOne(any())).thenReturn(mock(DeleteResult.class));

        boolean result = mongoDbFhirService.delete("DomainResource", new IdType("invalid"));

        assertFalse(result);
    }

    /*@Test
    public void testDeleteElementsNotStoredSince_TooManyElements() {
        when(mongoMultiTenantService.getCollection(anyString())).thenReturn(mock(MongoCollection.class));

        Exception exception = assertThrows(TooManyElementToDeleteException.class, () -> {
            mongoDbFhirService.deleteElementsNotStoredSince(System.currentTimeMillis());
        });

        assertTrue(exception.getMessage().contains("There is more than"));
    }*/

    @Test
    void testFindRevIncludes_ValidInput_ReturnsBundleEntries() {
        long searchRevision = 1L;
        Set<String> ids = new HashSet<>(Arrays.asList("id1", "id2"));
        Set<IncludeExpression<Bson>> includes = new HashSet<>();
        IncludeExpression<Bson> include = mock(IncludeExpression.class);
        includes.add(include);
        Document document = new Document("t_id", "docId").append("fhir", new Document());
        MongoCollection<Document> collectionIncluded = mock(MongoCollection.class);
        FindIterable<Document> findIterable = mock(FindIterable.class);
        MongoCursor<Document> mongoCursor = mock(MongoCursor.class);

        when(include.getType()).thenReturn("Practitioner");
        when(include.getName()).thenReturn("includeName");
        when(searchConfigService.getResources()).thenReturn(Set.of("Practitioner"));
        when(searchConfigService.getSearchConfigByPath(any())).thenReturn(Optional.of(new SearchParamConfig()));
        when(mongoMultiTenantService.getCollection(anyString())).thenReturn(collectionIncluded);
        when(mongoCursor.next()).thenReturn(document);
        when(mongoCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(findIterable.cursor()).thenReturn(mongoCursor);
        when(collectionIncluded.find(any(Bson.class))).thenReturn(findIterable);

        // Act
        List<FhirBundleBuilder.BundleEntry> result = mongoDbFhirService.findRevIncludes(searchRevision, ids, includes);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindRevIncludes_NotCorrectTypes_ThrowsException() {
        long searchRevision = 1L;
        Set<String> ids = new HashSet<>(Arrays.asList("id1", "id2"));
        Set<IncludeExpression<Bson>> includes = new HashSet<>();
        IncludeExpression<Bson> include = mock(IncludeExpression.class);
        includes.add(include);
        when(include.getType()).thenReturn("Other");
        when(searchConfigService.getResources()).thenReturn(Set.of("Practitioner"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            mongoDbFhirService.findRevIncludes(searchRevision, ids, includes);
        });
    }

    @Test
    void testFindRevIncludes_NotTypeDefined_ThrowsException() {
        long searchRevision = 1L;
        Set<String> ids = new HashSet<>(Arrays.asList("id1", "id2"));
        Set<IncludeExpression<Bson>> includes = new HashSet<>();
        IncludeExpression<Bson> include = mock(IncludeExpression.class);
        includes.add(include);
        when(include.getType()).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            mongoDbFhirService.findRevIncludes(searchRevision, ids, includes);
        });
    }

    @Test
    void testFindRevIncludes_SearchConfigNotFound_ThrowsException() {
        long searchRevision = 1L;
        Set<String> ids = new HashSet<>(Arrays.asList("id1", "id2"));
        Set<IncludeExpression<Bson>> includes = new HashSet<>();
        IncludeExpression<Bson> include = mock(IncludeExpression.class);
        when(include.getType()).thenReturn("validType");
        when(include.getName()).thenReturn("invalidName");
        includes.add(include);

        when(searchConfigService.getResources()).thenReturn(Set.of("validType"));
        when(searchConfigService.getSearchConfigByPath(any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CantReadFhirResource.class, () -> {
            mongoDbFhirService.findRevIncludes(searchRevision, ids, includes);
        });
    }

    @Test
    void testFindRevIncludes_CursorEmpty_ReturnsEmptyList() {
        long searchRevision = 1L;
        Set<String> ids = new HashSet<>(Arrays.asList("id1", "id2"));
        Set<IncludeExpression<Bson>> includes = new HashSet<>();
        IncludeExpression<Bson> include = mock(IncludeExpression.class);
        includes.add(include);
        MongoCollection<Document> collectionIncluded = mock(MongoCollection.class);
        FindIterable<Document> findIterable = mock(FindIterable.class);
        MongoCursor<Document> mongoCursor = mock(MongoCursor.class);

        when(include.getType()).thenReturn("Practitioner");
        when(include.getName()).thenReturn("includeName");
        when(searchConfigService.getResources()).thenReturn(Set.of("Practitioner"));
        when(searchConfigService.getSearchConfigByPath(any())).thenReturn(Optional.of(new SearchParamConfig()));
        when(mongoMultiTenantService.getCollection(anyString())).thenReturn(collectionIncluded);
        when(mongoCursor.hasNext()).thenReturn(false);
        when(findIterable.cursor()).thenReturn(mongoCursor);
        when(collectionIncluded.find(any(Bson.class))).thenReturn(findIterable);

        // Act
        List<FhirBundleBuilder.BundleEntry> result = mongoDbFhirService.findRevIncludes(searchRevision, ids, includes);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}