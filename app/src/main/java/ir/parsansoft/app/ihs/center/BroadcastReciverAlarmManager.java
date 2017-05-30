package ir.parsansoft.app.ihs.center;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.sql.Date;
import java.text.SimpleDateFormat;

import static ir.parsansoft.app.ihs.center.G.sendCrashLog;

public class BroadcastReciverAlarmManager extends BroadcastReceiver {

    private final static String DES_TIME = "DESCRIPTION";
    private final static String REQ_TIME = "REQUEST_CODE";
    private Context context;

    public BroadcastReciverAlarmManager() {
        context = G.context;
    }

    /**
     * in method zamani ejra mishe k zamane ejraye scenarioi
     * k bar asase zaman time bandi shode berese
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        //You can do the processing here update the widget/remote views.
        //        G.log("The Alarm Manager has ran the ring !");
        try {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                int requestCode = extras.getInt(REQ_TIME, -1);

                String description = extras.getString(DES_TIME, "");
                if (description.equals("delay")) {
                    Database.Scenario.Struct scenario = Database.Scenario.select(requestCode);
                    G.scenarioBP.runScenario(scenario);

                    return;
                }

                // niaz be check kardane sharte khasi baraye ejra nadarim
                // ghablan hame shart ha check shodan
                G.scenarioBP.runByTime(requestCode);
            }
        } catch (Exception e) {
            G.printStackTrace(e);
            sendCrashLog(e, "تایمر تنظیم شده یا تاخیر ایجاد شده در سناریو به مشکل بر خورد!", Thread.currentThread().getStackTrace()[2]);
        }
    }

    public void SetRepeatAlarm(int requestCode, String description, long startTimeInMillis, long RepeatTimeInMillis) {
        G.log("Set Alarm Manager to ring at next " + startTimeInMillis + " milliseconds then repeat every " + RepeatTimeInMillis + "milliseconds .");

        //\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\
        long yourmilliseconds = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
        Date resultdate = new Date(yourmilliseconds);
        G.log("Milliseconds : " + sdf.format(resultdate));
        //\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\


        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BroadcastReciverAlarmManager.class);
        intent.putExtra(DES_TIME, description);
        intent.putExtra(REQ_TIME, requestCode);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, startTimeInMillis, RepeatTimeInMillis, pi);

    }

    public void CancelAlarm(int requestCode) {
        Intent intent = new Intent(context, BroadcastReciverAlarmManager.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, requestCode, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    /**
     * methode set baraye in ast k faghat 1 bar ejra shavad va dg repeat nemishe
     */
    public void setOnetimeTimer(int requestCode, String description, long RepeatTimeInMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, BroadcastReciverAlarmManager.class);
        intent.putExtra(DES_TIME, description);
        intent.putExtra(REQ_TIME, requestCode);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, 0);
        am.set(AlarmManager.RTC_WAKEUP, RepeatTimeInMillis, pi);
    }
}
