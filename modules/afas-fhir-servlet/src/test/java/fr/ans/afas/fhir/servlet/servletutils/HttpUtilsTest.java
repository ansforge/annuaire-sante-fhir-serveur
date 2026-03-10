package fr.ans.afas.fhir.servlet.servletutils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpUtilsTest {

    @Test
    void testLastModifiedFromDate() {
        ZonedDateTime date = ZonedDateTime.parse("2024-01-01T10:15:30+01:00[Europe/Paris]");
        String expected = "Mon, 01 Jan 2024 10:15:30 CET";

        String result = HttpUtils.lastModifiedFromDate(date);

        assertEquals(expected, result);
    }

    @Test
    void testGetServerUrl() {
        String publicUrl = "http://example.com";
        String multiTenantContextPath = "/tenant1";
        String expected = "http://example.com/fhir/v2/tenant1";

        String result = HttpUtils.getServerUrl(publicUrl, multiTenantContextPath);

        assertEquals(expected, result);
    }

    @Test
    void testGetServerUrlFromRequest_HttpDefaultPort_NoContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("example.com");
        when(request.getServerPort()).thenReturn(80);
        when(request.getContextPath()).thenReturn("");

        String multiTenantContextPath = "/tenant1";

        String result = HttpUtils.getServerUrl(request, multiTenantContextPath);

        assertEquals("http://example.com/fhir/v2/tenant1", result);
    }

    @Test
    void testGetServerUrlFromRequest_HttpsDefaultPort_WithContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("secure.example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getContextPath()).thenReturn("/myapp");

        String multiTenantContextPath = "/tenant2";

        String result = HttpUtils.getServerUrl(request, multiTenantContextPath);

        assertEquals("https://secure.example.com/myapp/fhir/v2/tenant2", result);
    }

    @Test
    void testGetServerUrlFromRequest_HttpCustomPort_WithContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/fhir-app");

        String multiTenantContextPath = "/tenantX";

        String result = HttpUtils.getServerUrl(request, multiTenantContextPath);

        assertEquals("http://localhost:8080/fhir-app/fhir/v2/tenantX", result);
    }

    @Test
    void testGetServerUrlFromRequest_HttpsCustomPort_NoContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("secure.example.com");
        when(request.getServerPort()).thenReturn(8443);
        when(request.getContextPath()).thenReturn("");

        String multiTenantContextPath = "/tenantY";

        String result = HttpUtils.getServerUrl(request, multiTenantContextPath);

        assertEquals("https://secure.example.com:8443/fhir/v2/tenantY", result);
    }

    @Test
    void testGetSession_NewSessionCreated() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);

        HttpSession result = HttpUtils.getSession(request);

        assertNotNull(result);
        assertEquals(session, result);
    }

    @Test
    void testGetSession_ExistingSession() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        HttpSession result = HttpUtils.getSession(request);

        assertNotNull(result);
        assertEquals(session, result);
    }

    @Test
    void testGetSession_NullRequest() {
        HttpSession result = HttpUtils.getSession(null);

        assertNull(result);
    }

    @Test
    void testGetPagingUrls_ExistingAttribute() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        Map<Integer, String> pagingUrls = new HashMap<>();
        pagingUrls.put(1, "http://example.com/page1");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(HttpUtils.PAGING_URLS)).thenReturn(pagingUrls);

        Map<Integer, String> result = HttpUtils.getPagingUrls(request);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("http://example.com/page1", result.get(1));
    }

    @Test
    void testGetPagingUrls_NewAttributeCreated() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(HttpUtils.PAGING_URLS)).thenReturn(null);

        // Simule la création d'une nouvelle Map dans la session
        doAnswer(invocation -> {
            Object value = invocation.getArgument(1);
            when(session.getAttribute(HttpUtils.PAGING_URLS)).thenReturn(value);
            return null;
        }).when(session).setAttribute(eq(HttpUtils.PAGING_URLS), any());

        // Appel de la méthode
        Map<Integer, String> result = HttpUtils.getPagingUrls(request);

        // Vérifications
        assertNotNull(result, "La méthode doit retourner une nouvelle instance de Map.");
        assertTrue(result.isEmpty(), "La Map retournée doit être vide.");
        verify(session).setAttribute(eq(HttpUtils.PAGING_URLS), eq(result));
    }

    @Test
    void testGetPagingUrls_NoSession() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        Map<Integer, String> result = HttpUtils.getPagingUrls(request);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}