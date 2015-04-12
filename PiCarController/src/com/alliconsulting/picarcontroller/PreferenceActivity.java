package com.alliconsulting.picarcontroller;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class PreferenceActivity extends Activity {
	public static final String 	KEY_HOSTNAME 			= "hostname";
	public static final String 	KEY_SERVICEPORTNUM 		= "servicePortNum";
	public static final String 	KEY_STREAMPORTNUM		= "streamPortNum";
	public static final String 	KEY_WIDTH 				= "width";
	public static final String 	KEY_HEIGHT 				= "height";
	public static final String 	KEY_STREAMTYPE			= "streamType";
	
	private static final 	String 						LOG_TAG 				= "PiCarControllerActivity::PreferenceActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
    	String methodName = "onCreate";
    	Log.i(LOG_TAG, methodName );
		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		this.setContentView( R.layout.preference);
		
		// load stored data
		SharedPreferences sp = this.getPreferences( MODE_PRIVATE);
		String hostname = sp.getString( KEY_HOSTNAME, this.getString( R.string.defaultHostName));
		String servicePortNum = sp.getString( KEY_SERVICEPORTNUM, this.getString( R.string.defaultServicePortNum));
		String streamPortNum = sp.getString( KEY_STREAMPORTNUM, this.getString( R.string.defaultStreamPortNum));
		String width = sp.getString( KEY_WIDTH, this.getString( R.string.defaultWidth));
		String height = sp.getString( KEY_HEIGHT, this.getString( R.string.defaultHeight));

		String streamType = sp.getString( KEY_STREAMTYPE, this.getString( R.string.defaultStreamType));		
		
		// set stored parameters to the contents
		EditText editableTextVeneer = (EditText)findViewById( R.id.editText_hostname);
		editableTextVeneer.setText( hostname);
		
		editableTextVeneer = (EditText)findViewById( R.id.editText_servicePortNum);
		editableTextVeneer.setText( servicePortNum);

		editableTextVeneer = (EditText)findViewById( R.id.editText_streamPortNum);
		editableTextVeneer.setText( streamPortNum);
		
		editableTextVeneer = (EditText)findViewById( R.id.editText_width);
		editableTextVeneer.setText( width);
		
		editableTextVeneer = (EditText)findViewById( R.id.editText_height);
		editableTextVeneer.setText( height);

		RadioButton radioButtonVeneer = (RadioButton)findViewById( R.id.radioButtonMJPEG);
		
		if( "MJPEG".equalsIgnoreCase(streamType) )
			radioButtonVeneer.setChecked(true);		
		else{
			radioButtonVeneer =(RadioButton)findViewById( R.id.radioButtonGStream);
			radioButtonVeneer.setChecked(true);
		}

	}

	/*
	 * This function is called when user clicks start button.
	 */
	public void onClick( View view){
    	String methodName = "onClick";
    	Log.i(LOG_TAG, methodName );
		
		// get data from EditText components
		EditText 		etHost 				= (EditText)findViewById( R.id.editText_hostname);
		EditText		etServicePort 		= (EditText)findViewById( R.id.editText_servicePortNum);
		EditText 		etStreamPort 		= (EditText)findViewById( R.id.editText_streamPortNum);
		EditText 		etWidth 			= (EditText)findViewById( R.id.editText_width);
		EditText 		etHeight 			= (EditText)findViewById( R.id.editText_height);
		RadioButton 	radioButtonMJPEG	= (RadioButton)findViewById( R.id.radioButtonMJPEG);
		
		String 			hostname 			= etHost.getText().toString();
		String 			servicePortNum 		= etServicePort.getText().toString();
		String 			streamPortNum 		= etStreamPort.getText().toString();
		String 			width 				= etWidth.getText().toString();
		String 			height 				= etHeight.getText().toString();
		
		String 			streamType 			= ( radioButtonMJPEG.isChecked() ) ? "MJPEG" : "GStream" ;		
		
		// store the input data
		SharedPreferences sp = this.getPreferences( MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString( KEY_HOSTNAME, hostname);
		editor.putString( KEY_SERVICEPORTNUM, servicePortNum);
		editor.putString( KEY_STREAMPORTNUM, streamPortNum);
		editor.putString( KEY_WIDTH, width); 
		editor.putString( KEY_HEIGHT, height);
		editor.putString( KEY_STREAMTYPE, streamType);
		
		editor.commit();
		
		// set the image size
		GStreamerView.setImageSize( Integer.parseInt( width), Integer.parseInt(height));
		
		// launch PiCarControllerActivity
		Intent intent = new Intent( this, PiCarControllerActivity.class);
		intent.putExtra( KEY_HOSTNAME, hostname);
		intent.putExtra( KEY_SERVICEPORTNUM, servicePortNum);
		intent.putExtra( KEY_STREAMPORTNUM, streamPortNum);
		intent.putExtra( KEY_STREAMTYPE, streamType);
		this.startActivity( intent);
	}
}
