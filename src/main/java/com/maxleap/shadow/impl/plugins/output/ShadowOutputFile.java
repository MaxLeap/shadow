package com.maxleap.shadow.impl.plugins.output;

import com.maxleap.shadow.ShadowConfig;
import com.maxleap.shadow.ShadowOutput;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */
public class ShadowOutputFile<IN> extends ShadowOutputAbs<IN, String> implements ShadowOutput<IN> {

  private AsyncFile asyncFile;
  private OpenOptions openOptions = new OpenOptions()
    .setCreate(true)
    .setWrite(true)
    .setRead(true);

  private static final Logger logger = LoggerFactory.getLogger(ShadowOutputFile.class);

  @Override
  public CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String filePath = rootConfig.getString("path", "/tmp/shadow.log");
    vertx.fileSystem().open(filePath, openOptions, event -> {
      if (event.succeeded()) {
        asyncFile = event.result();
        asyncFile.setWritePos(0);
        asyncFile.exceptionHandler(ex -> logger.error(ex.getMessage(), ex));
        future.complete(null);
      } else {
        future.completeExceptionally(event.cause());
      }
    });
    return future;
  }

  @Override
  public void execute(IN content) {
    asyncFile.write(Buffer.buffer(defaultContent(content)));
  }

  @Override
  public CompletableFuture<Void> stop() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    asyncFile.close(event -> {
      if (event.succeeded()) {
        future.complete(null);
      } else {
        future.completeExceptionally(event.cause());
      }
    });
    return future;
  }
}
