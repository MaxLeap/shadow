package com.maxleap.shadow.impl.plugins.output;

import com.maxleap.shadow.ShadowCodec;
import com.maxleap.shadow.ShadowOutput;
import com.maxleap.shadow.impl.FnInvoker;

import java.util.Optional;

/**
 * Created by stream.
 */
public abstract class ShadowOutputAbs<IN, OUT> implements ShadowOutput<IN>, FnInvoker {

  protected Optional<ShadowCodec<IN, OUT>> decodec = Optional.empty();

  public void setDecodec(ShadowCodec<IN, OUT> decodec) {
    this.decodec = Optional.ofNullable(decodec);
  }

  /**
   * if decodec doesn't exist, we will use content directly.
   * @param content
   * @return DE_OUT
   */
  @SuppressWarnings("unchecked")
  protected OUT defaultContent(IN content) {
    return decodec.map(decodec -> decodec.translate(content)).orElse((OUT) content);
  }

}
