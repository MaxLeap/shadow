package cn.leapcloud.shadow.plugins.elasticsearch;

import java.net.UnknownHostException;

/**
 * Created by stream.
 */
public class ES5Output {
  public static void main(String[] args) throws UnknownHostException {
    //
//    TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
//      .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("10.10.10.196"), 9200));

    String jsonStr = "sdsdsd'password':'scacasdsd',bdsdad1";

    System.out.println(maskKeyString(jsonStr, "'password':", "hidden"));




  }

  private static String maskKeyString(String content, String prefixString, String maskString) {
    int passwordPosition = content.indexOf(prefixString);
    int prefixStringLength = prefixString.length();
    int maskStringLength = maskString.length();
    return content.substring(0, passwordPosition + prefixStringLength)
      + maskString + content.substring(passwordPosition + prefixStringLength + maskStringLength);
  }
}
