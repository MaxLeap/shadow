package com.maxleap.shadow;

import io.vertx.core.Vertx;

import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */
public interface ShadowInput {

  CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig);

  void reloadFn(ShadowConfig rootConfig);

  CompletableFuture<Void> start();

  CompletableFuture<Void> stop();

}
