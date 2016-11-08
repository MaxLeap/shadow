package cn.leapcloud.shadow;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by stream.
 */
@RunWith(VertxUnitRunner.class)
public class MyShadowDSLTest {

  @Rule
  public RunTestOnContext rule = new RunTestOnContext();

  @Test
  public void runMyShadowDSL(TestContext context) {
    Async async = context.async();
    Vertx vertx = rule.vertx();
    Shadow shadow = new Shadow(vertx);
    //mock output server
    vertx.createHttpServer().requestHandler(request ->
      request.exceptionHandler(context::fail).bodyHandler(bodyBuffer -> {
        JsonObject body = bodyBuffer.toJsonObject();
        context.assertEquals("bar", body.getString("foo"));
        async.complete();
        request.response().end();
      }))
      .listen(8081, "127.0.0.1", event -> {
        //
        shadow.startShadowDSL().setHandler(asyncResult -> {
          if (asyncResult.succeeded()) {
            vertx.createHttpClient()
              .post(8082, "127.0.0.1", "/log1")
              .putHeader("Content-Type", "application/json")
              .handler(response -> context.assertEquals(200, response.statusCode()))
              .end(new JsonObject().put("foo", "bar").encode());
          } else {
            context.fail(asyncResult.cause());
          }
        });
      });
  }

  @Test
  public void testHandler(TestContext context) {
    Async async = context.async();
    Future<String> future = Future.future();
    //Optional<Future<String>> opt = Optional.of(future.map(a -> "b"));
    future.map(a -> "b");
    future.setHandler(a -> {
      System.out.println(a.result());
    });

    future.map(a -> "c").setHandler(a -> {
      System.out.println(a.result());
    });

    future.complete("a");
  }

}
