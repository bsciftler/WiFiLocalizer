package io.swagger.client.model;

import io.swagger.client.model.Probe;

import io.swagger.annotations.*;
import com.google.gson.annotations.SerializedName;


@ApiModel(description = "")
public class InlineResponse200  {
  
  @SerializedName("data")
  private Probe data = null;
  @SerializedName("count")
  private Integer count = null;

  /**
   **/
  @ApiModelProperty(value = "")
  public Probe getData() {
    return data;
  }
  public void setData(Probe data) {
    this.data = data;
  }

  /**
   * Number of probes
   **/
  @ApiModelProperty(value = "Number of probes")
  public Integer getCount() {
    return count;
  }
  public void setCount(Integer count) {
    this.count = count;
  }


  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineResponse200 {\n");
    
    sb.append("  data: ").append(data).append("\n");
    sb.append("  count: ").append(count).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}
