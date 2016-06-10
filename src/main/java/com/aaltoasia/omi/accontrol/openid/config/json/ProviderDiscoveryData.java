package com.aaltoasia.omi.accontrol.openid.config.json;

/**
 * Created by romanfilippov on 10/06/16.
 */
public class ProviderDiscoveryData {

    private String issuer;
    private String authorization_endpoint;
    private String token_endpoint;
    private String userinfo_endpoint;
    private String revocation_endpoint;
    private String jwks_uri;

    public String providerName;

    public String getIssuer() { return this.issuer; };
    public void setIssuer(String issuer) { this.issuer = issuer; };

    public String getAuthorization_endpoint() { return this.authorization_endpoint; };
    public void setAuthorization_endpoint(String authorization_endpoint) { this.authorization_endpoint = authorization_endpoint; };

    public String getToken_endpoint() { return this.token_endpoint; };
    public void setToken_endpoint(String token_endpoint) { this.token_endpoint = token_endpoint; };

    public String getUserinfo_endpoint() { return this.userinfo_endpoint; };
    public void setUserinfo_endpoint(String userinfo_endpoint) { this.userinfo_endpoint = userinfo_endpoint; };

    public String getRevocation_endpoint() { return this.revocation_endpoint; };
    public void setRevocation_endpoint(String revocation_endpoint) { this.revocation_endpoint = revocation_endpoint; };

    public String getJwks_uri() { return this.jwks_uri; };
    public void setJwks_uri(String jwks_uri) { this.jwks_uri = jwks_uri; };
}
