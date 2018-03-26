package android.servervoting;

import com.android.volley.Header;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

public class StringRequestWithHeaders extends StringRequest {

    Voter voter;

    public StringRequestWithHeaders(int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener, Voter voter) {
        super(method, url, listener, errorListener);
        this.voter = voter;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        Response<String> stringResponse = super.parseNetworkResponse(response);

        // save csrf cookie name
        for (Header h : response.allHeaders) {
            if (h.getName().equals("Set-Cookie")) {
                String[] cookie = h.getValue().split(";")[0].split("=");
                this.voter.cookies.put(cookie[0], cookie[1]);
            }
        }
        return stringResponse;
    }

}
