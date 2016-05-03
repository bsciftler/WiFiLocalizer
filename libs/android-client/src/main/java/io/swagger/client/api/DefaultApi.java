package io.swagger.client.api;

import io.swagger.client.ApiExceptionAndroid;
import io.swagger.client.ApiInvokerAndroid;
import io.swagger.client.PairAndroid;

import java.util.*;

import io.swagger.client.model.InlineResponse200;

import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.util.Map;
import java.util.HashMap;


public class DefaultApi {
  String basePath = "https://172.16.42.1:8000";
  ApiInvokerAndroid apiInvoker = ApiInvokerAndroid.getInstance();

  public void addHeader(String key, String value) {
    getInvoker().addDefaultHeader(key, value);
  }

  public ApiInvokerAndroid getInvoker() {
    return apiInvoker;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public String getBasePath() {
    return basePath;
  }

  /**
   * Gets probes since last time endpoint was hit. Note, since the \nPineapple is not connected to the internet, any timestamp-based \ncode or data is not reliable since the clock drifts a lot.\n
   *
   * @return InlineResponse200
   */
  public InlineResponse200  probesGet () throws ApiExceptionAndroid {
    Object localVarPostBody = null;


    // create path and map variables
    String localVarPath = "/probes".replaceAll("\\{format\\}","json");

    // query params
    List<PairAndroid> localVarQueryParams = new ArrayList<PairAndroid>();
    // header params
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    // form params
    Map<String, String> localVarFormParams = new HashMap<String, String>();



    String[] localVarContentTypes = {

    };
    String localVarContentType = localVarContentTypes.length > 0 ? localVarContentTypes[0] : "application/json";

    if (localVarContentType.startsWith("multipart/form-data")) {
      // file uploading
      MultipartEntityBuilder localVarBuilder = MultipartEntityBuilder.create();


      localVarPostBody = localVarBuilder.build();
    } else {
      // normal form params
          }

    try {
      String localVarResponse = apiInvoker.invokeAPI(basePath, localVarPath, "GET", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarContentType);
      if(localVarResponse != null){
        return (InlineResponse200) ApiInvokerAndroid.deserialize(localVarResponse, "", InlineResponse200.class);
      }
      else {
        return null;
      }
    } catch (ApiExceptionAndroid ex) {
      throw ex;
    }
  }
}
