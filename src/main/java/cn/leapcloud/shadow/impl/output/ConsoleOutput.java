package cn.leapcloud.shadow.impl.output;

import cn.leapcloud.shadow.ShadowOutput;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.Optional;

/**
 * Created by stream.
 */
public class ConsoleOutput extends AbsShadowOutput<String, String, String> implements ShadowOutput<String, String, String> {

  @Override
  public Future<Void> start(Vertx vertx) {
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> stop() {
    return Future.succeededFuture();
  }

  @Override
  public void accept(Optional<String> token, String data) {
    System.out.println(data);
  }
}
