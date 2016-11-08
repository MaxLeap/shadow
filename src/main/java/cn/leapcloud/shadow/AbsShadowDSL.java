package cn.leapcloud.shadow;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by stream.
 */
public abstract class AbsShadowDSL implements ShadowDSL {

  private Map<String, ShadowOutput> outputs = new HashMap<>();
  private Map<String, ShadowInput> inputs = new HashMap<>();

  Map<String, ShadowOutput> getOutputs() {
    return outputs;
  }

  Map<String, ShadowInput> getInputs() {
    return inputs;
  }

  protected abstract void start();

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
