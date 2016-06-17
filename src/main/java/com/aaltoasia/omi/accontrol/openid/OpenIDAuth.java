package com.aaltoasia.omi.accontrol.openid;

import com.aaltoasia.omi.accontrol.config.ConfigHelper;
import com.aaltoasia.omi.accontrol.db.DBHelper;
import com.aaltoasia.omi.accontrol.db.objects.OMIUser;
import com.aaltoasia.omi.accontrol.openid.config.json.Provider;
import com.aaltoasia.omi.accontrol.openid.config.json.ProviderDiscoveryData;
import com.aaltoasia.omi.accontrol.openid.config.json.ServicesConfig;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.*;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Created by romanfilippov on 08/06/16.
 */
public class OpenIDAuth {

    private static final Logger logger = LoggerFactory.getLogger(OpenIDAuth.class);

    private static final OpenIDAuth instance = createInstance();
    public static OpenIDAuth getInstance() {
        return instance;
    }

    private ServicesConfig servicesConfig;
    private final URI callback = new URI("https://localhost/security/RegService");
    private HashMap<String,ProviderDiscoveryData> discoveredProviders = new HashMap<>();

    private static OpenIDAuth createInstance()
    {
        try {
            return new OpenIDAuth();
        } catch (Exception e) {
            logger.error("When initializing OpenIDAuth module ",e);

        }
        return null;
    }

    private OpenIDAuth() throws Exception
    {
        loadConfig();
    }

    private void discoverConfigForState(String configURL, State state) throws IOException
    {
        InputStream is = new URL(configURL).openStream();
        try {
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            ProviderDiscoveryData data = gson.fromJson(reader, ProviderDiscoveryData.class);

            discoveredProviders.put(state.toString(),data);

        } finally {
            is.close();
        }
    }

    public String getAuthURL(String providerName)
    {
        try {

            Provider serviceProvider = servicesConfig.getCredentials().get(providerName);
            // The client identifier provisioned by the server
            ClientID clientID = new ClientID(serviceProvider.getClientID());

//            URI clientEndPoint = new URI("https://accounts.google.com/o/oauth2/v2/auth");

            // Generate random state string for pairing the response to the request
            State state = new State();

            discoverConfigForState(serviceProvider.getConfigURL(),state);

            // Generate nonce
            Nonce nonce = new Nonce();

            ProviderDiscoveryData discoveredProvider = discoveredProviders.get(state.toString());
            discoveredProvider.providerName = providerName;

            // Compose the request (in code flow)
            AuthenticationRequest req = new AuthenticationRequest(
                    new URI(discoveredProvider.getAuthorization_endpoint()),
                    new ResponseType(ResponseType.Value.CODE),
                    Scope.parse("openid email profile"),
                    clientID,
                    callback,
                    state,
                    nonce);

            return req.toURI().toString();
        } catch (Exception ex) {
            logger.error("Error:",ex);
        }
        return null;
    }

    private void loadConfig() {

        Gson gson = new Gson();

        try {
            JsonReader reader = new JsonReader(new FileReader(ConfigHelper.openidCredentialsFileName));
            servicesConfig = gson.fromJson(reader, ServicesConfig.class);
        } catch (Exception ex) {
            logger.error("Error:",ex);
        }

    }

    public OMIUser getAccessTokenAndAuthUser(String authCode, String state)
    {
        try {

            ProviderDiscoveryData discoveredProvider = discoveredProviders.get(state);
            Provider providerConfig = servicesConfig.getCredentials().get(discoveredProvider.providerName);

            URI clientEndPoint = new URI(discoveredProvider.getToken_endpoint());
            ClientID clientID = new ClientID(providerConfig.getClientID());
            Secret clientSecret = new Secret(providerConfig.getClientSecret());
            AuthorizationCode code = new AuthorizationCode(authCode);

            TokenRequest tokenReq =
                    new TokenRequest(
                    clientEndPoint,
                    new ClientSecretBasic(clientID,clientSecret), new AuthorizationCodeGrant(code,
                    callback));

            HTTPResponse tokenHTTPResp = tokenReq.toHTTPRequest().send();

            // Parse and check response
            TokenResponse tokenResponse = OIDCTokenResponseParser.parse(tokenHTTPResp);

            if (tokenResponse instanceof TokenErrorResponse) {
                ErrorObject error = ((TokenErrorResponse) tokenResponse).getErrorObject();

                logger.error("Error:",error);
                return null;
            }

            OIDCAccessTokenResponse accessTokenResponse = (OIDCAccessTokenResponse) tokenResponse;
            AccessToken ac_token = accessTokenResponse.getAccessToken();
            JWT userID_token = accessTokenResponse.getIDToken();

            JSONObject receivedInfo = userID_token.getJWTClaimsSet().toJSONObject();

            if (!checkForUserData(receivedInfo)) {

                //If info does not contain username and email then we will explicitly ask for it
                receivedInfo = getUserData(discoveredProvider.getUserinfo_endpoint(),ac_token);

            }

            return registerUserForInfo(receivedInfo);

        } catch (Exception ex) {
            logger.error("Error:",ex);
            return null;
        }
    }

    private boolean checkForUserData(JSONObject object) {
        return (object.get("name") != null && object.get("email") != null);
    }

    private OMIUser registerUserForInfo(JSONObject object) {
        String userName = (String)object.get("name");
        String userEmail = (String)object.get("email");

        OMIUser user = new OMIUser();
        user.email = userEmail;
        user.username = userName;

        boolean userRegResult = DBHelper.getInstance().createUserIfNotExists(user);
        if (userRegResult)
            return user;
        else
            return null;
    }

    public JSONObject getUserData(String dataURL, AccessToken ac_token)
    {
        try {
            UserInfoRequest userInfoReq = new UserInfoRequest(
                    new URI(dataURL),
                    (BearerAccessToken) ac_token);

            HTTPResponse userInfoHTTPResp = userInfoReq.toHTTPRequest().send();

            UserInfoResponse userInfoResponse = UserInfoResponse.parse(userInfoHTTPResp);

            if (userInfoResponse instanceof UserInfoErrorResponse) {
                ErrorObject error = ((UserInfoErrorResponse) userInfoResponse).getErrorObject();
                logger.error("Error:", error);
                return null;
            }

            UserInfoSuccessResponse sResponse = (UserInfoSuccessResponse) userInfoResponse;
            JSONObject claims = sResponse.getUserInfo().toJSONObject();

            return claims;
        } catch (Exception ex) {
            logger.error("Error:",ex);
            return null;
        }
    }
}
