package com.maxleap.shadow.impl.codec;

import com.maxleap.shadow.ShadowCodec;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * Created by stream.
 */
public class StringToMap implements ShadowCodec<String, Map<String, Object>> {

  @Override
  public Map<String, Object> translate(String data) {
    return new JsonObject(data).getMap();
  }
}
