package io.swagger.client.api;

import io.swagger.client.ApiExceptionAndroid;
import io.swagger.client.ApiInvokerAndroid;
import io.swagger.client.PairAndroid;

import java.util.*;

import io.swagger.client.model.WifiBitmap;

import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.util.Map;
import java.util.HashMap;


public class WifiBitmapApi {
  String basePath = "http://192.168.1.123/v1";
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
   * Get a base64 encoded string of a map&#39;s bitmap image
   *
   * @param id Valid Map ID
   * @return WifiBitmap
   */
  public WifiBitmap  bitmapGet (Integer id) throws ApiExceptionAndroid {
    Object localVarPostBody = null;

    // verify the required parameter 'id' is set
    if (id == null) {
       throw new ApiExceptionAndroid(400, "Missing the required parameter 'id' when calling bitmapGet");
    }


    // create path and map variables
    String localVarPath = "/bitmap".replaceAll("\\{format\\}","json");

    // query params
    List<PairAndroid> localVarQueryParams = new ArrayList<PairAndroid>();
    // header params
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    // form params
    Map<String, String> localVarFormParams = new HashMap<String, String>();

    localVarQueryParams.addAll(ApiInvokerAndroid.parameterToPairs("", "id", id));


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
        return (WifiBitmap) ApiInvokerAndroid.deserialize(localVarResponse, "", WifiBitmap.class);
      }
      else {
        return null;
      }
    } catch (ApiExceptionAndroid ex) {
      throw ex;
    }
  }
  /**
   * Add a new bitmap for an image. There can only be at max one bitmap per image.
   *
   * @param data
   * @return WifiBitmap
   */
  public WifiBitmap  bitmapPost (WifiBitmap data) throws ApiExceptionAndroid {
    Object localVarPostBody = data;


    // create path and map variables
    String localVarPath = "/bitmap".replaceAll("\\{format\\}","json");

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
        return (WifiBitmap) ApiInvokerAndroid.deserialize(localVarResponse, "", WifiBitmap.class);
      }
      else {
        return null;
      }
    } catch (ApiExceptionAndroid ex) {
      throw ex;
    }
  }
}
