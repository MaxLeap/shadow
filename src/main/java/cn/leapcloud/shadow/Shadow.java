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
  private static final String DSL_FILE_PATH = "./conf/Shadow.java";
  private AbsShadowDSL shadowDSL;
  private Vertx vertx;

  public Shadow(Vertx vertx) {
    this.vertx = vertx;
  }

  public Shadow() {
    this.vertx = Vertx.vertx();
  }

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
    vertx.exceptionHandler(throwable -> logger.error("internal exception.", throwable));
    if (vertx.fileSystem().existsBlocking(DSL_FILE_PATH)) {
      CompilingClassLoader compilingLoader = new CompilingClassLoader(this.getClass().getClassLoader(), DSL_FILE_PATH);
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
    List<Future> futureList = shadowDSL.getInputs().stream().map(input -> input.start(vertx)).collect(Collectors.toList());
    futureList.addAll(shadowDSL.getOutputs().stream().map(output -> output.start(vertx)).collect(Collectors.toList()));
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
    List<Future> futureList = shadowDSL.getInputs().stream().map((Function<ShadowInput, Future>) ShadowInput::stop).collect(Collectors.toList());
    futureList.addAll(shadowDSL.getOutputs().stream().map((Function<ShadowOutput, Future>) ShadowOutput::stop).collect(Collectors.toList()));
    CompositeFuture.all(futureList).setHandler(asyncResult -> {
      if (asyncResult.failed()) logger.error("shutdown exception.", asyncResult.cause());
    });
  }

}
