package io.swagger.client.model;


import io.swagger.annotations.*;
import com.google.gson.annotations.SerializedName;


@ApiModel(description = "")
public class Reading  {
  
  @SerializedName("timestamp")
  private String timestamp = null;
  @SerializedName("map_x")
  private Double mapX = null;
  @SerializedName("map_y")
  private Double mapY = null;
  @SerializedName("rssi")
  private Integer rssi = null;
  @SerializedName("mac_address")
  private String macAddress = null;
  @SerializedName("map_id")
  private Integer mapId = null;

  /**
   * Time of capture
   **/
  @ApiModelProperty(value = "Time of capture")
  public String getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  /**
   * x-coordinate in pixels on corresponding bitmap
   **/
  @ApiModelProperty(value = "x-coordinate in pixels on corresponding bitmap")
  public Double getMapX() {
    return mapX;
  }
  public void setMapX(Double mapX) {
    this.mapX = mapX;
  }

  /**
   * y-coordinate in pixels on corresponding bitman
   **/
  @ApiModelProperty(value = "y-coordinate in pixels on corresponding bitman")
  public Double getMapY() {
    return mapY;
  }
  public void setMapY(Double mapY) {
    this.mapY = mapY;
  }

  /**
   * received signal strength intensity of reading
   **/
  @ApiModelProperty(value = "received signal strength intensity of reading")
  public Integer getRssi() {
    return rssi;
  }
  public void setRssi(Integer rssi) {
    this.rssi = rssi;
  }

  /**
   * Media Access Control (MAC) address of reading
   **/
  @ApiModelProperty(value = "Media Access Control (MAC) address of reading")
  public String getMacAddress() {
    return macAddress;
  }
  public void setMacAddress(String macAddress) {
    this.macAddress = macAddress;
  }

  /**
   * valid global unique id of a specific map
   **/
  @ApiModelProperty(value = "valid global unique id of a specific map")
  public Integer getMapId() {
    return mapId;
  }
  public void setMapId(Integer mapId) {
    this.mapId = mapId;
  }


  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Reading {\n");
    
    sb.append("  timestamp: ").append(timestamp).append("\n");
    sb.append("  mapX: ").append(mapX).append("\n");
    sb.append("  mapY: ").append(mapY).append("\n");
    sb.append("  rssi: ").append(rssi).append("\n");
    sb.append("  macAddress: ").append(macAddress).append("\n");
    sb.append("  mapId: ").append(mapId).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
