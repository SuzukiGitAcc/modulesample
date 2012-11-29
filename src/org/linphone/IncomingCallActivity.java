/*
IncomingCallActivity.java
Copyright (C) 2011  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.linphone;

import java.util.List;

import jp.co.aircast.module.ALModuleProxy;
import jp.co.aircast.module.R;

import org.linphone.LinphoneSimpleListener.LinphoneOnCallStateChangedListener;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.core.Log;
import org.linphone.core.LinphoneCall.State;
import org.linphone.ui.SlidingTab;
import org.linphone.ui.SlidingTab.OnTriggerListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity displayed when a call comes in.
 * It should bypass the screen lock mechanism.
 *
 * @author Guillaume Beraudo
 */
public class IncomingCallActivity extends Activity implements LinphoneOnCallStateChangedListener, OnClickListener {

	private TextView mNameView;
	private TextView mNumberView;
	private ImageView mPictureView;
	private TextView mIncomingView;
	private LinphoneCall mCall;
	public static boolean isVideoCall = false;
	//121113suzuki
	private boolean isButtonPush = false;
//	private SlidingTab mIncomingCallWidget;
	public static boolean isActive = false;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("IncomingCallActivity():onCreate");
		
		setContentView(R.get("layout" ,"incoming"));

		mNameView = (TextView) findViewById(R.get("id" ,"incoming_caller_name"));
		mNumberView = (TextView) findViewById(R.get("id" ,"incoming_caller_number"));
		mPictureView = (ImageView) findViewById(R.get("id" ,"incoming_picture"));
		
		mIncomingView = (TextView) findViewById(R.get("id", "incoming_text"));

        // set this flag so this activity will stay in front of the keyguard
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
        getWindow().addFlags(flags);
        
        //121103suzuki
        Button button = (Button) findViewById(R.get("id" ,"voice_call_accept"));
        button.setOnClickListener(this);

        button = (Button) findViewById(R.get("id" ,"video_call_accept"));
        button.setOnClickListener(this);
        button = (Button) findViewById(R.get("id" ,"video_call_disable_movie_accept"));
        button.setOnClickListener(this);
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if(pref.getBoolean(getString(R.get("string", "pref_video_enable_key")), false) == false)
        	button.setVisibility(View.GONE);

        button = (Button) findViewById(R.get("id" ,"incoming_off"));
        button.setOnClickListener(this);


        // "Dial-to-answer" widget for incoming calls.
        //mIncomingCallWidget = (SlidingTab) findViewById(R.get("id" ,"sliding_widget"));

        // For now, we only need to show two states: answer and decline.
        // TODO: make left hint work
//        mIncomingCallWidget.setLeftHintText(R.get("string" ,"slide_to_answer_hint"));
//        mIncomingCallWidget.setRightHintText(R.get("string" ,"slide_to_decline_hint"));

        //mIncomingCallWidget.setOnTriggerListener(this);
        LinphoneManager.getLc().setSpeakerMode(1);
        setButton();


        super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume() {
		Log.d("IncomingCallActivity():onResume");
		
		isActive = false;

		super.onResume();
		
		//121113suzuki
		isButtonPush = false;
		
		LinphoneManager.addListener(this);
		// Only one call ringing at a time is allowed
		List<LinphoneCall> calls = LinphoneUtils.getLinphoneCalls(LinphoneManager.getLc());
		for (LinphoneCall call : calls) {
			if (State.IncomingReceived == call.getState()) {
				mCall = call;
				break;
			}
		}
		if (mCall == null) {
			Log.e("Couldn't find incoming call");
			finish();
			return;
		}
		LinphoneAddress address = mCall.getRemoteAddress();
		// May be greatly sped up using a drawable cache
		Uri uri = LinphoneUtils.findUriPictureOfContactAndSetDisplayName(address, getContentResolver());
		LinphoneUtils.setImagePictureFromUri(this, mPictureView, uri, R.get("drawable" ,"unknown_person"));

		// To be done after findUriPictureOfContactAndSetDisplayName called
		mNameView.setText(address.getDisplayName());
		if (getResources().getBoolean(R.get("bool" ,"show_full_remote_address_on_incoming_call"))) {
			mNumberView.setText(address.asStringUriOnly());
		} else {
			mNumberView.setText(address.getUserName());
		}

		//121102suzuki
//		Log.d("IncomingCallActivity:thread start!!!");
		getUserInfoHandler.post(getUserInfoTask);
//		getUserInfoThread = new Thread(getUserInfoTask);
//		getUserInfoThread.start();
	}
	
	@Override
	protected void onPause() {
		if(isFinishing())
			getUserInfoHandler = null;
//			getUserInfoThread = null;

		super.onPause();
		LinphoneManager.removeListener(this);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		if (LinphoneManager.isInstanciated() && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)) {
//			LinphoneManager.getLc().terminateCall(mCall);
//			finish();
//		}
		if (LinphoneManager.isInstanciated() && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)) {
			if(!isFinishing())
			{
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setMessage(getText(R.get("string", "terminate_call_question")));
				ad.setPositiveButton(getText(R.get("string", "terminate_call_yes")), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						LinphoneManager.getLc().terminateCall(mCall);
						finish();
					}
				});
				ad.setNegativeButton(getText(R.get("string", "terminate_call_no")), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
					}
				});
				ad.create();
				ad.show();
			}
			return false;
		}		
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onCallStateChanged(LinphoneCall call, State state, String msg) {
		if (call == mCall && State.CallEnd == state) {
			finish();
		}
	}

	private void decline() {
		LinphoneManager.getLc().terminateCall(mCall);
	}
	private void answer(boolean isVideo, boolean enableMovie) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean(getString(R.get("string", "pref_video_enable_key")), isVideo);
		editor.putBoolean(getString(R.get("string" ,"pref_video_automatically_accept_video_key")), isVideo);
		editor.putBoolean(getString(R.get("string", "pref_video_automatically_share_my_video_key")), enableMovie);
		editor.commit();
		//mCall.enableCamera(isVideo);
		LinphoneCall c = LinphoneManager.getLc().getCurrentCall();
		if(c != null)
			c.enableCamera(isVideo);

		if (!LinphoneManager.getInstance().acceptCall(mCall)) {
			// the above method takes care of Samsung Galaxy S
			Toast.makeText(this, R.get("string" ,"couldnt_accept_call"), Toast.LENGTH_LONG);
		} else {
			if (isVideo == true)
			{
				Log.d("intent", "aa");
				LinphoneManager.getInstance().enableCamera(mCall, isVideo);
				Log.i("addVideo() = " + LinphoneManager.getInstance().addVideo());
				ALModuleProxy.startVideoActivity(mCall, 0);
				//LinphoneActivity.instance().startVideoActivity(mCall, 0);
			}
			else
			{
				Log.d("intent", "bb");
				ALModuleProxy.startIncallActivity(0);
//				LinphoneActivity.instance().startIncallActivity();
			}
		}
	}
//	@Override
//	public void onGrabbedStateChange(View v, int grabbedState) {
//	}

//	@Override
//	public void onTrigger(View v, int whichHandle) {
//		switch (whichHandle) {
//		case OnTriggerListener.LEFT_HANDLE:
//			answer(false);
//			finish();
//			break;
//		case OnTriggerListener.RIGHT_HANDLE:
//			decline(true);
//			finish();
//			break;
//		default:
//			break;
//		}
//	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		
		if(isButtonPush == true) return;
		
		if(id == R.get("id" ,"voice_call_accept"))
		{
			Log.d("voice");
			answer(false, false);
			finish();
			isButtonPush = true;
		}
		else if(id == R.get("id" ,"video_call_accept"))
		{
			Log.d("video");
			answer(true, true);
			finish();
			isButtonPush = true;
		}
		else if(id == R.get("id" ,"video_call_disable_movie_accept"))
		{
			Log.d("no video");
			answer(true, false);
			finish();
			isButtonPush = true;
		}
		else if(id == R.get("id" ,"incoming_off"))
		{
			decline();
			finish();
			isButtonPush = true;
		}
	}

	Runnable getUserInfoTask = new Runnable(){
		public void run()
		{
			LinphoneService ls = LinphoneService.instance();
			
			Log.d("IncomingCallActivity:thread run() 1111");

			setButton();
			if(ls == null)
			{
				return;
			}

			//get incoming type
			while(true)
			{
				String callType = ls.getCallType();
				if(callType != null)
				{
					Log.d("IncomingCallActivity:callType = " + callType);
					mIncomingView.setText(callType);
					break;
				}
			}
			setButton();
			
			Log.d("IncomingCallActivity:thread run() 2222");
			
			//get user name
			while(true)
			{
				String userName = ls.getUserName();
				if(userName != null)
				{
					Log.d("IncomingCallActivity:userName = " + userName);
					mNameView.setText(userName);
					break;
				}
			}
			
			Log.d("IncomingCallActivity:thread run() 33333");

			//get user image
			while(true)
			{
				Bitmap bmp = ls.getUserImage();
				if(bmp != null)
				{
					mPictureView.setImageBitmap(bmp);
					break;
				}
			}
			
			Log.d("IncomingCallActivity:thread run() 4444");
		}
	};
//	Thread getUserInfoThread;
	Handler getUserInfoHandler = new Handler();
	
	public void setButton()
	{
		Button b;
		
		//121119suzuki 
		if(!LinphoneService.isReady()) return;
		
		LinphoneService ls = LinphoneService.instance();
		String callType = ls.getCallType();
		
		if(callType == null)
		{
			b = (Button)findViewById(R.get("id", "voice_call_accept"));
			b.setVisibility(View.VISIBLE);
			b = (Button)findViewById(R.get("id", "video_call_accept"));
			b.setVisibility(View.VISIBLE);
			b = (Button)findViewById(R.get("id", "video_call_disable_movie_accept"));
			b.setVisibility(View.VISIBLE);
			b = (Button)findViewById(R.get("id", "incoming_off"));
			b.setVisibility(View.VISIBLE);
		}
		else
		{
			if(callType.startsWith("video") == true)
			{
				isVideoCall = true;
				b = (Button)findViewById(R.get("id", "voice_call_accept"));
				b.setVisibility(View.GONE);
				b = (Button)findViewById(R.get("id", "video_call_accept"));
				b.setVisibility(View.VISIBLE);
				b = (Button)findViewById(R.get("id", "video_call_disable_movie_accept"));
				b.setVisibility(View.VISIBLE);
				b = (Button)findViewById(R.get("id", "incoming_off"));
				b.setVisibility(View.VISIBLE);
			}
			else
			{
				isVideoCall = false;
				b = (Button)findViewById(R.get("id", "voice_call_accept"));
				b.setVisibility(View.VISIBLE);
				b = (Button)findViewById(R.get("id", "video_call_accept"));
				b.setVisibility(View.GONE);
				b = (Button)findViewById(R.get("id", "video_call_disable_movie_accept"));
				b.setVisibility(View.GONE);
				b = (Button)findViewById(R.get("id", "incoming_off"));
				b.setVisibility(View.VISIBLE);
			}
		}
	}
}
