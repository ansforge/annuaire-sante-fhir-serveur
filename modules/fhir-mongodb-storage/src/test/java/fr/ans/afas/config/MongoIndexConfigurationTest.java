package fr.ans.afas.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.Tenant;
import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;
import fr.ans.afas.filter.TenantFilter;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

class MongoIndexConfigurationTest {

    private MongoIndexConfiguration mongoIndexConfiguration;
    private MongoClient mongoClient;
    private SearchConfigService searchConfigService;
    private ServerSearchConfig serverSearchConfig;
    private MongoMultiTenantService mongoMultiTenantService;
    private MongoCollection<Document> mockCollection;
    private TenantFilter tenantFilter;
    @Mock
    private TenantSearchConfig tenantSearchConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialisation des mocks
        mongoClient = mock(MongoClient.class);
        searchConfigService = mock(SearchConfigService.class);
        serverSearchConfig = mock(ServerSearchConfig.class);
        mongoMultiTenantService = mock(MongoMultiTenantService.class);
        mockCollection = mock(MongoCollection.class);

        mongoIndexConfiguration = new MongoIndexConfiguration();
        mongoIndexConfiguration.mongoClient = mongoClient;
        mongoIndexConfiguration.searchConfigService = searchConfigService;
        mongoIndexConfiguration.serverSearchConfig = serverSearchConfig;
        mongoIndexConfiguration.mongoMultiTenantService = mongoMultiTenantService;
        mongoIndexConfiguration.dbName = "testDb";
    }

    @Test
    void testCreateIndexes() {
        // Initialisation d'un Tenant valide
        Tenant tenant = new Tenant();
        tenant.setName("tenant1");
        tenant.setPath("/tenant1");
        tenantFilter = new TenantFilter(serverSearchConfig);

        // Mock du tenantSearchConfig
        when(tenantSearchConfig.getTenantConfig()).thenReturn(tenant);

        // Mock des configurations
        Map<String, TenantSearchConfig> configMap = new HashMap<>();
        configMap.put("tenant1", tenantSearchConfig);
        when(serverSearchConfig.getConfigs()).thenReturn(configMap);

        // Mock des ressources et des index
        when(searchConfigService.getResources()).thenReturn(Set.of("Resource1"));
        when(searchConfigService.getIndexesByFhirResource("Resource1")).thenReturn(Set.of("index1", "index2"));
        when(mongoMultiTenantService.getCollection("Resource1")).thenReturn(mockCollection);

        // Appel de la méthode
        mongoIndexConfiguration.createIndexes();

        // Vérifications
        verify(mockCollection, times(1)).createIndex(new Document("index1", 1));
        verify(mockCollection, times(1)).createIndex(new Document("index2", 1));
    }

    @Test
    void testCreateJoins() throws Exception {
        // Préparation des mocks
        String resourceSearchConfig = "Resource1";

        // Appel de la méthode privée via réflexion
        Method createJoinsMethod = MongoIndexConfiguration.class.getDeclaredMethod("createJoins", String.class, MongoCollection.class);
        createJoinsMethod.setAccessible(true);
        createJoinsMethod.invoke(mongoIndexConfiguration, resourceSearchConfig, mockCollection);

        // Vérifications (ajustez selon votre logique)
        verify(mockCollection, never()).createIndex(any(Document.class));
    }




}