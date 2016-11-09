import cn.leapcloud.shadow.AbsShadowDSL;
import cn.leapcloud.shadow.ShadowOutput;
import cn.leapcloud.shadow.impl.input.HttpJsonInput;
import cn.leapcloud.shadow.impl.output.ConsoleOutput;
import cn.leapcloud.shadow.impl.output.HttpJsonOutput;
import cn.leapcloud.shadow.plugins.kafka.KafkaInput;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Created by stream.
 */
public class MyShadow extends AbsShadowDSL {

  @Override
  protected void start() {
    //output
    ShadowOutput<String, String, String> consoleOutput = new ConsoleOutput();
    addShadowOutput(consoleOutput);

    ShadowOutput<JsonObject, String, String> httpJsonOutput = new HttpJsonOutput()
      .config(new JsonObject()
        .put("defaultURI", "/")
        .put("hosts", new JsonArray().add("127.0.0.1:8081")))
      .tokenFunction((data, config) -> Optional.ofNullable(data.getString("defaultURI", config.getString("defaultURI"))))
      .encode(JsonObject::encode);
    addShadowOutput(httpJsonOutput);

    //es output
    ShadowOutput<JsonObject, String, String> esOutput = new HttpJsonOutput()
      .config(new JsonObject()
        .put("defaultURI", "/default/logs")
        .put("hosts", new JsonArray().add("10.10.10.149:9200")))
      .tokenFunction((data, config) -> Optional.ofNullable(data.getString("URI", config.getString("defaultURI"))))
      .encode(JsonObject::encode);
    addShadowOutput(esOutput);

    //==================================================================================================================
    //input
    addShadowInput(new HttpJsonInput()
      .config(new JsonObject()
        .put("host", "127.0.0.1")
        .put("port", 8082)
        .put("uriPatterns", new JsonArray().add("/log1").add("/log2")))
      .matchFunction((request, config) -> {
        if (HttpMethod.POST == request.method()) {
          @SuppressWarnings("unchecked")
          List<String> uriPatterns = config.getJsonArray("uriPatterns").getList();
          for (String pattern : uriPatterns)
            if (Pattern.matches(pattern, request.uri()))
              return true;
        }
        return false;
      })
      .decode(request -> {
        Future<JsonObject> body = Future.future();
        request.bodyHandler(buffer -> body.complete(buffer.toJsonObject())).exceptionHandler(body::fail);
        return body;
      })
      .handler((futureBody, config) -> futureBody)
      .addOutput(httpJsonOutput));

    //kafka input
    addShadowInput(new KafkaInput<Integer, String, JsonObject, JsonObject>()
      .config(new JsonObject()
        .put("props", new JsonObject()
          .put("bootstrap.servers", "0.0.0.0:9200")
          .put("group.id", "kafkaConsumer")
          .put("enable.auto.commit", true)
          .put("auto.commit.interval.ms", 5000)
          .put("session.timeout.ms", 30000)
          .put("key.deserializer", "org.apache.kafka.common.serialization.IntegerDeserializer")
          .put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"))
        .put("topics", new JsonArray().add("maxleapLogTopic"))
        .put("pollTimeout", 10000))
      .matchFunction((record, config) -> config.getJsonArray("topics").getList().contains(record.topic()))
      .decode(record -> new JsonObject(record.value()))
      .handler((value, config) -> {
        //file beat 发过来的文件，提取出 file path, host, ip, 然后解析 java log 转成新的json,
        JsonObject logContent = new JsonObject();
        String message = value.getString("message");
        String[] messageArr = message.split(" - ", 5);
        String dateTimeStr = messageArr[0];
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS"));

        logContent.put("@timestamp", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(dateTime.atZone(ZoneId.of("GMT"))));
        logContent.put("level", messageArr[1]);
        logContent.put("class_method", messageArr[2]);
        logContent.put("thread_name", messageArr[3]);
        logContent.put("message", messageArr[4]);
        //
        logContent.put("source_host", value.getJsonObject("beat").getString("hostname"));
        logContent.put("source_ip", value.getJsonObject("fields").getString("ip"));

        //根据日志路径转换为ES的索引路径,去掉opt以及相关日志目录。只取/tools/shadow,组合成/tools-shadow
        String[] pathArray = value.getString("source").split("/");
        //每个索引需要带上日期，也就是说一天一片 example: /tools/shadow-2016.08.19/[4]
        String esDate = dateTimeStr.split(" ")[0].replace("-", ".");
        logContent.put("URI", "/" + pathArray[3] + "-" + pathArray[4] + "-" + esDate);
        return logContent;
      })
      .addOutput(consoleOutput));

  }
}
