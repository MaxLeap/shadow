package cn.leapcloud.shadow.impl.output;

import cn.leapcloud.shadow.ShadowOutput;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Optional;

/**
 * Created by stream.
 */
public class ConsoleFutureOutput
  extends AbsShadowOutput<Future<Object>, Future<Object>, String>
  implements ShadowOutput<Future<Object>, Future<Object>, String> {

  private static final Logger logger = LoggerFactory.getLogger(ConsoleFutureOutput.class);

  @Override
  public Future<Void> start(Vertx vertx) {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> stop() {
    return Future.succeededFuture();
  }

  @Override
  public void accept(Optional<String> token, Future<Object> objectFuture) {
    objectFuture.setHandler((asyncResult) -> {
      if (asyncResult.succeeded()) {
        System.out.println(asyncResult.result().toString());
      } else {
        logger.error("console output failed.", asyncResult.cause());
      }
    });
  }
}
