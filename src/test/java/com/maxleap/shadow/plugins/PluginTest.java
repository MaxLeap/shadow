package com.maxleap.shadow.plugins;

import com.maxleap.shadow.ParserEngine;
import com.maxleap.shadow.ShadowInput;
import com.maxleap.shadow.impl.engine.nashorn.NashornParserEngine;
import com.maxleap.shadow.impl.plugins.input.dir.ShadowInputDir;
import com.maxleap.shadow.impl.plugins.input.forward.ShadowInputForward;
import com.maxleap.shadow.impl.plugins.input.http.ShadowInputHttp;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CompletableFuture;

/**
 * stream.
 */
@RunWith(VertxUnitRunner.class)
public class PluginTest {

  private ParserEngine parserEngine;
  private Vertx vertx;
  private CompletableFuture<Void> parserFuture;
  private static String workDir = System.getProperty("user.dir");
  private static String resourceDir = Thread.currentThread().getContextClassLoader().getResource(".").getFile();

  @Before
  public void before() {
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setBlockedThreadCheckInterval(10000000);
    vertx = Vertx.vertx(vertxOptions);
    parserEngine = new NashornParserEngine(vertx);
    parserFuture = parserEngine.init(resourceDir + "/js");
  }

  @Test
  public void http(TestContext context) {
    Async async = context.async();

    //target of output
    vertx.createHttpServer(new HttpServerOptions().setPort(8081)).requestHandler(request -> {
      context.assertEquals("/test/logs", request.uri());
      request.handler(content -> {
        context.assertEquals("{\"requestPath\":\"/log1\",\"logContent\":\"logContent\"}", content.toString());
        request.response().end();
        async.complete();
      });
    }).listen(listenHandler -> {
      parserFuture
          .thenCompose(aVoid -> {
            ShadowInput httpInput = parserEngine.getShadowInput(ShadowInputHttp.class);
            return httpInput.start();
          })
          .whenComplete((a, throwable) -> {
            if (throwable != null) {
              context.fail(throwable);
            } else {
              vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8082))
                  .post("/log1")
                  .handler(responseEvent -> context.assertEquals(200, responseEvent.statusCode()))
                  .exceptionHandler(context::fail)
                  .end("{\"message\":\"logContent\"}");
            }
          });
    });
  }

  @Test
  public void forward(TestContext context) {
    Async async = context.async();
    JsonObject message = new JsonObject().put("foo", "boo");

    //target shadow
    vertx.eventBus().consumer("targetShadow", (Handler<Message<JsonObject>>) event -> {
      context.assertEquals(event.headers().get("tag"), "leap.myLog");
      context.assertEquals(event.body().encode(), message.encode());
      async.complete();
    });

    parserFuture
        .thenCompose(bVoid -> {
          ShadowInput input = parserEngine.getShadowInput(ShadowInputForward.class);
          return input.start();
        })
        .whenComplete((a, throwable) -> {
          if (throwable != null) {
            context.fail(throwable);
          } else {
            DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("tag", "leap.myLog");
            vertx.eventBus().send("anotherShadow", message, deliveryOptions);
          }
        });
  }

  @Test
  public void dirAndFileOutput(TestContext context) {
    Async async = context.async();
    parserFuture
        .thenCompose(aVoid -> {
          ShadowInput shadowInput = parserEngine.getShadowInput(ShadowInputDir.class);
          return shadowInput.start();
        })
        .whenComplete((a, throwable) -> {
          if (throwable != null) {
            context.fail(throwable);
          } else {
            String logContent = "some content of log";
            //write a log to lof file.
            vertx.fileSystem().writeFile(workDir + "/my.log", Buffer.buffer(logContent + "\n"), result -> {
              if (result.succeeded()) {
                vertx.setTimer(1000, timerID -> vertx.fileSystem().readFile("/tmp/shadow.log", readResult -> {
                  if (readResult.failed()) context.fail(readResult.cause());
                  else {
                    context.assertTrue(readResult.result().length() > 0);
                    JsonObject jsonObject = new JsonObject(readResult.result().toString());
                    context.assertEquals(logContent, jsonObject.getString("log"));
                    async.complete();
                  }
                }));
              } else {
                context.fail(result.cause());
              }
            });
          }
        });
  }


}
