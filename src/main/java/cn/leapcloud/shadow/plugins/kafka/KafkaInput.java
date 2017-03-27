package cn.leapcloud.shadow.plugins.kafka;

import cn.leapcloud.shadow.ShadowInput;
import cn.leapcloud.shadow.impl.input.AbsShadowInput;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * Created by stream.
 */
public class KafkaInput<K, V, T, R>
  extends AbsShadowInput<ConsumerRecord<K, V>, T, R>
  implements ShadowInput<ConsumerRecord<K, V>, T, R> {

  private boolean isRunning = true;
  private KafkaConsumer<K, V> consumer;
  private static final Logger logger = LoggerFactory.getLogger(KafkaInput.class);

  @Override
  public Future<Void> start(Vertx vertx) {
    JsonArray topics = config.getJsonArray("topics");
    int timeout = config.getInteger("pollTimeout", 15000);

    //consumer message
    this.consumer = new KafkaConsumer<>(config.getJsonObject("props").getMap());
    consumer.subscribe(topics.getList());
    new Thread(() -> {
      while (isRunning) {
        ConsumerRecords<K, V> records = consumer.poll(timeout);
        records.forEach(this);
      }
    }, "KafkaConsumerThread").start();
    logger.info("Start kafka consumer, on broker " + config.getJsonObject("props").getString("bootstrap.servers"));
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> stop() {
    isRunning = false;
    this.consumer.close();
    return Future.succeededFuture();
  }

}
