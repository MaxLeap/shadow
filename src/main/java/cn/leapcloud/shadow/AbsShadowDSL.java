package cn.leapcloud.shadow;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by stream.
 */
public abstract class AbsShadowDSL implements ShadowDSL {

  private Set<ShadowOutput> outputs = new HashSet<>();
  private Set<ShadowInput> inputs = new HashSet<>();

  Set<ShadowOutput> getOutputs() {
    return outputs;
  }

  Set<ShadowInput> getInputs() {
    return inputs;
  }

  protected abstract void start();

  @Override
  public <IN, OUT, R> ShadowDSL addShadowInput(ShadowInput<IN, OUT, R> input) {
    inputs.add(input);
    return this;
  }

  @Override
  public <IN, OUT, T> ShadowDSL addShadowOutput(ShadowOutput<IN, OUT, T> output) {
    outputs.add(output);
    return this;
  }
}
