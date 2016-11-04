package cn.leapcloud.shadow;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by stream.
 */
@RunWith(VertxUnitRunner.class)
public class MyShadowDSLTest {

  @Test
  public void runMyShadowDSL(TestContext context) {
    Async async = context.async();
    Vertx vertx = Vertx.vertx();
    JsonObject config = vertx.fileSystem().readFileBlocking("shadow.json").toJsonObject();
    AbsShadowDSL shadowDSL = new MyShadowDSL();
    shadowDSL.start(config);

    List<Future> futureList = shadowDSL.getInputs().values().stream().map(input -> input.start(vertx)).collect(Collectors.toList());
    futureList.addAll(shadowDSL.getOutputs().values().stream().map(output -> output.start(vertx)).collect(Collectors.toList()));

    CompositeFuture.all(futureList).setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        System.out.println("shadow start up.");
      } else {
        context.fail(asyncResult.cause());
      }
    });

  }
}
