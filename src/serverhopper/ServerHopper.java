package serverhopper;

// import arc.*;
// import arc.math.*;
// import arc.struct.*;
// import arc.util.*;
// import com.mongodb.client.model.UpdateOneModel;
// import com.mongodb.client.model.WriteModel;
import mindustry.Vars;
// import mindustry.content.*;
// import mindustry.core.GameState.*;
import mindustry.core.NetServer.*;
import mindustry.game.EventType.*;
// import mindustry.game.*;
// import mindustry.game.Schematic.*;
// import mindustry.game.Teams.*;
// import mindustry.gen.*;
// import mindustry.mod.*;
// import mindustry.net.Packets.*;
// import mindustry.type.*;
// import mindustry.world.*;
// import mindustry.world.blocks.storage.*;
// import org.bson.Document;

import java.util.*;

import static arc.util.Log.*;
// import static com.mongodb.client.model.Updates.*;
// import static com.mongodb.client.model.Updates.push;
// import static java.lang.Math.max;
import static mindustry.Vars.*;

public class ServerHopper extends Plugin{
    //in seconds
    // public static final float spawnDelay = 60 * 4;
    // //health requirement needed to capture a hex; no longer used
    // public static final float healthRequirement = 3500;
    // //item requirement to captured a hex
    // public static final int itemRequirement = 3; // was 80

    // public static final int messageTime = 1;
    // //in ticks: 60 minutes
    // private final static int roundTime = 60 * 60 * 30; // should be 60*60*30
    //in ticks: 2 minutes
    //private final static int leaderboardTime = 60 * 60 * 2;

    // private final static int updateTime = 60 * 2;
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

    // public ObjectSet<String> joinedPlayers = new ObjectSet<>();
    // private List<Long> allMMR = new ArrayList<>();
    // public HashMap<String, Long> PlayersMMR = new HashMap<>();

    // private String mongoURL = "";

    @Override
    public void init(){
      // every hexServerCooldown, ping the hex server to check if it's up. If it's up, set hexserveractive to true and move all players to the hex server. If it's not up, set hexserveractive to false and kick all players with a message.
        Timer.schedule(() -> { 
              Vars.net.pingHost(hexURL, hexPORT, host -> {
                  hexserveractive = true;
                  Call.infoMessage("[green]Hex server is up! Moving players to hex server...");
                  move_to_hex();
                  failedConnectionCounter = 0;
              }, e -> {
                  hexserveractive = false;
                  Call.infoMessage("[red]Hex server is rebooting! Please wait..." + (failedConnectionCounter > 0 ? " (Refreshed: " + failedConnectionCounter + ")" : ""));
                  failedConnectionCounter++;
              }); 
            
        }, 20*1000, 20*1000); // check every 20 seconds



        //MMRsystem = MMR_config.getInstance();
        // PlayersWhoLeft = new HashMap<>();
        // org.json.JSONObject configData = configReader.get("config.alex");
        // assert configData != null;
        // if (configData.has("mongoURL")) {
        //     mongoURL = configData.getString("mongoURL");
        // }


        Events.on(PlayerJoin.class, event -> {
          String playeruuid = event.player.uuid();
          // After a player joins, check every 10 seconds, if the server can be pinged.
          // If the server can be pinged, move the player to the hex server.
          // If the server cannot be pinged, keep the player on the current server and check again after 15 seconds.
            // String playeruuid = event.player.uuid();
            // if(active() && PlayersWhoLeft.containsKey(playeruuid)){
            //     int prevTeamid = PlayersWhoLeft.get(playeruuid);
            //     Team prevTeam = Team.get(prevTeamid);
            //     if (prevTeam==Team.derelict){
            //         PlayersWhoLeft.remove(playeruuid);
            //         return;
            //     }
            //     // event.player.unit().kill();
            //     // event.player.team(prevTeam);
            //     // event.player.sendMessage("Welcome back");
            //     // return;
            // }
        });


        // TeamAssigner prev = netServer.assigner;
        // netServer.assigner = (player, players) -> {
        //     if (PlayersWhoLeft.containsKey(player.uuid())){
        //         return Team.get(PlayersWhoLeft.get(player.uuid()));
        //     }
        //     Seq<Player> arr = Seq.with(players);
        //     Seq<Team> leftTeams = new Seq<>();
        //     for (String uuid:
        //          PlayersWhoLeft.keySet()) {
        //         leftTeams.add(Team.get(PlayersWhoLeft.get(uuid)));
        //     }
        //     if(active()){
        //         //pick first inactive team
        //         for(Team team : Team.all){
        //             if(team.id > 5 && !team.active() && !leftTeams.contains(team)
        //                     && !arr.contains(p -> p.team() == team)
        //                     && !data.data(team).dying
        //                     && !data.data(team).chosen){
        //                 data.data(team).chosen = true;
        //                 return team;
        //             }
        //         }
        //         Call.infoMessage(player.con, "There are currently no empty hex spaces available.\nAssigning into spectator mode.");
        //         return Team.derelict;
        //     }else{
        //         return prev.assign(player, players);
        //     }
        // };
    }

    private static void move_to_hex() {
        for (Player player : Groups.player){
          if (player!= null && player.con != null){
              Call.connect(player.con, hexURL, hexPORT); 
          } 
          // Vars.net.pingHost(hexURL, hexPORT, host -> {
          //   if (player!= null && player.con != null){
          //     Call.connect(player.con, hexURL, hexPORT); 
          //     } 
          // }, (e) -> {
          //     netServer.kickAll(KickReason.serverRestarting);
          // });
      }
    }

    // void updateText(Player player){
    //     HexTeam team = data.data(player);
    //     int minsleft=(int)(roundTime - counter) / 60 / 60;
    //     StringBuilder message = new StringBuilder("[white]Hex #" + team.location.id + " [teal]( "+minsleft+"mins left )\n");

    //     if(!team.lastMessage.get()) return;

    //     if(team.location.controller == null){
    //         if(team.progressPercent > 0){
    //             message.append("[lightgray]Capture progress: [accent]").append((int)(team.progressPercent)).append("%");
    //         }else{
    //             message.append("[lightgray][[Empty]");
    //         }
    //     }else if(team.location.controller == player.team()){
    //         message.append("[yellow][[Captured]");
    //     }else if(team.location != null && team.location.controller != null && data.getPlayer(team.location.controller) != null){
    //         message.append("[#").append(team.location.controller.color).append("]Captured by ").append(data.getPlayer(team.location.controller).name);
    //     }else{
    //         message.append("<Unknown>");
    //     }

    //     Call.setHudText(player.con, message.toString());
    // }

    // @Override
    // public void registerClientCommands(CommandHandler handler){

    //     handler.<Player>register("captured", "Dispay the number of hexes you have captured.", (args, player) -> {
    //         if(player.team() == Team.derelict){
    //             player.sendMessage("[scarlet]You're spectating.");
    //         }else{
    //             player.sendMessage("[lightgray]You've captured[accent] " + data.getControlled(player).size + "[] hexes.");
    //         }
    //     });

    // }

}