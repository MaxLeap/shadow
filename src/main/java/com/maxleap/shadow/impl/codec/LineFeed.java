package com.maxleap.shadow.impl.codec;

import com.maxleap.shadow.ShadowCodec;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stream.
 */
public class LineFeed implements ShadowCodec<Buffer, List<LineFeed.LineFeedMeta>> {

  @Override
  public List<LineFeedMeta> translate(Buffer data) {
    List<LineFeedMeta> lineFeedMetaList = new ArrayList<>();
    RecordParser.newDelimited(System.lineSeparator(), result -> lineFeedMetaList.add(new LineFeedMeta(result.length() + 1, result.toString()))).handle(data);
    return lineFeedMetaList;
  }

  public class LineFeedMeta {

    private int sourceBufferSize;
    private String result;

    LineFeedMeta(int sourceBufferSize, String result) {
      this.sourceBufferSize = sourceBufferSize;
      this.result = result;
    }

    public void setSourceBufferSize(int sourceBufferSize) {
      this.sourceBufferSize = sourceBufferSize;
    }

    public void setResult(String result) {
      this.result = result;
    }

    public int getSourceBufferSize() {
      return sourceBufferSize;
    }

    public String getResult() {
      return result;
    }
  }

}


