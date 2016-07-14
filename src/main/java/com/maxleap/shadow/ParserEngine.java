package com.maxleap.shadow;

import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */
public interface ParserEngine {

  CompletableFuture<Void> init(String scriptFolder);

  //add reload method, and will stop all plugin and init start plugin.
  //or just reload Fn? how to get all the Fn from plugin meta?
  //shall we storage Fn? Or provide reload Fn method in interface of ShadowInput?
  CompletableFuture<Void> reloadPluginFn();

  CompletableFuture<Void> start();

  ShadowInput getShadowInput(String pluginName);

  ShadowOutput getShadowOutput(String pluginName);
}
