package com.aaltoasia.omi.accontrol;

import com.aaltoasia.omi.accontrol.db.DBHelper;
import com.aaltoasia.omi.accontrol.db.objects.OMIUser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FacebookApi;
import org.scribe.model.*;
import org.scribe.oauth.OAuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class that handles Facebook Authentication
 * Created by romanfilippov on 12/11/15.
 */
public class FacebookAuth {

    private final String apiKey = "";
    private final String apiSecret = "";
    private final String apiCallback = "";

    private static final Logger logger = LoggerFactory.getLogger(FacebookAuth.class);

    // see https://developers.facebook.com/docs/facebook-login/permissions
    private final String fbScope = "email,public_profile";

    private Token accessToken;
    private static final Token EMPTY_TOKEN = null;
    private final OAuthService service;

    private static final FacebookAuth instance = createInstance();
    public static FacebookAuth getInstance() {
        return instance;
    }

    private static FacebookAuth createInstance()
    {
        try {
            return new FacebookAuth();
        } catch (Exception e) {
            logger.error("When initializing FacebookAuth module ",e);
            System.exit(-1);
        }
        return null;
    }

    private FacebookAuth() throws Exception
    {
//        logger.setLevel(Level.INFO);

        this.accessToken = null;


        if (apiKey=="" || apiSecret=="" || apiCallback=="")
        {
            throw new Exception("No facebook app credentials found. Please put it  inside `src/main/java/com/aaltoasia/omi/accontrol/FacebookAuth.java`");
        }

        this.service = new ServiceBuilder()
                .provider(FacebookApi.class)
                .apiKey(apiKey)
                .apiSecret(apiSecret)
                .scope(fbScope)
                .callback(apiCallback)
                .build();
    }

    public String getAuthorizationURL()
    {
        return service.getAuthorizationUrl(EMPTY_TOKEN);
    }

    public Token getAccessToken(String authCode)
    {
        if (accessToken == null) {
            Verifier verifier = new Verifier(authCode);
            accessToken = service.getAccessToken(EMPTY_TOKEN, verifier);
        }
        return accessToken;
    }

    public String getUserInformation()
    {
        String graphAPIPath = "https://graph.facebook.com/me?fields=id,name,email";
        OAuthRequest request = new OAuthRequest(Verb.GET, graphAPIPath);
        service.signRequest(accessToken, request);
        Response response = request.send();
        return response.getBody();
    }

    public OMIUser createUserForInfo(String userData) {
        try {

            JsonObject newUserJSON = new JsonParser().parse(userData).getAsJsonObject();
            OMIUser newUser = new OMIUser();
            String userName = newUserJSON.getAsJsonPrimitive("name").getAsString();
            String userEmail = newUserJSON.getAsJsonPrimitive("email").getAsString();
            newUser.username = userName;
            newUser.email = userEmail;
            return newUser;
        } catch (Exception ex) {
            logger.warn(ex.getCause() + ":" + ex.getMessage());
            return null;
        }
    }

    public boolean registerOrLoginUser(OMIUser newUser) {
        try {

            return DBHelper.getInstance().createUserIfNotExists(newUser);

        } catch (Exception ex) {
            logger.warn(ex.getCause() + ":" + ex.getMessage());
            return false;
        }
    }
    public boolean registerOrLoginUser(String userData) {
        try {

            OMIUser newUser = createUserForInfo(userData);
            return DBHelper.getInstance().createUserIfNotExists(newUser);

        } catch (Exception ex) {
            logger.warn(ex.getCause() + ":" + ex.getMessage());
            return false;
        }
    }
}
