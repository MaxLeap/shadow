package cn.leapcloud.shadow.impl.output;

import cn.leapcloud.shadow.ShadowOutput;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by stream.
 */
public class HttpJsonOutput
  extends AbsShadowOutput<JsonObject, String, String>
  implements ShadowOutput<JsonObject, String, String> {

  private List<HttpClient> httpClients = new ArrayList<>();
  private String defaultURI;
  private int currentIndex = 0;

  private static final Logger logger = LoggerFactory.getLogger(HttpJsonOutput.class);

  @Override
  public Future<Void> start(Vertx vertx) {
    this.defaultURI = config.getString("defaultURI", "/");

    //TODO check jsonArray length
    httpClients = config.getJsonArray("hosts").stream()
      .map(s -> (String) s)
      .map(s -> {
        String[] hostAndPort = s.split(":");
        HttpClientOptions httpClientOptions = new HttpClientOptions()
          .setDefaultHost(hostAndPort[0])
          .setDefaultPort(Integer.valueOf(hostAndPort[1]));
        logger.info("init http client json output host " + hostAndPort[0] + " port " + hostAndPort[1]);
        return vertx.createHttpClient(httpClientOptions);
      })
      .collect(Collectors.toList());

    return Future.succeededFuture();
  }

  @Override
  public void accept(Optional<String> token, String data) {
    //Round robin
    HttpClient httpClient = httpClients.get(currentIndex >= httpClients.size() ? 0 : currentIndex);
    currentIndex++;

    String uri = token.orElse(defaultURI);
    httpClient
      .post(uri)
      .handler(response -> {
        if (response.statusCode() < 200 || response.statusCode() > 399) {
          logger.warn(String.format("send http message failed, http code %s, http message %s, URI %s",
            response.statusCode(), response.statusMessage(), defaultURI));
        }
      })
      .exceptionHandler(ex -> logger.error("http out exception.", ex))
      .end(data);
  }

  @Override
  public Future<Void> stop() {
    httpClients.forEach(HttpClient::close);
    return Future.succeededFuture();
  }
}
