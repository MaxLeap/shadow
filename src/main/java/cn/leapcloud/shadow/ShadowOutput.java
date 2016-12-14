package cn.leapcloud.shadow;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by stream.
 */
public interface ShadowOutput<IN, OUT, R, T> extends Consumer<IN>, BiConsumer<Optional<T>, R> {

  Future<Void> start(Vertx vertx);

  Future<Void> stop();

  /**
   * add config content to input
   *
   * @param config JsonObject format
   * @return this
   */
  ShadowOutput<IN, OUT, R, T> config(JsonObject config);

  /**
   * Extract token from message and config
   *
   * @param function function
   *                 JsonObject is config
   * @return this
   */
  ShadowOutput<IN, OUT, R, T> tokenFunction(BiFunction<IN, JsonObject, Optional<T>> function);

  /**
   * handler message which from input
   *
   * @param handler handler
   * @return this
   */
  ShadowOutput<IN, OUT, R, T> handler(BiFunction<IN, JsonObject, OUT> handler);

  /**
   * add encode function, the source from handler function.
   *
   * @param encode encode
   * @return this
   */
  ShadowOutput<IN, OUT, R, T> encode(Function<OUT, R> encode);


}
