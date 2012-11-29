/*
LinphoneLauncher.java
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

import static android.content.Intent.ACTION_MAIN;
import jp.co.aircast.module.ALModuleProxy;
import jp.co.aircast.module.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

/**
 * 
 * Launch Linphone main activity when Service is ready.
 * 
 * @author Guillaume Beraudo
 *
 */
public class LinphoneLauncherActivity extends Activity {

	private Handler mHandler;
	private ServiceWaitThread mThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("onModule", "LinphoneLauncherActivity:onCreate()");
		
		super.onCreate(savedInstanceState);
		setContentView(R.get("layout", "launcher"));
		
		mHandler = new Handler();

		if (LinphoneService.isReady()) {
			onServiceReady();
		} else {
			// start linphone as background  
			startService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
			mThread = new ServiceWaitThread();
			mThread.start();
		}
	}

	protected void onServiceReady() {
//		Intent intent = new Intent();
//		intent.addCategory(Intent.CATEGORY_LAUNCHER);
//		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//		intent.setClassName(ALModuleProxy.PKG_NAME, ALModuleProxy.PKG_NAME + "." + ALModuleProxy.ACT_NAME);
//		startActivity(intent);
		
//		LinphoneService.instance().setActivityToLaunchOnIncomingReceived(LinphoneActivity.class);
//		
//		startActivity(new Intent()
//		.setClass(this, LinphoneActivity.class)
//		.setData(getIntent().getData()));

		finish();
	}

	private class ServiceWaitThread extends Thread {
		public void run() {
			while (!LinphoneService.isReady()) {
				try {
					sleep(30);
				} catch (InterruptedException e) {
					throw new RuntimeException("waiting thread sleep() has been interrupted");
				}
			}

			mHandler.post(new Runnable() {
				@Override
				public void run() {
					onServiceReady();
				}
			});
			mThread = null;
		}
	}
}


