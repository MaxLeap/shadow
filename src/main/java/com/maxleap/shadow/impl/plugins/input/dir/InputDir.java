package com.maxleap.shadow.impl.plugins.input.dir;

import com.maxleap.shadow.ShadowCodec;
import com.maxleap.shadow.ShadowException;
import com.maxleap.shadow.ShadowOutput;
import com.maxleap.shadow.impl.FnInvoker;
import com.maxleap.shadow.impl.codec.LineFeed.LineFeedMeta;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by stream.
 */
class InputDir implements FnInvoker {

  private Vertx vertx;
  private String startPath;
  private String pattern;
  private int bufferSize = 2 << 11;
  private int depth = 4;
  private long pauseTime = 1000L;
  private ScriptObjectMirror matchFn;
  private ShadowOutput<JsonObject> shadowOutput;
  private ShadowCodec<Buffer, List<LineFeedMeta>> decode;
  private Map<String, TargetFile> scannedLog = new HashMap<>();
  private Pattern filterPattern;

  private static final Logger logger = LoggerFactory.getLogger(InputDir.class);

  private OpenOptions openOptions = new OpenOptions()
    .setRead(true)
    .setCreate(false)
    .setCreateNew(false)
    .setDeleteOnClose(false)
    .setWrite(false)
    .setSparse(false);

  InputDir(Vertx vertx, String startPath, String pattern, int depth, Object matchFn,
           ShadowCodec<Buffer, List<LineFeedMeta>> decode, ShadowOutput<JsonObject> shadowOutput) {
    this.vertx = vertx;
    this.startPath = startPath;
    this.pattern = pattern;
    this.filterPattern = Pattern.compile(pattern);
    this.depth = depth;
    this.matchFn = (ScriptObjectMirror) matchFn;
    this.decode = decode;
    this.shadowOutput = shadowOutput;
  }

  private Consumer<TargetFile> readLogFile = logFile -> {
    AsyncFile asyncFile = vertx.fileSystem().openBlocking(logFile.getFilePath(), openOptions);
    logFile.setAsyncFile(asyncFile);
    scannedLog.put(logFile.getFilePath(), logFile);
    readLog(logFile, asyncFile, Buffer.buffer(), readLength(logFile));
  };

  private Stream<TargetFile> targetFileStream(boolean tail) throws IOException {
    FileProps startPathProps = vertx.fileSystem().propsBlocking(startPath);
    if (startPathProps.isDirectory()) {
      return Files
        .walk(Paths.get(startPath), depth)
        .map(Path::toString)
        .filter(filePath -> filterPattern.matcher(filePath).matches())
        .map(filePath -> getLogFile(filePath, null, tail))
        .sorted();
    } else {
      return Stream.of(getLogFile(startPath, startPathProps, tail));
    }
  }

  void setMatchFn(ScriptObjectMirror matchFn) {
    this.matchFn = matchFn;
  }

  CompletableFuture<Void> scan(boolean tail) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    vertx.executeBlocking(event -> {
      try {
        Set<TargetFile> newLogFiles = targetFileStream(tail).collect(Collectors.toSet());
        Set<TargetFile> oldLogFiles = scannedLog.values().stream().collect(Collectors.toSet());
        Set<TargetFile> mutual = newLogFiles.stream().filter(oldLogFiles::contains).collect(Collectors.toSet());

        //redundant files
        oldLogFiles.stream()
          .filter(targetFile -> !mutual.contains(targetFile)).collect(Collectors.toSet())
          .forEach(targetFile -> {
            scannedLog.remove(targetFile.getFilePath());
            //close file
            targetFile.getAsyncFile().close(closeEvent -> {
              if (closeEvent.failed())
                logger.error(closeEvent.cause().getMessage(), closeEvent.cause());
            });
          });

        //appended files
        newLogFiles.removeAll(mutual);
        newLogFiles.forEach(readLogFile);
      } catch (IOException e) {
        event.fail(e);
      }
      event.complete();
    }, false, event -> {
      if (event.succeeded()) {
        vertx.runOnContext(h -> future.complete(null));
      } else {
        vertx.runOnContext(h -> future.completeExceptionally(event.cause()));
      }
    });
    return future;
  }

  CompletableFuture<Void> stop() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    scannedLog.values().forEach(targetFile -> targetFile.getAsyncFile().close());
    return future;
  }

  private int readLength(TargetFile targetFile) {
    return targetFile.getTotalSize() > bufferSize ? bufferSize : (int) targetFile.getTotalSize();
  }

  private TargetFile getLogFile(String filePath, FileProps fileProps, boolean tail) {
    if (fileProps == null) {
      fileProps = vertx.fileSystem().propsBlocking(filePath);
    }
    TargetFile targetFile = scannedLog.getOrDefault(filePath,
      new TargetFile(startPath + pattern, filePath, fileProps.size(), fileProps.lastModifiedTime(), tail ? fileProps.size() : 0));
    targetFile.setTotalSize(fileProps.size());
    targetFile.setLastModifiedTime(fileProps.lastModifiedTime());
    return targetFile;
  }

  private void readLog(TargetFile targetFile, AsyncFile asyncFile, Buffer remainBuffer, int readLength) {
    //the offset is relative read Buffer, not relative File
    //position should consider reset buffer and file pos.
    asyncFile.read(Buffer.buffer(readLength), 0, targetFile.getCurrentPos() + remainBuffer.length(), readLength, fileResult -> {
      //merge remainBuffer
      remainBuffer.appendBuffer(fileResult.result());
      //count position
      int pos = 0;
      List<LineFeedMeta> lineFeedMetas = decode.translate(remainBuffer);
      try {
        for (LineFeedMeta lineFeedMeta : lineFeedMetas) {
          pos += lineFeedMeta.getSourceBufferSize();
          JsonObject fnResult = invokeFnJsonObject(matchFn, targetFile.getFilePath(), lineFeedMeta.getResult());
          shadowOutput.execute(fnResult);
        }
      } catch (ShadowException ex) {
        logger.error(ex.getMessage(), ex);
      }
      targetFile.setCurrentPos(targetFile.getCurrentPos() + pos);
      Buffer resetBuff = Buffer.buffer(remainBuffer.getBytes(pos, remainBuffer.length()));
      if (targetFile.getCurrentPos() < targetFile.getTotalSize()) {
        long remainderLength = targetFile.getTotalSize() - resetBuff.length();
        int nextReadLength = remainderLength > bufferSize ? bufferSize : (int) remainderLength;
        readLog(targetFile, asyncFile, resetBuff, nextReadLength);
      } else {
        scannedLog.put(targetFile.getFilePath(), targetFile);
        vertx.setTimer(pauseTime, event -> vertx.fileSystem().exists(targetFile.getFilePath(), existEvent -> {
          if (existEvent.succeeded() && existEvent.result()) {
            readLog(targetFile, asyncFile, Buffer.buffer(0), readLength(targetFile));
          }
        }));
      }
    });
  }

}
