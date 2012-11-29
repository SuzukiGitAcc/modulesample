/*
VideoCallActivity.java
Copyright (C) 2010  Belledonne Communications, Grenoble, France

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

import jp.co.aircast.module.ALModuleProxy;
import jp.co.aircast.module.R;

import org.linphone.LinphoneSimpleListener.LinphoneOnCallStateChangedListener;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallParams;
import org.linphone.core.Log;
import org.linphone.mediastream.Version;
import org.linphone.mediastream.video.AndroidVideoWindowImpl;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.ui.Numpad;
import org.linphone.ui.ToggleImageButton;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

/**
 * For Android SDK >= 5
 * 
 * @author Guillaume Beraudo
 * 
 */
//@TargetApi(5)
public class VideoCallActivity extends Activity implements
		LinphoneOnCallStateChangedListener, OnClickListener {
	private final static int DELAY_BEFORE_HIDING_CONTROLS = 2000;
	private static final int numpadDialogId = 1;

	private SurfaceView mVideoViewReady;
	private SurfaceView mVideoCaptureViewReady;
	public static boolean launched = false;
	private LinphoneCall videoCall;
	private WakeLock mWakeLock;
	private Handler refreshHandler = new Handler();
	private Handler controlsHandler = new Handler();
	AndroidVideoWindowImpl androidVideoWindowImpl;
	private Runnable mCallQualityUpdater, mControls;
	private LinearLayout mControlsLayout;
	//121103suzuki 
	private Button incallButton,		//
				terminalCallButton,		//
				useFrontCameraButton,	//
//				resolutionButton,		//
				spekerStateButton,
				enableCameraButton;		//
	private boolean isTouchOk = false;

	public void onCreate(Bundle savedInstanceState) {
		isTouchOk = false;
		
		super.onCreate(savedInstanceState);
		if (!LinphoneManager.isInstanciated()
				|| LinphoneManager.getLc().getCallsNb() == 0) {
			Log.e("No service running: avoid crash by finishing ", getClass()
					.getName());
			// super.onCreate called earlier
			finish();
			return;
		}
		
//		LinphoneManager.getInstance().changeResolution();
		//121109 low resolusion
		BandwidthManager manager = BandwidthManager.getInstance();
		manager.setUserRestriction(true);

		setContentView(R.get("layout", "videocall"));

		SurfaceView videoView = (SurfaceView) findViewById(R.get("id", "video_surface"));
//		videoView.setOnClickListener(this);
		
		// ((FrameLayout)
		// findViewById(R.id.video_frame)).bringChildToFront(findViewById(R.id.imageView1));

		SurfaceView captureView = (SurfaceView) findViewById(R.get("id", "video_capture_surface"));
		captureView.getHolder()
				.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		//121103suzuki
//		incallButton = (Button) findViewById(R.id.incallButton);
//		incallButton.setOnClickListener(this);
		terminalCallButton = (Button) findViewById(R.get("id", "terminalCallButton"));
		terminalCallButton.setOnClickListener(this);
		useFrontCameraButton = (Button) findViewById(R.get("id", "useFrontCamera"));
		useFrontCameraButton.setOnClickListener(this);
//		resolutionButton = (Button) findViewById(R.id.resolution);
//		resolutionButton.setOnClickListener(this);
		spekerStateButton = (Button) findViewById(R.get("id" ,"spekerStateButton"));
		spekerStateButton.setOnClickListener(this);
		enableCameraButton = (Button) findViewById(R.get("id", "enableCamera"));
		enableCameraButton.setOnClickListener(this);
		setButton();

		/* force surfaces Z ordering */
		fixZOrder(videoView, captureView);

		androidVideoWindowImpl = new AndroidVideoWindowImpl(videoView,
				captureView);
		androidVideoWindowImpl
				.setListener(new AndroidVideoWindowImpl.VideoWindowListener() {

					public void onVideoRenderingSurfaceReady(
							AndroidVideoWindowImpl vw, SurfaceView surface) {
						LinphoneManager.getLc().setVideoWindow(vw);
						mVideoViewReady = surface;
					}

					public void onVideoRenderingSurfaceDestroyed(
							AndroidVideoWindowImpl vw) {
						Log.d("VIDEO WINDOW destroyed!\n");
						LinphoneManager.getLc().setVideoWindow(null);
					}

					public void onVideoPreviewSurfaceReady(
							AndroidVideoWindowImpl vw, SurfaceView surface) {
						mVideoCaptureViewReady = surface;
//						prevWindowEnable(videoCall.cameraEnabled());
						LinphoneManager.getLc().setPreviewWindow(
								mVideoCaptureViewReady);
					}

					public void onVideoPreviewSurfaceDestroyed(
							AndroidVideoWindowImpl vw) {
						// Remove references kept in jni code and restart camera
						LinphoneManager.getLc().setPreviewWindow(null);
					}
				});

		androidVideoWindowImpl.init();

		videoCall = LinphoneManager.getLc().getCurrentCall();
		if (videoCall != null) {
			LinphoneManager lm = LinphoneManager.getInstance();
			if (!lm.shareMyCamera()	&& videoCall.cameraEnabled()) {
				lm.sendStaticImage(true);
			}

			updatePreview(videoCall.cameraEnabled());
//			prevWindowEnable(videoCall.cameraEnabled());
		}

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
				| PowerManager.ON_AFTER_RELEASE, Log.TAG);
		mWakeLock.acquire();

		mControlsLayout = (LinearLayout) findViewById(R.get("id", "incall_controls_layout"));
		videoView.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
			//	openOptionsMenu();
				if(mControlsLayout != null
					&& mControlsLayout.getVisibility() == View.GONE)
				{
					mControlsLayout.setVisibility(View.VISIBLE);
					controlsHandler.postDelayed(mControls = new Runnable() {
						public void run() {
							mControlsLayout.setVisibility(View.GONE);
						}
					}, DELAY_BEFORE_HIDING_CONTROLS);
					return false;
				}
				return true;
			}
		});

		if (!AndroidCameraConfiguration.hasSeveralCameras()) {
			View v=findViewById(R.get("id", "switch_camera"));
			if (v!=null) v.setVisibility(View.GONE);
		}

		if (Version.isXLargeScreen(this)) {
//			findViewById(R.id.toggleMuteMic).setOnClickListener(this);
//			findViewById(R.id.toggleSpeaker).setOnClickListener(this);
//			findViewById(R.id.incallNumpadShow).setOnClickListener(this);
//			findViewById(R.id.back).setOnClickListener(this);
//			findViewById(R.id.incallHang).setOnClickListener(this);
//			findViewById(R.id.switch_camera).setOnClickListener(this);
//			findViewById(R.id.conf_simple_pause).setOnClickListener(this);
//			findViewById(R.id.conf_simple_video).setOnClickListener(this);

			if (LinphoneManager.getInstance().isSpeakerOn()) {
//				ToggleImageButton speaker = (ToggleImageButton) findViewById(R.get("id", "toggleSpeaker"));
//				speaker.setChecked(true);
//				speaker.setEnabled(false);
			}
		}
	}

	void updateQualityOfSignalIcon(float quality) {
		ImageView qos = (ImageView) findViewById(R.get("id", "QoS"));
		if (quality >= 4) // Good Quality
		{
			qos.setImageDrawable(getResources().getDrawable(
					R.get("drawable", "stat_sys_signal_4")));
		} else if (quality >= 3) // Average quality
		{
			qos.setImageDrawable(getResources().getDrawable(
					R.get("drawable", "stat_sys_signal_3")));
		} else if (quality >= 2) // Low quality
		{
			qos.setImageDrawable(getResources().getDrawable(
					R.get("drawable", "stat_sys_signal_2")));
		} else if (quality >= 1) // Very low quality
		{
			qos.setImageDrawable(getResources().getDrawable(
					R.get("drawable", "stat_sys_signal_1")));
		} else // Worst quality
		{
			qos.setImageDrawable(getResources().getDrawable(
					R.get("drawable", "stat_sys_signal_0")));
		}
	}

	private void rewriteToggleCameraItem() {
		LinphoneCall c = LinphoneManager.getLc().getCurrentCall();
		if(c == null) return;

		if (c.cameraEnabled())
		{
			enableCameraButton.setText(R.get("string", "camera_disp_state_on"));
		}
		else
		{
			enableCameraButton.setText(R.get("string", "camera_disp_state_off"));
		}
//		if (LinphoneManager.getLc().getCurrentCall().cameraEnabled()) {
//			item.setTitle(getString(R.string.menu_videocall_toggle_camera_disable));
//		} else {
//			item.setTitle(getString(R.string.menu_videocall_toggle_camera_enable));
//		}
	}
	private boolean isSpeker = true;
	private void rewriteSpekerStateItem(){
		//spekerStateButton
		if(isSpeker)
			spekerStateButton.setText(R.get("string", "speker_state_on"));
		else
			spekerStateButton.setText(R.get("string", "speker_state_off"));
	}
	
	private void changeSpeker(boolean b)
	{
		isSpeker = b;
		
		if(isSpeker == true)
			LinphoneManager.getInstance().routeAudioToSpeaker();
		else
			LinphoneManager.getInstance().routeAudioToReceiver();
	}
//	private void rewriteChangeResolutionItem() {
//		if (BandwidthManager.getInstance().isUserRestriction())
//		{
//			resolutionButton.setText("");
//		}
//		else
//		{
//			resolutionButton.setText("");
//		}
//	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		if (Version.isXLargeScreen(this))
//			return false;
//		
//		// Inflate the currently selected menu XML resource.
//		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.videocall_activity_menu, menu);
//
//		rewriteToggleCameraItem(menu
//				.findItem(R.id.videocall_menu_toggle_camera));
//		rewriteChangeResolutionItem(menu
//				.findItem(R.id.videocall_menu_change_resolution));
//
//		if (!AndroidCameraConfiguration.hasSeveralCameras()) {
//			menu.findItem(R.id.videocall_menu_switch_camera).setVisible(false);
//		}
//		return true;
//	}

//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		int id = item.getItemId();
//		if (id == R.id.videocall_menu_back_to_dialer) {
//			finish();
//			LinphoneActivity.instance().startIncallActivity();
//		}
//		else if (id == R.id.videocall_menu_change_resolution) {
//			LinphoneManager.getInstance().changeResolution();
//			// previous call will cause graph reconstruction -> regive preview
//			// window
//			if (mVideoCaptureViewReady != null)
//				LinphoneManager.getLc()
//						.setPreviewWindow(mVideoCaptureViewReady);
//			rewriteChangeResolutionItem(item);
//		}
//		else if (id == R.id.videocall_menu_terminate_call) {
//			LinphoneManager.getInstance().terminateCall();
//		}
//		else if (id == R.id.videocall_menu_toggle_camera) {
//			boolean camEnabled = LinphoneManager.getInstance()
//					.toggleEnableCamera();
//			updatePreview(camEnabled);
//			Log.e("winwow camera enabled: " + camEnabled);
//			rewriteToggleCameraItem(item);
//			// previous call will cause graph reconstruction -> regive preview
//			// window
//			if (camEnabled) {
//				if (mVideoCaptureViewReady != null)
//					LinphoneManager.getLc().setPreviewWindow(
//							mVideoCaptureViewReady);
//			} else
//				LinphoneManager.getLc().setPreviewWindow(null);
//		}
//		else if (id == R.id.videocall_menu_switch_camera) {
//			int videoDeviceId = LinphoneManager.getLc().getVideoDevice();
//			videoDeviceId = (videoDeviceId + 1) % AndroidCameraConfiguration.retrieveCameras().length;
//			LinphoneManager.getLc().setVideoDevice(videoDeviceId);
//			CallManager.getInstance().updateCall();
//			// previous call will cause graph reconstruction -> regive preview
//			// window
//			if (mVideoCaptureViewReady != null)
//				LinphoneManager.getLc()
//						.setPreviewWindow(mVideoCaptureViewReady);
//		}
//		else {
//			Log.e("Unknown menu item [", item, "]");
//		}
//		return true;
//	}

	void updatePreview(boolean cameraCaptureEnabled) {
		mVideoCaptureViewReady = null;
		if (cameraCaptureEnabled) {
			findViewById(R.get("id", "imageView1")).setVisibility(View.INVISIBLE);
			findViewById(R.get("id", "video_capture_surface"))
					.setVisibility(View.VISIBLE);
		} else {
			findViewById(R.get("id", "video_capture_surface")).setVisibility(
					View.INVISIBLE);
			findViewById(R.get("id", "imageView1")).setVisibility(View.VISIBLE);
		}
		findViewById(R.get("id", "video_frame")).requestLayout();
	}

	void fixZOrder(SurfaceView video, SurfaceView preview) {
		video.setZOrderOnTop(false);
		preview.setZOrderOnTop(true);
	}

	@Override
	protected void onResume() {
		if (!LinphoneManager.getLc().isIncall())
		{
			Log.d("VideoCallActivity():onResume():finish");
			finish();
		}
		
		super.onResume();
		if (mVideoViewReady != null)
			((GLSurfaceView) mVideoViewReady).onResume();
		synchronized (androidVideoWindowImpl) {
			LinphoneManager.getLc().setVideoWindow(androidVideoWindowImpl);
		}
		launched = true;
		LinphoneManager.addListener(this);

		refreshHandler.postDelayed(mCallQualityUpdater = new Runnable() {
			LinphoneCall mCurrentCall = LinphoneManager.getLc()
					.getCurrentCall();

			public void run() {
				if (mCurrentCall == null) {
					mCallQualityUpdater = null;
					return;
				}
				int oldQuality = 0;
				float newQuality = mCurrentCall.getCurrentQuality();
				if ((int) newQuality != oldQuality) {
					updateQualityOfSignalIcon(newQuality);
				}
				if (launched) {
					refreshHandler.postDelayed(this, 1000);
				} else
					mCallQualityUpdater = null;
			}
		}, 1000);

		if (mControlsLayout != null)
			mControlsLayout.setVisibility(View.GONE);

		//121112suzuki 
//		if(videoCall != null)
//		{
////			Log.d("enable camera!!!!");
//			videoCall.enableCamera(true);
//		}
		
		if (videoCall != null && LinphoneManager.getInstance().shareMyCamera()) {
//			videoCall.enableCamera(true);
			updatePreview(videoCall.cameraEnabled());
		}
		
		Log.d("VideoCallActivity():onResume End");
		isTouchOk = true;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (LinphoneUtils.onKeyVolumeAdjust(keyCode))
			return true;
		if (Version.isXLargeScreen(this) && LinphoneUtils.onKeyBackGoHome(this, keyCode, event)) {
			return true;
		}
		else if (!Version.isXLargeScreen(this) && keyCode == KeyEvent.KEYCODE_BACK) {
			if(!isFinishing())
			{
				AlertDialog.Builder ad = new AlertDialog.Builder(this);
				ad.setMessage(getText(R.get("string", "terminate_call_question")));
				ad.setPositiveButton(getText(R.get("string", "terminate_call_yes")), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						LinphoneManager.getInstance().terminateCall();
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
//			LinphoneActivity.instance().startIncallActivity();
//			Log.d("VideoCallActivity():onKeyDown():finish()");
//			finish();
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onDestroy() {
		if (androidVideoWindowImpl != null) { // Prevent linphone from crashing
												// if correspondent hang up
												// while you are rotating
			androidVideoWindowImpl.release();
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d("onPause VideoCallActivity (isFinishing:", isFinishing(),
				", inCall:", LinphoneManager.getLc().isIncall(), ")");
		LinphoneManager.removeListener(this);
		
		// Send NoWebcam since Android 4.0 can't get the video from the
		// webcam if the activity is not in foreground
		if (videoCall != null)
			videoCall.enableCamera(false);
		
		if (isFinishing()) {
			videoCall = null; // release reference
		}
		launched = false;
		synchronized (androidVideoWindowImpl) {
			/*
			 * this call will destroy native opengl renderer which is used by
			 * androidVideoWindowImpl
			 */
			LinphoneManager.getLc().setVideoWindow(null);
		}

		if (mCallQualityUpdater != null) {
			refreshHandler.removeCallbacks(mCallQualityUpdater);
			mCallQualityUpdater = null;
		}
		if (mControls != null) {
			controlsHandler.removeCallbacks(mControls);
			mControls = null;
		}

		if (mWakeLock.isHeld())
			mWakeLock.release();
		super.onPause();
		if (mVideoViewReady != null)
			((GLSurfaceView) mVideoViewReady).onPause();
		
		isTouchOk = false;
	}

	@Override
	public void onCallStateChanged(LinphoneCall call, State state,
			String message) {
		if (call == videoCall && state == State.CallEnd) {
			BandwidthManager.getInstance().setUserRestriction(false);
			LinphoneManager.getInstance().resetCameraFromPreferences();
			Log.d("VideoCallActivity():onCallStateChanged():finish()1");
			finish();
		}
		else if (!call.getCurrentParamsCopy().getVideoEnabled()) {
			//121119suzuki
			call.getCurrentParamsCopy().setVideoEnabled(true);
			Log.d("VideoCallActivity():onCallStateChanged():set video enabled!!");
//			Log.d("VideoCallActivity():onCallStateChanged():finish()2");
//			finish();
		}
	}
	
	private int dpToPixels(int dp){
        Resources r = getResources();
        int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, 
        r.getDisplayMetrics());
        return px;
	}

	private void resizePreview() {
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		
		int rotation = Compatibility.getRotation(display);
		LayoutParams params;

		int w, h;
		if (Version.isXLargeScreen(this)) {
			w = 176;
			h = 148;
		} else {
			w = 74;
			h = 88;
		}

		if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
			params = new LayoutParams(dpToPixels(h), dpToPixels(w));
		} else {
			params = new LayoutParams(dpToPixels(w), dpToPixels(h));
		}
		params.setMargins(0, 0, 15, 15);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		if (mVideoViewReady != null && mVideoCaptureViewReady != null)
			mVideoCaptureViewReady.setLayoutParams(params);
	}

	public void onConfigurationChanged(Configuration newConfig) {
		resizePreview();
		super.onConfigurationChanged(null);
	}

	private void resetControlsLayoutExpiration() {
		if (mControls != null) {
			controlsHandler.removeCallbacks(mControls);
		}

		controlsHandler.postDelayed(mControls = new Runnable() {
			public void run() {
				if(mControlsLayout != null)
					mControlsLayout.setVisibility(View.GONE);
			}
		}, DELAY_BEFORE_HIDING_CONTROLS);
	}
	
	private void setButton()
	{
		rewriteToggleCameraItem();
		rewriteSpekerStateItem();
//		rewriteChangeResolutionItem();

		if(!AndroidCameraConfiguration.hasFrontCamera()) {
			useFrontCameraButton.setVisibility(View.GONE);
		}
	}

	public void onClick(View v) {
		resetControlsLayoutExpiration();
		int id = v.getId();
//		if(id == R.id.incallButton)
//		{
//			finish();
//			LinphoneActivity.instance().startIncallActivity();
//		}
		
		if(isTouchOk == false) return;
		
		if(id == R.get("id", "terminalCallButton"))
		{
			Log.d("VideoCallActivity():Teminate!!!");
			LinphoneManager.getInstance().terminateCall();
			isTouchOk = false;
		}
		else if(id == R.get("id", "useFrontCamera"))
		{
			int videoDeviceId = LinphoneManager.getLc().getVideoDevice();
			videoDeviceId = (videoDeviceId + 1) % AndroidCameraConfiguration.retrieveCameras().length;
			LinphoneManager.getLc().setVideoDevice(videoDeviceId);
			CallManager.getInstance().updateCall();
			// previous call will cause graph reconstruction -> regive preview
			// window
//			prevWindowEnable(videoCall.cameraEnabled());
			if (mVideoCaptureViewReady != null)
				LinphoneManager.getLc()
						.setPreviewWindow(mVideoCaptureViewReady);
		}
//		else if(id == R.id.resolution)
//		{
//			LinphoneManager.getInstance().changeResolution();
//			// previous call will cause graph reconstruction -> regive preview
//			// window
//			prevWindowEnable(videoCall.cameraEnabled());
////			if (mVideoCaptureViewReady != null)
////				LinphoneManager.getLc()
////						.setPreviewWindow(mVideoCaptureViewReady);
//			rewriteChangeResolutionItem();
//		}
		else if(id == R.get("id", "enableCamera"))
		{
			boolean camEnabled = LinphoneManager.getInstance()
			.toggleEnableCamera();
			updatePreview(camEnabled);
			Log.e("winwow camera enabled: " + camEnabled);
			rewriteToggleCameraItem();
			// previous call will cause graph reconstruction -> regive preview
			// window
//			prevWindowEnable(camEnabled);
			if (camEnabled) {
				if (mVideoCaptureViewReady != null)
					LinphoneManager.getLc().setPreviewWindow(
							mVideoCaptureViewReady);
			} else
				LinphoneManager.getLc().setPreviewWindow(null);
		}
		else if(id == R.get("id" ,"spekerStateButton"))
		{
			changeSpeker(!isSpeker);
			rewriteSpekerStateItem();
		}
//		int id = v.getId();
//		if (id == R.id.incallHang) {
//			terminateCurrentCallOrConferenceOrAll();
//		}
//		else if (id == R.id.toggleSpeaker) {
//			if (((Checkable) v).isChecked()) {
//				LinphoneManager.getInstance().routeAudioToSpeaker();
//			} else {
//				LinphoneManager.getInstance().routeAudioToReceiver();
//			}
//		}
//		else if (id == R.id.incallNumpadShow) {
//			showDialog(numpadDialogId);
//		}
//		else if (id == R.id.toggleMuteMic) {
//			LinphoneManager.getLc().muteMic(((Checkable) v).isChecked());
//		}
//		else if (id == R.id.switch_camera) {
//			int videoDeviceId = LinphoneManager.getLc().getVideoDevice();
//			videoDeviceId = (videoDeviceId + 1) % AndroidCameraConfiguration.retrieveCameras().length;
//			LinphoneManager.getLc().setVideoDevice(videoDeviceId);
//			CallManager.getInstance().updateCall();
//			// previous call will cause graph reconstruction -> regive preview
//			// window
//			if (mVideoCaptureViewReady != null)
//				LinphoneManager.getLc()
//						.setPreviewWindow(mVideoCaptureViewReady);
//		}
//		else if (id == R.id.conf_simple_pause) {
//			finish();
//			LinphoneActivity.instance().startIncallActivity();
//			LinphoneManager.getLc().pauseCall(videoCall);
//		}
//		else if (id == R.id.conf_simple_video) {
//			LinphoneCallParams params = videoCall.getCurrentParamsCopy();
//			params.setVideoEnabled(false);
//			LinphoneManager.getLc().updateCall(videoCall, params);
//		}
//		else if (id == R.id.back) {
//			finish();
//			LinphoneActivity.instance().startIncallActivity();
//		}
//		else if(id == R.id.video_surface)
//		{
//			
//		}
	}

	protected Dialog onCreateDialog(final int id) {
		switch (id) {
		case numpadDialogId:
			Numpad numpad = new Numpad(this, true);
			return new AlertDialog.Builder(this)
					.setView(numpad)
					.setPositiveButton(R.get("string", "close_button_text"),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dismissDialog(id);
								}
							}).create();
		default:
			throw new IllegalArgumentException("unkown dialog id " + id);
		}
	}

//	private void terminateCurrentCallOrConferenceOrAll() {
//		LinphoneCall currentCall = LinphoneManager.getLc().getCurrentCall();
//		if (currentCall != null) {
//			LinphoneManager.getLc().terminateCall(currentCall);
//		} else if (LinphoneManager.getLc().isInConference()) {
//			LinphoneManager.getLc().terminateConference();
//		} else {
//			LinphoneManager.getLc().terminateAllCalls();
//		}
//		finish();
//	}
//	public void prevWindowEnable(boolean b)
//	{
//		if(b)
//		{
//			if (mVideoCaptureViewReady != null)
//				LinphoneManager.getLc().setPreviewWindow(
//						mVideoCaptureViewReady);
//		}
//		else
//		{
//			LinphoneManager.getLc().setPreviewWindow(null);
//		}
//	}
}
