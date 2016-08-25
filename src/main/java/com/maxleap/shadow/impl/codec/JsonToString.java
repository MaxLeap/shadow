package com.maxleap.shadow.impl.codec;

import com.maxleap.shadow.ShadowCodec;
import io.vertx.core.json.JsonObject;

/**
 * Created by stream.
 */
public class JsonToString implements ShadowCodec<JsonObject, String> {

  @Override
  public String translate(JsonObject data) {
    return data.encode();
  }
}
