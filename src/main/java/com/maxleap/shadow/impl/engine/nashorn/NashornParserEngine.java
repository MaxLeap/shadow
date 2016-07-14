package com.maxleap.shadow.impl.engine.nashorn;

import com.maxleap.shadow.*;
import com.maxleap.shadow.impl.plugins.input.ShadowInputAbs;
import com.maxleap.shadow.impl.plugins.output.ShadowOutputAbs;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by stream.
 */
public class NashornParserEngine implements ParserEngine {

  private final ScriptEngine engine;
  private Vertx vertx;
  private String scriptFolder;
  private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
  private Map<String, ShadowInput> inputPlugins = new HashMap<>();
  private Map<String, ShadowOutput> outputPlugins = new HashMap<>();
  private Map<String, ShadowCodec> codecs = new HashMap<>();

  //default using stdout for output.
  private static final String DEFAULT_OUTPUT_CLASS_PATH = "as.leap.shadow.impl.plugins.output.shadowOutputStdout";
  private static final Logger logger = LoggerFactory.getLogger(NashornParserEngine.class);

  public NashornParserEngine(Vertx vertx) {
    ScriptEngineManager mgr = new ScriptEngineManager();
    engine = mgr.getEngineByName("nashorn");
    this.vertx = vertx;
    if (engine == null) {
      throw new IllegalStateException("Cannot find Nashorn JavaScript engine - maybe you are not running with Java 8 or later?");
    }
  }

  //for test
  ScriptEngine getEngine() {
    return engine;
  }

  @Override
  public CompletableFuture<Void> init(String scriptFolder) {
    this.scriptFolder = scriptFolder;
    //loadJSFiles utils
    CompletableFuture<Void> future = new CompletableFuture<>();
    try {
      engine.eval(JS_CONSOLE);
      future.complete(null);
    } catch (ScriptException e) {
      future.completeExceptionally(e);
    }
    //loadJSFiles script
    return future.thenCompose(aVoid -> loadJSFiles()).thenCompose(aVoid -> loadPlugins());
  }

  @Override
  public CompletableFuture<Void> reloadPluginFn() {
    return loadJSFiles().thenCompose(aVoid -> {
      ScriptObjectMirror inputs = (ScriptObjectMirror) engine.get("shadowInput");
      inputs.values().forEach(inputPlugin -> {
        ScriptObjectMirror plugin = (ScriptObjectMirror) inputPlugin;
        ShadowConfig config = new ShadowConfig((Map<String, Object>) plugin.get("config"));
        inputPlugins.get(plugin.get("pluginClass")).reloadFn(config);
      });
      return CompletableFuture.completedFuture(null);
    });
  }

  private CompletableFuture<Void> loadJSFiles() {
    final CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.executeBlocking(event -> {
      List<String> filesResult = vertx.fileSystem().readDirBlocking(scriptFolder, ".*.js");
      try {
        for (String filePath : filesResult) {
          engine.eval(vertx.fileSystem().readFileBlocking(filePath).toString());
        }
      } catch (ScriptException e) {
        logger.error("script exception", e);
        event.fail(e);
      }
      event.complete();
    }, event -> {
      if (event.succeeded()) {
        future.complete(null);
      } else {
        future.completeExceptionally(event.cause());
      }
    });
    return future;
  }

  @Override
  public CompletableFuture<Void> start() {
    return CompletableFuture.allOf(inputPlugins.values().stream()
      .map(ShadowInput::start)
      .collect(Collectors.toList())
      .toArray(new CompletableFuture[inputPlugins.entrySet().size()]));
  }

  @Override
  public ShadowInput getShadowInput(String pluginName) {
    return inputPlugins.get(pluginName);
  }

  @Override
  public ShadowOutput getShadowOutput(String pluginName) {
    return outputPlugins.get(pluginName);
  }

  private CompletableFuture<Void> loadPlugins() {
    ScriptObjectMirror shadowOutput = (ScriptObjectMirror) engine.get("shadowOutput");
    CompletableFuture[] outputInitFutures = shadowOutput.entrySet().stream()
      .map(this::getOutputPlugin)
      .collect(Collectors.toList())
      .toArray(new CompletableFuture[shadowOutput.entrySet().size()]);

    return CompletableFuture.allOf(outputInitFutures)
      .thenCompose((aVoid) -> {
        ScriptObjectMirror shadowInput = (ScriptObjectMirror) engine.get("shadowInput");
        CompletableFuture[] inputInitFutures = shadowInput.entrySet().stream()
          .map(this::getInputPlugin)
          .collect(Collectors.toList())
          .toArray(new CompletableFuture[shadowInput.entrySet().size()]);
        return CompletableFuture.allOf(inputInitFutures);
      });
  }

  private CompletableFuture<Void> getInputPlugin(Map.Entry<String, Object> inputEntry) {
    //TODO:split all the plugin to process, so we can reload them.
    String inputPluginName = inputEntry.getKey();
    ScriptObjectMirror inputPluginJS = (ScriptObjectMirror) inputEntry.getValue();
    String pluginClassPath = (String) inputPluginJS.get("pluginClass");
    CompletableFuture<Void> future = new CompletableFuture<>();
    try {
      ShadowInputAbs shadowInputPlugin = loadClass(classLoader, pluginClassPath);
      //inject default output plugins
      String outputName = (String) inputPluginJS.get("shadowOutputName");
      if (outputName != null) {
        shadowInputPlugin.setOutputPlugin(outputPlugins.get(outputName));
      } else {
        //TODO std out
        shadowInputPlugin.setOutputPlugin(outputPlugins.get(DEFAULT_OUTPUT_CLASS_PATH));
      }

      //inject codec TODO refactor
      String decodecClassPath = (String) inputPluginJS.get("decodec");
      if (decodecClassPath != null) {
        try {
          shadowInputPlugin.setDecodec(loadCodec(decodecClassPath));
        } catch (ShadowException e) {
          future.completeExceptionally(e);
        }
      }

      String encodecClassPath = (String) inputPluginJS.get("encodec");
      if (encodecClassPath != null) {
        try {
          shadowInputPlugin.setEncodec(loadCodec(encodecClassPath));
        } catch (ShadowException e) {
          future.completeExceptionally(e);
        }
      }

      Map<String, Object> config = (Map<String, Object>) inputPluginJS.get("config");
      future = shadowInputPlugin.init(vertx, new ShadowConfig(config)).thenAccept(aVoid -> {
        inputPlugins.put(inputPluginName, shadowInputPlugin);
        logger.info(String.format("init %s success.", pluginClassPath));
      });
    } catch (ShadowException e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  @SuppressWarnings("unchecked")
  private CompletableFuture<Void> getOutputPlugin(Map.Entry<String, Object> outputEntry) {
    Map<String, Object> outputPluginJS = (Map<String, Object>) outputEntry.getValue();
    CompletableFuture<Void> future = new CompletableFuture<>();
    String outputPluginName = outputEntry.getKey();
    String pluginClassPath = (String) outputPluginJS.get("pluginClass");
    String decodecClassPath = (String) outputPluginJS.get("decodec");
    //
    Map<String, Object> outputConfig = (Map<String, Object>) outputPluginJS.get("config");
    try {
      ShadowOutput shadowOutputPlugin = loadClass(classLoader, pluginClassPath);
      if (decodecClassPath != null) {
        ShadowCodec shadowCodec = loadCodec(decodecClassPath);
        ((ShadowOutputAbs) shadowOutputPlugin).setDecodec(shadowCodec);
      }
      future = shadowOutputPlugin.init(vertx, new ShadowConfig(outputConfig)).thenAccept(aVoid -> {
        outputPlugins.put(outputPluginName, shadowOutputPlugin);
        logger.info(String.format("load output plugin %s success.", outputEntry.getKey()));
      });
    } catch (ShadowException e) {
      future.completeExceptionally(e);
    }
    return future;
  }

  private <T> T loadClass(ClassLoader classLoader, String classPath) throws ShadowException {
    try {
      Class<T> clazz = (Class<T>) Class.forName(classPath, true, classLoader);
      return clazz.newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new ShadowException(String.format("can not loadJSFiles class %s", classPath), e);
    }
  }

  @SuppressWarnings("unchecked")
  private ShadowCodec loadCodec(String classPath) throws ShadowException {
    try {
      ShadowCodec shadowCodec = codecs.get(classPath);
      if (shadowCodec == null) {
        Class<ShadowCodec> codecClass = (Class<ShadowCodec>) Class.forName(classPath);
        shadowCodec = codecClass.newInstance();
        codecs.put(classPath, shadowCodec);
        logger.info(String.format("init shadowCodec %s success.", classPath));
      }
      return shadowCodec;
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      logger.warn(String.format("can not load shadowCodec class %s.", classPath), e);
      throw new ShadowException(e.getMessage(), e);
    }
  }

  private static final String JS_CONSOLE = "var stdout = java.lang.System.out;\n" +
    "var stderr = java.lang.System.err;\n" +
    "\n" +
    "var console = {\n" +
    "\n" +
    "  log: function(msg) {\n" +
    "    stdout.println(msg);\n" +
    "  },\n" +
    "\n" +
    "  warn: function(msg) {\n" +
    "    stderr.println(msg);\n" +
    "  },\n" +
    "\n" +
    "  error: function(msg) {\n" +
    "    stderr.println(msg);\n" +
    "  }\n" +
    "};\n";
}