/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.domain;


public class FhirBundleBuilder {


    public static String wrapBundleEntry(BundleEntry content) {
        return "\n{" +
                "\"fullUrl\":" +
                "\"/" + content.type + "/" + content.id + "\"," +
                "\"resource\":" +
                content.content +
                "}";
    }


    public String getHeader(Long total) {
        var sb = new StringBuilder();

        sb.append("{");
        sb.append("\"resourceType\": \"Bundle\",");
        sb.append("\"type\": \"searchset\",");

        if (total != null) {
            sb.append("\"total\":");
            sb.append(total);
            sb.append(",");
        }

        sb.append("\"entry\": [");
        return sb.toString();
    }

    public String getFooter(String serverUrl, String nextPageId) {
        var sb = new StringBuilder();
        sb.append("]");

        //,{"relation":
        if (nextPageId != null) {

            sb.append(",\"link\": [ {");
            sb.append("\"relation\": \"next\",");
            sb.append("\"url\": \"").append(serverUrl).append("/fhir/v2-alpha/_page?id=");
            sb.append(nextPageId);
            sb.append("\"}]");

        }
        sb.append("}");
        return sb.toString();
    }


    public static class BundleEntry {
        String content;
        String id;

        String type;

        public BundleEntry(String type, String id, String content) {
            this.content = content;
            this.id = id;
            this.type = type;
        }
    }

}
