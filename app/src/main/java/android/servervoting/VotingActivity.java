package android.servervoting;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class VotingActivity extends AppCompatActivity {

    public static int MAX_CONNECTION_RETRIES = 2;
    public static int MAX_WAIT_INTERVALS = 10;
    public static int WAIT_INTERVAL_MS = 1000;

    private static String nextVoteKey = "nextVoteTimestamp";

    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, HH:mm");
    SharedPreferences preferences = null;
    Queue<Account> accounts = null;
    Map<String, Server> servers = null;
    private boolean dataConnectionOn;
    private int numConnectionRetries;
    private int numWaitInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getPreferences(Context.MODE_PRIVATE);
        setContentView(R.layout.activity_voting);
        Long nextVoteTimestamp = preferences.getLong(nextVoteKey, 0);
        TextView nextVoteView = findViewById(R.id.nextVoteText);
        String s = getString(R.string.ready);
        if (new Date().getTime() < nextVoteTimestamp) {
            s = getString(R.string.nextVote) + " " + dateFormat.format(new Date(nextVoteTimestamp));
        }
        nextVoteView.setText(s);
    }

    public void startVoting(View view) {
        Log.d("startVoting", "voting started");
        TextView errorView = findViewById(R.id.errorText);
        TextView nextVoteView = findViewById(R.id.nextVoteText);
        ProgressBar progress = findViewById(R.id.progressBar);

        // update display
        nextVoteView.setText(getString(R.string.started));
        errorView.setText("");
        errorView.setVisibility(View.INVISIBLE);

        // load accounts from json
        this.accounts = new LinkedList<>();
        JsonResources json = new JsonResources(getResources());
        try {
            for (Map<String, String> obj : json.loadArray(R.raw.accounts)) {
                Account acc = new Account(obj.get("name"), obj.get("pw"), obj.get("server"));
                this.accounts.add(acc);
            }
        } catch (IOException e) {
            errorView.setText(e.getMessage());
            errorView.setVisibility(View.VISIBLE);
            nextVoteView.setText("Error occurred when loading accounts.");
            return;
        }

        // load servers from json
        this.servers = new HashMap<>();
        try {
            for (Map.Entry<String, Map<String, String>> pair : json.loadObject(R.raw.servers).entrySet()) {
                Map<String, String> values = pair.getValue();
                Server srvr = new Server(values.get("landingpage"), values.get("loginurl"),
                        values.get("votingpage"), values.get("votingurl"), values.get("votingids"));
                this.servers.put(pair.getKey(), srvr);
            }
        } catch (IOException e) {
            errorView.setText(e.getMessage());
            errorView.setVisibility(View.VISIBLE);
            nextVoteView.setText("Error occurred when loading servers.");
            return;
        }

        // check if any accounts registered
        if (this.accounts.isEmpty()) {
            nextVoteView.setText("No accounts registered.");
            return;
        }

        // init progress
        progress.setProgress(0);
        progress.setMax(this.accounts.size());
        progress.setVisibility(View.VISIBLE);

        // init network state
        this.dataConnectionOn = false;
        this.numConnectionRetries = 0;
        this.numWaitInterval = 0;

        // start voting
        prepareVote();
    }

    public void reset(View view) {
        Log.d("reset", "view reset");
        TextView errorView = findViewById(R.id.errorText);
        TextView nextVoteView = findViewById(R.id.nextVoteText);
        TextView statusView = findViewById(R.id.statusText);
        ProgressBar progress = findViewById(R.id.progressBar);
        errorView.setText("");
        errorView.setVisibility(View.INVISIBLE);
        nextVoteView.setText(getString(R.string.ready));
        statusView.setText("");
        statusView.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.INVISIBLE);
    }


    private void waitForNetwork() {
        Log.d("waitForNetwork", "called");
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean flag = MobileConnection.isConnected(getApplicationContext());
                if (dataConnectionOn == flag) {
                    numWaitInterval = 0;
                    startNextAction(flag);
                } else if (numWaitInterval < MAX_WAIT_INTERVALS) {
                    numWaitInterval++;
                    waitForNetwork();
                } else if (numConnectionRetries < MAX_CONNECTION_RETRIES) {
                    numConnectionRetries++;
                    resetNetwork();
                } else {
                    TextView errorView = findViewById(R.id.errorText);
                    errorView.setText("Waiting for network took too long.");
                    errorView.setVisibility(View.VISIBLE);
                    finishVoting();
                }
            }
        }, WAIT_INTERVAL_MS);
    }

    private void resetNetwork() {
        Log.d("resetNetwork", "resetting network");
        try {
            MobileConnection.set(getApplicationContext(), !dataConnectionOn);
            MobileConnection.set(getApplicationContext(), dataConnectionOn);
        } catch (Exception e) {
            TextView errorView = findViewById(R.id.errorText);
            errorView.setText("Error resetting connection: " + e.getMessage());
            errorView.setVisibility(View.VISIBLE);
            finishVoting();
        }
        waitForNetwork();
    }

    private void startNextAction(boolean isConnected) {
        Log.d("startNextAction", "startingNextAction");
        if (isConnected) {
            startVoter();
        } else {
            prepareVote();
        }
    }

    private void prepareVote() {
        Log.d("prepareVote", "preparing");
        Account acc = this.accounts.peek();
        if (acc == null) {
            finishVoting();
        } else {
            Log.d("prepareVote", "setting mobile connection to true");
            TextView statusView = findViewById(R.id.statusText);
            statusView.setText(getString(R.string.currentAcc) + " " + acc.name);
            statusView.setVisibility(View.VISIBLE);
            this.dataConnectionOn = true;
            try {
                MobileConnection.set(getApplicationContext(), true);
            } catch (Exception e) {
                TextView errorView = findViewById(R.id.errorText);
                errorView.setText("Couldn't enable data connection.");
                errorView.setVisibility(View.VISIBLE);
                return;
            }
            waitForNetwork();
        }
    }

    private void startVoter() {
        Log.d("startVoter", "starting voter");
        TextView errorView = findViewById(R.id.errorText);
        Account acc = this.accounts.poll();
        Voter voter = new Voter(acc, this.servers.get(acc.server), getApplicationContext(),
                errorView, new FinishPreviousVote());
        voter.vote();
    }

    public class FinishPreviousVote implements Callback {

        @Override
        public void call(boolean flag) {
            Log.d("FinishPreviousVote", "updating progress, terminating connection");
            if (flag) {
                ProgressBar progress = findViewById(R.id.progressBar);
                progress.setProgress(progress.getProgress() + 1);
            }

            // disable data connection
            VotingActivity.this.dataConnectionOn = false;
            try {
                MobileConnection.set(getApplicationContext(), false);
            } catch (Exception e) {
                TextView errorView = findViewById(R.id.errorText);
                errorView.setText("Couldn't disable data connection.");
                errorView.setVisibility(View.VISIBLE);
                return;
            }
            waitForNetwork();
        }
    }

    private void finishVoting() {
        Log.d("finishVoting", "finish voting");
        TextView nextVoteView = findViewById(R.id.nextVoteText);
        TextView statusView = findViewById(R.id.statusText);
        ProgressBar progress = findViewById(R.id.progressBar);

        // voting finished
        statusView.setText(getString(R.string.finished));
        statusView.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.INVISIBLE);

        // show time of next vote
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR_OF_DAY, 12);
        Date nextVote = cal.getTime();
        nextVoteView.setText(getString(R.string.nextVote) + " " + dateFormat.format(nextVote));

        // save time of next voting
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(nextVoteKey, nextVote.getTime());
        editor.commit();

        // add alarm to AlarmManager
        Context context = getApplicationContext();
        AlarmManager aMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent, 0);
        aMgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pending);
    }

}