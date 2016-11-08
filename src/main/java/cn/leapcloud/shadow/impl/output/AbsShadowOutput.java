package cn.leapcloud.shadow.impl.output;

import cn.leapcloud.shadow.ShadowOutput;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * Created by stream.
 */
public abstract class AbsShadowOutput<IN, OUT, T> implements ShadowOutput<IN, OUT, T> {

  protected Function<IN, OUT> encode = (in) -> (OUT) in;
  protected JsonObject config;
  protected BiFunction<IN, JsonObject, Optional<T>> tokenFunction = (in, config) -> Optional.empty();

  @Override
  public ShadowOutput<IN, OUT, T> encode(Function<IN, OUT> encode) {
    this.encode = encode;
    return this;
  }

  @Override
  public ShadowOutput<IN, OUT, T> config(JsonObject config) {
    this.config = config;
    return this;
  }

  @Override
  public ShadowOutput<IN, OUT, T> tokenFunction(BiFunction<IN, JsonObject, Optional<T>> function) {
    this.tokenFunction = function;
    return this;
  }

  @Override
  public void accept(IN in) {
    Optional<T> token = tokenFunction.apply(in, config);
    OUT out = encode.apply(in);
    accept(token, out);
  }
}
