package cn.leapcloud.shadow;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by stream.
 */
public interface ShadowInput<IN, OUT, R> extends Consumer<IN> {

  /**
   * start this plugin up.
   *
   * @param vertx vertx
   * @return Future
   */
  Future<Void> start(Vertx vertx);

  /**
   * stop this plugin
   *
   * @return Future
   */
  Future<Void> stop();

  /**
   * add decode function
   *
   * @param decode decode
   * @return this
   */
  ShadowInput<IN, OUT, R> decode(Function<IN, OUT> decode);


  /**
   * add config content to input
   *
   * @param config JsonObject format
   * @return this
   */
  ShadowInput<IN, OUT, R> config(JsonObject config);

  ShadowInput<IN, OUT, R> matchFunction(BiFunction<IN, JsonObject, Boolean> matchFunction);

  ShadowInput<IN, OUT, R> handler(BiFunction<OUT, JsonObject, R> handler);

  ShadowInput<IN, OUT, R> addOutput(Consumer<R> output);


}
