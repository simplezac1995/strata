// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import org.springframework.http.HttpStatus;

/** SCIM error carrying the HTTP status and SCIM error detail; the controller renders the SCIM error schema. */
public class ScimException extends RuntimeException {

    private final HttpStatus status;
    private final String scimType;

    public ScimException(HttpStatus status, String detail, String scimType) {
        super(detail);
        this.status = status;
        this.scimType = scimType;
    }

    public ScimException(HttpStatus status, String detail) {
        this(status, detail, null);
    }

    public HttpStatus status() {
        return status;
    }

    public String scimType() {
        return scimType;
    }
}
