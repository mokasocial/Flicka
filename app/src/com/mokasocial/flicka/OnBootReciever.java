package com.mokasocial.flicka;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootReciever extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		NotifyMgmt.initNotifications(context);
	}
}
