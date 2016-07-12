package com.maxleap.shadow.impl.codec;

import com.maxleap.shadow.ShadowCodec;

/**
 * Created by stream.
 */
public class Transparency<IN, OUT> implements ShadowCodec<IN, OUT> {

  @Override
  public OUT translate(IN data) {
    return (OUT) data;
  }
}
