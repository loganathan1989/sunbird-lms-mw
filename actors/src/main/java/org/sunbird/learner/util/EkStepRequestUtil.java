/**
 * 
 */
package org.sunbird.learner.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sunbird.common.models.util.ConfigUtil;
import org.sunbird.common.models.util.HttpUtil;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;

/**
 * This class will make the call to EkStep content
 * search
 * @author Manzarul
 *
 */
public class EkStepRequestUtil {

	private static ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * 
	 * @param params String
	 * @param headers Map<String, String>
	 * @return Object[]
	 */
	public static Object[] searchContent(String params, Map<String, String> headers) {
		Object[] result = null;
		String response = "";
		JSONObject data;
		JSONObject jObject;
		try {
		  String baseSearchUrl = ConfigUtil.getString(JsonKey.EKSTEP_BASE_URL);
		  headers.put(JsonKey.AUTHORIZATION, ConfigUtil.getString(JsonKey.AUTHORIZATION));
		  headers.put("Content-Type", "application/json");
		  response = HttpUtil.sendPostRequest(baseSearchUrl+
			    ConfigUtil.getString(JsonKey.EKSTEP_CONTENT_SEARCH_URL), params, headers);
			jObject = new JSONObject(response);
			data = jObject.getJSONObject(JsonKey.RESULT);
			ProjectLogger.log("Total number of content fetched from Ekstep while assembling page data : "+data.get("count"));
			JSONArray contentArray = data.getJSONArray(JsonKey.CONTENT);
			result = mapper.readValue(contentArray.toString(), Object[].class);
		} catch (IOException | JSONException e) {
			ProjectLogger.log(e.getMessage(), e);
		} 
		return result;
	}
}
