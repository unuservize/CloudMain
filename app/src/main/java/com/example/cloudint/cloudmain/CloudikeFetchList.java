package com.example.cloudint.cloudmain;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by PiskunovI on 25.11.2014.
 */
public class CloudikeFetchList extends AsyncTask<String,Void,FilesArray> {

    String LOG_TAG = "CloudikeFetchList";

    @Override
    protected FilesArray doInBackground(String... getParams) {
        ArrayList<String> metadataParams = new ArrayList<String>();

        try {

            Collections.addAll(metadataParams, getParams);

            String token = metadataParams.get(0);

            String url = "https://be-saas.cloudike.com/api/1/metadata_full_listing/";

            String listing_request_id = "";

            //Get listing_request_id
            listing_request_id = cloudikeRequest("listing_request_id", token, url, listing_request_id);
            listing_request_id = getListingRequestId(listing_request_id);

            Log.d(LOG_TAG, listing_request_id);

            //Get jsonURL
            String jsonURL = cloudikeRequest("jsonURL", token, url, listing_request_id);
            int count = 0;

            while(jsonURL.indexOf("progress") != -1) {
                Log.d(LOG_TAG, jsonURL);
                jsonURL = cloudikeRequest("jsonURL", token, url, listing_request_id);
                if (count >= 20){
                    break;
                }
                else{
                    count++;
                }
            }
            //Log.d(LOG_TAG, jsonURL);

            jsonURL = getJsonURL(jsonURL);

            Log.d(LOG_TAG, jsonURL);

            String jsonObject = getJsonObject(jsonURL);

            Log.d(LOG_TAG, jsonObject);

            Gson gson = new GsonBuilder().registerTypeAdapter(FilesArray.class, new CustomDeserializer()).create();

            return gson.fromJson(jsonObject,FilesArray.class);

        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    String cloudikeRequest (String item, String token, String url, String listing_request_id) throws IOException {
        Log.d(LOG_TAG, item + " requested");

        if (item.equals("jsonURL")){
            List<BasicNameValuePair> setParams = new LinkedList<BasicNameValuePair>();
            setParams.add(new BasicNameValuePair("listing_request_id", listing_request_id));
            String paramString = URLEncodedUtils.format(setParams, "utf-8");
            url+="?"+paramString;
        }
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        get.setHeader("Mountbit-Auth", token);

        HttpResponse response = client.execute(get);
        return getResponse(response);
    }

    String getResponse(HttpResponse response) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }

    //url
    String getJsonURL(String response) {
            JsonObject o = new JsonParser().parse(response).getAsJsonObject();
            return o.getAsJsonPrimitive("url").toString().substring(1, o.getAsJsonPrimitive("url").toString().length() - 1);
    }

    //listing_request
    String getListingRequestId(String response){
        JsonObject o = new JsonParser().parse(response).getAsJsonObject();
        return o.getAsJsonPrimitive("listing_request_id").toString().substring(1,o.getAsJsonPrimitive("listing_request_id").toString().length()-1);
    }

    String getJsonObject(String _url) throws IOException {
        URL url = new URL(_url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try{
            InputStream in = new BufferedInputStream(connection.getInputStream());
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            return total.toString();
        }
        finally {
            connection.disconnect();
        }

    }


    class CustomDeserializer implements JsonDeserializer {

        @Override
        public FilesArray deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ArrayList<CloudikeObject> result = new ArrayList<CloudikeObject>();
            JsonObject object = json.getAsJsonObject();
            JsonArray array = object.getAsJsonArray("content");

            result.addAll(getElements(array));

            return new FilesArray(result);
        }
    }

    ArrayList<CloudikeObject> getElements(JsonArray array){

        ArrayList<CloudikeObject> result = new ArrayList<CloudikeObject>();
        CloudikeObject file;

        for (JsonElement element : array) {
            JsonObject object1 = element.getAsJsonObject();
            file = new CloudikeObject();
            file.isFolder = object1.get("folder").getAsBoolean();
            if (file.isFolder){
                JsonArray array2 = object1.get("content").getAsJsonArray();
                result.addAll(getElements(array2));
            }
            else {
                file.mbytes = String.valueOf(new BigDecimal((object1.get("bytes").getAsDouble()/1024/1024)).setScale(2, RoundingMode.HALF_EVEN));
                file.path = object1.get("path").getAsString();
                result.add(file);
            }
        }

        return new ArrayList<CloudikeObject>(result);
    }


}
