package cn.leapcloud.shadow.plugins.elasticsearch;

import io.vertx.core.json.JsonObject;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by stream.
 */
public class ES5Output {

  public static void main(String[] args) throws UnknownHostException, InterruptedException {
    CountDownLatch countDownLatch = new CountDownLatch(1);

    TransportClient client = new PreBuiltTransportClient(Settings.builder().put("cluster.name", "es-leapcloud").build())
      .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.10.196"), 9300));

    //TODO 后期考虑使用BulkRequest,15秒刷一次，或者100条记录刷一次
    IndexRequest indexRequest = new IndexRequest("test-index", "type");
    indexRequest.source(new JsonObject().put("test", "test").put("haha", 1).encode());

    client.index(indexRequest, new ActionListener<IndexResponse>() {
      @Override
      public void onResponse(IndexResponse indexResponse) {
        System.out.println(indexResponse.toString());
        System.out.println("创建文档成功");
      }

      @Override
      public void onFailure(Exception e) {
        System.out.println("创建文档失败");
        e.printStackTrace();
      }
    });

    countDownLatch.await();
  }
}
