package org.example.response;

import lombok.Data;

@Data
public class JokeMeta {
    private String description;
    private String language;
    private String background;
    private String category;
    private String date;
    private Joke joke;

    @Data
  public static class Joke {
    private String title;
    private String lang;
    private String length;
    private String clean;
    private String racial;
    private String id;
    private String text;
    private String date;
  }
}