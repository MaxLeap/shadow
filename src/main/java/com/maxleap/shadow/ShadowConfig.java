package com.maxleap.shadow;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.*;

/**
 * Vert.x's jsonObject can not contain js function, we extend jsonObject to jsObject which
 * can contain js function.
 *
 * Created by stream.
 */
public class ShadowConfig {

  private ShadowConfig root;
  private JsonObject innerJson;

  private Map<Integer, ScriptObjectMirror> scriptFnMaps = new HashMap<>();

  public ShadowConfig(Map<String, Object> origin) {
    this.innerJson = parserMap(origin);
  }

  public ShadowConfig(JsonObject jsonObject) {
    this(jsonObject, null);
  }

  public ShadowConfig(JsonObject jsonObject, ShadowConfig root) {
    this.innerJson = jsonObject;
    this.root = root;
  }

  public ShadowConfig(Map<String, Object> origin, ShadowConfig root) {
    this.innerJson = parserMap(origin);
    this.root = root;
  }

  public Set<String> fieldNames() {
    return innerJson.fieldNames();
  }

  public ScriptObjectMirror getFn(String key) {
    ScriptObjectMirror fn = null;
    Integer fnHashCode = innerJson.getInteger(key);
    if (fnHashCode != null) {
      fn = scriptFnMaps.get(fnHashCode);
      if (fn == null && root != null) {
        fn = root.getFn(fnHashCode);
      }
    }
    return fn;
  }

  public ScriptObjectMirror getFn(int fnHashCode) {
    return scriptFnMaps.get(fnHashCode);
  }

  public String getString(String key) {
    return innerJson.getString(key);
  }

  public String getString(String key, String def) {
    return innerJson.getString(key, def);
  }

  public Integer getInteger(String key) {
    return innerJson.getInteger(key);
  }

  public Integer getInteger(String key, Integer def) {
    return innerJson.getInteger(key, def);
  }

  public Long getLong(String key) {
    return innerJson.getLong(key);
  }

  public Long getLong(String key, Long def) {
    return innerJson.getLong(key, def);
  }

  public Double getDouble(String key) {
    return innerJson.getDouble(key);
  }

  public Double getDouble(String key, Double def) {
    return innerJson.getDouble(key, def);
  }

  public Float getFloat(String key) {
    return innerJson.getFloat(key);
  }

  public Float getFloat(String key, Float def) {
    return innerJson.getFloat(key, def);
  }

  public Boolean getBoolean(String key) {
    return innerJson.getBoolean(key);
  }

  public Boolean getBoolean(String key, Boolean def) {
    return innerJson.getBoolean(key, def);
  }

  public JsonObject getJsonObject(String key) {
    return innerJson.getJsonObject(key);
  }

  public JsonObject getJsonObject(String key, JsonObject def) {
    return innerJson.getJsonObject(key, def);
  }

  public JsonArray getJsonArray(String key) {
    return innerJson.getJsonArray(key);
  }

  public JsonArray getJsonArray(String key, JsonArray def) {
    return innerJson.getJsonArray(key, def);
  }

  public Object getValue(String key) {
    return innerJson.getValue(key);
  }

  public Object getValue(String key, Object def) {
    return innerJson.getValue(key, def);
  }

  public boolean containsKey(String key) {
    return innerJson.containsKey(key);
  }

  private JsonObject parserMap(Map<String, Object> origin) {
    JsonObject jsonObject = new JsonObject();
    origin.entrySet().stream().forEach(entry -> {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof ScriptObjectMirror) {
        ScriptObjectMirror scriptObject = (ScriptObjectMirror) value;
        if (scriptObject.isFunction()) {
          jsonObject.put(key, value.hashCode());
          scriptFnMaps.put(value.hashCode(), (ScriptObjectMirror) value);
        } else if (scriptObject.isArray()) {
          jsonObject.put(key, parserList(scriptObject.values()));
        } else {
          jsonObject.put(key, value);
        }
      } else {
        jsonObject.put(key, value);
      }
    });
    return jsonObject;
  }

  private JsonArray parserList(Collection<Object> list) {
    JsonArray jsonArray = new JsonArray();
    list.forEach(value -> {
      if (value instanceof Map) {
        jsonArray.add(parserMap((Map) value));
      } else if (value instanceof List) {
        jsonArray.add(parserList((List) value));
      } else {
        jsonArray.add(value);
      }
    });
    return jsonArray;
  }


}
