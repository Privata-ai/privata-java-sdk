package ventures.blockbird.auth;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ventures.blockbird.util.PropsUtil;

public class FirebaseAuth {

    final static Logger logger = LogManager.getLogger(FirebaseAuth.class);

    private static final String AUTH_BASE_URL = "https://www.googleapis.com/identitytoolkit/v3/relyingparty/";
    private static final String VERIFY_BASE_URL = "https://securetoken.googleapis.com/v1/token";
    private static final String OPERATION_AUTH = "verifyPassword";

    private String firebaseKey;
    private String refreshToken;
    private Long expiryTime;
    private String idToken = null;

    public FirebaseAuth(boolean sandbox) {
        PropsUtil util = new PropsUtil("/project.properties");
        if (sandbox) {
            this.firebaseKey = util.getProps("firebaseApiKeyLocal");
        } else {
            this.firebaseKey = util.getProps("firebaseApiKey");
        }
    }

    /**
     * Authenticate using email and password on
     * 
     * @param dbId
     * @param dbSecret
     * @return idToken
     * @throws Exception
     */
    public void auth(String dbId, String dbSecret) throws Exception {
        HttpURLConnection urlRequest = null;
        try {
            URL url = new URL(AUTH_BASE_URL + OPERATION_AUTH + "?key=" + this.firebaseKey);
            urlRequest = (HttpURLConnection) url.openConnection();
            urlRequest.setDoOutput(true);
            urlRequest.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            OutputStream os = urlRequest.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write("{\"email\":\"" + dbId + "@blockbird.ventures" + "\",\"password\":\"" + dbSecret
                    + "\",\"returnSecureToken\":true}");
            osw.flush();
            osw.close();

            urlRequest.connect();

            JsonParser jp = new JsonParser();
            // Convert the input stream to a json element
            JsonElement root = jp.parse(new InputStreamReader((InputStream) urlRequest.getContent()));
            // May be an array, may be an object.
            JsonObject rootObj = root.getAsJsonObject();
            this.idToken = rootObj.get("idToken").getAsString();
            this.refreshToken = rootObj.get("refreshToken").getAsString();
            // get expiry time in miliseconds
            this.expiryTime = new Date().getTime() + (rootObj.get("expiresIn").getAsLong() * 1000);
        } catch (Exception e) {
            logger.error(e);
            throw e;
        } finally {
            urlRequest.disconnect();
        }
        return;
    }

    /**
     * Verify token
     * 
     * @return idToken returns the idToken
     */
    public String getIdToken() throws Exception {
        if (this.idToken == null) {
            logger.error("Need to authenticate before verifying token. idToken is null");
            throw new Error("Need to authenticate before verifying token. idToken is null");
        }
        // check if expired
        long now = new Date().getTime();
        HttpURLConnection urlRequest = null;
        String params = "grant_type=refresh_token&refresh_token=" + refreshToken;

        if (now > expiryTime) {
            try {
                URL url = new URL(VERIFY_BASE_URL + "?key=" + firebaseKey);
                urlRequest = (HttpURLConnection) url.openConnection();
                byte[] postData = params.getBytes(StandardCharsets.UTF_8);
                int postDataLength = postData.length;

                urlRequest.setDoOutput(true);
                urlRequest.setInstanceFollowRedirects(false);
                urlRequest.setRequestMethod("POST");
                urlRequest.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlRequest.setRequestProperty("charset", "utf-8");
                urlRequest.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                urlRequest.setUseCaches(false);
                try (DataOutputStream wr = new DataOutputStream(urlRequest.getOutputStream())) {
                    wr.write(postData);
                }

                urlRequest.connect();
                JsonParser jp = new JsonParser();
                // Convert the input stream to a json element
                JsonElement root = jp.parse(new InputStreamReader((InputStream) urlRequest.getContent()));
                // May be an array, may be an object.
                JsonObject rootObj = root.getAsJsonObject();
                this.idToken = rootObj.get("id_token").getAsString();
                // Reset the timer
                this.expiryTime = new Date().getTime() + (rootObj.get("expires_in").getAsLong() * 1000);
            } catch (Exception e) {
                logger.error(e);
                throw e;
            } finally {
                urlRequest.disconnect();
            }
        }
        return this.idToken;
    }

}