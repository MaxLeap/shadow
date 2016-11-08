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
public class ConsoleOutput
  extends AbsShadowOutput<Future<String>, Future<String>, String>
  implements ShadowOutput<Future<String>, Future<String>, String> {

  private static final Logger logger = LoggerFactory.getLogger(ConsoleOutput.class);

  @Override
  public Future<Void> start(Vertx vertx) {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> stop() {
    return Future.succeededFuture();
  }

  @Override
  public void accept(Optional<String> token, Future<String> future) {
    future.setHandler((asyncResult) -> {
      if (asyncResult.succeeded()) {
        System.out.println(asyncResult.result());
      } else {
        logger.error("console output failed.", asyncResult.cause());
      }
    });
  }
}
