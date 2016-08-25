package com.maxleap.shadow.impl.codec;

import com.maxleap.shadow.ShadowCodec;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

/**
 * Created by stream.
 */
public class BufferToJson implements ShadowCodec<Buffer, JsonObject> {

  @Override
  public JsonObject translate(Buffer data) {
    return data.toJsonObject();
  }
}
