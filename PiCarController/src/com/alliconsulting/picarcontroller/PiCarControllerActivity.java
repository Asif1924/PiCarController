package com.alliconsulting.picarcontroller;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.alliconsulting.picarcontroller.R;
import com.alliconsulting.picarcontroller.PiCommand;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.alliconsulting.picarcontroller.MjpegView;

import android.app.Activity;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class PiCarControllerActivity extends Activity {
	private 				GStreamerView 				gsView 					= null;
	private					MjpegView 					mjpegView				= null;
	private 				String 						STREAM_URL 				= "";	
    
    private					int							deviceWidth 			= 0;
    private					int							deviceHeight 			= 0;
    
    private					Thread						carThread 				= null;	
	private					Socket						carSocket 				= null;
	private static final 	String 						LOG_TAG 				= "PiCarControllerActivity";

    private 				String 						hostname 				= "";
    private					String 						servicePortNum 			= "";        
    private					String 						streamPortNum 			= "";
    private					String 						streamType 				= "";
	
	
	private 				TextView					textview;
	private 				SeekBar						seekbar;	
	private					ImageButton 				upButton;
	private					ImageButton 				nogoButton;
	private					ImageButton 				downButton;
	private					ImageButton 				leftButton;
	private					ImageButton 				rightButton;
	private					ImageButton 				stopButton;

	private					ImageButton 				startEngineSoundButton;
	private					ImageButton 				hornSoundButton;
	private					ImageButton 				frontlightsButton;
	private					ImageButton 				resetButton;
	private					ImageButton 				shutdownButton;
	private					ImageButton 				emergButton;
	
	
	private					int							carSpeed = 0;
	private					String						direction = "forward";
	private					String						frontlightsState = "off";
	
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String methodName = "onCreate";

        
        // receive parameters from PreferenceActivity
        Bundle bundle = getIntent().getExtras();
        hostname = bundle.getString( PreferenceActivity.KEY_HOSTNAME);
        servicePortNum =  bundle.getString( PreferenceActivity.KEY_SERVICEPORTNUM);        
        streamPortNum =  bundle.getString( PreferenceActivity.KEY_STREAMPORTNUM);
        streamType = bundle.getString( PreferenceActivity.KEY_STREAMTYPE);

        Log.i(LOG_TAG, methodName + ": streamType=" + streamType);
        

        
        if("GStream".equalsIgnoreCase(streamType)){
        	Log.i(LOG_TAG, methodName + ": hostname=" + hostname + ", streamPortNum=" + streamPortNum);

        	requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
            
            getDeviceResolution();

        	setContentView(R.layout.main);
            
            initializeControls();
            gsView = (GStreamerView) findViewById(R.id.gstreamview);
            bindControls();            
            
        	new GStreamerTask().execute( streamType, hostname, streamPortNum, String.valueOf(deviceWidth), String.valueOf(deviceHeight) );	
        }else{
        	STREAM_URL 				= "http://" + hostname + ":" + streamPortNum + "/?action=stream";
        	
        	Log.i(LOG_TAG, methodName + ": STREAM_URL=" + STREAM_URL);
        	
            setContentView(R.layout.mjpeg_streamer);
            
            initializeControls();
            mjpegView = (MjpegView) findViewById(R.id.streamView1);
            bindControls();
            
        	new StreamerTask().execute( STREAM_URL );	        	
        }
        
    }

    private void updateSpeed( int speedValue ){
		Log.i(LOG_TAG, "onPause");
		carSpeed = speedValue;
		textview.setText("" + seekbar.getProgress());
	}

    private void bindControls(){
        String methodName = "bindControls";
        Log.i( LOG_TAG, methodName);
    	
		seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				Log.i(LOG_TAG, "onProgressChanged");
				
				updateSpeed(seekbar.getProgress());
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				Log.i(LOG_TAG, "onStartTrackingTouch");
				
				updateSpeed(seekbar.getProgress());
				String params = "{action:\"" + direction + "\",speed:" + carSpeed + "}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Log.i(LOG_TAG, "onStopTrackingTouch");				
				
				updateSpeed(seekbar.getProgress());
				String params = "{action:\"" + direction + "\",speed:" + carSpeed + "}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}
			
		}
		);
		
		upButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "upButton.onClick");
				
				
				direction = "forward";
				String params = "{action:\"" + direction + "\",speed:" + carSpeed + "}";				
				sendMessageToServer(params,hostname,servicePortNum);
				
			}
			
		});

		nogoButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "nogoButton.onClick");
				
				
				direction = "middle";
				String params = "{action:\"" + direction + "\",speed:" + carSpeed + "}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}
			
		});	
		
		downButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "downButton.onClick");
				

				direction = "backward";
				String params = "{action:\"" + direction + "\",speed:" + carSpeed + "}";				
				sendMessageToServer(params,hostname,servicePortNum);
				
			}
			
		});
		
		leftButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "leftButton.onClick");
				

				direction = "left";
				String params = "{action:\"" + direction + "\",speed:" + carSpeed + "}";				
				sendMessageToServer(params,hostname,servicePortNum);

			}
			
		});
		
		rightButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "rightButton.onClick");
				

				direction = "right";
				String params = "{action:\"" + direction + "\",speed:" + carSpeed + "}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}
			
		});		
		
		stopButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "stopButton.onClick");
				

				direction = "stop";
				String params = "{action:\"" + "stop" + "\",speed:" + 0 + "}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}
			
		});		
		
		startEngineSoundButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "startEngineSoundButton.onClick");

				String params = "{action:\"start1\"}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}
			
		});		
		
		hornSoundButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "hornSoundButton.onClick");

				String params = "{action:\"honk\"}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}
			
		});			

		frontlightsButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "frontlightsButton.onClick");

				if( "on".equals(frontlightsState) ){
					frontlightsState="off";
				}
				else if( "off".equals(frontlightsState) ) {
					frontlightsState="on"; 
				}
				String params = "{action:\"frontlights\"" + ",state=\"" + frontlightsState + "\"}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}
			
		});			

		resetButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "resetButton.onClick");

				String params = "{action:\"restart\"}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}
			
		});		

		shutdownButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "shutdownButton.onClick");

				String params = "{action:\"shutdown\"}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}
			
		});		
		
		emergButton.setOnClickListener( new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.i(LOG_TAG, "emergButton.onClick");

				String params = "{action:\"emerg\"}";				
				sendMessageToServer(params,hostname,servicePortNum);
			}
			
		});	
		
		
    }
    
	private void initializeControls() {
        String methodName = "onCreate";
        Log.i( LOG_TAG, methodName);
		
		seekbar = (SeekBar) findViewById(R.id.seekBar1);
		//mjpegView = (MjpegView) findViewById(R.id.streamView1);

		textview = (TextView) findViewById(R.id.textView1);
		
		upButton = (ImageButton) findViewById(R.id.upButton);
		upButton.setMaxHeight(48);
		upButton.setMaxWidth(48);
		
		nogoButton = (ImageButton) findViewById(R.id.nogoButton);
		nogoButton.setMaxHeight(48);
		nogoButton.setMaxWidth(48);
		
		downButton = (ImageButton) findViewById(R.id.downButton);
		downButton.setMaxHeight(48);
		downButton.setMaxWidth(48);
		
		leftButton = (ImageButton) findViewById(R.id.leftButton);
		leftButton.setMaxHeight(48);
		leftButton.setMaxWidth(48);
		
		rightButton = (ImageButton) findViewById(R.id.rightButton);
		rightButton.setMaxHeight(48);
		rightButton.setMaxWidth(48);
		
		stopButton = (ImageButton) findViewById(R.id.stopButton);
		stopButton.setMaxHeight(48);
		stopButton.setMaxWidth(48);
		
		
		startEngineSoundButton = (ImageButton) findViewById(R.id.startButton);		
		hornSoundButton = (ImageButton) findViewById(R.id.hornButton);		
		frontlightsButton = (ImageButton) findViewById(R.id.frontlightsButton);		
		
		
		resetButton = (ImageButton) findViewById(R.id.resetButton);		
		shutdownButton = (ImageButton) findViewById(R.id.shutdownButton);		
		emergButton = (ImageButton) findViewById(R.id.emergButton);
				
     }    
    
    private void getDeviceResolution(){
        String methodName = "getDeviceResolution";
        Log.i( LOG_TAG, methodName);

        Display display = getWindowManager().getDefaultDisplay();
    	Point size = new Point();
    	display.getSize(size);
    	deviceWidth = size.x;
    	deviceHeight = size.y;
    }
    
    public void onResume() {
        String methodName = "onResume";
        Log.i( LOG_TAG, methodName);

        super.onResume();
        if(gsView!=null){
        	gsView.resumePlayback();
        }
        
        //if(mjpegView!=null){
        //	mjpegView.resumePlayback();
        //}

    }

    public void onStart() {
        String methodName = "onStart";
        Log.i( LOG_TAG, methodName);

        super.onStart();
    }
    public void onPause() {
        String methodName = "onPause";
        Log.i( LOG_TAG, methodName);

        super.onPause();
        
        if(gsView!=null){
        	gsView.stopPlayback();
        }
        
        
        //if(mjpegView!=null){
        //	mjpegView.stopPlayback();
        //}
    }
    public void onStop() {
        String methodName = "onStop";
        Log.i( LOG_TAG, methodName);
        super.onStop();
    }

    public void onDestroy() {
        String methodName = "onDestroy";
        Log.i( LOG_TAG, methodName);
    	
    	if(gsView!=null){
    		gsView.freeCameraMemory();
    	}
    	
        super.onDestroy();
    }
    
	private void sendMessageToServer(final String params, final String argHost, final String argServicePort ) {
        String methodName = "sendMessageToServer";
        Log.i( LOG_TAG, methodName);
		
		carThread = new Thread( new Runnable(){

			@Override
			public void run() {
				Log.i(LOG_TAG, "sendMessageToServer; params = " + params);		

				String JSONCommand = params;
				Gson gson = new GsonBuilder().create();
				PiCommand command = gson.fromJson(JSONCommand, PiCommand.class);
				
				String fakeURLCommand = "SOCKET ";
				fakeURLCommand = fakeURLCommand + "//?action=" + command.action;
				//if( action=='forward' or action=='backward' or action=='left' or action=='right' or action=='stop' or action=='middle' ):
				if( "forward".equals(command.action) || "backward".equals(command.action) || "left".equals(command.action) || "right".equals(command.action) || "stop".equals(command.action) || "middle".equals(command.action) ){
					fakeURLCommand = fakeURLCommand + "&speed=" + command.speed;
				}else if( "frontlights".equals(command.action) ){
					fakeURLCommand = fakeURLCommand + "&state=" + command.state;
				}
						

				Log.i(LOG_TAG, "sendMessageToServer; fakeURLCommand = " + fakeURLCommand);
				
				try {
					Log.i(LOG_TAG, "sendMessageToServer; creating socket" );
					
					carSocket = new Socket( argHost, Integer.valueOf(argServicePort) );				
					PrintWriter _outputStream = new PrintWriter(carSocket.getOutputStream(), true);
					_outputStream.println(fakeURLCommand);
					
					_outputStream.flush();
					
					if( _outputStream.checkError() )
						System.out.println("sendMessageToServer error: true");
					
					Log.i(LOG_TAG, "sendMessageToServer; closing outputstream" );
					_outputStream.close();
					
					Log.i(LOG_TAG, "sendMessageToServer; closing outputstream" );
				} catch (Exception e) {
					Log.i(LOG_TAG,"sendMessageToServer exception: " + e.getMessage());
				}				
			}
			
		});		

		carThread.start();
	}	
    
    public class GStreamerTask extends AsyncTask<String, Void, GStreamerInputStream> {
    	protected GStreamerInputStream doInBackground( String... params){
    		String methodName = "doInBackground";
    		
    		Socket socket = null;
    		try {
    			Log.i( LOG_TAG, methodName+": " + "GStreamerTask server:" + params[1] + ", port:" + params[2]);
				socket = new Socket( params[1], Integer.valueOf( params[2]));
				return (new GStreamerInputStream(socket.getInputStream()));
			} catch (UnknownHostException e) {
                Log.i(LOG_TAG + ": GStreamerTask", methodName + ": Exception: UnknownHostException", e);
				e.printStackTrace();
			} catch (IOException e) {
                Log.i(LOG_TAG + ": GStreamerTask", methodName + ": Exception: IOException", e);
				e.printStackTrace();
            } catch( Exception e ){
            	Log.i(LOG_TAG + ": GStreamerTask", methodName + ": Exception: Request failed-Exception", e);
            }
    		return null;
    	}
    	
        protected void onPostExecute(GStreamerInputStream result) {
            gsView.setSource(result);
            if(result!=null) result.setSkip(1);
            gsView.setDisplayMode(GStreamerView.SIZE_BEST_FIT);
            gsView.showDeviceResolution(true,deviceWidth,deviceHeight);
            gsView.showFps(true);
        }
    }
    
    public class StreamerTask  extends AsyncTask<String, Void, MjpegInputStream> {
    	
        protected MjpegInputStream doInBackground(String... url) {
        	String methodName = "doInBackground";
        	
    		Log.i(LOG_TAG + ": StreamerTask", methodName);
    		
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse 		response = null;
            DefaultHttpClient 	httpclient = new DefaultHttpClient();     
            MjpegInputStream	mjpegStream = null;
            
            Log.i(LOG_TAG + ": StreamerTask", methodName + ": Sending request to url " + url[0]);
            try {
                response = httpclient.execute(new HttpGet(URI.create(url[0])));
                
                Log.i(LOG_TAG + ": StreamerTask", methodName + ": Request finished, status = " + response.getStatusLine().getStatusCode());
                //if(response.getStatusLine().getStatusCode()==401){
                    //You must turn off camera User Access Control before this will work
                //    return null;
                //}
                mjpegStream = new MjpegInputStream(response.getEntity().getContent()); 
                mjpegView.setStream(mjpegStream);
                return  mjpegStream;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.i(LOG_TAG + ": StreamerTask", methodName + ": Exception: Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(LOG_TAG + ": StreamerTask", methodName + ": Exception: Request failed-IOException", e);
                //Error connecting to camera
            } catch( Exception e ){
            	Log.i(LOG_TAG + ": StreamerTask", methodName + ": Exception: Request failed-Exception", e);
            }

            return null;
        }

        protected void onPostExecute(MjpegInputStream argMJInputStream) {
        	String methodName = "onPostExecute";
        	Log.i(LOG_TAG + ": StreamerTask", methodName);
        	
        	mjpegView.setSource(argMJInputStream);
        	mjpegView.setDisplayMode(MjpegView.SIZE_BEST_FIT);
        	mjpegView.showFps(true);
        }

    	public void setMJpegView(MjpegView mjpegView) {
    		mjpegView = mjpegView;
    	}
    }    
    
}