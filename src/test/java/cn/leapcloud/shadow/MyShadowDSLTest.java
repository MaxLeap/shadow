package cn.leapcloud.shadow;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Created by stream.
 */
@RunWith(VertxUnitRunner.class)
public class MyShadowDSLTest {

  private static Supplier<String> kafkaAddress = () -> {
    String address = "localhost";
    try {
      address = InetAddress.getByName("kafka").getHostAddress();
    } catch (UnknownHostException e) {
      //
    }
    return address;
  };

  private static String KAFKA_ADDRESS = kafkaAddress.get();

  @Ignore
  @Test
  public void runSimpleMyShadowDSL(TestContext context) {
    Async async = context.async();
    Vertx vertx = Vertx.vertx();
    Shadow shadow = new Shadow(vertx);
    //mock output server
    vertx.createHttpServer().requestHandler(request ->
      request.exceptionHandler(context::fail).bodyHandler(bodyBuffer -> {
        JsonObject body = bodyBuffer.toJsonObject();
        context.assertEquals("bar", body.getString("foo"));
        async.complete();
        request.response().end();
      }))
      .listen(8081, "localhost", event -> {
        //
        shadow.startShadowDSL().setHandler(asyncResult -> {
          if (asyncResult.succeeded()) {
            vertx.createHttpClient()
              .post(8082, "localhost", "/log1")
              .putHeader("Content-Type", "application/json")
              .handler(response -> context.assertEquals(200, response.statusCode()))
              .end(new JsonObject().put("foo", "bar").encode());
          } else {
            context.fail(asyncResult.cause());
          }
        });
      });
  }

  @Test
  public void maxleapLog(TestContext context) throws InterruptedException {
    Async async = context.async();
    AtomicBoolean finish = new AtomicBoolean(false);
    Vertx vertx = Vertx.vertx();
    Shadow shadow = new Shadow(vertx);
    String sampleLog = "{\n" +
      "      \"@timestamp\": \"2016-11-09T07:00:51.157Z\",\n" +
      "      \"beat\": {\n" +
      "      \"hostname\": \"boaRiver.local\",\n" +
      "        \"name\": \"boaRiver.local\",\n" +
      "        \"version\": \"5.0.0\"\n" +
      "    },\n" +
      "      \"fields\": {\n" +
      "      \"ip\": \"127.0.0.1\"\n" +
      "    },\n" +
      "      \"input_type\": \"log\",\n" +
      "      \"message\": \"2016-11-07 16:14:51:472 - ERROR - com.maxleap.circe.service.CirceServiceTest.stdout(CirceServiceTest.java:60) - main - some error.\\njava.lang.RuntimeException: some exception.\\n\\tat com.maxleap.circe.service.CirceServiceTest.stdout(CirceServiceTest.java:60) ~[test-classes/:?]\\n\\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:1.8.0_111]\\n\\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[?:1.8.0_111]\\n\\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:1.8.0_111]\\n\\tat java.lang.reflect.Method.invoke(Method.java:498) ~[?:1.8.0_111]\\n\\tat io.vertx.ext.unit.junit.VertxUnitRunner.invokeTestMethod(VertxUnitRunner.java:93) ~[vertx-unit-3.3.3.jar:?]\\n\\tat io.vertx.ext.unit.junit.VertxUnitRunner.lambda$invokeExplosively$0(VertxUnitRunner.java:114) ~[vertx-unit-3.3.3.jar:?]\\n\\tat io.vertx.ext.unit.impl.TestContextImpl$Step.run(TestContextImpl.java:122) ~[vertx-unit-3.3.3.jar:?]\\n\\tat io.vertx.ext.unit.impl.TestContextImpl$Step.access$600(TestContextImpl.java:30) ~[vertx-unit-3.3.3.jar:?]\\n\\tat io.vertx.ext.unit.impl.TestContextImpl.run(TestContextImpl.java:221) ~[vertx-unit-3.3.3.jar:?]\\n\\tat io.vertx.ext.unit.junit.VertxUnitRunner.lambda$invokeExplosively$1(VertxUnitRunner.java:127) ~[vertx-unit-3.3.3.jar:?]\\n\\tat io.vertx.core.impl.ContextImpl.lambda$wrapTask$2(ContextImpl.java:316) ~[vertx-core-3.3.3.jar:?]\\n\\tat io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java:163) [netty-common-4.1.5.Final.jar:4.1.5.Final]\\n\\tat io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:418) [netty-common-4.1.5.Final.jar:4.1.5.Final]\\n\\tat io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:440) [netty-transport-4.1.5.Final.jar:4.1.5.Final]\\n\\tat io.netty.util.concurrent.SingleThreadEventExecutor$5.run(SingleThreadEventExecutor.java:873) [netty-common-4.1.5.Final.jar:4.1.5.Final]\\n\\tat java.lang.Thread.run(Thread.java:745) [?:1.8.0_111]\\nCaused by: java.lang.NullPointerException\\n\\tat com.example.myproject.Book.getId(Book.java:22)\\n\\tat com.example.myproject.Author.getBookIds(Author.java:35)\\n\\t... 1 more\",\n" +
      "      \"offset\": 2409,\n" +
      "      \"source\": \"/opt/logs/tools/shadow/example.log\",\n" +
      "      \"type\": \"log\"\n" +
      "    }";
    //mock kafka producer
    JsonObject kafkaConfig = new JsonObject()
      .put("bootstrap.servers", KAFKA_ADDRESS + ":9092")
      .put("client.id", "KafkaProducer")
      .put("key.serializer", "org.apache.kafka.common.serialization.IntegerSerializer")
      .put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    KafkaProducer<Integer, String> producer = new KafkaProducer<>(kafkaConfig.getMap());
    ProducerRecord<Integer, String> record = new ProducerRecord<>("leapCloudLog", sampleLog.hashCode(), sampleLog);
    vertx.executeBlocking(event -> producer.send(record, (metadata, exception) -> {
      if (exception != null) {
        event.fail(exception);
      } else {
        System.out.println("have send kafka message.");
        event.complete();
      }
    }), false, event -> {
      if (event.failed()) context.fail(event.cause());
      else {
        //mock es httpServer
        vertx.createHttpServer().requestHandler(request -> {
          request.bodyHandler(bodyBuffer -> {
            JsonObject bodyContent = bodyBuffer.toJsonObject();
            context.assertEquals("127.0.0.1", bodyContent.getString("source_ip"));
            context.assertEquals("boaRiver.local", bodyContent.getString("source_host"));
            context.assertEquals("main", bodyContent.getString("thread_name"));
            request.response().end();
            //set a flag in case async.complete be executed multi times.
            if (!finish.get()) {
              finish.set(true);
              async.complete();
            }
          });
        }).listen(9200, httpServer -> {
          if (httpServer.succeeded()) {
            //start kafka consumer.
            shadow.startShadowDSL().setHandler(asyncResult -> {
              if (asyncResult.failed()) {
                context.fail(asyncResult.cause());
              }
            });
          } else context.fail(httpServer.cause());
        });
      }
    });
  }

  @Ignore
  @Test
  public void testEx() {
    String content = "2016-11-09 14:27:47:531 - INFO - com.maxleap.log4j2.LoggerTest.log4j2(LoggerTest.java:19) - main - info";
    //DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.ofEpochMilli(event.getTimeMillis()).atZone(ZoneId.of("GMT")))
    //DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.of)
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SS");
    LocalDateTime dateTime = LocalDateTime.parse(content.split(" - ", 5)[0], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS"));
    System.out.println(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime.atZone(ZoneId.of("GMT"))));
    //System.out.println(content.split(" - ", 5)[0]);

  }

}
