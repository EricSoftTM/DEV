package client.messages.commands;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleStat;
import client.Skill;
import client.SkillFactory;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.CheaterData;
import handling.world.World;
import java.awt.Point;
import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.EventInstanceManager;
import scripting.EventManager;
import server.ItemInformation;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleSquad;
import server.MapleSquad.MapleSquadType;
import server.Randomizer;
import server.Timer.EventTimer;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleReactor;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CWvsContext;

/**
 *
 * @author Emilyx3
 */
public class InternCommand {
    
    static boolean usedCommandIntern = false;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.INTERN;
    }

    public static boolean executeInternCommand (MapleClient c, String[] splitted) {
        if (c.getPlayer().gmLevel() >= 3) {
            MapleCharacter player = c.getPlayer();
            MapleCharacter victim;
            // MapleCharacter target;
            MapleMap targetmap;
            MapleMap map = c.getPlayer().getMap();
            StringBuilder sb = new StringBuilder();
            if (player.gmLevel() < 6 && usedCommandIntern == false) {
                    FileoutputUtil.log("GMLog.txt", "\r\nIGN: " + player.getName() + " || Command: " + InternCommand.joinStringFrom(splitted, 0) + " \r\n");
                    usedCommandIntern = true;
                    EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                           usedCommandIntern = false;  
                        }
                    }, 10);
                }
            switch (splitted[0].substring(1).toLowerCase()) {
                // Start of Eric's Commands
                case "fametoggle":
                    if (player.wantFame() == 1) {
                        player.fameToggle(0);
                        player.dropMessage(6, "You have set your Fame Toggle OFF. Players can fame you now.");
                    } else if (player.wantFame() == 0) {
                        player.fameToggle(1);
                        player.dropMessage(6, "You have set your Fame Toggle ON. Players can't fame you now.");
                    } else {
                        player.dropMessage("fametoggle=null;");
                    }
                 return true;
                case "gmchat":
                    World.Broadcast.broadcastGMMessage(player.getWorld(), CWvsContext.serverNotice(6, "[GM Chat - " + player.getName() + "]: " + joinStringFrom(splitted, 1)));
                    return true;
                case "me":
                    String prefix = "[" + c.getPlayer().getName() + "] ";
                    String message = prefix + joinStringFrom(splitted, 1);
                    c.getChannelServer().broadcastPacket(CWvsContext.serverNotice(6, message));
                    return true;
                case "fly":
                    player.fly(player);
                    return true;
                case "invincible":
                    player.setInvincible(player.isInvincible() ? false : true);
                    player.dropMessage(6, (player.isInvincible() ? "Invincibility activated." : "Invincibility deactivated."));
                    return true;
                    
                case "whosthere":
                    //	MessageCallback callback = new ServernoticeMapleClientMessageCallback(c);
            StringBuilder builder = new StringBuilder("Players on Map: ");
            for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                if (builder.length() > 150) { // wild guess :o
                    builder.setLength(builder.length() - 2);
                    player.dropMessage(builder.toString());
                    builder = new StringBuilder();
                }
                builder.append(MapleCharacter.makeMapleReadable(chr.getName()));
                builder.append(", ");
            }
                builder.setLength(builder.length() - 2);
                player.dropMessage(builder.toString());
                // c.getSession().write(CWvsContext.serverNotice(6, builder.toString()));
            return true;
                // End of Eric's Commands
                case "hide": // fixed
                    //if (c.getPlayer().isHidden()) {
                      //  SkillFactory.getSkill(9101004).getEffect(1).applyTo(c.getPlayer(), c.getPlayer(), true, c.getPlayer().getPosition(), 0);
                    //} else {
                       //SkillFactory.getSkill(9101004).getEffect(1).applyTo(c.getPlayer());
                    //}
                    player.toggleHide(false, player.isHidden() ? false : true);
                    return true;
                case "map":
                    if (Integer.parseInt(splitted[1]) == 682000700) {
                        return true; // no no
                    }
                    c.getPlayer().changeMap(Integer.parseInt(splitted[1]), 0);
                    return true;
                case "gmmap":
                    c.getPlayer().changeMap(180000000, 0);
                    return true;
                case "whereami":
                    c.getPlayer().dropMessage(5, "You are on map " + c.getPlayer().getMapId());
                    return true;
                case "online":
                    for (ChannelServer ch : LoginServer.getInstance().getWorld(c.getWorld()).getChannels()) {
                        c.getPlayer().dropMessage(6, "Characters connected to channel " + ch.getChannel() + ":");
                        c.getPlayer().dropMessage(6, ChannelServer.getInstance(c.getWorld(), ch.getChannel()).getPlayerStorage().getOnlinePlayers(true));
                    }
                return true;
                case "onlinechannel":
                    c.getPlayer().dropMessage(6, "Characters connected to channel " + Integer.parseInt(splitted[1]) + ":");
                    c.getPlayer().dropMessage(6, LoginServer.getInstance().getWorld(c.getWorld()).getChannel(Integer.parseInt(splitted[1])).getPlayerStorage().getOnlinePlayers(true));
                    return true;
                case "itemcheck":
                    if (splitted.length < 3 || splitted[1] == null || splitted[1].equals("") || splitted[2] == null || splitted[2].equals("")) {
                        c.getPlayer().dropMessage(6, "!itemcheck <playername> <itemid>");
                        return true;
                    } else {
                        int item = Integer.parseInt(splitted[2]);
                        MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                        int itemamount = chr.getItemQuantity(item, true);
                        if (itemamount > 0) {
                            c.getPlayer().dropMessage(6, chr.getName() + " has " + itemamount + " (" + item + ").");
                        } else {
                            c.getPlayer().dropMessage(6, chr.getName() + " doesn't have (" + item + ")");
                        }
                    }
                    return true;
                case "song":
                    c.getPlayer().getMap().broadcastMessage(CField.musicChange(splitted[1]));
                    return true;
                case "checkpoints":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "Need playername.");
                        return true;
                    }
                    MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (chrs == null) {
                        c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
                    } else {
                        c.getPlayer().dropMessage(6, chrs.getName() + " has " + chrs.getPoints() + " points.");
                    }
                    return true;
                case "checkvpoints":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "Need playername.");
                        return true;
                    }
                    chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (chrs == null) {
                        c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
                    } else {
                        c.getPlayer().dropMessage(6, chrs.getName() + " has " + chrs.getVPoints() + " vpoints.");
                    }
                    return true;
                case "charinfo":
                    builder = new StringBuilder();
                    MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (other == null) {
                        builder.append("...does not exist");
                        c.getPlayer().dropMessage(6, builder.toString());
                        return true;
                    }
                    if (other.getClient().getLastPing() <= 0) {
                        other.getClient().sendPing();
                    }
                    builder.append(MapleClient.getLogMessage(other, ""));
                    builder.append(" at ").append(other.getPosition().x);
                    builder.append(" /").append(other.getPosition().y);

                    builder.append(" || HP : ");
                    builder.append(other.getStat().getHp());
                    builder.append(" /");
                    builder.append(other.getStat().getCurrentMaxHp());

                    builder.append(" || MP : ");
                    builder.append(other.getStat().getMp());
                    builder.append(" /");
                    builder.append(other.getStat().getCurrentMaxMp(other.getJob()));

                    builder.append(" || BattleshipHP : ");
                    builder.append(other.currentBattleshipHP());

                    builder.append(" || WATK : ");
                    builder.append(other.getStat().getTotalWatk());
                    builder.append(" || MATK : ");
                    builder.append(other.getStat().getTotalMagic());
                    builder.append(" || MAXDAMAGE : ");
                    builder.append(other.getStat().getCurrentMaxBaseDamage());
                    builder.append(" || DAMAGE% : ");
                    builder.append(other.getStat().dam_r);
                    builder.append(" || BOSSDAMAGE% : ");
                    builder.append(other.getStat().bossdam_r);
                    builder.append(" || CRIT CHANCE : ");
                    builder.append(other.getStat().passive_sharpeye_rate());
                    builder.append(" || CRIT DAMAGE : ");
                    builder.append(other.getStat().passive_sharpeye_percent());

                    builder.append(" || STR : ");
                    builder.append(other.getStat().getStr());
                    builder.append(" || DEX : ");
                    builder.append(other.getStat().getDex());
                    builder.append(" || INT : ");
                    builder.append(other.getStat().getInt());
                    builder.append(" || LUK : ");
                    builder.append(other.getStat().getLuk());

                    builder.append(" || Total STR : ");
                    builder.append(other.getStat().getTotalStr());
                    builder.append(" || Total DEX : ");
                    builder.append(other.getStat().getTotalDex());
                    builder.append(" || Total INT : ");
                    builder.append(other.getStat().getTotalInt());
                    builder.append(" || Total LUK : ");
                    builder.append(other.getStat().getTotalLuk());

                    builder.append(" || EXP : ");
                    builder.append(other.getExp());
                    builder.append(" || MESO : ");
                    builder.append(other.getMeso());

                    builder.append(" || party : ");
                    builder.append(other.getParty() == null ? -1 : other.getParty().getId());

                    c.getPlayer().dropNPC(builder.toString());
                    return true;
                case "nearestportal":
                    MaplePortal portal = c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition());
                    c.getPlayer().dropMessage(6, portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName());
                    return true;
                case "spawndebug":
                    c.getPlayer().dropMessage(6, c.getPlayer().getMap().spawnDebug());
                    return true;
                case "fakerelog":
                    c.getPlayer().fakeRelog();
                    return true;
                case "cleardrops":
                    c.getPlayer().dropMessage(5, "Cleared " + c.getPlayer().getMap().getNumItems() + " drops");
                    c.getPlayer().getMap().removeDrops();
                    return true;
                case "listsquads":
                    for (Entry<MapleSquad.MapleSquadType, MapleSquad> squads : c.getChannelServer().getAllSquads().entrySet()) {
                        c.getPlayer().dropMessage(5, "TYPE: " + squads.getKey().name() + ", Leader: " + squads.getValue().getLeader().getName() + ", status: " + squads.getValue().getStatus() + ", numMembers: " + squads.getValue().getSquadSize() + ", numBanned: " + squads.getValue().getBannedMemberSize());
                    }
                    return true;
                case "listinstances":
                    EventManager em = c.getChannelServer().getEventSM().getEventManager(StringUtil.joinStringFrom(splitted, 1));
                    if (em == null || em.getInstances().size() <= 0) {
                        c.getPlayer().dropMessage(5, "none");
                    } else {
                        for (EventInstanceManager eim : em.getInstances()) {
                            c.getPlayer().dropMessage(5, "Event " + eim.getName() + ", charSize: " + eim.getPlayers().size() + ", dcedSize: " + eim.getDisconnected().size() + ", mobSize: " + eim.getMobs().size() + ", eventManager: " + em.getName() + ", timeLeft: " + eim.getTimeLeft() + ", iprops: " + eim.getProperties().toString() + ", eprops: " + em.getProperties().toString());
                        }
                    }
                    return true;
                case "uptime":
                    c.getPlayer().dropMessage(6, "Server has been up for " + StringUtil.getReadableMillis(ChannelServer.serverStartTime, System.currentTimeMillis()));
                    return true;
                case "eventinstance":
                if (c.getPlayer().getEventInstance() == null) {
                    c.getPlayer().dropMessage(5, "none");
                } else {
                    EventInstanceManager eim = c.getPlayer().getEventInstance();
                    c.getPlayer().dropMessage(5, "Event " + eim.getName() + ", charSize: " + eim.getPlayers().size() + ", dcedSize: " + eim.getDisconnected().size() + ", mobSize: " + eim.getMobs().size() + ", eventManager: " + eim.getEventManager().getName() + ", timeLeft: " + eim.getTimeLeft() + ", iprops: " + eim.getProperties().toString() + ", eprops: " + eim.getEventManager().getProperties().toString());
                }
                return true;
                case "goto":
                    HashMap<String, Integer> gotomaps = new HashMap<String, Integer>();
                    gotomaps.put("gmmap", 180000000);
                    gotomaps.put("southperry", 2000000);
                    gotomaps.put("amherst", 1010000);
                    gotomaps.put("henesys", 100000000);
                    gotomaps.put("ellinia", 101000000);
                    gotomaps.put("perion", 102000000);
                    gotomaps.put("kerning", 103000000);
                    gotomaps.put("harbor", 104000000);
                    gotomaps.put("sleepywood", 105000000);
                    gotomaps.put("florina", 120000300);
                    gotomaps.put("orbis", 200000000);
                    gotomaps.put("happyville", 209000000);
                    gotomaps.put("elnath", 211000000);
                    gotomaps.put("ludibrium", 220000000);
                    gotomaps.put("aquaroad", 230000000);
                    gotomaps.put("leafre", 240000000);
                    gotomaps.put("mulung", 250000000);
                    gotomaps.put("herbtown", 251000000);
                    gotomaps.put("omegasector", 221000000);
                    gotomaps.put("koreanfolktown", 222000000);
                    gotomaps.put("newleafcity", 600000000);
                    gotomaps.put("sharenian", 990000000);
                    gotomaps.put("pianus", 230040420);
                    gotomaps.put("horntail", 240060200);
                    gotomaps.put("chorntail", 240060201);
                    gotomaps.put("griffey", 240020101);
                    gotomaps.put("manon", 240020401);
                    gotomaps.put("zakum", 280030000);
                    gotomaps.put("czakum", 280030001);
                    gotomaps.put("papulatus", 220080001);
                    gotomaps.put("showatown", 801000000);
                    gotomaps.put("zipangu", 800000000);
                    gotomaps.put("ariant", 260000100);
                    gotomaps.put("nautilus", 120000000);
                    gotomaps.put("boatquay", 541000000);
                    gotomaps.put("malaysia", 550000000);
                    gotomaps.put("erev", 130000000);
                    gotomaps.put("ellin", 300000000);
                    gotomaps.put("kampung", 551000000);
                    gotomaps.put("singapore", 540000000);
                    gotomaps.put("amoria", 680000000);
                    gotomaps.put("timetemple", 270000000);
                    gotomaps.put("pinkbean", 270050100);
                    gotomaps.put("fm", 910000000);
                    gotomaps.put("freemarket", 910000000);
                    gotomaps.put("oxquiz", 109020001);
                    gotomaps.put("ola", 109030101);
                    gotomaps.put("fitness", 109040000);
                    gotomaps.put("snowball", 109060000);
                    gotomaps.put("golden", 950100000);
                    gotomaps.put("phantom", 610010000);
                    gotomaps.put("cwk", 610030000);
                    gotomaps.put("rien", 140000000);
                    gotomaps.put("edel", 310000000);
                    gotomaps.put("ardent", 910001000);
                    gotomaps.put("craft", 910001000);
                    gotomaps.put("pvp", 960000000);
                    gotomaps.put("future", 271000000);
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "Syntax: !goto <mapname>");
                    } else {
                        if (gotomaps.containsKey(splitted[1])) {
                            targetmap = c.getChannelServer().getMapFactory().getMap(gotomaps.get(splitted[1]));
                            if (targetmap == null) {
                                c.getPlayer().dropMessage(6, "Map does not exist");
                                return true;
                            }
                            MaplePortal targetPortal = targetmap.getPortal(0);
                            c.getPlayer().changeMap(targetmap, targetPortal);
                        } else {
                            if (splitted[1].equals("locations")) {
                                c.getPlayer().dropMessage(6, "Use !goto <location>. Locations are as follows:");
                                for (String s : gotomaps.keySet()) {
                                    sb.append(s).append(", ");
                                }
                                c.getPlayer().dropMessage(6, sb.substring(0, sb.length() - 2));
                            } else {
                                c.getPlayer().dropMessage(6, "Invalid command syntax - Use !goto <location>. For a list of locations, use !goto locations.");
                            }
                        }
                    }
                    return true;
                case "monsterdebug":
                    double range = Double.POSITIVE_INFINITY;

                    if (splitted.length > 1) {
                        //&& !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster") && !splitted[0].equals("!hitmonsterbyoid") && !splitted[0].equals("!killmonsterbyoid")) {
                        int irange = Integer.parseInt(splitted[1]);
                        if (splitted.length <= 2) {
                            range = irange * irange;
                        } else {
                            map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                        }
                    }
                    if (map == null) {
                        c.getPlayer().dropMessage(6, "Map does not exist");
                        return true;
                    }
                    MapleMonster mob;
                    for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                        mob = (MapleMonster) monstermo;
                        c.getPlayer().dropMessage(6, "Monster " + mob.toString());
                    }
                    return true;
                case "rr":
                    c.getPlayer().dropMessage(5, "[Russian Roulette] Number: " + Randomizer.rand(1, Integer.parseInt(splitted[1])));
                    return true;
                case "event":
                    if (!c.getPlayer().getClient().getChannelServer().eventOn) {
                        for (ChannelServer cs : c.getWorldServer().getChannels()) {
                            cs.eventOn = true;
                            cs.eventMap = c.getPlayer().getMapId();
                            cs.eventChannel = (byte) c.getPlayer().getClient().getChannel();
                        }
                        try {
                            World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, c.getChannel(), "[Event] " + StringUtil.joinStringFrom(splitted, 1) + " - Type @joinevent on channel " + c.getChannel() +" to join."));
                        } catch (NumberFormatException nfe) {}
                    } else {
                        for (ChannelServer cs : c.getWorldServer().getChannels()) {
                            cs.eventOn = false;
                            cs.eventMap=0;
                        }
                        try {
                            World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, c.getChannel(), "[Event] Access to the event has ended since the time finished."));
                        } catch (NumberFormatException nfe) {}
                        return true;
                    }
                    return true;
                case "looknpc":
                    for (MapleMapObject reactor1l : c.getPlayer().getMap().getAllNPCsThreadsafe()) {
                        MapleNPC reactor2l = (MapleNPC) reactor1l;
                        c.getPlayer().dropMessage(5, "NPC: oID: " + reactor2l.getObjectId() + " npcID: " + reactor2l.getId() + " Position: " + reactor2l.getPosition().toString() + " Name: " + reactor2l.getName());
                    }
                    return true;
                case "lookreactor":
                    for (MapleMapObject reactor1l : c.getPlayer().getMap().getAllReactorsThreadsafe()) {
                        MapleReactor reactor2l = (MapleReactor) reactor1l;
                        c.getPlayer().dropMessage(5, "Reactor: oID: " + reactor2l.getObjectId() + " reactorID: " + reactor2l.getReactorId() + " Position: " + reactor2l.getPosition().toString() + " State: " + reactor2l.getState() + " Name: " + reactor2l.getName());
                    }
                    return true;
                case "lookportals":
                    for (MaplePortal portal1l : c.getPlayer().getMap().getPortals()) {
                        c.getPlayer().dropMessage(5, "Portal: ID: " + portal1l.getId() + " script: " + portal1l.getScriptName() + " name: " + portal1l.getName() + " pos: " + portal1l.getPosition().x + "," + portal1l.getPosition().y + " target: " + portal1l.getTargetMapId() + " / " + portal1l.getTarget());
                    }
                    return true;
                case "mynpcpos":
                    Point pos = c.getPlayer().getPosition();
                    c.getPlayer().dropMessage(6, "X: " + pos.x + " | Y: " + pos.y + " | RX0: " + (pos.x + 50) + " | RX1: " + (pos.x - 50) + " | FH: " + c.getPlayer().getFH());
                    return true;
                case "warphere":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (!player.isAdmin() && victim.isAdmin()) {
                        player.dropMessage(5, "Don't warp Owners.");
                        return true;
                    }
                    if (victim != null) {
                        victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition()));
                    } else {
                        int ch = World.Find.findChannel(splitted[1]);
                        if (ch < 0) {
                            c.getPlayer().dropMessage(5, "Not found.");
                            return true;
                        }
                        if (victim == null || victim.inPVP() || (!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                            c.getPlayer().dropMessage(5, "Try again later.");
                            return true;
                        }
                        c.getPlayer().dropMessage(5, "Victim is cross changing channel.");
                        victim.dropMessage(5, "Cross changing channel.");
                        if (victim.getMapId() != c.getPlayer().getMapId()) {
                            final MapleMap mapp = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                            victim.changeMap(mapp, mapp.findClosestPortal(c.getPlayer().getTruePosition()));
                        }
                        victim.changeChannel(c.getChannel());
                    }
                    return true;
                case "warp":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel() && !victim.inPVP() && !c.getPlayer().inPVP()) {
                        if (splitted.length == 2) {
                            c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getTruePosition()));
                        } else {
                            targetmap = ChannelServer.getInstance(c.getWorld(), c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                            if (targetmap == null || targetmap.getId() == 682000700) {
                                c.getPlayer().dropMessage(6, "Map does not exist");
                                return true;
                            }
                            MaplePortal targetPortal = null;
                            if (splitted.length > 3) {
                                try {
                                    targetPortal = targetmap.getPortal(Integer.parseInt(splitted[3]));
                                } catch (IndexOutOfBoundsException e) {
                                    // noop, assume the gm didn't know how many portals there are
                                    c.getPlayer().dropMessage(5, "Invalid portal selected.");
                                } catch (NumberFormatException a) {
                                    // noop, assume that the gm is drunk
                                }
                            }
                            if (targetPortal == null) {
                                targetPortal = targetmap.getPortal(0);
                            }
                            victim.changeMap(targetmap, targetPortal);
                        }
                    } else {
                        try {
                            int ch = World.Find.findChannel(splitted[1]);
                            if (ch < 0) {
                                targetmap = ChannelServer.getInstance(c.getWorld(), c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[1]));
                                if (targetmap == null || targetmap.getId() == 682000700) {
                                    c.getPlayer().dropMessage(6, "Map does not exist");
                                    return true;
                                }
                                MaplePortal targetPortal = null;
                                if (splitted.length > 2) {
                                    try {
                                        targetPortal = targetmap.getPortal(Integer.parseInt(splitted[2]));
                                    } catch (IndexOutOfBoundsException e) {
                                        // noop, assume the gm didn't know how many portals there are
                                        c.getPlayer().dropMessage(5, "Invalid portal selected.");
                                    } catch (NumberFormatException a) {
                                        // noop, assume that the gm is drunk
                                    }
                                }
                                if (targetPortal == null) {
                                    targetPortal = targetmap.getPortal(0);
                                }
                                c.getPlayer().changeMap(targetmap, targetPortal);
                            } else {
                                c.getPlayer().dropMessage(6, "Cross changing channel. Please wait.");
                                c.getPlayer().changeChannel(ch);
                                if (victim.getMapId() != c.getPlayer().getMapId()) {
                                    final MapleMap mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                                    c.getPlayer().changeMap(mapp, mapp.findClosestPortal(victim.getTruePosition()));
                                }
                            }
                        } catch (Exception e) {
                            c.getPlayer().dropMessage(6, "Something went wrong " + e.getMessage());
                            return true;
                        }
                    }
                    return true;
                case "clock":
                    c.getPlayer().getMap().broadcastMessage(CField.getClock(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 60)));
                    return true;
                case "listallsquads":
                    for (ChannelServer cserv : LoginServer.getInstance().getWorld(c.getWorld()).getChannels()) {
                        for (Entry<MapleSquad.MapleSquadType, MapleSquad> squads : cserv.getAllSquads().entrySet()) {
                            c.getPlayer().dropMessage(5, "[Channel " + cserv.getChannel() + "] TYPE: " + squads.getKey().name() + ", Leader: " + squads.getValue().getLeader().getName() + ", status: " + squads.getValue().getStatus() + ", numMembers: " + squads.getValue().getSquadSize() + ", numBanned: " + squads.getValue().getBannedMemberSize());
                        }
                    }
                    return true;
                case "say":
                    if (splitted.length > 1) {
                        World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, "[" + player.getName() + "] : " + StringUtil.joinStringFrom(splitted, 1)));
                    } else {
                        c.getPlayer().dropMessage(6, "Syntax: say <message>");
                        return true;
                      }
                    return true;
                case "letter":
                case "spell":
                    if (splitted.length < 3) {
                        c.getPlayer().dropMessage(6, "syntax: !letter <color (green/red)> <word>");
                        return true;
                    }
                    int start, nstart;
                    if (splitted[1].equalsIgnoreCase("green")) {
                        start = 3991026;
                        nstart = 3990019;
                    } else if (splitted[1].equalsIgnoreCase("red")) {
                        start = 3991000;
                        nstart = 3990009;
                    } else {
                        c.getPlayer().dropMessage(6, "Unknown color!");
                        return true;
                    }
                    String splitString = StringUtil.joinStringFrom(splitted, 2);
                    List<Integer> chars = new ArrayList<Integer>();
                    splitString = splitString.toUpperCase();
                    // System.out.println(splitString);
                    for (int i = 0; i < splitString.length(); i++) {
                        char chr = splitString.charAt(i);
                        if (chr == ' ') {
                            chars.add(-1);
                        } else if ((int) (chr) >= (int) 'A' && (int) (chr) <= (int) 'Z') {
                            chars.add((int) (chr));
                        } else if ((int) (chr) >= (int) '0' && (int) (chr) <= (int) ('9')) {
                            chars.add((int) (chr) + 200);
                        }
                    }
                    final int w = 32;
                    int dStart = c.getPlayer().getPosition().x - (splitString.length() / 2 * w);
                    for (Integer i : chars) {
                        if (i == -1) {
                            dStart += w;
                        } else if (i < 200) {
                            int val = start + i - (int) ('A');
                            client.inventory.Item item = new client.inventory.Item(val, (byte) 0, (short) 1);
                            c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), item, new Point(dStart, c.getPlayer().getPosition().y), false, false);
                            dStart += w;
                        } else if (i >= 200 && i <= 300) {
                            int val = nstart + i - (int) ('0') - 200;
                            client.inventory.Item item = new client.inventory.Item(val, (byte) 0, (short) 1);
                            c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), item, new Point(dStart, c.getPlayer().getPosition().y), false, false);
                            dStart += w;
                        }
                    }
                    return true;
                case "lookup": 
                case "search":
                case "find":
                    if (splitted.length == 1) {
                        c.getPlayer().dropMessage(6, splitted[0] + ": <NPC> <MOB> <ITEM> <MAP> <SKILL> <QUEST>");
                    } else if (splitted.length == 2) {
                        c.getPlayer().dropMessage(6, "Provide something to search.");
                    } else {
                        String searchType = splitted[1];
                        String search = StringUtil.joinStringFrom(splitted, 2);
                        MapleData data = null;
                        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
                        c.getPlayer().dropMessage(6, "<<Type: " + searchType + " | Search: " + search + ">>");

                        if (searchType.equalsIgnoreCase("NPC")) {
                            List<String> retNpcs = new ArrayList<String>();
                            data = dataProvider.getData("Npc.img");
                            List<Pair<Integer, String>> npcPairList = new LinkedList<Pair<Integer, String>>();
                            for (MapleData npcIdData : data.getChildren()) {
                                npcPairList.add(new Pair<Integer, String>(Integer.parseInt(npcIdData.getName()), MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")));
                            }
                            for (Pair<Integer, String> npcPair : npcPairList) {
                                if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                                    retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                                }
                            }
                            if (retNpcs != null && retNpcs.size() > 0) {
                                for (String singleRetNpc : retNpcs) {
                                    c.getPlayer().dropMessage(6, singleRetNpc);
                                }
                            } else {
                                c.getPlayer().dropMessage(6, "No NPC's Found");
                            }

                        } else if (searchType.equalsIgnoreCase("MAP")) {
                            List<String> retMaps = new ArrayList<String>();
                            data = dataProvider.getData("Map.img");
                            List<Pair<Integer, String>> mapPairList = new LinkedList<Pair<Integer, String>>();
                            for (MapleData mapAreaData : data.getChildren()) {
                                for (MapleData mapIdData : mapAreaData.getChildren()) {
                                    mapPairList.add(new Pair<Integer, String>(Integer.parseInt(mapIdData.getName()), MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME")));
                                }
                            }
                            for (Pair<Integer, String> mapPair : mapPairList) {
                                if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                                    retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                                }
                            }
                            if (retMaps != null && retMaps.size() > 0) {
                                for (String singleRetMap : retMaps) {
                                    c.getPlayer().dropMessage(6, singleRetMap);
                                }
                            } else {
                                c.getPlayer().dropMessage(6, "No Maps Found");
                            }
                        } else if (searchType.equalsIgnoreCase("MOB")) {
                            List<String> retMobs = new ArrayList<String>();
                            data = dataProvider.getData("Mob.img");
                            List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
                            for (MapleData mobIdData : data.getChildren()) {
                                mobPairList.add(new Pair<Integer, String>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
                            }
                            for (Pair<Integer, String> mobPair : mobPairList) {
                                if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                                    retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                                }
                            }
                            if (retMobs != null && retMobs.size() > 0) {
                                for (String singleRetMob : retMobs) {
                                    c.getPlayer().dropMessage(6, singleRetMob);
                                }
                            } else {
                                c.getPlayer().dropMessage(6, "No Mobs Found");
                            }

                        } else if (searchType.equalsIgnoreCase("ITEM")) {
                            List<String> retItems = new ArrayList<String>();
                            for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                                if (itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(search.toLowerCase())) {
                                    retItems.add(itemPair.itemId + " - " + itemPair.name);
                                }
                            }
                            if (retItems != null && retItems.size() > 0) {
                                for (String singleRetItem : retItems) {
                                    c.getPlayer().dropMessage(6, singleRetItem);
                                }
                            } else {
                                c.getPlayer().dropMessage(6, "No Items Found");
                            }
                        } else if (searchType.equalsIgnoreCase("QUEST")) {
                            List<String> retItems = new ArrayList<String>();
                            for (MapleQuest itemPair : MapleQuest.getAllInstances()) {
                                if (itemPair.getName().length() > 0 && itemPair.getName().toLowerCase().contains(search.toLowerCase())) {
                                    retItems.add(itemPair.getId() + " - " + itemPair.getName());
                                }
                            }
                            if (retItems != null && retItems.size() > 0) {
                                for (String singleRetItem : retItems) {
                                    c.getPlayer().dropMessage(6, singleRetItem);
                                }
                            } else {
                                c.getPlayer().dropMessage(6, "No Quests Found");
                            }
                        } else if (searchType.equalsIgnoreCase("SKILL")) {
                            List<String> retSkills = new ArrayList<String>();
                            for (Skill skil : SkillFactory.getAllSkills()) {
                                if (skil.getName() != null && skil.getName().toLowerCase().contains(search.toLowerCase())) {
                                    retSkills.add(skil.getId() + " - " + skil.getName());
                                }
                            }
                            if (retSkills != null && retSkills.size() > 0) {
                                for (String singleRetSkill : retSkills) {
                                    c.getPlayer().dropMessage(6, singleRetSkill);
                                }
                            } else {
                                c.getPlayer().dropMessage(6, "No Skills Found");
                            }
                        } else {
                            c.getPlayer().dropMessage(6, "Sorry, that search call is unavailable");
                        }
                    }
                    return true;
                case "id": // Re-made by Eric, will now StringBuild an NPC and includes maps without /m
                    sb = new StringBuilder();
                    long startt = System.currentTimeMillis();
                    if (splitted.length == 1) {
                        player.dropNPC(splitted[0] + ": <NPC> <MOB> <ITEM> <MAP> <SKILL> <QUEST>");
                        return true;
                    } else if (splitted.length == 2) {
                        player.dropNPC("#rProvide something to search.#k");
                        return true;
                    } else {
                        String searchType = splitted[1];
                        String search = StringUtil.joinStringFrom(splitted, 2);
                        MapleData data = null;
                        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz"));
                        String results = "<<Type: " + searchType + " | Search: " + search + ">>\r\n\r\n";
                        if (searchType.equalsIgnoreCase("NPC")) {
                            data = dataProvider.getData("Npc.img");
                            List<Pair<Integer, String>> npcPairList = new LinkedList<>();
                            for (MapleData npcIdData : data.getChildren()) {
                                npcPairList.add(new Pair<>(Integer.parseInt(npcIdData.getName()), MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")));
                            }
                            for (Pair<Integer, String> npcPair : npcPairList) {
                                if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                                    sb.append("#b").append(npcPair.getLeft()).append("#k - #r").append((String)npcPair.getRight()).append("#k\r\n");
                                }
                            }
                        } else if (searchType.equalsIgnoreCase("MAP")) {
                            data = dataProvider.getData("Map.img");
                            List<Pair<Integer, String>> mapPairList = new LinkedList<>();
                            for (MapleData mapAreaData : data.getChildren()) {
                                for (MapleData mapIdData : mapAreaData.getChildren()) {
                                    mapPairList.add(new Pair<>(Integer.parseInt(mapIdData.getName()), MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME")));
                                }
                            }
                            for (Pair<Integer, String> mapPair : mapPairList) {
                                if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                                    sb.append("#b").append(mapPair.getLeft()).append("#k - #r").append((String)mapPair.getRight()).append("#k\r\n");
                                }
                            }
                        } else if (searchType.equalsIgnoreCase("MOB")) {
                            data = dataProvider.getData("Mob.img");
                            List<Pair<Integer, String>> mobPairList = new LinkedList<>();
                            for (MapleData mobIdData : data.getChildren()) {
                                mobPairList.add(new Pair<>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
                            }
                            for (Pair<Integer, String> mobPair : mobPairList) {
                                if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                                    sb.append("#b").append(mobPair.getLeft()).append("#k - #r").append((String)mobPair.getRight()).append("#k\r\n");
                                }
                            }
                        } else if (searchType.equalsIgnoreCase("ITEM")) {
                            for (ItemInformation itemPair : MapleItemInformationProvider.getInstance().getAllItems()) {
                                if (itemPair != null && itemPair.name != null && itemPair.name.toLowerCase().contains(search.toLowerCase())) {
                                    sb.append("#b").append(itemPair.itemId).append("#k - #r").append(itemPair.name).append("#k\r\n");
                                }
                            }
                        } else if (searchType.equalsIgnoreCase("QUEST")) {
                            for (MapleQuest itemPair : MapleQuest.getAllInstances()) {
                                if (itemPair.getName().length() > 0 && itemPair.getName().toLowerCase().contains(search.toLowerCase())) {
                                    sb.append("#b").append(itemPair.getId()).append("#k - #r").append(itemPair.getName()).append("#k\r\n");
                                }
                            }
                        } else if (searchType.equalsIgnoreCase("SKILL")) {
                            for (Skill skil : SkillFactory.getAllSkills()) {
                                if (skil.getName() != null && skil.getName().toLowerCase().contains(search.toLowerCase())) {
                                    sb.append("#b").append(skil.getId()).append("#k - #r").append(skil.getName()).append("#k\r\n");
                                }
                            }
                        } else {
                            sb.append("#rSorry, that search call is unavailable.#k\r\n");
                        }
                        if (sb.length() == 0) { // if we result with 0 items, let's notify rather then returning empty.
                            sb.append("#bNo ").append(splitted[1].toLowerCase()).append("s found.\r\n");
                        } else if (sb.length() > 32650) { // if you search "a", or a popular search term.
                            sb.append("#bDue to too many results, the results could not load without crashing your client.#k");
                        }
                        sb.append("\r\n#kLoaded within #e").append((System.currentTimeMillis() - startt) / 1000.0D).append("#n seconds.");
                        player.dropNPC(results + sb.toString());
                    }
                    return true;
                case "warpmap":
                    for (MapleCharacter chr : player.getMap().getCharacters()) {
                        chr.changeMap(c.getChannelServer().getMapFactory().getMap(Integer.valueOf(splitted[1])));
                    }
                    return true;
                case "killall":
                    range = Double.POSITIVE_INFINITY;
                    if (splitted.length > 1) {
                        int irange = Integer.parseInt(splitted[1]);
                        if (splitted.length <= 2) {
                            range = irange * irange;
                        } else {
                            map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                        }
                    }
                    if (map == null) {
                        c.getPlayer().dropMessage(6, "Map does not exist");
                        return true;
                    }
                    for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                        mob = (MapleMonster) monstermo;
                        if (!mob.getStats().isBoss() || mob.getStats().isPartyBonus() || c.getPlayer().isGM()) {
                            map.killMonster(mob, c.getPlayer(), false, false, (byte) 1);
                        }
                    }
                    return true;
                default:
                    if (c.getPlayer().gmLevel() >= 4) {
                        return GMCommand.executeGMCommand(c, splitted);
                    } else {
                        return SuperDonatorCommand.executeSuperDonatorCommand(c, splitted); 
                    }
                    // c.getPlayer().showMessage(splitted[0].substring(1) + " does not exist.");
                    // return false;
            }
        } else {
            c.getPlayer().showMessage("You are not a GM Level 3 (Intern), how the fuck did you get this far?!");
            return true;
        }
    }
    
    static String joinStringFrom(String arr[], int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i != arr.length - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }
    
    public static class WhoComparator implements Comparator<Pair<String, Long>>, Serializable {
        @Override
        public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
            if (o1.right > o2.right) {
                return 1;
            } else if (o1.right == o2.right) {
                return 0;
            } else {
                return -1;
            }
        }
    }
}
