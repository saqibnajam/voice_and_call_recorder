package com.recorder.voicerecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class CallReceiver extends BroadcastReceiver {

    private static boolean isRecording = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            int state = -1;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }
            onCallStateChanged(context, state);
        }
    }

    private void onCallStateChanged(Context context, int state) {
        SharedPreferences prefs = context.getSharedPreferences("VoiceRecorderPrefs", Context.MODE_PRIVATE);
        boolean isCallRecordingEnabled = prefs.getBoolean("call_recording_enabled", false);

        if (!isCallRecordingEnabled) {
            return;
        }

        switch (state) {
            case TelephonyManager.CALL_STATE_OFFHOOK:
                if (!isRecording) {
                    Intent serviceIntent = new Intent(context, RecordingService.class);
                    context.startService(serviceIntent);
                    isRecording = true;
                    Toast.makeText(context, "Call Recording Started", Toast.LENGTH_SHORT).show();
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                if (isRecording) {
                    Intent serviceIntent = new Intent(context, RecordingService.class);
                    context.stopService(serviceIntent);
                    isRecording = false;
                    Toast.makeText(context, "Call Recording Stopped & Saved", Toast.LENGTH_SHORT).show();
                }
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                // Nothing to do here for now
                break;
        }
    }
}
