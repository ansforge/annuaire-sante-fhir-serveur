/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.audit;

import fr.ans.afas.SimpleTestApp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SimpleTestApp.class)
@ActiveProfiles("with-audit")
public class WithAuditTest extends BaseAuditTest {

    @Inject
    private AuditFilter auditFilter;


    @Test
    public void withAudit() {
        Assert.assertNotNull(auditFilter);
        Assert.assertNull(defaultReadAuditService);
        Assert.assertNotNull(defaultWriteAuditService);
    }

}
