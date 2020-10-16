package org.folio.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import org.folio.rest.tools.client.Response;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface HttpClient {

  /**
   * This method makes http call with request without body
   *
   * @param method http method
   * @param endpoint endpoint
   * @param okapiHeaders headers
   * @return CompletableFuture with response
   */
  CompletableFuture<Response> request(HttpMethod method, String endpoint, Map<String, String> okapiHeaders);

  /**
   * This method makes http call with request without body
   *
   * @param method http method
   * @param body requests body
   * @param endpoint endpoint
   * @param okapiHeaders headers
   * @return CompletableFuture with response
   * @throws Exception
   */
  CompletableFuture<Response> request(HttpMethod method, Buffer body, String endpoint, Map<String, String> okapiHeaders);
}
