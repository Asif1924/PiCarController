package com.alliconsulting.picarcontroller;

import java.io.IOException;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;
import com.alliconsulting.picarcontroller.MjpegInputStream;
import com.alliconsulting.picarcontroller.MjpegView;

public class StreamerTask  extends AsyncTask<String, Void, MjpegInputStream> {
	
	private 	static 		final 			String 		LOG_TAG 				= "StreamerTask";
	private		static						MjpegView 	_mjpegView				= null;
	
    protected MjpegInputStream doInBackground(String... url) {
		Log.i(LOG_TAG, "doInBackground");
        //TODO: if camera has authentication deal with it and don't just not work
        HttpResponse 		response = null;
        DefaultHttpClient 	httpclient = new DefaultHttpClient();     

        Log.i(LOG_TAG, "1. Sending http request");
        try {
            response = httpclient.execute(new HttpGet(URI.create(url[0])));
            Log.i(LOG_TAG, "3. Request finished, status = " + response.getStatusLine().getStatusCode());
            if(response.getStatusLine().getStatusCode()==401){
                //You must turn off camera User Access Control before this will work
                return null;
            }
            return new MjpegInputStream(response.getEntity().getContent());  
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            Log.i(LOG_TAG, "Request failed-ClientProtocolException", e);
            //Error connecting to camera
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(LOG_TAG, "Request failed-IOException", e);
            //Error connecting to camera
        }

        return null;
    }

    protected void onPostExecute(MjpegInputStream result) {
    	Log.i(LOG_TAG, "OnPostExecute");
    	_mjpegView.setSource(result);
    	_mjpegView.setDisplayMode(MjpegView.SIZE_BEST_FIT);
    	_mjpegView.showFps(true);
    }

	public void setMJpegView(MjpegView mjpegView) {
		_mjpegView = mjpegView;
	}
}