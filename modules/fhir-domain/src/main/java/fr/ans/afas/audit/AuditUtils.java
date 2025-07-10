/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.audit;

import java.util.Objects;

/**
 * Access audit information of the request
 */
public class AuditUtils {

    /**
     * Default ip (no ip)
     */
    public static final String EMPTY_IP = "-";

    /**
     * Thread local that store audit information
     */
    private static final ThreadLocal<AuditInformation> contextHolder = new ThreadLocal<>();

    private AuditUtils() {
    }

    /**
     * Store audit information
     *
     * @param auditInformation the information to store
     */
    public static void store(AuditInformation auditInformation) {
        contextHolder.set(auditInformation);
    }

    /**
     * Get current audit information
     *
     * @return audit information
     */
    public static AuditInformation get() {
        var found = contextHolder.get();
        return Objects.requireNonNullElseGet(found, () -> new AuditInformation(EMPTY_IP));
    }

    /**
     * Clean audit information
     */
    public static void clean() {
        contextHolder.remove();
    }
}
