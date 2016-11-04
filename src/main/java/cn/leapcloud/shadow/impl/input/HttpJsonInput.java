package cn.leapcloud.shadow.impl.input;

import cn.leapcloud.shadow.ShadowInput;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Created by stream.
 */
public class HttpJsonInput
  extends AbsShadowInput<HttpServerRequest, Future<JsonObject>, Future<JsonObject>>
  implements ShadowInput<HttpServerRequest, Future<JsonObject>, Future<JsonObject>> {

  private static final Logger logger = LoggerFactory.getLogger(HttpJsonInput.class);
  private HttpServer httpServer;

  @Override
  public Future<Void> start(Vertx vertx) {
    Future<Void> future = Future.future();
    String host = config.getString("host", "localhost");
    int port = config.getInteger("port", 8082);

    HttpServerOptions httpServerOptions = new HttpServerOptions().setHost(host).setPort(port);
    httpServer = vertx.createHttpServer(httpServerOptions).requestHandler(request -> {
      accept(request);
      request.response().end();
    });
    httpServer.listen(port, host, event -> {
      if (event.succeeded()) {
        logger.info("http json input server start up.");
        future.complete();
      }
      else future.fail(event.cause());
    });
    return future;
  }

  @Override
  public Future<Void> stop() {
    Future<Void> future = Future.future();
    httpServer.close(future.completer());
    return future;
  }
}
