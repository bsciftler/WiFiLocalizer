package io.swagger.client.model;


import io.swagger.annotations.*;
import com.google.gson.annotations.SerializedName;


@ApiModel(description = "")
public class AccessPoint  {
  
  @SerializedName("mac_address")
  private String macAddress = null;

  /**
   * Media Access Control (MAC) Address of an access point
   **/
  @ApiModelProperty(value = "Media Access Control (MAC) Address of an access point")
  public String getMacAddress() {
    return macAddress;
  }
  public void setMacAddress(String macAddress) {
    this.macAddress = macAddress;
  }


  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccessPoint {\n");
    
    sb.append("  macAddress: ").append(macAddress).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
