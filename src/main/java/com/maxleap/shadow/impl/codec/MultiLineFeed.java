package com.maxleap.shadow.impl.codec;

import com.maxleap.shadow.ShadowCodec;
import io.vertx.core.buffer.Buffer;

import java.util.List;

/**
 * Created by stream.
 */
public class MultiLineFeed implements ShadowCodec<Buffer, String> {

  private LineFeed lineFeed;

  @Override
  public String translate(Buffer data) {
    List<LineFeed.LineFeedMeta> lines =  lineFeed.translate(data);

    return null;
  }
}
