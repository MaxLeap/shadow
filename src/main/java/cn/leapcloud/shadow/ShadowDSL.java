package cn.leapcloud.shadow;

/**
 * Created by stream.
 */
public interface ShadowDSL {

  <IN, OUT, R> ShadowDSL addShadowInput(ShadowInput<IN, OUT, R> input);

  <IN, OUT, R, T> ShadowDSL addShadowOutput(ShadowOutput<IN, OUT, R, T> output);


}