package android.servervoting;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class Voter {

    public static final int initialTimeoutMs = 20000;
    public static final int maxNumRetries = 0;

    Account account;
    Server server;
    AtomicInteger finished;
    RequestQueue queue;
    Map<String, String> cookies;
    TextView errorView;
    Queue<String> idsToVote;
    Callback callback;

    public Voter(Account account, Server server, Context context, TextView errorView, Callback callback) {
        this.account = account;
        this.server = server;
        this.finished = new AtomicInteger(0);
        this.queue = Volley.newRequestQueue(context);
        this.cookies = new HashMap<>();
        this.errorView = errorView;
        this.idsToVote = new LinkedList<>();
        this.idsToVote.addAll(server.votingids);
        this.callback = callback;
    }

    public void vote() {
        openLandingPage();
    }

    private void logError(String tag, String message) {
        this.errorView.setText("Exception in " + tag + ": " + message);
        this.errorView.setVisibility(View.VISIBLE);
        finish(false);
    }

    private void openLandingPage() {
        StringRequest req = new StringRequestWithHeaders(Request.Method.GET, this.server.landingpage,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d("openLandingPage", "successful.");
                    Log.d("openLandingPage", "Cookies: " + Voter.this.cookies.toString());
                    login();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("openLandingPage", error.toString());
                    logError("openLandingPage", error.getMessage());
                }
            },Voter.this){

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<String, String>();
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> cookie : Voter.this.cookies.entrySet()) {
                    sb.append(cookie.getKey() + "=" + cookie.getValue() + "; ");
                }
                headers.put("Cookie", sb.toString());
                return headers;
            }
        };
        this.queue.add(req);
    }

    private void login() {
        StringRequest req = new StringRequest(Request.Method.POST, this.server.loginurl,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d("login", "successful.");
                    openVotingPage();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("login", error.toString());
                    logError("login", error.getMessage());
                }
            }
        ){
            @Override
            protected Map<String,String> getParams(){
                Map<String, String> params = new HashMap<String, String>();
                params.put("csrf_token_name", Voter.this.cookies.get("csrf_cookie_name"));
                params.put("login_username", Voter.this.account.name);
                params.put("login_password", Voter.this.account.password);
                params.put("login_submit", "Log+in!");
                return params;
            }
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> headers = new HashMap<String, String>();
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> cookie : Voter.this.cookies.entrySet()) {
                    sb.append(cookie.getKey() + "=" + cookie.getValue() + "; ");
                }
                headers.put("Cookie", sb.toString());
                return headers;
            }
        };
        this.queue.add(req);
    }

    private void openVotingPage() {
        if (this.server.votingpage == null || this.server.votingpage.isEmpty()) {
            callVoting();
        } else {
            StringRequest req = new StringRequest(Request.Method.GET, this.server.votingpage,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            callVoting();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("openVotingPage", error.toString());
                            logError("openVotingPage", error.getMessage());
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> cookie : Voter.this.cookies.entrySet()) {
                        sb.append(cookie.getKey() + "=" + cookie.getValue() + "; ");
                    }
                    headers.put("Cookie", sb.toString());
                    return headers;
                }
            };
            this.queue.add(req);
        }
    }

    private void callVoting() {
        for (int i = 1; i <= this.server.votingids.size(); i++) {
            StringRequest req = new StringRequest(Request.Method.POST, this.server.votingurl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            int numFinished = Voter.this.finished.incrementAndGet();
                            if (numFinished == 4) {
                                Log.d("callVoting", "successful vote.");
                                finish(true);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("callVoting", error.toString());
                            logError("callVoting", error.getMessage());
                        }
                    }
            ) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<String, String>();
                    String id = Voter.this.idsToVote.poll();
                    Log.d("callVoting.getParams", "Voting for id: " + id);
                    params.put("csrf_token_name", Voter.this.cookies.get("csrf_cookie_name"));
                    params.put("id", id);
                    params.put("isFirefoxHerpDerp", "true");
                    return params;
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<String, String>();
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> cookie : Voter.this.cookies.entrySet()) {
                        sb.append(cookie.getKey() + "=" + cookie.getValue() + "; ");
                    }
                    headers.put("Cookie", sb.toString());
                    return headers;
                }
            };
            req.setRetryPolicy(new DefaultRetryPolicy(initialTimeoutMs, maxNumRetries,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            this.queue.add(req);
        }
    }

    private void finish(boolean success) {
        callback.call(success);
    }
}
