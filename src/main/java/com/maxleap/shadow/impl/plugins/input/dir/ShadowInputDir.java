package com.maxleap.shadow.impl.plugins.input.dir;

import com.maxleap.shadow.ShadowConfig;
import com.maxleap.shadow.ShadowException;
import com.maxleap.shadow.ShadowInput;
import com.maxleap.shadow.impl.codec.LineFeed.LineFeedMeta;
import com.maxleap.shadow.impl.plugins.input.ShadowInputAbs;
import io.vertx.core.TimeoutStream;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by stream.
 */

public class ShadowInputDir extends ShadowInputAbs<Buffer, List<LineFeedMeta>, Map<String, Object>, JsonObject> implements ShadowInput {

  private static final Logger logger = LoggerFactory.getLogger(ShadowInputDir.class);
  private final Map<String, InputDir> targetDirs = new HashMap<>();
  private TimeoutStream folderWatcher;
  private boolean tail;

  @Override
  public CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig) {
    this.tail = rootConfig.getBoolean("tail", true);
    folderWatcher = vertx.periodicStream(3000L);
    //
    CompletableFuture<Void> future = new CompletableFuture<>();
    rootConfig.getJsonArray("paths").forEach(jsConfig -> {
      ShadowConfig dirJSConfig = new ShadowConfig((JsonObject) jsConfig, rootConfig);
      ScriptObjectMirror matchFn = dirJSConfig.getFn("match");
      String startPath = dirJSConfig.getString("startPath");
      String pattern = dirJSConfig.getString("pattern");
      try {
        InputDir inputDir = new InputDir(vertx, startPath, pattern, matchFn,
          decodec.orElseThrow(() -> new ShadowException("decodec can not be null.")),
          encodec.orElseThrow(() -> new ShadowException("encodec can not be null.")),
          shadowOutput);
        targetDirs.put(startPath + pattern, inputDir);
      } catch (ShadowException ex) {
        future.completeExceptionally(ex);
      }
    });
    future.complete(null);
    return future;
  }

  @Override
  public void reloadFn(ShadowConfig rootConfig) {
    rootConfig.getJsonArray("paths").forEach(jsConfig -> {
      ShadowConfig dirJSConfig = new ShadowConfig((JsonObject) jsConfig, rootConfig);
      ScriptObjectMirror matchFn = dirJSConfig.getFn("match");
      String startPath = dirJSConfig.getString("startPath");
      String pattern = dirJSConfig.getString("pattern");

      InputDir inputDir = targetDirs.get(startPath + pattern);
      if (inputDir != null) {
        inputDir.setMatchFn(matchFn);
      }
    });
  }

  @Override
  public CompletableFuture<Void> start() {
    CompletableFuture<Void> finalFuture = new CompletableFuture<>();

    CompletableFuture[] futures = targetDirs.values().stream()
      .map(inputDir -> inputDir.scan(tail))
      .collect(Collectors.toList())
      .toArray(new CompletableFuture[targetDirs.values().size()]);
    return CompletableFuture.allOf(futures).whenComplete((aVoid, throwable) -> {
      if (throwable != null) {
        finalFuture.completeExceptionally(throwable);
      } else {
        finalFuture.complete(null);
        //watch folders by timer
        folderWatcher.handler(aLong -> targetDirs.values().forEach(inputDir ->
          inputDir.scan(false).whenComplete((bVoid, ex) -> {
            if (ex != null) {
              logger.error(ex.getMessage(), ex);
            }
          })));
      }
    });
  }

  @Override
  public CompletableFuture<Void> stop() {
    folderWatcher.cancel();
    CompletableFuture[] futures = targetDirs.values().stream()
      .map(InputDir::stop)
      .collect(Collectors.toList())
      .toArray(new CompletableFuture[targetDirs.values().size()]);

    return CompletableFuture.allOf(futures).whenComplete((aVoid, throwable) -> {
      if (throwable != null) {
        logger.error(throwable.getMessage(), throwable);
      }
    });
  }


}
