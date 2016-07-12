package com.maxleap.shadow;

/**
 * Created by stream.
 */
@FunctionalInterface
public interface ShadowCodec<IN, OUT> {

  OUT translate(IN data);

}
