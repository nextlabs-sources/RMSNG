/**
 *
 */
package com.nextlabs.rms.repository.sharepoint.response;

import com.google.gson.annotations.SerializedName;

/**
 * @author nnallagatla
 *
 */
public class CreateAnonymousLink {

    @SerializedName("d")
    private CreateAnonymousLinkResponse spResponse;

    public CreateAnonymousLinkResponse getCreateAnonymousLinkResponse() {
        return spResponse;
    }

    public static class CreateAnonymousLinkResponse {

        @SerializedName("CreateAnonymousLinkVerbose")
        private String anonymousLink;

        public String getAnonymousLink() {
            return anonymousLink;
        }

        public void setAnonymousLink(String anonymousLink) {
            this.anonymousLink = anonymousLink;
        }
    }
}
