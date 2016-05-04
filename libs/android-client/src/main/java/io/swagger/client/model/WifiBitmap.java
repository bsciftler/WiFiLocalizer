package io.swagger.client.model;


import io.swagger.annotations.*;
import com.google.gson.annotations.SerializedName;


@ApiModel(description = "")
public class WifiBitmap  {
  
  @SerializedName("map_id")
  private Integer mapId = null;
  @SerializedName("data")
  private String data = null;

  
  /**
   * global unique id of a specific map
   **/
  @ApiModelProperty(required = true, value = "global unique id of a specific map")
  public Integer getMapId() {
    return mapId;
  }
  public void setMapId(Integer mapId) {
    this.mapId = mapId;
  }

  
  /**
   * base64 encoding of an ImageView compatible image e.g. jpg
   **/
  @ApiModelProperty(required = true, value = "base64 encoding of an ImageView compatible image e.g. jpg")
  public String getData() {
    return data;
  }
  public void setData(String data) {
    this.data = data;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WifiBitmap {\n");
    
    sb.append("  mapId: ").append(mapId).append("\n");
    sb.append("  data: ").append(data).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
