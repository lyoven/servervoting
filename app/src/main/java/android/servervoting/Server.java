package android.servervoting;

import java.util.ArrayList;
import java.util.List;

public class Server {

    String landingpage;
    String loginurl;
    String votingpage;
    String votingurl;
    List<String> votingids;


    public Server(String lp, String lu, String vp, String vu, String vis) {
        this.landingpage = lp;
        this.loginurl = lu;
        this.votingpage = vp;
        this.votingurl = vu;
        this.votingids = new ArrayList<>();
        String[] ids = vis.split(",");
        for (String id : ids) {
            this.votingids.add(id.trim());
        }
    }

}
