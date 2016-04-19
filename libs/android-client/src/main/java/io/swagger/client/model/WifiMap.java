package io.swagger.client.model;


import io.swagger.annotations.*;
import com.google.gson.annotations.SerializedName;


@ApiModel(description = "")
public class WifiMap  {
  
  @SerializedName("map_id")
  private Integer mapId = null;
  @SerializedName("name")
  private String name = null;

  /**
   * global unique id of a specific map
   **/
  @ApiModelProperty(value = "global unique id of a specific map")
  public Integer getMapId() {
    return mapId;
  }
  public void setMapId(Integer mapId) {
    this.mapId = mapId;
  }

  /**
   * human readable name of the map
   **/
  @ApiModelProperty(value = "human readable name of the map")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }


  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WifiMap {\n");
    
    sb.append("  mapId: ").append(mapId).append("\n");
    sb.append("  name: ").append(name).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
