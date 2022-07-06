package org.example.response;

import lombok.Data;

@Data
public class Error {
  private int code;
  private String message;
}
