package cn.leapcloud.shadow.plugins.kafka;

import cn.leapcloud.shadow.ShadowInput;
import cn.leapcloud.shadow.impl.input.AbsShadowInput;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Created by stream.
 */
public class KafkaInput<K, V, T, R>
  extends AbsShadowInput<ConsumerRecord<K, V>, T, R>
  implements ShadowInput<ConsumerRecord<K, V>, T, R> {

  private boolean isRunning;
  private KafkaConsumer<K, V> consumer;

  @Override
  public Future<Void> start(Vertx vertx) {
    //TODO topics
    JsonArray topics = config.getJsonArray("topics");
    int timeout = config.getInteger("pollTimeout", 10000);

    //consumer message
    this.consumer = new KafkaConsumer<>(config.getJsonObject("props").getMap());
    consumer.subscribe(topics.getList());
    new Thread(() -> {
      while (isRunning) {
        ConsumerRecords<K, V> records = consumer.poll(timeout);
        records.forEach(this::accept);
      }
    }, "KafkaConsumerThread").start();
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> stop() {
    isRunning = false;
    this.consumer.close();
    return Future.succeededFuture();
  }

}
