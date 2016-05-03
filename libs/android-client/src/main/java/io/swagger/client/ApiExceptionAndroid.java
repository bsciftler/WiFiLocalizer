package io.swagger.client;

public class ApiExceptionAndroid extends Exception {
  int code = 0;
  String message = null;

  public ApiExceptionAndroid() {}

  public ApiExceptionAndroid(int code, String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
