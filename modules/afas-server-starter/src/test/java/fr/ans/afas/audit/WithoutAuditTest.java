/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.audit;

import fr.ans.afas.SimpleTestApp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SimpleTestApp.class)
@ActiveProfiles("with-no-audit")
public class WithoutAuditTest extends BaseAuditTest {

    @Autowired(required = false)
    private AuditFilter auditFilter;


    @Test
    public void testAudit() {
        Assert.assertNull(auditFilter);
        Assert.assertNull(defaultReadAuditService);
        Assert.assertNull(defaultWriteAuditService);
    }

}
