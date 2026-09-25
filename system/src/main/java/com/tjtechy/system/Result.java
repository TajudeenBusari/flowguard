package com.tjtechy.system;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result {
  @JsonProperty("message")
  private String message;
  @JsonProperty("flag")
  private boolean flag;
  @JsonProperty("data")
  private Object data;
  @JsonProperty("code")
  private Integer code;

  /** Because the Lombok will not handle the constructor with only 3 arguments,
   * we need to create a custom constructor for that.
   * That is construct a new Result object with the specified message, flag, and code.
   */
  public Result(String message, boolean flag, Integer code) {
    this.message = message;
    this.flag = flag;
    this.code = code;
  }
}
