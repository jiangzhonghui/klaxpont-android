/*
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klaxpont.android;

import java.io.File;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.BaseRequestListener;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.LoginButton;
import com.facebook.android.SessionEvents;
import com.facebook.android.SessionStore;
import com.facebook.android.Util;
import com.facebook.android.SessionEvents.AuthListener;
import com.facebook.android.SessionEvents.LogoutListener;

import com.klaxpont.android.Constants;

import com.dailymotion.android.Dailymotion;
import com.dailymotion.android.FilePickerActivity;

public class Main extends Activity implements SurfaceHolder.Callback{
	
	private static final int REQUEST_PICK_FILE = 1;
	private File fSelectedFile;
	private Button bFileSelect;
	private TextView tFileSelected;
	private TextView tVideoIdPublished;
	private Button bSendFile;
	private LoginButton bLoginButton;
    private TextView tFacebookName;
    private TextView tFacebookId;
    
    private Preview mPreview;
    private boolean iCameraEnable;
    private Camera mCamera;

    private Facebook mFacebook;
    private AsyncFacebookRunner mAsyncRunner;
    
    //private String sAccessTokenDailymotion="";
    private String sVideoId="";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setiCameraEnable(false);
        //setiCameraEnable(true);
        
        setContentView(R.layout.main);
        tVideoIdPublished = (TextView) Main.this.findViewById(R.id.video_published_id);
        bFileSelect = (Button) findViewById(R.id.select_file_button);
        tFileSelected = (TextView) Main.this.findViewById(R.id.file_selected);
        bSendFile = (Button) findViewById(R.id.send_file_button);
        bLoginButton = (LoginButton) findViewById(R.id.login);
        tFacebookName = (TextView) Main.this.findViewById(R.id.txtname);
        tFacebookId = (TextView) Main.this.findViewById(R.id.txtid);
        
        mPreview = (Preview) Main.this.findViewById(R.id.preview_camera);
        mPreview.init(this);
        
       	mFacebook = new Facebook(Constants.FACEBOOK_APP_ID);
       	mAsyncRunner = new AsyncFacebookRunner(mFacebook);

       	bSendFile.setOnClickListener(new View.OnClickListener() {
       		@Override
       	    public void onClick(View v){
       			try {
       				if(!Dailymotion.isSessionValid()) {
       					if(Dailymotion.login(
       							Constants.DAILYMOTION_API_KEY,
       							Constants.DAILYMOTION_SECRET_API_KEY,
	       						Constants.DAILYMOTION_USER,
	       						Constants.DAILYMOTION_PASSWORD)) {
       						Log.i("Dailymotion","Login");
       					}else{
       						Log.w("Dailymotion","Unable to login, verify tour credential");
       						return;
       					}
       					
       				}       				
       				//TODO
       				sVideoId=Dailymotion.publish_a_video(
							fSelectedFile,
							"klaxpontTest",
							"fun",
							"klaxpont,cripont,cri,klaxon,pont,connards",
							"true");
					tVideoIdPublished.setText("Published Video ID:"+sVideoId);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
       		}
       	});
       	
       	bFileSelect.setOnClickListener(new View.OnClickListener() {
       		@Override
       	    public void onClick(View v){
       			Intent intent = new Intent(Main.this, FilePickerActivity.class);
       			startActivityForResult(intent, REQUEST_PICK_FILE);
       		}
       	});
       	SessionStore.restore(mFacebook, this);
        SessionEvents.addAuthListener(new SampleAuthListener());
        SessionEvents.addLogoutListener(new SampleLogoutListener());
        bLoginButton.init(this, mFacebook);
        if (mFacebook.isSessionValid())
        	mAsyncRunner.request("me", new SampleRequestListener());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
	    switch(requestCode) {
        case REQUEST_PICK_FILE:
        	if(resultCode == RESULT_OK) {
        		if(data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH)) {
                // Get the file path
            	fSelectedFile = new File(data.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH));
                // Set the file path text view
                tFileSelected.setText(fSelectedFile.getPath());
                //Now you have your selected file, You can do your additional requirement with file.
        		}
            }
        	return;
		default :
            
            break;
        }
    	mFacebook.authorizeCallback(requestCode, resultCode, data);
    }

    public class SampleAuthListener implements AuthListener {

        @Override
		public void onAuthSucceed() {
            tFacebookName.setText("You have logged in! ");
            //asking the facebook login...
            mAsyncRunner.request("me", new SampleRequestListener());
        }

        @Override
		public void onAuthFail(String error) {
            tFacebookName.setText("Login Failed: " + error);
        }
    }

    public class SampleLogoutListener implements LogoutListener {
        @Override
		public void onLogoutBegin() {
            tFacebookName.setText("Logging out...");
            tFacebookId.setVisibility(View.INVISIBLE);
        }

        @Override
		public void onLogoutFinish() {
            tFacebookName.setText("You have logged out! ");
        }
    }

    public class SampleRequestListener extends BaseRequestListener {

        @Override
		public void onComplete(final String response, final Object state) {
            try {
                // process the response here: executed in background thread
                Log.d("Facebook-Main", "Response: " + response.toString());
                JSONObject json = Util.parseJson(response);
                final String name = json.getString("name");
                final String id = json.getString("id");

                // then post the processed result back to the UI thread
                // if we do not do this, an runtime exception will be generated
                // e.g. "CalledFromWrongThreadException: Only the original
                // thread that created a view hierarchy can touch its views."
                Main.this.runOnUiThread(new Runnable() {
                    @Override
					public void run() {
                        tFacebookName.setText("name:" + name);
                        tFacebookId.setVisibility(View.VISIBLE);
                        tFacebookId.setText("id:" + id);
                    }
                });
            } catch (JSONException e) {
                Log.w("Facebook-Main", "JSON Error in response");
            } catch (FacebookError e) {
                Log.w("Facebook-Main", "Facebook Error: " + e.getMessage());
            }
        }
    }
    
    public boolean openCamera() {
    	if(getiCameraEnable())
    	{
	    	try{
		   		mCamera = Camera.open(0);
		   		if (mCamera != null){
		   			Camera.Parameters params = mCamera.getParameters();
		   			mCamera.setParameters(params);
		   		}
		   		else {
		   			Toast.makeText(getApplicationContext(), "Camera not available!", Toast.LENGTH_LONG).show();
		   			return false;
		   		}
	    	} catch (RuntimeException e) {
	            Log.w("Camera", "Unable to open : "+ e.getMessage());
	            mCamera = null;
	            return false;
	        }
			return true;
    	}else
    		return false;
    }
    
    public void closeCamera() {
    	if(getiCameraEnable())
    	{
    		if (mCamera != null) {
                mPreview.setCamera(null);
                mCamera.release();
                mCamera = null;
            }
    	}
    }
    
    @Override
   	public void surfaceCreated(SurfaceHolder holder) {
    	openCamera();
   	}

    @Override
    protected void onResume() {
        super.onResume();

        // Open the default i.e. the first rear facing camera.
        if(openCamera())
        	mPreview.setCamera(mCamera);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        closeCamera();
    }
    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		
	}

	public boolean getiCameraEnable() {
		return iCameraEnable;
	}

	public void setiCameraEnable(boolean iCameraEnable) {
		this.iCameraEnable = iCameraEnable;
	}
}
