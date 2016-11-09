package cn.leapcloud.shadow.impl.input;

import cn.leapcloud.shadow.ShadowInput;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by stream.
 */
public abstract class AbsShadowInput<IN, OUT, R> implements ShadowInput<IN, OUT, R> {

  private Function<IN, OUT> decode = (in) -> (OUT) in;
  private BiFunction<IN, JsonObject, Boolean> matchFunction;
  private BiFunction<OUT, JsonObject, R> handler = (out, config) -> (R) out;
  private List<Consumer<R>> outputs = new ArrayList<>();

  private static final Logger logger = LoggerFactory.getLogger(AbsShadowInput.class);

  protected JsonObject config;

  @Override
  public ShadowInput<IN, OUT, R> decode(Function<IN, OUT> decode) {
    this.decode = decode;
    return this;
  }

  @Override
  public ShadowInput<IN, OUT, R> config(JsonObject config) {
    this.config = config;
    return this;
  }

  @Override
  public ShadowInput<IN, OUT, R> matchFunction(BiFunction<IN, JsonObject, Boolean> matchFunction) {
    this.matchFunction = matchFunction;
    return this;
  }

  @Override
  public ShadowInput<IN, OUT, R> handler(BiFunction<OUT, JsonObject, R> handler) {
    this.handler = handler;
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public ShadowInput<IN, OUT, R> addOutput(Consumer output) {
    outputs.add(output);
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void accept(IN data) {
    if (matchFunction.apply(data, config)) {
      R result = handler.apply(decode.apply(data), config);
      //The result could be Future, so we have to set a handler to get async result.
      if (result instanceof Future) {
        ((Future<Object>) result).setHandler(event -> {
          if (event.succeeded()) {
            outputs.forEach(output -> output.accept((R) event.result()));
          } else {
            logger.error("handle async result failed.", event.cause());
          }
        });
      } else {
        outputs.forEach(output -> output.accept(result));
      }
    }
  }


}
