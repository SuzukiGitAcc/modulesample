package jp.co.aircast.module;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.view.TiUIView;
import org.linphone.LinphoneManager;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.mediastream.video.AndroidVideoWindowImpl;

import android.app.Activity;
import android.graphics.Canvas;
import android.opengl.GLSurfaceView;
import android.view.SurfaceView;

@Kroll.proxy(creatableInModule=ModulesampleModule.class)
public class VideoViewProxy extends TiViewProxy{
	private VideoView vv;
	AndroidVideoWindowImpl androidVideoWindowImpl;
//	private GLSurfaceView mVideoViewReady;
	
	public VideoViewProxy()
	{
		super();
	}

	public void destroy()
	{
		synchronized (androidVideoWindowImpl) {
			LinphoneManager.getLc().setVideoWindow(null);
		}
		
		if(androidVideoWindowImpl != null)
			androidVideoWindowImpl.release();
	}

	@Override
	public TiUIView createView(Activity a) {
		this.vv = new VideoView(this);
		this.vv.getLayoutParams().autoFillsHeight = true;
		this.vv.getLayoutParams().autoFillsWidth  = true;
		
		init();

		return vv;
	}
	
	@Override
	public void handleCreationDict(KrollDict options)
	{
		super.handleCreationDict(options);
	}

//	@Kroll.method
//	public void regist()
//	{
//		ALModuleProxy.getInstance().regist();
//	}
//	
//	@Kroll.method
//	public void call(String target)
//	{
//		ALModuleProxy.getInstance().call(target);
//	}

	@Kroll.method
	public void init()
	{
		androidVideoWindowImpl = new AndroidVideoWindowImpl(vv.getView(),
				null);
		androidVideoWindowImpl
				.setListener(new AndroidVideoWindowImpl.VideoWindowListener() {

					public void onVideoRenderingSurfaceReady(
							AndroidVideoWindowImpl vw, SurfaceView surface) {
//						surface.getHolder();
						LinphoneManager.getLc().setVideoWindow(vw);
					//	mVideoViewReady = surface;
					}

					public void onVideoRenderingSurfaceDestroyed(
							AndroidVideoWindowImpl vw) {
						LinphoneManager.getLc().setVideoWindow(null);
					}

					public void onVideoPreviewSurfaceReady(
							AndroidVideoWindowImpl vw, SurfaceView surface) {
//						mVideoCaptureViewReady = surface;
//						LinphoneManager.getLc().setPreviewWindow(
//								mVideoCaptureViewReady);
					}

					public void onVideoPreviewSurfaceDestroyed(
							AndroidVideoWindowImpl vw) {
						// Remove references kept in jni code and restart camera
						LinphoneManager.getLc().setPreviewWindow(null);
					}
				});
		androidVideoWindowImpl.init();
		synchronized (androidVideoWindowImpl) {
			LinphoneManager.getLc().setVideoWindow(androidVideoWindowImpl);
		}
		
		nowColler = 0;
	}
	
	@Kroll.method
	public void call(String target)
	{
		if(ALModuleProxy.getInstance() != null)
			ALModuleProxy.getInstance().call(target, ALModuleProxy._CHATCHER, true, "", "", "", "");
		nowColler = 0;
		
		mThread = new CheckCallPause();
		mThread.start();
	}
	
	private int nowColler = 0;
	@Kroll.method
	public void nextCaller()
	{
		LinphoneCore lc = LinphoneManager.getLc();
		int callNB = lc.getCallsNb();

		nowColler = (nowColler + 1) % callNB;
		setCaller(nowColler);
		
//		if(callNB >= 2)
//		{
//			lc.pauseAllCalls();
//			nowColler++;
//			lc.resumeCall(lc.getCalls()[(nowColler) % callNB]);
//		}
	}
	
	@Kroll.method
	public void setCaller(int caller)
	{
		LinphoneCore lc = LinphoneManager.getLc();
		int callNB = lc.getCallsNb();
		
		if(caller < 0 || caller >= callNB)
			return;
		
		lc.pauseAllCalls();
		lc.resumeCall(lc.getCalls()[caller]);
	}
	
	@Kroll.method
	public void terminate()
	{
		LinphoneManager.getLc().terminateAllCalls();
		destroy();
		ALModuleProxy.mode = ALModuleProxy._NOMAL;
	}
	
	private CheckCallPause mThread;
	private class CheckCallPause extends Thread {
		public void run(){
			while(true)
			{
				try {
					sleep(30);
				} catch (InterruptedException e) {
					throw new RuntimeException("waiting thread sleep() has been interrupted");
				}
				
				LinphoneCore lc = LinphoneManager.getLc();
				int callsNb = lc.getCallsNb();
				int i;
				LinphoneCall call;
				for(i = 0; i < callsNb; ++i)
				{
					call = lc.getCalls()[i];
					
					if(call.getState() != LinphoneCall.State.Paused)
						break;
				}
				
				if(i >= callsNb)
				{
					ALModuleProxy.mode = ALModuleProxy._STOP;
					nowColler = 0;
					setCaller(nowColler);
					mThread = null;
					break;
				}
			}
		}
	}
}
