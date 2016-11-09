import cn.leapcloud.shadow.AbsShadowDSL;
import cn.leapcloud.shadow.ShadowOutput;
import cn.leapcloud.shadow.impl.input.HttpJsonInput;
import cn.leapcloud.shadow.impl.output.ConsoleOutput;
import cn.leapcloud.shadow.impl.output.HttpJsonOutput;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

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
    addShadowOutput("console", consoleOutput);

    ShadowOutput<JsonObject, String, String> httpJsonOutput = new HttpJsonOutput()
      .config(new JsonObject()
        .put("defaultURI", "/")
        .put("hosts", new JsonArray().add("127.0.0.1:8081")))
      .tokenFunction((data, config) -> Optional.ofNullable(data.getString("defaultURI", config.getString("defaultURI"))))
      .encode(JsonObject::encode);
    addShadowOutput("httpJsonOutput", httpJsonOutput);

    //input
    addShadowInput("httpJsonInput", new HttpJsonInput()
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
  }
}
