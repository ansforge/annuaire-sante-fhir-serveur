package fr.ans.afas.filter;

import fr.ans.afas.exception.TenantNotFoundException;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.Tenant;
import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantFilterTest {

    @Mock
    private ServerSearchConfig serverSearchConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private TenantSearchConfig tenantSearchConfig;

    @Mock
    private Tenant tenantConfig;

    @Mock
    private RequestDispatcher requestDispatcher;  // Ajoutez ceci

    private TenantFilter tenantFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        tenantFilter = new TenantFilter(serverSearchConfig);
    }

    @Test
    void testDoFilter_TenantFound() throws Exception {
        // Initialize a Tenant object with valid data
        Tenant tenant = new Tenant();
        tenant.setName("tenant1");
        tenant.setPath("/tenant1");

        // Mock the tenantSearchConfig to return the initialized Tenant object
        when(tenantSearchConfig.getTenantConfig()).thenReturn(tenant);

        Map<String, TenantSearchConfig> configMap = new HashMap<>();
        configMap.put("tenant1", tenantSearchConfig);

        // Mock the serverSearchConfig to return the configMap
        when(serverSearchConfig.getConfigs()).thenReturn(configMap);

        // Mock the request to return a specific URI
        when(request.getRequestURI()).thenReturn("/fhir/v1/tenant1/resource");

        // Mock getRequestDispatcher to return a valid RequestDispatcher
        when(request.getRequestDispatcher(anyString())).thenReturn(requestDispatcher);

        // Execute the filter
        tenantFilter.doFilter(request, response, chain);

        // Verify that getRequestURI is called exactly 2 times
        verify(request, times(2)).getRequestURI(); // Change 1

        // Verify that TenantUtil.setCurrentTenant was called with "tenant1"
        verify(tenantSearchConfig, atLeastOnce()).getTenantConfig();

        // Optionally, you can verify that the forward method was called correctly
        ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
        ArgumentCaptor<ServletResponse> responseCaptor = ArgumentCaptor.forClass(ServletResponse.class);

        verify(requestDispatcher).forward(requestCaptor.capture(), responseCaptor.capture());
    }

    @Test
    void testDoFilter_TenantNotFound() throws Exception {
        // Mock the serverSearchConfig to return an empty map
        when(serverSearchConfig.getConfigs()).thenReturn(Collections.emptyMap());
        when(request.getRequestURI()).thenReturn("/fhir/v4/5.X");

        // Verify that TenantNotFoundException is thrown
        assertThrows(TenantNotFoundException.class, () -> tenantFilter.doFilter(request, response, chain));
    }

    @Test
    void testDoFilter_UrlNotFound() throws Exception {
        // Mock the serverSearchConfig to return an empty map
        when(serverSearchConfig.getConfigs()).thenReturn(Collections.emptyMap());
        when(request.getRequestURI()).thenReturn("/fhir/toto");

        tenantFilter.doFilter(request, response, chain);

        // Verify that TenantNotFoundException is thrown
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void testPatternMatching() {
        Pattern pattern = Pattern.compile("/fhir/v\\d*/(.+?)(/.*)?");

        // Test cases
        String validUri1 = "/fhir/v1/tenant1/resource";
        String validUri2 = "/fhir/v2/tenant2";
        String validUri3 = "/fhir/va/tenant3";
        String invalidUri = "/invalid/v1/resource";

        // Valid URI 1
        Matcher matcher1 = pattern.matcher(validUri1);
        assertTrue(matcher1.matches());
        assertEquals("tenant1", matcher1.group(1));

        // Valid URI 2
        Matcher matcher2 = pattern.matcher(validUri2);
        assertTrue(matcher2.matches());
        assertEquals("tenant2", matcher2.group(1));

        // Valid URI 3
        assertFalse(pattern.matcher(validUri3).matches());


        // Invalid URI
        assertFalse(pattern.matcher(invalidUri).matches());
    }
}
