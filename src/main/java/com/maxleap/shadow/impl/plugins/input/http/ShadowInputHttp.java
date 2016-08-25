package com.maxleap.shadow.impl.plugins.input.http;


import com.maxleap.shadow.ShadowCodec;
import com.maxleap.shadow.ShadowConfig;
import com.maxleap.shadow.ShadowException;
import com.maxleap.shadow.ShadowInput;
import com.maxleap.shadow.impl.FnInvoker;
import com.maxleap.shadow.impl.plugins.input.ShadowInputAbs;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Created by stream.
 */
public class ShadowInputHttp extends ShadowInputAbs<Buffer, JsonObject, JsonObject> implements ShadowInput {
  private HttpServer httpServer;
  private boolean matchAll;
  private Map<String, HttpRequestHandler> allPaths = new LinkedHashMap<>();
  private ShadowCodec<Buffer, JsonObject> messageDecodec;

  private static final Logger logger = LoggerFactory.getLogger(ShadowInputHttp.class);

  @Override
  public CompletableFuture<Void> init(Vertx vertx, ShadowConfig rootConfig) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    String host = rootConfig.getString("host", "localhost");
    int port = rootConfig.getInteger("port", 8082);
    matchAll = rootConfig.getBoolean("matchAll", false);

    try {
      messageDecodec = decodec.orElseThrow(() -> new ShadowException("decodec can not be null."));
      future.complete(null);
    } catch (ShadowException ex) {
      future.completeExceptionally(ex);
      return future;
    }

    rootConfig.getJsonArray("paths").forEach(jsConfig -> {
      ShadowConfig httpJSConfig = new ShadowConfig((JsonObject) jsConfig, rootConfig);
      ScriptObjectMirror matchFn = httpJSConfig.getFn("match");
      String pattern = httpJSConfig.getString("path");

      HttpRequestHandler httpRequestHandler = new HttpRequestHandler(matchFn);
      allPaths.put(pattern, httpRequestHandler);
    });
    HttpServerOptions httpServerOptions = new HttpServerOptions().setHost(host).setPort(port);
    httpServer = vertx.createHttpServer(httpServerOptions);
    future.complete(null);
    return future;
  }

  @Override
  public void reloadFn(ShadowConfig rootConfig) {
    rootConfig.getJsonArray("paths").forEach(jsConfig -> {
      ShadowConfig httpJSConfig = new ShadowConfig((JsonObject) jsConfig, rootConfig);
      ScriptObjectMirror matchFn = httpJSConfig.getFn("match");
      String pattern = httpJSConfig.getString("path");

      HttpRequestHandler httpRequestHandler = allPaths.get(pattern);
      if (httpRequestHandler != null) httpRequestHandler.matchFn = matchFn;
    });
  }

  @Override
  public CompletableFuture<Void> start() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    httpServer.requestHandler(httpRequest -> {
      httpRequest.exceptionHandler(event -> logger.error(event.getMessage(), event));
      String uri = httpRequest.uri();
      httpRequest.bodyHandler(buffer -> {
        boolean matched = false;
        if (uri.endsWith("health") && httpRequest.method() == HttpMethod.GET) {
          matched = true;
          httpRequest.response().setStatusCode(200).end();
        } else if (HttpMethod.POST == httpRequest.method()) {
          for (String pattern : allPaths.keySet()) {
            if (Pattern.matches(pattern, uri)) {
              matched = true;
              allPaths.get(pattern).handle(uri, buffer, httpRequest.response());
              if (!matchAll) break;
            }
          }
        } else {
          matched = false;
        }
        if (!matched) {
          logger.warn("unKnow the uri:" + uri);
          httpRequest.response().setStatusCode(404).setStatusMessage("unKnow uri.").end();
        }
      });
    }).listen(event -> {
      if (event.succeeded()) {
        future.complete(null);
      } else {
        future.completeExceptionally(event.cause());
      }
    });
    return future;
  }

  @Override
  public CompletableFuture<Void> stop() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    httpServer.close(event -> {
      if (event.succeeded()) {
        future.complete(null);
      } else {
        future.completeExceptionally(event.cause());
      }
    });
    return future;
  }

  private class HttpRequestHandler implements FnInvoker {
    private ScriptObjectMirror matchFn;

    HttpRequestHandler(ScriptObjectMirror matchFn) {
      this.matchFn = matchFn;
    }

    @SuppressWarnings("unchecked")
    void handle(String requestPath, Buffer requestBuffer, HttpServerResponse response) {
      try {
        JsonObject deResult = messageDecodec.translate(requestBuffer);
        JsonObject fnResult = invokeFnJsonObject(matchFn, requestPath, deResult);
        shadowOutput.execute(fnResult);
      } catch (Exception e) {
        response.setStatusMessage(e.getMessage());
        response.setStatusCode(500);
        logger.error(e.getMessage(), e);
      }
    }
  }


}
