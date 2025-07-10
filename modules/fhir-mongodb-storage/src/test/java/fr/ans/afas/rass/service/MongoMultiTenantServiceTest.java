package fr.ans.afas.rass.service;

import com.mongodb.client.*;
import fr.ans.afas.exception.TenantNotFoundException;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.Tenant;
import fr.ans.afas.fhirserver.service.DefaultMultiTenantService;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.when;

class MongoMultiTenantServiceTest {

    private static final String TEST_TENANT_1 = "tenant-1";

    private static final Tenant tenant1 = new Tenant();

    static {
        tenant1.setName(TEST_TENANT_1);
        tenant1.setPath("/t1");
        tenant1.setDbname("dbtest");
        tenant1.setSuffixCollection("_0.1");
    }

    @InjectMocks
    private MongoMultiTenantService mongoMultiTenantService;

    @Mock
    private ServerSearchConfig serverSearchConfig;

    @Mock
    private MongoClient mongoClient;

    @Mock
    private MongoDatabase mongoDatabase;

    @Mock
    private MongoCollection<Document> mongoCollection;

    @Mock
    private FindIterable<Document> findIterable;

    @Mock
    private MongoCursor<Document> mongoCursor;

    @Mock
    private DefaultMultiTenantService defaultMultiTenantService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void getCollection() {
        String resourceType = "Device";

        Document document = new Document();
        document.put("resourceType", "Device");
        document.put("id", "1");

        when(mongoCursor.next()).thenReturn(document);
        when(findIterable.iterator()).thenReturn(mongoCursor);
        when(defaultMultiTenantService.getTenant()).thenReturn(tenant1);
        when(mongoCollection.countDocuments()).thenReturn(1L);
        when(mongoCollection.find()).thenReturn(findIterable);
        when(mongoDatabase.getCollection(resourceType + tenant1.getSuffixCollection())).thenReturn(mongoCollection);
        when(mongoClient.getDatabase(tenant1.getDbname())).thenReturn(mongoDatabase);
        MongoCollection<Document> collection = mongoMultiTenantService.getCollection(resourceType);
        Assertions.assertEquals(1L, collection.countDocuments());
        Assertions.assertEquals(document, collection.find().iterator().next());
    }

    @Test
    void getCollectionThrowException() {
        when(defaultMultiTenantService.getTenant()).thenThrow(TenantNotFoundException.class);
        String resourceType = "Device";
        Assertions.assertThrows(TenantNotFoundException.class, () -> mongoMultiTenantService.getCollection(resourceType));
    }
}