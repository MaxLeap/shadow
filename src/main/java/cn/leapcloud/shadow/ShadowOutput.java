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
public interface ShadowOutput<IN, OUT, T> extends Consumer<IN>, BiConsumer<Optional<T>, OUT> {

  Future<Void> start(Vertx vertx);

  Future<Void> stop();

  /**
   * add config content to input
   * @param config JsonObject format
   * @return this
   */
  ShadowOutput<IN, OUT, T> config(JsonObject config);

  /**
   * add encode function
   * @param encode
   * @return
   */
  ShadowOutput<IN, OUT, T> encode(Function<IN, OUT> encode);

  /**
   * Extract token from message and config
   *
   * @param function function
   *                 JsonObject is config
   * @return this
   */
  ShadowOutput<IN, OUT, T> tokenFunction(BiFunction<IN, JsonObject, Optional<T>> function);



}
