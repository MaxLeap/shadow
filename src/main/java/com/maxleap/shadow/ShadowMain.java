package com.maxleap.shadow;

import com.maxleap.shadow.impl.engine.nashorn.NashornParserEngine;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import sun.misc.Signal;

/**
 * Created by stream.
 */
public class ShadowMain {

  private static final Logger logger = LoggerFactory.getLogger(ShadowMain.class);

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    if (vertx.fileSystem().existsBlocking("conf/zookeeper.json")) {
      JsonObject zkConfig = vertx.fileSystem().readFileBlocking("conf/zookeeper.json").toJsonObject();
      ClusterManager zkClusterManager = new ZookeeperClusterManager(zkConfig);

      VertxOptions vertxOptions = new VertxOptions();
      vertxOptions.setClustered(true);
      vertxOptions.setClusterManager(zkClusterManager);

      Vertx.clusteredVertx(vertxOptions, event -> {
        if (event.succeeded()) {
          Vertx clusterVertx = event.result();
          startUpShadow(clusterVertx);
        } else {
          logger.error("start cluster shadow failed.", event.cause());
        }
      });
    } else {
      startUpShadow(vertx);
    }
  }

  private static void startUpShadow(Vertx vertx) {
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
              logger.info("start shadow success for debug file collection.....");
              listenSignal(parserEngine);
            }
          });
      } else {
        logger.error("can not found config.json", fileEvent.cause());
      }
    });
  }

  private static void listenSignal(ParserEngine parserEngine) {
    Signal.handle(new Signal("USR2"), signal -> parserEngine.reloadPluginFn()
      .whenComplete((aVoid, throwable) -> {
        if (throwable != null) {
          logger.error("reload failed.", throwable);
        } else {
          logger.info("reload success.....");
        }
      }));
  }

}
