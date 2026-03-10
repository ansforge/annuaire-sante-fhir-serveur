/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.domain;

import java.util.Map;

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

    public String getFooter(String nextUrl, String currentUrl, String nextPageId, int pageNumber, Map<Integer, String> pagingUrls) {
        pagingUrls.put(pageNumber, currentUrl);
        var sbJson = new StringBuilder("]");
        sbJson.append(",\"link\": [");
        if (nextPageId != null) {
            // On forme l'URL de la page suivante
            StringBuilder sbNextUrl = new StringBuilder(nextUrl);
            if (!nextUrl.endsWith("/") && !nextUrl.endsWith(PAGE_ATTRIBUTE)) {
                sbNextUrl.append("/");
            }

            // Vérifier si nextUrl contient déjà "_page" pour éviter le doublon
            if (!nextUrl.contains(PAGE_ATTRIBUTE)) {
                sbNextUrl.append(PAGE_ATTRIBUTE);
            }
            sbNextUrl.append("?id=")
                    .append(nextPageId);
            // On l'injecte l'URL dans le map pour la page suivante
            pagingUrls.put(pageNumber + 1, sbNextUrl.toString());
            // On ajoute également l'URL au JSON
            sbJson.append(" {")
                    .append("\"relation\": \"next\",")
                    .append("\"url\": \"")
                    .append(sbNextUrl)
                    .append("\"},");
        }
        // URL current
        sbJson.append(" {")
                .append("\"relation\": \"self\",")
                .append("\"url\": \"")
                .append(currentUrl).append("\"}");

        // URL previous
        if (pageNumber > 0 && pagingUrls.get(pageNumber - 1) != null) {
            sbJson.append(", {")
                    .append("\"relation\": \"previous\",")
                    .append("\"url\": \"")
                    .append(pagingUrls.get(pageNumber - 1));
            sbJson.append("\"}");
        }

        sbJson.append("]");
        sbJson.append("}");
        return sbJson.toString();
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
