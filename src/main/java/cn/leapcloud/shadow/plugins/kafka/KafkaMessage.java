package cn.leapcloud.shadow.plugins.kafka;

/**
 * Created by stream.
 */
public class KafkaMessage<K, V> {

  private final K key;
  private final V value;

  public KafkaMessage(K key, V value) {
    this.key = key;
    this.value = value;
  }

  public K key() {
    return key;
  }

  public V value() {
    return value;
  }
}
