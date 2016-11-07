package cn.leapcloud.shadow;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by stream.
 */
@RunWith(VertxUnitRunner.class)
public class MyShadowDSLTest {

  @Test
  public void runMyShadowDSL(TestContext context) {
    Async async = context.async();
    Vertx vertx = Vertx.vertx();

    //mock output server
    vertx.createHttpServer().requestHandler(request ->
      request.exceptionHandler(context::fail).bodyHandler(bodyBuffer -> {
        JsonObject body = bodyBuffer.toJsonObject();
        context.assertEquals("bar", body.getString("foo"));
        request.response().end();
      })).listen(8081, "127.0.0.1");

    //
    Shadow shadow = new Shadow();
    shadow.startShadowDSL().setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        vertx.createHttpClient()
          .post(8082, "127.0.0.1", "/log1")
          .putHeader("Content-Type", "application/json")
          .handler(response -> {
            context.assertEquals(200, response.statusCode());
            async.complete();
          })
          .end(new JsonObject().put("foo", "bar").encode());
      } else {
        context.fail(asyncResult.cause());
      }
    });
  }
}
