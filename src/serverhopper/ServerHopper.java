package serverhopper;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.core.NetServer.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Packets.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import org.bson.Document;

import java.util.*;

import static arc.util.Log.*;
import static com.mongodb.client.model.Updates.*;
import static com.mongodb.client.model.Updates.push;
import static java.lang.Math.max;
import static mindustry.Vars.*;

public class ServerHopper extends Plugin{
    //in seconds
    public static final float spawnDelay = 60 * 4;
    //health requirement needed to capture a hex; no longer used
    public static final float healthRequirement = 3500;
    //item requirement to captured a hex
    public static final int itemRequirement = 3; // was 80

    public static final int messageTime = 1;
    //in ticks: 60 minutes
    private final static int roundTime = 60 * 60 * 30; // should be 60*60*30
    //in ticks: 2 minutes
    private final static int leaderboardTime = 60 * 60 * 2;

    private final static int updateTime = 60 * 2;

    private final static int winCondition = 25;

    private final static int timerBoard = 0, timerUpdate = 1, timerWinCheck = 2;

    private final Rules rules = new Rules();
    private Interval interval = new Interval(5);

    private HexData data;
    private boolean restarting = false, registered = false;

    private Schematic start,start1,midgamestart,worldblock;
    private double counter = 0f;
    private int lastMin;
    public HashMap<String, Integer> PlayersWhoLeft;
    //public MMR_config MMRsystem;
    private static final String hubURL = "172.245.187.143"; // attack usa 
    private static final int hubPORT = 25588; // attack usa
    //private static final String hubURL = "92.119.127.171"; // racknerd FN test server
    //private static final int hubPORT = 6889; // racknerd FN test server

    public ObjectSet<String> joinedPlayers = new ObjectSet<>();
    private List<Long> allMMR = new ArrayList<>();
    public HashMap<String, Long> PlayersMMR = new HashMap<>();

    public MMR_mongo mmrmongo;
    private String mongoURL = "";

    @Override
    public void init(){
        //MMRsystem = MMR_config.getInstance();
        PlayersWhoLeft = new HashMap<>();
        org.json.JSONObject configData = configReader.get("config.alex");
        assert configData != null;
        if (configData.has("mongoURL")) {
            mongoURL = configData.getString("mongoURL");
            //mmrmongo = new MMR_mongo(mongoURL);
        }

        Events.on(BlockDestroyEvent.class, event -> {
            //reset last spawn times so this hex becomes vacant for a while.
            if(event.tile.block() instanceof CoreBlock){
                Hex hex = data.getHex(event.tile.pos());

                if(hex != null){
                    //update state
                    hex.spawnTime.reset();
                    // this is redundant hex.updateController(data.hexcounts_per_team);
                    data.updateControl();
                    clearTilesInHex(hex);
                    // todo destroy half the units in the block
                }
            }
        });

        Events.on(PlayerLeave.class, event -> {
            if(active() && event.player.team() != Team.derelict){
                // old ver
                // killTiles(event.player.team());
                PlayersWhoLeft.put(event.player.uuid(),event.player.team().id);
            }
        });
        Events.on(EventType.Trigger.class,event->{
            if(event.equals(Trigger.newGame)){
                Log.info("new game triggered, wiping player stats only");
                clear_player_data();
                // reloadplayerhex();
            }
        });
        Events.on(PlayerJoin.class, event -> {
            String playeruuid = event.player.uuid();
            if(active() && PlayersWhoLeft.containsKey(playeruuid)){
                int prevTeamid = PlayersWhoLeft.get(playeruuid);
                Team prevTeam = Team.get(prevTeamid);
                if (prevTeam==Team.derelict){
                    PlayersWhoLeft.remove(playeruuid);
                    return;
                }
                event.player.unit().kill();
                event.player.team(prevTeam);
                event.player.sendMessage("Welcome back");
                return;
            }
            assign_hex_to_new_joins(event.player);
        });

        Events.on(ProgressIncreaseEvent.class, event -> updateText(event.player));

        Events.on(HexCaptureEvent.class, event -> updateText(event.player));
        Events.on(HexMoveEvent.class, event -> updateText(event.player));

        TeamAssigner prev = netServer.assigner;
        netServer.assigner = (player, players) -> {
            if (PlayersWhoLeft.containsKey(player.uuid())){
                return Team.get(PlayersWhoLeft.get(player.uuid()));
            }
            Seq<Player> arr = Seq.with(players);
            Seq<Team> leftTeams = new Seq<>();
            for (String uuid:
                 PlayersWhoLeft.keySet()) {
                leftTeams.add(Team.get(PlayersWhoLeft.get(uuid)));
            }
            if(active()){
                //pick first inactive team
                for(Team team : Team.all){
                    if(team.id > 5 && !team.active() && !leftTeams.contains(team)
                            && !arr.contains(p -> p.team() == team)
                            && !data.data(team).dying
                            && !data.data(team).chosen){
                        data.data(team).chosen = true;
                        return team;
                    }
                }
                Call.infoMessage(player.con, "There are currently no empty hex spaces available.\nAssigning into spectator mode.");
                return Team.derelict;
            }else{
                return prev.assign(player, players);
            }
        };
    }


    void updateText(Player player){
        HexTeam team = data.data(player);
        int minsleft=(int)(roundTime - counter) / 60 / 60;
        StringBuilder message = new StringBuilder("[white]Hex #" + team.location.id + " [teal]( "+minsleft+"mins left )\n");

        if(!team.lastMessage.get()) return;

        if(team.location.controller == null){
            if(team.progressPercent > 0){
                message.append("[lightgray]Capture progress: [accent]").append((int)(team.progressPercent)).append("%");
            }else{
                message.append("[lightgray][[Empty]");
            }
        }else if(team.location.controller == player.team()){
            message.append("[yellow][[Captured]");
        }else if(team.location != null && team.location.controller != null && data.getPlayer(team.location.controller) != null){
            message.append("[#").append(team.location.controller.color).append("]Captured by ").append(data.getPlayer(team.location.controller).name);
        }else{
            message.append("<Unknown>");
        }

        Call.setHudText(player.con, message.toString());
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("hexed2", "Begin hosting with the Hexed gamemode.", args -> {
            if(!state.is(State.menu)){
                Log.err("Stop the server first.");
                return;
            }

            data = new HexData();

            logic.reset();
            Log.info("Generating map... v3");
            HexedGenerator generator = new HexedGenerator();
            world.loadGenerator(Hex.size, Hex.size, generator);
            data.initHexes(generator.getHex());
            info("Map generated.");
            state.rules = rules.copy();
            logic.play();
            netServer.openServer();
            // set tiles here? (27,5) (28,5)
            Log.info("world tile setting");
            initWorldBlock();
            Log.info("world tile set");
            reloadplayerhex();
        });

        // handler.register("countdown", "Get the hexed restart countdown.", args -> {
        //     Log.info("Time until round ends: &lc@ minutes", (int)(roundTime - counter) / 60 / 60);
        // });

        // handler.register("end", "End the game.", args -> endGame());

        // handler.register("r", "Restart the server.", args -> System.exit(2));
    }
    private void clear_player_data(){
        joinedPlayers.clear();
        allMMR.clear();
        PlayersMMR.clear();
        PlayersWhoLeft.clear();
    }


    @Override
    public void registerClientCommands(CommandHandler handler){
        if(registered) return;
        registered = true;

        handler.<Player>register("spectate", "Enter spectator mode. This destroys your base.", (args, player) -> {
             if(player.team() == Team.derelict){
                 player.sendMessage("[scarlet]You're already spectating.");
             }else{
                 killTiles(player.team());
                 player.unit().kill();
                 player.team(Team.derelict);
             }
        });

        handler.<Player>register("captured", "Dispay the number of hexes you have captured.", (args, player) -> {
            if(player.team() == Team.derelict){
                player.sendMessage("[scarlet]You're spectating.");
            }else{
                player.sendMessage("[lightgray]You've captured[accent] " + data.getControlled(player).size + "[] hexes.");
            }
        });

        handler.<Player>register("leaderboard", "Display the leaderboard", (args, player) -> {
            player.sendMessage(getLeaderboard());
        });

        handler.<Player>register("hexstatus", "Get hex status at your position.", (args, player) -> {
            Hex hex = data.data(player).location;
            if(hex != null){
                hex.updateController(data.hexcounts_per_team);
                StringBuilder builder = new StringBuilder();
                builder.append("| [lightgray]Hex #").append(hex.id).append("[]\n");
                builder.append("| [lightgray]Owner:[] ").append(hex.controller != null && data.getPlayer(hex.controller) != null ? data.getPlayer(hex.controller).name : "<none>").append("\n");
                for(TeamData data : state.teams.getActive()){
                    if(hex.getProgressPercent(data.team) > 0){
                        builder.append("|> [accent]").append(this.data.getPlayer(data.team).name).append("[lightgray]: ").append((int)hex.getProgressPercent(data.team)).append("% captured\n");
                    }
                }
                player.sendMessage(builder.toString());
            }else{
                player.sendMessage("[scarlet]No hex found.");
            }
        });
    }

    private static void kick_to_hub() {
        // netServer.kickAll(KickReason.serverRestarting);
        for (Player player : Groups.player){
          Vars.net.pingHost(hubURL, hubPORT, host -> {
            if (player!= null && player.con != null){
              Call.connect(player.con, hubURL, hubPORT); 
              } 
          }, (e) -> {
              netServer.kickAll(KickReason.serverRestarting);
          });
      }
    }



}
