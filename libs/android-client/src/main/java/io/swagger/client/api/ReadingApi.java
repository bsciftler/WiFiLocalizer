package io.swagger.client.api;

import io.swagger.client.ApiExceptionAndroid;
import io.swagger.client.ApiInvokerAndroid;
import io.swagger.client.PairAndroid;

import java.util.*;

import io.swagger.client.model.Reading;

import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.util.Map;
import java.util.HashMap;


public class ReadingApi {
  String basePath = "http://192.168.1.123:8080/v1";
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
   * Get a list of readings for a map ID
   *
   * @param mapId Map ID
   * @param dataSince ISO-formatted date string to get readings sent to cloud after date
   * @return List<Reading>
   */
  public List<Reading>  readingsGet (Integer mapId, String dataSince) throws ApiExceptionAndroid {
    Object localVarPostBody = null;

    // verify the required parameter 'mapId' is set
    if (mapId == null) {
       throw new ApiExceptionAndroid(400, "Missing the required parameter 'mapId' when calling readingsGet");
    }


    // create path and map variables
    String localVarPath = "/readings".replaceAll("\\{format\\}","json");

    // query params
    List<PairAndroid> localVarQueryParams = new ArrayList<PairAndroid>();
    // header params
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    // form params
    Map<String, String> localVarFormParams = new HashMap<String, String>();

    localVarQueryParams.addAll(ApiInvokerAndroid.parameterToPairs("", "map_id", mapId));
    localVarQueryParams.addAll(ApiInvokerAndroid.parameterToPairs("", "data_since", dataSince));


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
        return (List<Reading>) ApiInvokerAndroid.deserialize(localVarResponse, "array", Reading.class);
      }
      else {
        return null;
      }
    } catch (ApiExceptionAndroid ex) {
      throw ex;
    }
  }
  /**
   * Insert readings
   *
   * @param data Readings to append
   * @return List<Integer>
   */
  public List<Integer>  readingsPost (List<Reading> data) throws ApiExceptionAndroid {
    Object localVarPostBody = data;

    // verify the required parameter 'data' is set
    if (data == null) {
       throw new ApiExceptionAndroid(400, "Missing the required parameter 'data' when calling readingsPost");
    }


    // create path and map variables
    String localVarPath = "/readings".replaceAll("\\{format\\}","json");

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
      String localVarResponse = apiInvoker.invokeAPI(basePath, localVarPath, "POST", localVarQueryParams, localVarPostBody, localVarHeaderParams, localVarFormParams, localVarContentType);
      if(localVarResponse != null){
        return (List<Integer>) ApiInvokerAndroid.deserialize(localVarResponse, "array", Integer.class);
      }
      else {
        return null;
      }
    } catch (ApiExceptionAndroid ex) {
      throw ex;
    }
  }
}
