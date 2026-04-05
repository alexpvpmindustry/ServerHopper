package serverhopper;

import arc.*;
//import arc.math.*;
//import arc.struct.*;
import arc.util.*;
// import com.mongodb.client.model.UpdateOneModel;
// import com.mongodb.client.model.WriteModel;
import mindustry.Vars;
//import mindustry.content.*;
//import mindustry.core.GameState.*;
//import mindustry.core.NetServer.*;
import mindustry.game.EventType.*;
//import mindustry.game.*;
//import mindustry.game.Schematic.*;
//import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.mod.*;
//import mindustry.net.Packets.*;
//import mindustry.type.*;
//import mindustry.world.*;
// import mindustry.world.blocks.storage.*;
// import org.bson.Document;

// import java.util.Timer;
// import java.util.TimerTask;

//import static arc.util.Log.*;
// import static com.mongodb.client.model.Updates.*;
// import static com.mongodb.client.model.Updates.push;
//import static java.lang.Math.max;
//import static mindustry.Vars.*;

public class ServerHopper extends Plugin{
    //private final static int leaderboardTime = 60 * 60 * 2;
    private boolean hexserveractive = false;
    // private CooldownTimer hexServerCooldown = new CooldownTimer(60 * 20);
    // 20 seconds cooldown before trying to connect to hex server again after a failed attempt
    private int failedConnectionCounter = 0;
    // private double counter = 0f;
    // private int lastMin;
    // public HashMap<String, Integer> PlayersWhoLeft;
    //public MMR_config MMRsystem;
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
        arc.util.Time.runTask(20f, () -> { // loops every 20 seconds
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