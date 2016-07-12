package com.maxleap.shadow.impl.plugins.input.forward;

import com.maxleap.shadow.ShadowCodec;
import com.maxleap.shadow.ShadowConfig;
import com.maxleap.shadow.ShadowException;
import com.maxleap.shadow.ShadowInput;
import com.maxleap.shadow.impl.plugins.input.ShadowInputAbs;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Created by stream.
 */
public class ShadowInputForward<DE_IN, DE_OUT, EN_IN, EN_OUT> extends ShadowInputAbs<DE_IN, DE_OUT, EN_IN, EN_OUT> implements ShadowInput {

  private boolean matchAll;
  private MessageConsumer<DE_IN> messageConsumer;
  private Map<String, ForwardRequestHandler> allTagPatterns = new LinkedHashMap<>();
  private ShadowCodec<DE_IN, DE_OUT> messageDecodec;
  private ShadowCodec<EN_IN, EN_OUT> messageEncodec;

  private static final Logger logger = LoggerFactory.getLogger(ShadowInputForward.class);

  @Override
  public CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    String address = rootConfig.getString("address");
    matchAll = rootConfig.getBoolean("matchAll", false);
    messageConsumer = vertx.eventBus().consumer(address);
    try {
      messageDecodec = decodec.orElseThrow(() -> new ShadowException("decodec can not be null."));
      messageEncodec = encodec.orElseThrow(() -> new ShadowException("encodec can not be null."));
      JsonArray tags = rootConfig.getJsonArray("tags");
      if (tags == null) throw new IllegalArgumentException("tags can not be null.");
      tags.forEach(jsConfig -> {
        ShadowConfig forwardJSConfig = new ShadowConfig((JsonObject) jsConfig, rootConfig);
        String tag = forwardJSConfig.getString("tag");
        ScriptObjectMirror matchFn = forwardJSConfig.getFn("match");

        //get output plugin
        ForwardRequestHandler forwardRequestHandler = new ForwardRequestHandler(matchFn);
        allTagPatterns.put(tag, forwardRequestHandler);
      });
      future.complete(null);
    } catch (ShadowException ex) {
      future.completeExceptionally(ex);
    }
    return future;
  }

  @Override
  public void reloadFn(ShadowConfig rootConfig) {
    rootConfig.getJsonArray("tags").forEach(jsConfig -> {
      ShadowConfig forwardJSConfig = new ShadowConfig((JsonObject) jsConfig, rootConfig);
      String tag = forwardJSConfig.getString("tag");
      ScriptObjectMirror matchFn = forwardJSConfig.getFn("match");
      ForwardRequestHandler forwardRequestHandler = allTagPatterns.get(tag);
      if (forwardRequestHandler != null) forwardRequestHandler.matchFn = matchFn;
    });
  }

  @Override
  public CompletableFuture<Void> start() {
    messageConsumer.exceptionHandler(ex -> logger.error(ex.getMessage(), ex));
    messageConsumer.handler(event -> {
      Optional<String> tagOptional = Optional.ofNullable(event.headers().get("tag"));
      tagOptional.ifPresent(tag -> {
        for (Map.Entry<String, ForwardRequestHandler> tagPatternEntry : allTagPatterns.entrySet()) {
          if (Pattern.matches(tagPatternEntry.getKey(), tag)) {
            tagPatternEntry.getValue().handle(event, tag);
            if (!matchAll) break;
          }
        }
      });
    });
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> stop() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    messageConsumer.unregister(event -> {
      if (event.succeeded()) {
        future.complete(null);
      } else {
        future.completeExceptionally(event.cause());
      }
    });
    return future;
  }

  private class ForwardRequestHandler {
    private ScriptObjectMirror matchFn;

    ForwardRequestHandler(ScriptObjectMirror matchFn) {
      this.matchFn = matchFn;
    }

    void handle(Message<DE_IN> message, String tag) {
      try {
        DE_OUT deResult = messageDecodec.translate(message.body());
        EN_IN fnResult = (EN_IN) matchFn.call(matchFn, tag, deResult);
        EN_OUT enResult = messageEncodec.translate(fnResult);
        shadowOutput.execute(enResult);
      } catch (Exception e) {
        logger.error(e.getMessage(), e);
      }
    }
  }
}
