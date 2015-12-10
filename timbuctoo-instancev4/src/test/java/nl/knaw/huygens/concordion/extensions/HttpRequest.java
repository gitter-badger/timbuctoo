package nl.knaw.huygens.concordion.extensions;

import java.util.AbstractMap;
import java.util.List;

public class HttpRequest {
  public final String method;
  public final String url;
  public final List<AbstractMap.SimpleEntry<String, String>> headers;
  public final String body;

  public HttpRequest(String method, String url, List<AbstractMap.SimpleEntry<String, String>> headers, String body) {
    this.method = method;
    this.url = url;
    this.headers = headers;
    this.body = body;
  }
}
