package com.maxleap.shadow.impl.codec;

import com.maxleap.shadow.ShadowCodec;

/**
 * Created by stream.
 */
public class ObjectToString<DE_IN> implements ShadowCodec<DE_IN, String> {

  @Override
  public String translate(DE_IN data) {
    return data.toString();
  }
}
