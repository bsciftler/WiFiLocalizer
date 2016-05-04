package io.swagger.client.api;

import io.swagger.client.ApiExceptionAndroid;
import io.swagger.client.ApiInvokerAndroid;
import io.swagger.client.PairAndroid;

import java.util.*;

import io.swagger.client.model.WifiMap;

import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.util.Map;
import java.util.HashMap;


public class WifiMapApi {
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
   * Get all of the maps the cloud server knows about
   *
   * @return List<WifiMap>
   */
  public List<WifiMap>  mapsAllGet () throws ApiExceptionAndroid {
    Object localVarPostBody = null;


    // create path and map variables
    String localVarPath = "/maps/_all".replaceAll("\\{format\\}","json");

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
        return (List<WifiMap>) ApiInvokerAndroid.deserialize(localVarResponse, "array", WifiMap.class);
      }
      else {
        return null;
      }
    } catch (ApiExceptionAndroid ex) {
      throw ex;
    }
  }

  /**
   * Get a map by ID or exact name
   *
   * @param id Cloud Map ID
   * @param name Exact name of Map
   * @return List<WifiMap>
   */
  public List<WifiMap>  mapsGet (Integer id, String name) throws ApiExceptionAndroid {
    Object localVarPostBody = null;


    // create path and map variables
    String localVarPath = "/maps".replaceAll("\\{format\\}","json");

    // query params
    List<PairAndroid> localVarQueryParams = new ArrayList<PairAndroid>();
    // header params
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    // form params
    Map<String, String> localVarFormParams = new HashMap<String, String>();


    localVarQueryParams.addAll(ApiInvokerAndroid.parameterToPairs("", "id", id));

    localVarQueryParams.addAll(ApiInvokerAndroid.parameterToPairs("", "name", name));




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
        return (List<WifiMap>) ApiInvokerAndroid.deserialize(localVarResponse, "array", WifiMap.class);
      }
      else {
        return null;
      }
    } catch (ApiExceptionAndroid ex) {
      throw ex;
    }
  }

  /**
   * Add a new map name
   *
   * @param mapName
   * @return WifiMap
   */
  public WifiMap  mapsPost (String mapName) throws ApiExceptionAndroid {
    Object localVarPostBody = null;

    // verify the required parameter 'mapName' is set
    if (mapName == null) {
       throw new ApiExceptionAndroid(400, "Missing the required parameter 'mapName' when calling controllersDefaultControllerMapsPost");
    }


    // create path and map variables
    String localVarPath = "/maps".replaceAll("\\{format\\}","json");

    // query params
    List<PairAndroid> localVarQueryParams = new ArrayList<PairAndroid>();
    // header params
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    // form params
    Map<String, String> localVarFormParams = new HashMap<String, String>();


    localVarQueryParams.addAll(ApiInvokerAndroid.parameterToPairs("", "map_name", mapName));




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
        return (WifiMap) ApiInvokerAndroid.deserialize(localVarResponse, "", WifiMap.class);
      }
      else {
        return null;
      }
    } catch (ApiExceptionAndroid ex) {
      throw ex;
    }
  }

  /**
   * Search for a map given an idea of its name
   *
   * @param keywords case-insensitive keywords
   * @return List<WifiMap>
   */
  public List<WifiMap>  mapsSearchGet (String keywords) throws ApiExceptionAndroid {
    Object localVarPostBody = null;

    // verify the required parameter 'keywords' is set
    if (keywords == null) {
       throw new ApiExceptionAndroid(400, "Missing the required parameter 'keywords' when calling controllersDefaultControllerMapsSearchGet");
    }


    // create path and map variables
    String localVarPath = "/maps/search".replaceAll("\\{format\\}","json");

    // query params
    List<PairAndroid> localVarQueryParams = new ArrayList<PairAndroid>();
    // header params
    Map<String, String> localVarHeaderParams = new HashMap<String, String>();
    // form params
    Map<String, String> localVarFormParams = new HashMap<String, String>();


    localVarQueryParams.addAll(ApiInvokerAndroid.parameterToPairs("", "keywords", keywords));




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
        return (List<WifiMap>) ApiInvokerAndroid.deserialize(localVarResponse, "array", WifiMap.class);
      }
      else {
        return null;
      }
    } catch (ApiExceptionAndroid ex) {
      throw ex;
    }
  }

}
