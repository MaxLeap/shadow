package com.maxleap.shadow.impl.plugins.output;

import com.maxleap.shadow.ShadowConfig;
import com.maxleap.shadow.ShadowOutput;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;

import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */
public class ShadowOutputForward<DE_IN, DE_OUT> extends ShadowOutputAbs<DE_IN, DE_OUT> implements ShadowOutput<DE_IN> {

  private EventBus eb;
  private DeliveryOptions deliveryOptions;
  private String address;

  @Override
  public CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    address = rootConfig.getString("address");
    String tag = rootConfig.getString("tag", "shadow.default");
    deliveryOptions = new DeliveryOptions();
    deliveryOptions.addHeader("tag", tag);
    eb = vertx.eventBus();
    future.complete(null);
    return future;
  }

  @Override
  public void execute(DE_IN content) {
    eb.send(address, defaultContent(content), deliveryOptions);
  }

  @Override
  public CompletableFuture<Void> stop() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    eb.close(event -> {
      if (event.succeeded()) {
        future.complete(null);
      } else {
        future.completeExceptionally(event.cause());
      }
    });
    return future;
  }
}
