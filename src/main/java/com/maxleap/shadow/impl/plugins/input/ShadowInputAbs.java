package com.maxleap.shadow.impl.plugins.input;

import com.maxleap.shadow.ShadowCodec;
import com.maxleap.shadow.ShadowInput;
import com.maxleap.shadow.ShadowOutput;
import com.maxleap.shadow.impl.FnInvoker;

import java.util.Optional;

/**
 * Created by stream.
 */
public abstract class ShadowInputAbs<IN, OUT, T> implements ShadowInput, FnInvoker {

  protected ShadowOutput<T> shadowOutput;

  protected Optional<ShadowCodec<IN, OUT>> decodec = Optional.empty();

  public void setDecodec(ShadowCodec<IN, OUT> decodec) {
    this.decodec = Optional.ofNullable(decodec);
  }

  public void setOutputPlugin(ShadowOutput<T> shadowOutput) {
    this.shadowOutput = shadowOutput;
  }

  /**
   * if decodec doesn't exist, we will use content directly.
   * @param content default content
   * @return DE_OUT
   */
  @SuppressWarnings("unchecked")
  protected OUT defaultContent(IN content) {
    if (decodec.isPresent()) {
      return decodec.get().translate(content);
    } else {
      return (OUT) content;
    }
  }
}
