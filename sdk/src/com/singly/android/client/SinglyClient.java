package com.singly.android.client;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.entity.ByteArrayEntity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.singly.android.util.SinglyUtils;

/**
 * A client that handles authentication and requests to the Singly API.
 * 
 * @see https://singly.com/docs/api
 */
public class SinglyClient {

  private static final String TAG = SinglyClient.class.getSimpleName();
  private static SinglyClient instance = null;

  private String clientId;
  private String clientSecret;
  private Class authenticationActivity = AuthenticationActivity.class;

  private SinglyClient()
    throws IOException {
    
    this.clientId = "your_client_id";
    this.clientSecret = "your_client_secret";
  }

  /**
   * Returns an instance of the SinglyClient singleton.  It creates an instance
   * of the SinglyClient if one did not previously exist.
   */
  public static SinglyClient getInstance() {
    if (instance == null) {
      try {
        instance = new SinglyClient();
      }
      catch (IOException e) {
        instance = null;
        Log.e(TAG, "Error loading singly.properties", e);
      }
    }
    return instance;
  }

  /**
   * Returns true if the application has been previously authenticated and 
   * has a Singly access token.
   * 
   * @param context The context from which isAuthenticated is called.
   * 
   * @return True if the app has a Singly access token.  False otherwise.
   */
  public boolean isAuthenticated(Context context) {
    SharedPreferences prefs = context.getSharedPreferences("singly",
      Context.MODE_PRIVATE);
    String accessToken = prefs.getString("accessToken", null);
    return accessToken != null;
  }

  /**
   * Called to authenticate a user through Singly for a specific service.
   * 
   * An application needs to be authenticated with at least one service prior
   * to making api calls.
   * 
   * Authenticate will open a new AuthenticationActivity to authenticate the
   * user with the service. If the user successfully authenticates with the 
   * service then the Singly access token will be placed into the shared 
   * preferences under the key accessToken.  This access token will then be
   * used when making api callas.
   * 
   * Expert: The Activity class that handles the authentication process can be
   * changed by calling the {@link #setAuthenticationActivity(Class)} method. 
   * The assumption is that the new Activity will store the Singly access token 
   * by using {@link SinglyUtils#saveAccessToken(Context, String)} method upon a
   * successful authentication with the service.
   * 
   * @param context The context from which authenticate is called.
   * @param service The service to authenticate the user against.
   */
  public void authenticate(Context context, String service) {

    Intent authIntent = new Intent(context, authenticationActivity);
    authIntent.putExtra("clientId", clientId);
    authIntent.putExtra("clientSecret", clientSecret);
    authIntent.putExtra("service", service);
    context.startActivity(authIntent);
  }

  /**
   * Performs a GET request to the Singly API.
   * 
   * All network communication is performed in a separate thread.  As such it 
   * is fine to call this method from the main UI thread.  
   * 
   * The AsyncApiResponseHandler parameter is an asynchronous callback handler
   * that is used to retrieve the result of the network request to the API.  On
   * success the response from the API is returned.  On failure a Throwable 
   * error object will be returned.
   * 
   * @param context The current android context.
   * @param apiEndpoint The Singly API endpoint to call.
   * @param queryParams Any query parameters to send along with the request.
   * @param responseHandler An asynchronous callback handler for the request.
   * 
   * @see https://singly.com/docs/api For documentation on Singly api calls.
   * 
   * @throws IllegalStateException if no access token is found, meaning the 
   * client has not been authenticated.
   */
  public void doGetApiRequest(Context context, String apiEndpoint,
    Map<String, String> queryParams,
    final AsyncApiResponseHandler responseHandler) {

    // fail if no access token
    String accessToken = SinglyUtils.getAccessToken(context);
    if (accessToken == null) {
      throw new IllegalStateException("No access token found");
    }

    // get the http client and add api url
    AsyncHttpClient client = new AsyncHttpClient();
    Map<String, String> params = new LinkedHashMap<String, String>();
    params.put("access_token", accessToken);
    if (queryParams != null) {
      params.putAll(queryParams);
    }
    String getApiCallUrl = SinglyUtils.createSinglyURL(apiEndpoint);

    // do an async get request
    client.get(getApiCallUrl, new RequestParams(params),
      new AsyncHttpResponseHandler() {

        @Override
        public void onSuccess(String response) {
          responseHandler.onSuccess(response);
        }

        @Override
        public void onFailure(Throwable error) {
          responseHandler.onFailure(error);
        }
      });
  }

  /**
   * Performs a POST request to the Singly API.
   * 
   * All network communication is performed in a separate thread.  As such it 
   * is fine to call this method from the main UI thread.  
   * 
   * The AsyncApiResponseHandler parameter is an asynchronous callback handler
   * that is used to retrieve the result of the network request to the API.  On
   * success the response from the API is returned.  On failure a Throwable 
   * error object will be returned.
   * 
   * @param context The current android context.
   * @param apiEndpoint The Singly API endpoint to call.
   * @param queryParams Any query parameters to send along with the request.
   * @param responseHandler An asynchronous callback handler for the request.
   * 
   * @see https://singly.com/docs/api For documentation on Singly api calls.
   * 
   * @throws IllegalStateException if no access token is found, meaning the 
   * client has not been authenticated.
   */
  public void doPostApiRequest(Context context, String apiEndpoint,
    Map<String, String> queryParams,
    final AsyncApiResponseHandler responseHandler) {

    // fail if no access token
    String accessToken = SinglyUtils.getAccessToken(context);
    if (accessToken == null) {
      throw new IllegalStateException("No access token found");
    }

    // get the http client and add api url
    AsyncHttpClient client = new AsyncHttpClient();
    Map<String, String> params = new LinkedHashMap<String, String>();
    params.put("access_token", accessToken);
    if (queryParams != null) {
      params.putAll(queryParams);
    }
    String postApiCallUrl = SinglyUtils.createSinglyURL(apiEndpoint);

    // do an async post request
    client.post(postApiCallUrl, new RequestParams(params),
      new AsyncHttpResponseHandler() {

        @Override
        public void onSuccess(String response) {
          responseHandler.onSuccess(response);
        }

        @Override
        public void onFailure(Throwable error) {
          responseHandler.onFailure(error);
        }
      });
  }

  /**
   * Performs a POST request to the Singly API that allows specifying the body
   * content of the request.  This is used when you need to POST raw content,
   * such as text or images, to the API.
   * 
   * Any query parameters passed are appended to the URL versus being passed
   * in the body of the POST request.
   * 
   * All network communication is performed in a separate thread.  As such it 
   * is fine to call this method from the main UI thread.  
   * 
   * The AsyncApiResponseHandler parameter is an asynchronous callback handler
   * that is used to retrieve the result of the network request to the API.  On
   * success the response from the API is returned.  On failure a Throwable 
   * error object will be returned.
   * 
   * @param context The current android context.
   * @param apiEndpoint The Singly API endpoint to call.
   * @param queryParams Any query parameters to send along with the request.
   * @param body The content to use as the body of the request.
   * @param contentType The MIME content type being sent.
   * @param responseHandler An asynchronous callback handler for the request.
   * 
   * @see https://singly.com/docs/api For documentation on Singly api calls.
   * 
   * @throws IllegalStateException if no access token is found, meaning the 
   * client has not been authenticated.
   */
  public void doBodyApiRequest(Context context, String apiEndpoint,
    Map<String, String> queryParams, byte[] body, String contentType,
    final AsyncApiResponseHandler responseHandler) {

    // fail if no access token
    String accessToken = SinglyUtils.getAccessToken(context);
    if (accessToken == null) {
      throw new IllegalStateException("No access token found");
    }

    // get the http client and add api url
    AsyncHttpClient client = new AsyncHttpClient();
    Map<String, String> params = new LinkedHashMap<String, String>();
    params.put("access_token", accessToken);
    if (queryParams != null) {
      params.putAll(queryParams);
    }
    String postApiCallUrl = SinglyUtils.createSinglyURL(apiEndpoint, params);

    // do an async post request with the raw body content
    client.post(context, postApiCallUrl, new ByteArrayEntity(body),
      contentType, new AsyncHttpResponseHandler() {

        @Override
        public void onSuccess(String response) {
          responseHandler.onSuccess(response);
        }

        @Override
        public void onFailure(Throwable error) {
          responseHandler.onFailure(error);
        }
      });
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public Class getAuthenticationActivity() {
    return authenticationActivity;
  }

  public void setAuthenticationActivity(Class authenticationActivity) {
    this.authenticationActivity = authenticationActivity;
  }

}