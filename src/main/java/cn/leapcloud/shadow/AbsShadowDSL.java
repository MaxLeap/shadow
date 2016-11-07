package cn.leapcloud.shadow;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by stream.
 */
public abstract class AbsShadowDSL implements ShadowDSL {

  private Map<String, ShadowOutput> outputs = new HashMap<>();
  private Map<String, ShadowInput> inputs = new HashMap<>();

  protected <E> Consumer<E> getOutput(String name) {
    return (Consumer<E>) outputs.get(name);
  }

  Map<String, ShadowOutput> getOutputs() {
    return outputs;
  }

  Map<String, ShadowInput> getInputs() {
    return inputs;
  }

  protected abstract void start(JsonObject config);

  protected Future<Void> stop() {
    List<Future> futureList = getInputs().values().stream().map((Function<ShadowInput, Future>) ShadowInput::stop).collect(Collectors.toList());
    futureList.addAll(getOutputs().values().stream().map((Function<ShadowOutput, Future>) ShadowOutput::stop).collect(Collectors.toList()));

    Future<Void> future = Future.future();
    CompositeFuture.all(futureList).setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        future.complete();
      } else {
        future.fail(asyncResult.cause());
      }
    });
    return future;
  }

  @Override
  public <IN, OUT, R> ShadowDSL addShadowInput(String name, ShadowInput<IN, OUT, R> input) {
    inputs.put(name, input);
    return this;
  }

  @Override
  public <IN, OUT, T> ShadowDSL addShadowOutput(String name, ShadowOutput<IN, OUT, T> output) {
    outputs.put(name, output);
    return this;
  }
}
