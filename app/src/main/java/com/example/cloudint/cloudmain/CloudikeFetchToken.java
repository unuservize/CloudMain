package com.example.cloudint.cloudmain;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by PiskunovI on 21.11.2014.
 */
public class CloudikeFetchToken extends AsyncTask<String,Void,String> {

    String LOG_TAG = "CloudLikeFetch";
    private String token = "";

    @Override
    protected String doInBackground(String... params) {
        ArrayList<String> signInParams = new ArrayList<String>();

        Collections.addAll(signInParams, params);

        String email = signInParams.get(0);
        String password = signInParams.get(1);

        try {

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("https://be-saas.cloudike.com/api/1/accounts/login/");

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("email", email));
            nameValuePairs.add(new BasicNameValuePair("password", password));

            post.setEntity(new UrlEncodedFormEntity(nameValuePairs,"UTF8"));
            String response = client.execute(post, new BasicResponseHandler());

            Log.d(LOG_TAG, response);

            setToken(response);

            return token;

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }

        return null;
    }

    String setToken(String response){
        JsonObject o = new JsonParser().parse(response).getAsJsonObject();
        token = o.getAsJsonPrimitive("token").toString().substring(1,o.getAsJsonPrimitive("token").toString().length()-1);
        return token;
    }
}
