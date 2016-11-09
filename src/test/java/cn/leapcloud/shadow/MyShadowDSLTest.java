package cn.leapcloud.shadow;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
  public void testEx() {
    String content = "2016-11-09 14:27:47:531 - INFO - com.maxleap.log4j2.LoggerTest.log4j2(LoggerTest.java:19) - main - info";
    //DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.ofEpochMilli(event.getTimeMillis()).atZone(ZoneId.of("GMT")))
    //DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.of)
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SS");
    LocalDateTime dateTime = LocalDateTime.parse(content.split(" - ", 5)[0], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS"));
    System.out.println(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime.atZone(ZoneId.of("GMT"))));
    //System.out.println(content.split(" - ", 5)[0]);

  }

}
