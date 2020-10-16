package org.folio.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.folio.rest.tools.client.HttpClientFactory;
import org.folio.rest.tools.client.Response;
import org.folio.rest.tools.client.interfaces.HttpClientInterface;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class OkapiHttpClient implements HttpClient {

  public static final String OKAPI_URL = "x-okapi-url";
  private static final String OPTIONS = "options";
  private static final String DEFAULT_HOST = "defaultHost";
  private final HttpClientInterface client;

  private OkapiHttpClient() {
    this.client = HttpClientFactory.getHttpClient(StringUtils.EMPTY, StringUtils.EMPTY);
  }

  public static OkapiHttpClient getInstance() {
    return Holder.instance;
  }

  @Override
  public CompletableFuture<Response> request(HttpMethod method, String endpoint, Map<String, String> okapiHeaders) {
    try {
      setOkapiUrl(client, okapiHeaders);
      return client.request(method, endpoint, okapiHeaders);
    } catch(Exception e) {
      CompletableFuture<Response> future = new CompletableFuture<>();
      future.completeExceptionally(e);
      return future;
    }
  }

  @Override
  public CompletableFuture<Response> request(HttpMethod method, Buffer body, String endpoint, Map<String, String> okapiHeaders) {
    try {
      setOkapiUrl(client, okapiHeaders);
      return client.request(method, body, endpoint, okapiHeaders);
    } catch(Exception e) {
      CompletableFuture<Response> future = new CompletableFuture<>();
      future.completeExceptionally(e);
      return future;
    }
  }

  private void setOkapiUrl(HttpClientInterface client, Map<String, String> okapiHeaders) throws IllegalAccessException {
    final String okapiURL = okapiHeaders.getOrDefault(OKAPI_URL, StringUtils.EMPTY);
    FieldUtils.writeDeclaredField(FieldUtils.readDeclaredField(client, OPTIONS, true), DEFAULT_HOST, okapiURL, true);
  }

  private static class Holder {
    private static final OkapiHttpClient instance = new OkapiHttpClient();
  }
}
