package com.maxleap.shadow;

import io.vertx.core.Vertx;

import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */
public interface ShadowOutput<T> {

  CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig);

  void execute(T content);

  CompletableFuture<Void> stop();

}
