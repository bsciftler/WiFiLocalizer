package io.swagger.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import io.swagger.client.model.*;

public class JsonUtilAndroid {
  public static GsonBuilder gsonBuilder;

  static {
    gsonBuilder = new GsonBuilder();
    gsonBuilder.serializeNulls();
    gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  }

  public static Gson getGson() {
    return gsonBuilder.create();
  }

  public static String serialize(Object obj){
    return getGson().toJson(obj);
  }

  public static <T> T deserializeToList(String jsonString, Class cls){
    return getGson().fromJson(jsonString, getListTypeForDeserialization(cls));
  }

  public static <T> T deserializeToObject(String jsonString, Class cls){
    return getGson().fromJson(jsonString, getTypeForDeserialization(cls));
  }

  public static Type getListTypeForDeserialization(Class cls) {
    String className = cls.getSimpleName();

    if ("AccessPoint".equalsIgnoreCase(className)) {
      return new TypeToken<List<AccessPoint>>(){}.getType();
    }

    if ("ApiError".equalsIgnoreCase(className)) {
      return new TypeToken<List<ApiError>>(){}.getType();
    }

    if ("ProbeRequest".equalsIgnoreCase(className)) {
      return new TypeToken<List<ProbeRequest>>(){}.getType();
    }

    if ("Reading".equalsIgnoreCase(className)) {
      return new TypeToken<List<Reading>>(){}.getType();
    }

    if ("WifiBitmap".equalsIgnoreCase(className)) {
      return new TypeToken<List<WifiBitmap>>(){}.getType();
    }

    if ("WifiMap".equalsIgnoreCase(className)) {
      return new TypeToken<List<WifiMap>>(){}.getType();
    }

    return new TypeToken<List<Object>>(){}.getType();
  }

  public static Type getTypeForDeserialization(Class cls) {
    String className = cls.getSimpleName();

    if ("AccessPoint".equalsIgnoreCase(className)) {
      return new TypeToken<AccessPoint>(){}.getType();
    }

    if ("ApiError".equalsIgnoreCase(className)) {
      return new TypeToken<ApiError>(){}.getType();
    }

    if ("ProbeRequest".equalsIgnoreCase(className)) {
      return new TypeToken<ProbeRequest>(){}.getType();
    }

    if ("Reading".equalsIgnoreCase(className)) {
      return new TypeToken<Reading>(){}.getType();
    }

    if ("WifiBitmap".equalsIgnoreCase(className)) {
      return new TypeToken<WifiBitmap>(){}.getType();
    }

    if ("WifiMap".equalsIgnoreCase(className)) {
      return new TypeToken<WifiMap>(){}.getType();
    }

    return new TypeToken<Object>(){}.getType();
  }

};
