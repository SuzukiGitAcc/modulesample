
package jp.co.aircast.module;

import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;

import android.util.Log;

public class R {

	public static int get(String type, String name)
	{
		int ret = 0;
		
		try{
			ret = TiRHelper.getApplicationResource(type + "." + name);
		} catch (ResourceNotFoundException e) {
			e.printStackTrace();
			Log.d("", e.getMessage());
		}
		return ret;
	}

	private static KrollProxy krollProxy;
	public static void setKrollProxy(KrollProxy krollProxy)
	{
		R.krollProxy = krollProxy;
	}

	public static final String REGIST_SUCCESS = "regist_success";
	public static final String REGIST_FAIL    = "regist_fail";
	public static void fireEvent(String name, Object args[])
	{
		if(krollProxy == null) return;
		
		Object value = krollProxy.getProperty(name);
		if(value != null && value instanceof KrollFunction)
		{
			KrollFunction kf = (KrollFunction)value;
			kf.callAsync(krollProxy.getKrollObject(), args);
		}
	}
}
