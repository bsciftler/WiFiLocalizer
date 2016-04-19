package io.swagger.client.model;

import io.swagger.client.model.Probe;
import java.util.*;

import io.swagger.annotations.*;
import com.google.gson.annotations.SerializedName;


@ApiModel(description = "")
public class InlineResponse200  {
  
  @SerializedName("data")
  private List<Probe> data = null;
  @SerializedName("count")
  private Integer count = null;

  /**
   **/
  @ApiModelProperty(value = "")
  public List<Probe> getData() {
    return data;
  }
  public void setData(List<Probe> data) {
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
