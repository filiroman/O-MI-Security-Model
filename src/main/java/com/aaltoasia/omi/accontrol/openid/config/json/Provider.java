package com.aaltoasia.omi.accontrol.openid.config.json;

/**
 * Created by romanfilippov on 10/06/16.
 */
public class Provider {

//    private String name;
    private String configURL;
    private String clientID;
    private String clientSecret;

//    public String getName() { return this.name; }
//    public void setName(String name) { this.name = name; }

    public String getConfigURL() { return this.configURL; }
    public void setConfigURL(String configURL) { this.configURL = configURL; }

    public String getClientID() { return this.clientID; }
    public void setClientID(String clientID) { this.clientID = clientID; }

    public String getClientSecret() { return this.clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
}
