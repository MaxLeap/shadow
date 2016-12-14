package cn.leapcloud.shadow.impl.output;

import cn.leapcloud.shadow.ShadowOutput;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * Created by stream.
 */
public abstract class AbsShadowOutput<IN, OUT, R, T> implements ShadowOutput<IN, OUT, R, T> {

  protected JsonObject config;
  private BiFunction<IN, JsonObject, Optional<T>> tokenFunction = (in, config) -> Optional.empty();
  private BiFunction<IN, JsonObject, OUT> handler = (in, config) -> (OUT) in;
  private Function<OUT, R> encode = (in) -> (R) in;

  @Override
  public ShadowOutput<IN, OUT, R, T> encode(Function<OUT, R> encode) {
    this.encode = encode;
    return this;
  }

  @Override
  public ShadowOutput<IN, OUT, R, T> config(JsonObject config) {
    this.config = config;
    return this;
  }

  @Override
  public ShadowOutput<IN, OUT, R, T> tokenFunction(BiFunction<IN, JsonObject, Optional<T>> function) {
    this.tokenFunction = function;
    return this;
  }

  @Override
  public ShadowOutput<IN, OUT, R, T> handler(BiFunction<IN, JsonObject, OUT> handler) {
    this.handler = handler;
    return this;
  }

  @Override
  public void accept(IN in) {
    Optional<T> token = tokenFunction.apply(in, config);
    OUT out = handler.apply(in, config);
    R result = encode.apply(out);
    accept(token, result);
  }
}
