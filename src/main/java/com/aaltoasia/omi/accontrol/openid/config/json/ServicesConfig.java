package com.aaltoasia.omi.accontrol.openid.config.json;

import java.util.Map;

/**
 * Created by romanfilippov on 10/06/16.
 */
public class ServicesConfig {

    private Map<String, Provider> credentials;

    public Map<String, Provider> getCredentials() { return this.credentials; }
    public void setCredentials(Map<String, Provider> credentials) { this.credentials = credentials; }
}
