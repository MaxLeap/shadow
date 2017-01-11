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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Created by stream.
 */
public class MyShadow extends AbsShadowDSL {

  private JsonObject kafkaConfig = new JsonObject()
    .put("bootstrap.servers", "10.10.10.123:9092,10.10.10.135:9092")
    .put("group.id", "kafkaShadowConsumer")
    .put("enable.auto.commit", true)
    .put("auto.commit.interval.ms", 5000)
    .put("session.timeout.ms", 30000)
    .put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    .put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

  private String maskKeyString(String content, String prefixString, String maskString) {
    int passwordPosition = content.indexOf(prefixString);
    int prefixStringLength = prefixString.length();
    int maskStringLength = maskString.length();
    return content.substring(0, passwordPosition + prefixStringLength)
      + maskString + content.substring(passwordPosition + prefixStringLength + maskStringLength);
  }


  @Override
  protected void start() {
    //output
    ShadowOutput<Object, String, String, String> consoleOutput = new ConsoleOutput().encode(String::toString);
    addShadowOutput(consoleOutput);

    ShadowOutput<JsonObject, JsonObject, String, String> httpJsonOutput = new HttpJsonOutput()
      .config(new JsonObject()
        .put("defaultURI", "/")
        .put("hosts", new JsonArray().add("127.0.0.1:8081")))
      .tokenFunction((data, config) -> Optional.ofNullable(data.getString("defaultURI", config.getString("defaultURI"))))
      .encode(JsonObject::encode);
    addShadowOutput(httpJsonOutput);

    //es output
    ShadowOutput<JsonObject, JsonObject, String, String> esOutput = new HttpJsonOutput()
      .config(new JsonObject()
        .put("defaultURI", "/default/logs")
        .put("hosts", new JsonArray().add("10.10.10.149:9200")))
      .tokenFunction((data, config) -> Optional.ofNullable(data.getString("URI", config.getString("defaultURI"))))
      .handler((data, config) -> {
        //delete redundancy field.
        data.remove("URI");
        return data;
      })
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

    //leapcloud rpc trace kafka input
    addShadowInput(new KafkaInput<String, String, JsonObject, JsonObject>()
      .config(new JsonObject()
        .put("props", kafkaConfig)
        .put("topics", new JsonArray().add("leapCloudRPCTrace"))
        .put("pollTimeout", 10000))
      .matchFunction((record, config) -> config.getJsonArray("topics").getList().contains(record.topic()))
      .decode(record -> new JsonObject(record.value()))
      .handler((value, config) -> {
        JsonObject traceLog = new JsonObject();

        JsonObject rpcJson = new JsonObject(value.getString("message"));
        OffsetDateTime startTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(rpcJson.getLong("startTime")), ZoneId.of("GMT"));
        OffsetDateTime endTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(rpcJson.getLong("endTime")), ZoneId.of("GMT"));
        String startTimeStr = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(startTime);
        String endTimeStr = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(endTime);
        //
        traceLog.put("@timestamp", startTimeStr);
        traceLog.put("traceID", rpcJson.getString("traceID"));
        traceLog.put("SPI", rpcJson.getString("serviceName"));
        traceLog.put("methodName", rpcJson.getString("methodName"));
        traceLog.put("args", rpcJson.getString("args"));
        traceLog.put("response", rpcJson.getString("response"));
        if (rpcJson.getString("exceptionMessage") != null) {
          traceLog.put("exceptionMessage", rpcJson.getString("exceptionMessage"));
        } else {
          traceLog.put("isSuccess", true);
        }
        traceLog.put("startTime", startTimeStr);
        traceLog.put("endTime", endTimeStr);
        traceLog.put("spentTime", rpcJson.getInteger("spentTime"));
        traceLog.put("isBegin", rpcJson.getBoolean("isBegin", false));

        traceLog.put("source_host", value.getJsonObject("beat").getString("hostname"));
        traceLog.put("source_ip", value.getJsonObject("fields").getString("ip"));
        //get service name
        String[] pathArray = value.getString("source").split("/");
        String serviceName = pathArray[4].toLowerCase();
        traceLog.put("serviceName", serviceName);
        //
        String monthNumber = startTime.getMonthValue() < 10 ? "0" + startTime.getMonthValue() : "" + startTime.getMonthValue();
        String dayNumber = startTime.getDayOfMonth() < 10 ? "0" + startTime.getDayOfMonth() : "" + startTime.getDayOfMonth();
        traceLog.put("URI", "/tools-rpctrace-" + startTime.getYear() + "." + monthNumber + "." + dayNumber + "/rpctrace");
        return traceLog;
      })
      .addOutput(esOutput));

    //leapcloud log kafka input
    addShadowInput(new KafkaInput<String, String, JsonObject, JsonObject>()
      .config(new JsonObject()
        .put("props", kafkaConfig)
        .put("topics", new JsonArray().add("leapCloudLog"))
        .put("pollTimeout", 10000))
      .matchFunction((record, config) -> config.getJsonArray("topics").getList().contains(record.topic()))
      .decode(record -> new JsonObject(record.value()))
      .handler((value, config) -> {
        //file beat
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

        String[] pathArray = value.getString("source").split("/");
        String esDate = dateTimeStr.split(" ")[0].replace("-", ".");
        String serviceName = pathArray[4].toLowerCase();
        logContent.put("URI", "/" + pathArray[3] + "-" + serviceName + "-" + esDate + "/" + pathArray[4]);
        //for log stash report
        logContent.put("serviceName", serviceName);
        return logContent;
      })
      .addOutput(esOutput));

    //leapcloud nginx log kafka input
    addShadowInput(new KafkaInput<String, String, JsonObject, JsonObject>()
      .config(new JsonObject()
        .put("props", kafkaConfig)
        .put("topics", new JsonArray().add("nginxLog"))
        .put("pollTimeout", 10000))
      .matchFunction((record, config) -> config.getJsonArray("topics").getList().contains(record.topic()))
      .decode(record -> {
        //把\x22全部替换成单引号,并且转换成json对象
        String jsonStr = record.value().replaceAll("\\\\x22", "'");
        return new JsonObject(jsonStr);
      })
      .handler((value, config) -> {
        //过滤掉request_body里的password
        String requestBody = value.getString("request_body");
        if (requestBody != null && requestBody.contains("'password':")) {
          value.put("request_body", maskKeyString(requestBody, "'password':", "hidden"));
        }
        return value;
      }).addOutput(esOutput));
  }
}
