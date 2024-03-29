package org.gbif.collections.duplicates.issues;

import java.io.IOException;

import okhttp3.*;

/** Interceptor for the {@link OkHttpClient} to add basic auth in all the requests. */
public class BasicAuthInterceptor implements Interceptor {

  private final String credentials;

  public BasicAuthInterceptor(String user, String password) {
    this.credentials = Credentials.basic(user, password);
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    Request authenticatedRequest =
        request
            .newBuilder()
            .header("Authorization", credentials)
            .cacheControl(new CacheControl.Builder().noCache().build())
            .build();
    return chain.proceed(authenticatedRequest);
  }
}
