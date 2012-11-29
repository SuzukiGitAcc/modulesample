package jp.co.aircast.module;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement;
import org.linphone.LinphoneSimpleListener.LinphoneOnCallStateChangedListener;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.mediastream.video.display.GL2JNIView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.view.SurfaceView;
import android.view.View;

public class VideoView extends TiUIView {
	CostomView cv;
	
	public VideoView(TiViewProxy proxy) {
		super(proxy);
		LayoutArrangement arrangement = LayoutArrangement.DEFAULT;
		
		if(proxy.hasProperty(TiC.PROPERTY_LAYOUT))
		{
			String layoutProperty = TiConvert.toString(proxy.getProperty(TiC.PROPERTY_LAYOUT));
			if(layoutProperty.equals(TiC.LAYOUT_HORIZONTAL))
			{
				arrangement = LayoutArrangement.HORIZONTAL;
			}
			else if(layoutProperty.equals(TiC.LAYOUT_VERTICAL))
			{
				arrangement = LayoutArrangement.VERTICAL;
			}
		}
		
		this.cv = new CostomView(proxy.getActivity().getApplicationContext());
		
		cv.setZOrderOnTop(false);
		setNativeView(this.cv);
	}

//	public class CostomView extends SurfaceView {
	public class CostomView extends GL2JNIView {		
		public CostomView(Context c)
		{
			super(c);
//			this.setBackgroundColor(Color.BLACK);
		}
	}
	
	public CostomView getView()
	{
		return this.cv;
	}
}
