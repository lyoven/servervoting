package android.servervoting;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Context.NOTIFICATION_SERVICE;

public class AlarmReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        // build intent that is triggered when notification is selected
        Intent triggered = new Intent(context, VotingActivity.class);
        PendingIntent pending = PendingIntent.getActivity(context, 0, triggered, 0);

        // build notification
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentTitle("Voting ready!")
                .setContentText("Ready for next round of voting.")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setVibrate(new long[] { 0, 100, 100, 100, 500, 100})
                .build();
        NotificationManager nMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        nMgr.notify(0, notification);
    }

}
