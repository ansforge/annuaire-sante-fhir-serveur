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

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SimpleTestApp.class)
@ActiveProfiles("with-audit-read")
public class WithAuditReadTest extends BaseAuditTest {


    /**
     * By default, only the write audit is enabled
     */
    @Test
    public void withAuditRead() {
        Assert.assertNotNull(auditFilter);
        Assert.assertNotNull(defaultReadAuditService);
        Assert.assertNull(defaultWriteAuditService);
    }

}
