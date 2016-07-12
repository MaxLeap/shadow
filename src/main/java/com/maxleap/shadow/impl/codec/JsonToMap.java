package com.maxleap.shadow.impl.codec;

import com.maxleap.shadow.ShadowCodec;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * Created by stream.
 */
public class JsonToMap implements ShadowCodec<JsonObject, Map<String, Object>> {

  @Override
  public Map<String, Object> translate(JsonObject data) {
    return data.getMap();
  }
}
