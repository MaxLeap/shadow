package com.maxleap.shadow.impl;

import com.maxleap.shadow.ShadowException;
import io.vertx.core.json.JsonObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.Map;

/**
 * Created by stream.
 */
public interface FnInvoker {

  default JsonObject invokeFnJsonObject(ScriptObjectMirror fn, Object... args) throws ShadowException {
    try {
      for (int i = 0; i < args.length; i++) {
        if (args[i] instanceof JsonObject) {
          args[i] = ((JsonObject) args[i]).getMap();
        }
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> fnResult = (Map<String, Object>) fn.call(fn, args);
      return new JsonObject(fnResult);
    } catch (Exception ex) {
      throw new ShadowException("invoke dynamic function failed.", ex);
    }
  }

  default String invokeFnString(ScriptObjectMirror fn, Object... args) throws ShadowException {
    try {
      for (int i = 0; i < args.length; i++) {
        if (args[i] instanceof JsonObject) {
          args[i] = ((JsonObject) args[i]).getMap();
        }
      }
      @SuppressWarnings("unchecked")
      String fnResult = (String) fn.call(fn, args);
      return fnResult;
    } catch (Exception ex) {
      throw new ShadowException("invoke dynamic function failed.", ex);
    }
  }

}
