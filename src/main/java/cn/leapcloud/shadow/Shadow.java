package cn.leapcloud.shadow;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.verticle.CompilingClassLoader;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by stream.
 */
public class Shadow {

  private static final Logger logger = LoggerFactory.getLogger(Shadow.class);
  private AbsShadowDSL shadowDSL;

  public static void main(String[] args) {
    Shadow shadow = new Shadow();
    shadow.startShadowDSL().setHandler(asyncResult -> {
      if (asyncResult.failed()) {
        logger.error("start up shadow failed.", asyncResult.cause());
      } else {
        if (shadow.shadowDSL != null)
          Runtime.getRuntime().addShutdownHook(new Thread(shadow::shutdownShadow, "shadow-shutdown-hook"));
      }
    });
  }

  public Future<Void> startShadowDSL() {
    Future<Void> future = Future.future();
    Vertx vertx = Vertx.vertx();
    if (vertx.fileSystem().existsBlocking("Shadow.java")) {
      CompilingClassLoader compilingLoader = new CompilingClassLoader(this.getClass().getClassLoader(), "Shadow.java");
      String className = compilingLoader.resolveMainClassName();
      try {
        Class clazz = compilingLoader.loadClass(className);
        this.shadowDSL = (AbsShadowDSL) clazz.newInstance();
        future = startShadowPlugin(vertx);
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
        future.fail(e);
      }
    } else {
      future.fail("can not found Shadow.java in any path.");
    }
    return future;
  }

  private Future<Void> startShadowPlugin(Vertx vertx) {
    Future<Void> future = Future.future();
    //dsl start
    shadowDSL.start();

    //plugin start.
    List<Future> futureList = shadowDSL.getInputs().values().stream().map(input -> input.start(vertx)).collect(Collectors.toList());
    futureList.addAll(shadowDSL.getOutputs().values().stream().map(output -> output.start(vertx)).collect(Collectors.toList()));
    CompositeFuture.any(futureList).setHandler(asyncResult -> {
      if (asyncResult.failed()) {
        future.fail(asyncResult.cause());
      } else {
        future.complete();
      }
    });
    return future;
  }

  public void shutdownShadow() {
    List<Future> futureList = shadowDSL.getInputs().values().stream().map((Function<ShadowInput, Future>) ShadowInput::stop).collect(Collectors.toList());
    futureList.addAll(shadowDSL.getOutputs().values().stream().map((Function<ShadowOutput, Future>) ShadowOutput::stop).collect(Collectors.toList()));
    CompositeFuture.all(futureList).setHandler(asyncResult -> {
      if (asyncResult.failed()) logger.error("shutdown exception.", asyncResult.cause());
    });
  }

}
