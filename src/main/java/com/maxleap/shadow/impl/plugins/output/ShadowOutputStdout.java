package com.maxleap.shadow.impl.plugins.output;

import com.maxleap.shadow.ShadowConfig;
import com.maxleap.shadow.ShadowOutput;
import io.vertx.core.Vertx;

import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */
public class ShadowOutputStdout<IN, OUT> extends ShadowOutputAbs<IN, OUT> implements ShadowOutput<IN> {

  @Override
  public CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void execute(IN content) {
    System.out.println(defaultContent(content));
  }

  @Override
  public CompletableFuture<Void> stop() {
    return CompletableFuture.completedFuture(null);
  }


}
