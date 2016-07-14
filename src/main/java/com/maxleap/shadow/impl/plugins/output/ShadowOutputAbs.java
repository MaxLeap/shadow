package com.maxleap.shadow.impl.plugins.output;

import com.maxleap.shadow.ShadowCodec;
import com.maxleap.shadow.ShadowOutput;
import com.maxleap.shadow.impl.codec.ObjectToString;

import java.util.Optional;

/**
 * Created by stream.
 */
public abstract class ShadowOutputAbs<DE_IN, DE_OUT> implements ShadowOutput<DE_IN> {

  protected Optional<ShadowCodec<DE_IN, DE_OUT>> decodec = Optional.empty();

  protected ShadowCodec<DE_IN, String> defaultDecodec = new ObjectToString<>();

  public void setDecodec(ShadowCodec<DE_IN, DE_OUT> decodec) {
    this.decodec = Optional.ofNullable(decodec);
  }

  /**
   * if decodec doesn't exist, we will use content directly.
   * @param content
   * @return DE_OUT
   */
  @SuppressWarnings("unchecked")
  protected DE_OUT defaultContent(DE_IN content) {
    return decodec.map(decodec -> decodec.translate(content)).orElse((DE_OUT) content);
  }

}
