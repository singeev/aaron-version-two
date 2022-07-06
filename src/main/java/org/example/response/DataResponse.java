package org.example.response;

import java.util.List;
import lombok.Data;

@Data
public class DataResponse {
  private Error error;
  private Success success;
  private Contents contents;

  @Data
  public static class Success {
    private int total;
  }

  @Data
  public static class Contents {
    private List<Category> categories;
    private List<JokeMeta> jokes;
    private String copyright;
  }
}
