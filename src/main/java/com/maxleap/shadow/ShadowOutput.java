package com.maxleap.shadow;

import io.vertx.core.Vertx;

import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */
public interface ShadowOutput<DE_IN> {

  CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig);

  void execute(DE_IN content);

  CompletableFuture<Void> stop();

}
