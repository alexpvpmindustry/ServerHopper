package serverhopper;

import arc.util.*;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.mod.*;

public class ServerHopper extends Plugin{
    //private final static int leaderboardTime = 60 * 60 * 2;
    private boolean hexserveractive = false;
    private int failedConnectionCounter = 0;
    // private static final String hexURL = "172.245.187.143"; // attack usa 
    // private static final int hexPORT = 25588; // attack usa
    //private static final String hexURL = "92.119.127.171"; // racknerd FN test server
    //private static final int hexPORT = 6889; // racknerd FN test server
    private static final String hexURL = "172.245.187.143"; // hex
    private static final int hexPORT = 6868; // hex
    @Override
    public void init(){
        schedulePing();
    }

    private void schedulePing(){
        arc.util.Time.runTask(60f*20f, () -> { // loops every 20 seconds
            Log.info("ping hex server %s".formatted(hexserveractive));
            Vars.net.pingHost(hexURL, hexPORT, result -> {
                hexserveractive = true;
                Call.infoMessage("[green]Hex server is up! Moving players to hex server...");
                move_to_hex();
                failedConnectionCounter = 0;
            }, e -> {
                hexserveractive = false;
                Call.infoMessage("[red]Hex server is rebooting! Please wait..." +
                        (failedConnectionCounter > 0 ? " (Refreshed: " + failedConnectionCounter + ")" : ""));
                failedConnectionCounter++;
            });

            // reschedule itself (loop)
            schedulePing();
        });
    }

    private static void move_to_hex() {
        for (Player player : Groups.player){
          if (player!= null && player.con != null){
              Call.connect(player.con, hexURL, hexPORT); 
          }
      }
    }
}