package jp.co.aircast.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class MakeIntentActivity extends Activity {
	private Map map = new HashMap();
	static String TAG = "MakeIntentActivity";
	
	boolean isEnabled = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		readFile();

		PackageManager pm = getPackageManager();
		Intent intent = makeIntent();
		List<ResolveInfo> activities = pm.queryIntentActivities(
				intent, 0);
		isEnabled = (activities.size() != 0);
		
		if(isEnabled)
			start(activities);
		else
			sendResult(null, isEnabled, false);
	}
	
	Intent makeIntent()
	{
		Intent from = getIntent();
		String type = from.getStringExtra("type");
		if(type == null) type = "video/*";
		
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(type);
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		
		return intent;
	}
	
	void start(List<ResolveInfo> activities)
	{
		Intent intent = makeIntent();
		int num = activities.size();
		String type = getType();
		
		Log.d(TAG, "start():type = " + type);
		
		if(num >= 2 && type != null)
		{
			String name = (String) map.get(Build.MODEL);
			Log.d(TAG, "start():name = " + name);
			if(name != null)
			{
				for(int i = 0; i < num; ++i)
				{
					ResolveInfo ri = activities.get(i);
					if(ri.activityInfo.name.equals(name) == true)
					{
						intent.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
						Log.d(TAG, "start():hit!!");
						break;
					}
				}
			}
//			for(int i = 0; i < num; ++i)
//			{
//				ResolveInfo ri = activities.get(i);
//				
//				Log.d("MakeIntentActivity", ri.activityInfo.name);
//				Log.d("MakeIntentActivity", ri.activityInfo.packageName);
//				
//				if(ri.activityInfo.name.endsWith("Gallery"))
//				{
//					intent.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
//					break;
//				}
//			}
		}
		
		startActivityForResult(intent, 100);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == 100)
		{
			if(resultCode == RESULT_OK)
			{
				String path = data.getData().toString();
				sendResult(path, true, false);
			}
			if(resultCode == RESULT_CANCELED)
				sendResult("", true, true);
		}
	}
	
	private void sendResult(String path, boolean enabled, boolean canceled)
	{
		Intent form = getIntent();
		Messenger msger = form.getParcelableExtra("VOICE_RESULT_MESSENGER");
		Message msg = new Message();
		Bundle bundle = new Bundle();
        bundle.putBoolean("VOICE_RECOGNITION_CANCELED", canceled);
        bundle.putBoolean("VOICE_RECOGNITION_ENABLED", enabled);
        bundle.putString("VOICE_RESULT", path);
        msg.setData(bundle);
        try {
            msger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        finish();
	}
	
	private String getType()
	{
		Intent from = getIntent();
		String type = from.getStringExtra("type");
		
		Log.d(TAG, "getType():type = " + type);
		
		if(type.startsWith("video/") == true)
			return "video";
		else if(type.startsWith("audio/") == true)
			return "audio";
		
		return null;
	}
	
	private void readFile()
	{
		try {
			String type = getType();
			
			InputStream is = getAssets().open("value/" + type + ".xml");
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line = "";
			map.clear();
			
			while((line = br.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, ",");
				
				if(!st.hasMoreElements()) break;
				
				String key = st.nextToken();
				String val = st.nextToken();
				
				map.put(key, val);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Log.d(TAG, "readFile():map.size = " + map.size());
	}
}
