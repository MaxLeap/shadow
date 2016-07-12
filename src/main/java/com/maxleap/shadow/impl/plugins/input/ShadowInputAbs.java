package com.maxleap.shadow.impl.plugins.input;

import com.maxleap.shadow.ShadowCodec;
import com.maxleap.shadow.ShadowInput;
import com.maxleap.shadow.ShadowOutput;

import java.util.Optional;

/**
 * Created by stream.
 */
public abstract class ShadowInputAbs<DE_IN, DE_OUT, EN_IN, EN_OUT> implements ShadowInput {

  protected ShadowOutput<EN_OUT> shadowOutput;
  protected Optional<ShadowCodec<DE_IN, DE_OUT>> decodec = Optional.empty();
  protected Optional<ShadowCodec<EN_IN, EN_OUT>> encodec = Optional.empty();

  public void setDecodec(ShadowCodec<DE_IN, DE_OUT> decodec) {
    this.decodec = Optional.ofNullable(decodec);
  }

  public void setEncodec(ShadowCodec<EN_IN, EN_OUT> encodec) {
    this.encodec = Optional.ofNullable(encodec);
  }

  public void setOutputPlugin(ShadowOutput<EN_OUT> shadowOutput) {
    this.shadowOutput = shadowOutput;
  }

  /**
   * if decodec doesn't exist, we will use content directly.
   * @param content
   * @return Object
   */
  protected Object defaultContent(DE_IN content) {
    if (decodec.isPresent()) {
      return decodec.get().translate(content);
    } else {
      return content;
    }
  }
}
