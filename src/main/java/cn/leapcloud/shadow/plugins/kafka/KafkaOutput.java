package cn.leapcloud.shadow.plugins.kafka;

import cn.leapcloud.shadow.ShadowOutput;
import cn.leapcloud.shadow.impl.output.AbsShadowOutput;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by stream.
 */
public class KafkaOutput<K, V, IN>
  extends AbsShadowOutput<IN, KafkaMessage<K, V>, KafkaMessage<K, V>, String>
  implements ShadowOutput<IN, KafkaMessage<K, V>, KafkaMessage<K, V>, String> {

  private KafkaProducer<K, V> producer;
  private String defaultTopic;
  private Vertx vertx;

  private static final Logger logger = LoggerFactory.getLogger(KafkaOutput.class);

  @Override
  public Future<Void> start(Vertx vertx) {
    this.vertx = vertx;
    this.defaultTopic = config.getString("topic");
    Future<Void> future = Future.future();
    vertx.executeBlocking(event -> {
      this.producer = new KafkaProducer<>(config.getJsonObject("props").getMap());
      event.complete();
    }, false, event -> future.completer());
    return future;
  }

  @Override
  public void accept(Optional<String> topicOption, KafkaMessage<K, V> result) {
    String topic = topicOption.orElse(defaultTopic);
    vertx.executeBlocking(event -> {
      ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, result.key(), result.value());
      producer.send(producerRecord, (metadata, exception) -> {
        if (exception != null) {
          logger.error(exception.getMessage(), exception);
        }
      });
    }, false, event -> {
      if (event.failed()) logger.error("send kafka message failed.", event.cause());
    });

  }

  @Override
  public Future<Void> stop() {
    Future<Void> future = Future.future();
    vertx.executeBlocking(event -> {
      producer.flush();
      producer.close(5, TimeUnit.SECONDS);
      event.complete();
    }, false, future.completer());
    return future;
  }


}
