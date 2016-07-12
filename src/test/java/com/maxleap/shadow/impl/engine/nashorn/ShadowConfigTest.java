package com.maxleap.shadow.impl.engine.nashorn;


import com.maxleap.shadow.ShadowConfig;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */
@RunWith(VertxUnitRunner.class)
public class ShadowConfigTest {
  private static String resourceDir = Thread.currentThread().getContextClassLoader().getResource(".").getFile();

  @Test
  public void shadowConfigParser(TestContext context) {
    Async async = context.async();
    VertxOptions vertxOptions = new VertxOptions();
    Vertx vertx = Vertx.vertx(vertxOptions);
    NashornParserEngine parserEngine = new NashornParserEngine(vertx);
    CompletableFuture<Void> parserFuture = parserEngine.init(resourceDir + "/js/mock");

    parserFuture.whenComplete((aVoid, ex) -> {
      ScriptObjectMirror shadowConfig = (ScriptObjectMirror) parserEngine.getEngine().get("shadowConfigMock");
      ShadowConfig config = new ShadowConfig((Map<String, Object>) shadowConfig.get("config"));
      ShadowConfig realConfig = new ShadowConfig(config.getJsonArray("paths").getJsonObject(0), config);
      context.assertNotNull(realConfig.getFn("match"));
      async.complete();
    });

  }
}
