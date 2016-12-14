package cn.leapcloud.shadow;

/**
 * Created by stream.
 */
public class ShadowException extends Exception {

  public ShadowException() {
  }

  public ShadowException(String message) {
    super(message);
  }

  public ShadowException(String message, Throwable cause) {
    super(message, cause);
  }
}
