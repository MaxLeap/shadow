import cn.leapcloud.shadow.AbsShadowDSL;
import cn.leapcloud.shadow.impl.input.HttpJsonInput;
import cn.leapcloud.shadow.impl.output.ConsoleFutureOutput;
import cn.leapcloud.shadow.impl.output.ConsoleOutput;
import cn.leapcloud.shadow.impl.output.HttpJsonOutput;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Created by stream.
 */
public class Shadow extends AbsShadowDSL {

  @Override
  protected void start(JsonObject config) {
    //output
    addShadowOutput("console", new ConsoleOutput());
    addShadowOutput("consoleFuture", new ConsoleFutureOutput());

    addShadowOutput("httpJsonOutput", new HttpJsonOutput()
      .config(config.getJsonObject("httpJsonOutput"))
      .tokenFunction((data, outputConfig) ->
        Optional.ofNullable(data.getString("targetURI"))));

    //input
    addShadowInput("httpJsonInput", new HttpJsonInput()
      .config(config.getJsonObject("httpJsonInput"))
      .matchFunction((request, inputConfig) -> {
        if (HttpMethod.POST == request.method()) {
          @SuppressWarnings("unchecked")
          List<String> uriPatterns = inputConfig.getJsonArray("uriPatterns").getList();
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
      .addOutput(getOutput("consoleFuture")));

  }
}
