/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.audit;

import lombok.Builder;
import lombok.Getter;

/**
 * Hold information used by the audit trail
 */
@Getter
public class AuditInformation {
    /**
     * The client ip address
     */
    private final String ip;

    @Builder
    public AuditInformation(String ip) {
        this.ip = ip;
    }
}
