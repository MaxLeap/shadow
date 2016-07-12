package com.maxleap.shadow.impl.codec;

import com.maxleap.shadow.ShadowCodec;
import io.vertx.core.json.JsonObject;

/**
 * stream.
 */
public class StringToJson implements ShadowCodec<String, JsonObject> {

  @Override
  public JsonObject translate(String data) {
    return new JsonObject(data);
  }
}
