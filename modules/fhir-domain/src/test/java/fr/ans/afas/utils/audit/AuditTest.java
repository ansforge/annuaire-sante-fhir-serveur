/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils.audit;

import fr.ans.afas.audit.AuditFilter;
import fr.ans.afas.audit.AuditUtils;
import fr.ans.afas.fhirserver.hook.event.*;
import fr.ans.afas.fhirserver.hook.service.HookService;
import fr.ans.afas.utils.MinimalSbApplication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayDeque;

@SpringBootTest(classes = MinimalSbApplication.class)
@RunWith(SpringRunner.class)
public class AuditTest {

    @Rule
    public final LoggerRule loggerRule = new LoggerRule();
    @Inject
    HookService hookService;

    @Test
    public void testWrite() {
        hookService.callHook(BeforeCreateResourceEvent.builder()
                .resource(new Device().setId("id-d"))
                .build());
        hookService.callHook(AfterCreateResourceEvent.builder()
                .resource(new Patient().setId("id-d"))
                .build());
        hookService.callHook(new BeforeDeleteAllEvent());
        hookService.callHook(new AfterDeleteAllEvent());
        hookService.callHook(BeforeDeleteEvent.builder()
                .resourceId(new IdType("Observation", "id-d"))
                .build());
        hookService.callHook(AfterDeleteEvent.builder()
                .resourceId(new IdType("Organization", "id-d"))
                .build());


        Assert.assertEquals(6, loggerRule.getFormattedMessages().size());

        Assert.assertEquals("Event:BeforeCreateResourceEvent\tClientIp:-\tAppVersion:-\tEvent:BeforeCreateResourceEvent\tRequest:Device/id-d", loggerRule.getFormattedMessages().get(0));
        Assert.assertEquals("Event:AfterCreateResourceEvent\tClientIp:-\tAppVersion:-\tEvent:AfterCreateResourceEvent\tRequest:Patient/id-d", loggerRule.getFormattedMessages().get(1));
        Assert.assertEquals("Event:BeforeDeleteAllEvent\tClientIp:-\tAppVersion:-\tEvent:BeforeDeleteAllEvent\tRequest:null", loggerRule.getFormattedMessages().get(2));
        Assert.assertEquals("Event:AfterDeleteAllEvent\tClientIp:-\tAppVersion:-\tEvent:AfterDeleteAllEvent\tRequest:null", loggerRule.getFormattedMessages().get(3));
        Assert.assertEquals("Event:BeforeDeleteEvent\tClientIp:-\tAppVersion:-\tEvent:BeforeDeleteEvent\tRequest:Observation/id-d", loggerRule.getFormattedMessages().get(4));
        Assert.assertEquals("Event:AfterDeleteEvent\tClientIp:-\tAppVersion:-\tEvent:AfterDeleteEvent\tRequest:Organization/id-d", loggerRule.getFormattedMessages().get(5));


    }


    @Test
    public void testFilter() throws ServletException, IOException {
        var sampleIp1 = "192.168.1.5";
        var sampleIp2 = "192.168.1.6";

        var ipsBuffer = new ArrayDeque<String>();
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        FilterChain filterChain = (servletRequest, servletResponse) -> ipsBuffer.add(AuditUtils.get().getIp());
        var auditFilter = new AuditFilter();


        // test without proxy:
        Mockito.when(request.getAttribute(AuditFilter.ALREADY_FILTERED)).thenReturn(null);
        Mockito.when(request.getHeader(AuditFilter.HEADER_X_FORWARDED_FOR)).thenReturn(null);
        Mockito.when(request.getRemoteAddr()).thenReturn(sampleIp1);
        auditFilter.doFilter(request, response, filterChain);


        Assert.assertEquals(1, ipsBuffer.size());
        Assert.assertEquals(sampleIp1, ipsBuffer.getLast());

        // test with proxy:
        ipsBuffer.clear();
        Mockito.when(request.getAttribute(AuditFilter.ALREADY_FILTERED)).thenReturn(null);
        Mockito.when(request.getHeader(AuditFilter.HEADER_X_FORWARDED_FOR)).thenReturn(sampleIp2);
        Mockito.when(request.getRemoteAddr()).thenReturn(sampleIp1);
        auditFilter.doFilter(request, response, filterChain);

        Assert.assertEquals(1, ipsBuffer.size());
        Assert.assertEquals(sampleIp2, ipsBuffer.getLast());

        // test double filtering:
        ipsBuffer.clear();
        Mockito.when(request.getAttribute(AuditFilter.ALREADY_FILTERED)).thenReturn(Boolean.TRUE);
        auditFilter.doFilter(request, response, filterChain);
        Assert.assertEquals(AuditUtils.EMPTY_IP, ipsBuffer.getLast());
    }


}
