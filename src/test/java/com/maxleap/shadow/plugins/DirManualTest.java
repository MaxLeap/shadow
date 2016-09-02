package com.maxleap.shadow.plugins;

import com.maxleap.shadow.ParserEngine;
import com.maxleap.shadow.ShadowInput;
import com.maxleap.shadow.impl.engine.nashorn.NashornParserEngine;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Created by stream.
 */

@RunWith(VertxUnitRunner.class)
public class DirManualTest {

  private static String resourceDir = Thread.currentThread().getContextClassLoader().getResource(".").getFile();

  @Ignore
  @Test
  public void fileOutput(TestContext context) {
    Async async = context.async();
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setBlockedThreadCheckInterval(10000000);
    Vertx vertx = Vertx.vertx(vertxOptions);
    ParserEngine parserEngine = new NashornParserEngine(vertx);
    CompletableFuture<Void> parserFuture = parserEngine.init(resourceDir + "/js");

    writeLog(vertx);

    parserFuture
      .thenCompose(aVoid -> {
        ShadowInput shadowInput = parserEngine.getShadowInput("myDir");
        return shadowInput.start();
      })
      .whenComplete((a, throwable) -> {
        if (throwable != null) {
          context.fail(throwable);
        } else {
          System.out.println("working.....");
        }
      });
  }

  private void writeLog(Vertx vertx) {
    vertx.setPeriodic(1000L, event -> {
      Logger logger = LoggerFactory.getLogger(DirManualTest.class);
      String now = Instant.now().toEpochMilli() + " | ";
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 50; i++) sb.append(now);
      logger.info(sb.toString());
    });
  }

  @Ignore
  @Test
  public void readInodeInfo(TestContext context) throws IOException {
    Async async = context.async();
    String file = resourceDir + "/js/" + "shadow.js";
    Path path = Paths.get(file);
    BasicFileAttributes bfa = Files.readAttributes(path, BasicFileAttributes.class);
    String str = bfa.fileKey().toString();
    String[] devAndInode = str.split("=");
    System.out.println(str);
    String inode = devAndInode[2].substring(0, devAndInode[2].length() - 1);
    System.out.println(inode);
  }

}
