package com.maxleap.shadow.impl.plugins.output;

import com.maxleap.shadow.ShadowConfig;
import com.maxleap.shadow.ShadowOutput;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */
public class ShadowOutputHttp<DE_IN> extends ShadowOutputAbs<DE_IN, String> implements ShadowOutput<DE_IN> {

  private HttpClient httpClient;
  private String uri;

  private static final Logger logger = LoggerFactory.getLogger(ShadowOutput.class);

  @Override
  public CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig) {
    HttpClientOptions httpClientOptions = new HttpClientOptions()
      .setDefaultHost(rootConfig.getString("host", "localhost"))
      .setDefaultPort(rootConfig.getInteger("port", 8081));
    uri = rootConfig.getString("uri", "/");
    httpClient = vertx.createHttpClient(httpClientOptions);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void execute(DE_IN content) {
    httpClient
      .post(uri)
      .handler(response -> {
        if (response.statusCode() != 200) {
          logger.warn(String.format("send http message failed, http code %s, http message %s", response.statusCode(), response.statusMessage()));
        }
      })
      .exceptionHandler(ex -> logger.error(ex.getMessage(), ex))
      .end(decodec.orElse(defaultDecodec).translate(content));
  }

  @Override
  public CompletableFuture<Void> stop() {
    httpClient.close();
    return CompletableFuture.completedFuture(null);
  }
}
