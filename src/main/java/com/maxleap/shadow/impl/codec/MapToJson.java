package com.maxleap.shadow.impl.codec;

import com.maxleap.shadow.ShadowCodec;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * Created by stream.
 */
public class MapToJson implements ShadowCodec<Map<String, Object>, JsonObject> {

  @Override
  public JsonObject translate(Map<String, Object> data) {
    return new JsonObject(data);
  }
}
