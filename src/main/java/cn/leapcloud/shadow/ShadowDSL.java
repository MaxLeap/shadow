package cn.leapcloud.shadow;

/**
 * Created by stream.
 */
public interface ShadowDSL {

  <IN, OUT, R> ShadowDSL addShadowInput(ShadowInput<IN, OUT, R> input);

  <IN, OUT, T> ShadowDSL addShadowOutput(ShadowOutput<IN, OUT, T> output);


}