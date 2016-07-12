package com.maxleap.shadow.impl.plugins.output;

import com.maxleap.shadow.ShadowConfig;
import com.maxleap.shadow.ShadowOutput;
import io.vertx.core.Vertx;

import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */
public class ShadowOutputStdout<DE_IN, DE_OUT> extends ShadowOutputAbs<DE_IN, DE_OUT> implements ShadowOutput<DE_IN> {

  @Override
  public CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void execute(DE_IN content) {
    System.out.println(defaultContent(content));
  }

  @Override
  public CompletableFuture<Void> stop() {
    return CompletableFuture.completedFuture(null);
  }


}
