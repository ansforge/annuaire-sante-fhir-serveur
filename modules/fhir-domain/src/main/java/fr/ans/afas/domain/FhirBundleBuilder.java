/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.domain;


public class FhirBundleBuilder {

    private static final String PAGE_ATTRIBUTE = "_page";

    public static String wrapBundleEntry(String serverUrl, BundleEntry content) {
        return "\n{" +
                "\"fullUrl\":" +
                "\"" + serverUrl + "/" + content.type + "/" + content.id + "\"," +
                "\"resource\":" +
                content.content +
                "}";
    }


    public String getHeader(String bundleId, Long total) {
        var sb = new StringBuilder();

        sb.append("{");
        sb.append("\"resourceType\": \"Bundle\",");
        sb.append("\"type\": \"searchset\",");
        sb.append("\"id\": \"");
        sb.append(bundleId);
        sb.append("\",");

        if (total != null) {
            sb.append("\"total\":");
            sb.append(total);
            sb.append(",");
        }

        sb.append("\"entry\": [");
        return sb.toString();
    }

    public String getFooter(String nextUrl, String currentUrl, String nextPageId) {
        var sb = new StringBuilder("]");
        sb.append(",\"link\": [");
        if (nextPageId != null) {
            sb.append(" {")
                    .append("\"relation\": \"next\",")
                    .append("\"url\": \"")
                    .append(nextUrl);
            if (!nextUrl.endsWith("/") && !nextUrl.endsWith(PAGE_ATTRIBUTE)) {
                sb.append("/");
            }

            // Vérifier si nextUrl contient déjà "_page" pour éviter le doublon
            if (!nextUrl.contains(PAGE_ATTRIBUTE)) {
                sb.append(PAGE_ATTRIBUTE);
            }
            sb.append("?id=")
                    .append(nextPageId)
                    .append("\"},");
        }

        sb.append(" {")
                .append("\"relation\": \"self\",")
                .append("\"url\": \"")
                .append(currentUrl).append("\"}");

        sb.append("]");
        sb.append("}");
        return sb.toString();
    }


    public static class BundleEntry {
        final String content;
        final String id;

        final String type;

        public BundleEntry(String type, String id, String content) {
            this.content = content;
            this.id = id;
            this.type = type;
        }
    }

}
