package cn.leapcloud.shadow;

/**
 * Created by stream.
 */
public interface ShadowDSL {

  <IN, OUT, R> ShadowDSL addShadowInput(String name, ShadowInput<IN, OUT, R> input);

  <IN, OUT, T> ShadowDSL addShadowOutput(String name, ShadowOutput<IN, OUT, T> output);


}