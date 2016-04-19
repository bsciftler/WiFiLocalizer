package io.swagger.client.model;


import io.swagger.annotations.*;
import com.google.gson.annotations.SerializedName;


@ApiModel(description = "")
public class Probe  {
  
  @SerializedName("fingerprint")
  private String fingerprint = null;

  /**
   **/
  @ApiModelProperty(value = "")
  public String getFingerprint() {
    return fingerprint;
  }
  public void setFingerprint(String fingerprint) {
    this.fingerprint = fingerprint;
  }


  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Probe {\n");
    
    sb.append("  fingerprint: ").append(fingerprint).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
