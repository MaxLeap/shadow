package com.maxleap.shadow.impl.plugins.output;

import com.maxleap.shadow.ShadowConfig;
import com.maxleap.shadow.ShadowOutput;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by stream.
 */
public class ShadowOutputHttp<DE_IN> extends ShadowOutputAbs<DE_IN, String> implements ShadowOutput<DE_IN> {

  private List<HttpClient> httpClients = new ArrayList<>();
  private String defaultURI;
  private Optional<ScriptObjectMirror> dynamicURI;
  private int currentIndex = 0;

  private static final Logger logger = LoggerFactory.getLogger(ShadowOutput.class);

  @Override
  public CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig) {
    defaultURI = rootConfig.getString("defaultURI", "/");
    dynamicURI = Optional.ofNullable(rootConfig.getFn("dynamicURI"));
    //TODO check jsonArray length
    httpClients = rootConfig.getJsonArray("hosts").stream()
      .map(s -> (String) s)
      .map(s -> {
        String[] hostAndPort = s.split(":");
        HttpClientOptions httpClientOptions = new HttpClientOptions()
          .setDefaultHost(hostAndPort[0])
          .setDefaultPort(Integer.valueOf(hostAndPort[1]));
        logger.info("host " + hostAndPort[0] + " port " + hostAndPort[1]);
        return vertx.createHttpClient(httpClientOptions);
      })
      .collect(Collectors.toList());
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void execute(DE_IN content) {
    //Round robin
    HttpClient httpClient = httpClients.get(currentIndex >= httpClients.size() ? 0 : currentIndex);
    currentIndex++;
    //get uri
    String uri = dynamicURI.map(fn -> fn.call(fn, content).toString()).orElse(defaultURI);
    httpClient
      .post(uri)
      .handler(response -> {
        if (response.statusCode() < 200 || response.statusCode() > 399) {
          logger.warn(String.format("send http message failed, http code %s, http message %s, defaultURI %s",
            response.statusCode(), response.statusMessage(), uri));
        }
      })
      .exceptionHandler(ex -> logger.error(ex.getMessage(), ex))
      .end(decodec.orElse(defaultDecodec).translate(content));
  }

  @Override
  public CompletableFuture<Void> stop() {
    httpClients.forEach(HttpClient::close);
    return CompletableFuture.completedFuture(null);
  }
}
