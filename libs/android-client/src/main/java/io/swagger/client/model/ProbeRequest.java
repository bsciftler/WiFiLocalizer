package io.swagger.client.model;


import io.swagger.annotations.*;
import com.google.gson.annotations.SerializedName;


@ApiModel(description = "")
public class ProbeRequest  {
  
  @SerializedName("map_x")
  private Double mapX = null;
  @SerializedName("map_y")
  private Double mapY = null;
  @SerializedName("fingerprint")
  private String fingerprint = null;
  @SerializedName("rssi")
  private Integer rssi = null;
  @SerializedName("map_id")
  private Integer mapId = null;

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
   * device Media Access Control address
   **/
  @ApiModelProperty(value = "device Media Access Control address")
  public String getFingerprint() {
    return fingerprint;
  }
  public void setFingerprint(String fingerprint) {
    this.fingerprint = fingerprint;
  }

  /**
   * received signal strength intensity of probe request
   **/
  @ApiModelProperty(value = "received signal strength intensity of probe request")
  public Integer getRssi() {
    return rssi;
  }
  public void setRssi(Integer rssi) {
    this.rssi = rssi;
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
    sb.append("class ProbeRequest {\n");
    
    sb.append("  mapX: ").append(mapX).append("\n");
    sb.append("  mapY: ").append(mapY).append("\n");
    sb.append("  fingerprint: ").append(fingerprint).append("\n");
    sb.append("  rssi: ").append(rssi).append("\n");
    sb.append("  mapId: ").append(mapId).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
