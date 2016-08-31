package com.maxleap.shadow.plugins;

import com.maxleap.shadow.ParserEngine;
import com.maxleap.shadow.ShadowInput;
import com.maxleap.shadow.impl.engine.nashorn.NashornParserEngine;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */

@RunWith(VertxUnitRunner.class)
public class DirManualTest {

  private static String resourceDir = Thread.currentThread().getContextClassLoader().getResource(".").getFile();

  @Ignore
  @Test
  public void fileOutput(TestContext context) {
    Async async = context.async();

    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setBlockedThreadCheckInterval(10000000);
    Vertx vertx = Vertx.vertx(vertxOptions);
    ParserEngine parserEngine = new NashornParserEngine(vertx);
    CompletableFuture<Void> parserFuture = parserEngine.init(resourceDir + "/js");

    parserFuture
      .thenCompose(aVoid -> {
        ShadowInput shadowInput = parserEngine.getShadowInput("myDir");
        return shadowInput.start();
      })
      .whenComplete((a, throwable) -> {
        if (throwable != null) {
          context.fail(throwable);
        } else {
          System.out.println("working.....");
        }
      });
  }

}
