package com.maxleap.shadow;

import com.maxleap.shadow.impl.engine.nashorn.NashornParserEngine;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import sun.misc.Signal;

/**
 * Created by stream.
 */
public class ShadowMain {

  private static final Logger logger = LoggerFactory.getLogger(ShadowMain.class);

  public static void main(String[] args) {
    VertxOptions vertxOptions = new VertxOptions();
    //vertxOptions.setBlockedThreadCheckInterval(10000000);
    Vertx vertx = Vertx.vertx(vertxOptions);
    final ParserEngine parserEngine = new NashornParserEngine(vertx);

    vertx.fileSystem().readFile("conf/config.json", fileEvent -> {
      if (fileEvent.succeeded()) {
        JsonObject config = new JsonObject(new String(fileEvent.result().getBytes()));

        //"/Users/stream/codes/java/leap/shadow/src/main/resources/js"
        parserEngine.init(config.getString("scriptLocalPath"))
          .thenCompose(aVoid -> parserEngine.start())
          .whenComplete((aVoid, throwable) -> {
            if (throwable != null) {
              logger.error("shadow startup failed.", throwable);
            } else {
              logger.info("start shadow success.....");
              listenSignal(parserEngine);
            }
          });
      } else {
        logger.error("can not found config.json", fileEvent.cause());
      }
    });

//    vertx.setPeriodic(1000, event -> {
//      Buffer buffer = vertx.fileSystem().readFileBlocking("/Users/stream/codes/java/leap/my.log");
//      buffer.appendString(Instant.now().getEpochSecond() + "\n");
//      vertx.fileSystem().writeFileBlocking("/Users/stream/codes/java/leap/my.log", buffer);
//    });
  }

  private static void listenSignal(ParserEngine parserEngine) {
    Signal.handle(new Signal("USR2"), signal -> {
      parserEngine.reloadPluginFn()
        .whenComplete((aVoid, throwable) -> {
          if (throwable != null) {
            logger.error("reload failed.", throwable);
          } else {
            logger.info("reload success.....");
          }
        });
    });
  }

}
