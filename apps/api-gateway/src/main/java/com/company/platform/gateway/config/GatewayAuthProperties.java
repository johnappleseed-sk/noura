package com.company.platform.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {

    private boolean enabled = false;
    private boolean forwardClaims = true;
    private String subjectHeader = "X-Auth-Subject";
    private String usernameHeader = "X-Auth-Username";
    private String rolesHeader = "X-Auth-Roles";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isForwardClaims() {
        return forwardClaims;
    }

    public void setForwardClaims(boolean forwardClaims) {
        this.forwardClaims = forwardClaims;
    }

    public String getSubjectHeader() {
        return subjectHeader;
    }

    public void setSubjectHeader(String subjectHeader) {
        this.subjectHeader = subjectHeader;
    }

    public String getUsernameHeader() {
        return usernameHeader;
    }

    public void setUsernameHeader(String usernameHeader) {
        this.usernameHeader = usernameHeader;
    }

    public String getRolesHeader() {
        return rolesHeader;
    }

    public void setRolesHeader(String rolesHeader) {
        this.rolesHeader = rolesHeader;
    }
}
