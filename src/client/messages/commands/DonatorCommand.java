package client.messages.commands;

import client.MapleCharacter;
import client.MapleClient;
import client.SkillFactory;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.util.Arrays;
import java.util.List;
import server.MapleInventoryManipulator;
import server.Timer.EventTimer;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.FileoutputUtil;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CWvsContext;

/**
 *
 * @author Eric
 * @rev: 2.7 - Added a new !tickle power command.
 * 
 */
public class DonatorCommand {
    static boolean usedCommandDonator;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.DONATOR;
    }
    
    public static boolean executeDonatorCommand(MapleClient c, String[] splitted) {
        MapleCharacter player = c.getPlayer();
        if (!player.isGM() && GameConstants.isJail(c.getPlayer().getMapId())) {
            c.getPlayer().dropMessage(1, "You may not use commands in this map.");
            return true;
        }
        if (!player.isGM() && c.getPlayer().inJQ()) {
            switch (splitted[0].substring(1).toLowerCase()) {
                case "exit":
                    if (GameConstants.isJail(c.getPlayer().getMapId())) {
                        player.dropMessage(6, "Nice try. :)");
                        return true;
                    }
                    player.changeMap(100000000); // should i give a choice fm/henesys?
                    return true;
                default: 
                    player.dropMessage(-1, "You can't use @commands during a Jump Quest. To exit the Jump Quest, type @exit.");
                    return false;
            }
        }
        if (!player.isGM() && c.getPlayer().inTutorial()) {
            switch (splitted[0].substring(1).toLowerCase()) {
                case "leave":
                    if (GameConstants.isJail(c.getPlayer().getMapId())) {
                        player.dropMessage(6, "Nice try. :)");
                        return true;
                    }
                    c.getSession().write(CWvsContext.clearMidMsg());
                    player.changeMap(100000000); // should i give a choice fm/henesys?
                    return true;
                default: 
                    player.dropMessage(-1, "The only way to leave this map is via @leave.");
                    return false;
            }
        }
        if (player.gmLevel() >= 1) {
             if (player.gmLevel() < 3 && usedCommandDonator == false) {
                    FileoutputUtil.log("DonorLog.txt", "\r\nIGN: " + player.getName() + " || Command: " + InternCommand.joinStringFrom(splitted, 0) + " \r\n");
                    usedCommandDonator = true;
                    EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                           usedCommandDonator = false;  
                        }
                    }, 10);
                }
            switch (splitted[0].substring(1).toLowerCase()) {
                case "tickle":
                    if (player.getTicklePower() == 0) {
                        player.toggleTickle(1);
                        player.dropMessage(5, "[Tickle]: Tickle power activated!");
                        player.dropMessage(5, "Tickle power gets de-activated upon logoff/CC. Re-enable if wanted");
                    } else if (player.getTicklePower() == 1) {
                        player.toggleTickle(0);
                        player.dropMessage(5, "[Tickle]: Tickle power de-activated!");
                    } else { // wonder how they could even get to over 1..?
                        player.dropMessage(5, "tickle=null; return=" + player.getTicklePower());
                        return true;
                    }
                    return true;
                case "donline":
                    if (player.isGM()) { // GMs visible within the check
                        for (ChannelServer ch : LoginServer.getInstance().getWorld(c.getWorld()).getChannels()) {
                            String s = "Current Characters Online [Channel " + ch.getChannel() + " Online: " + ch.getPlayerStorage().getAllCharacters().size() + "] : ";
                            if (ch.getPlayerStorage().getAllCharacters().size() < 999) {
                                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                                    s += MapleCharacter.makeMapleReadable(chr.getName()) + ", ";
                                }
                                player.dropMessage(s.substring(0, s.length() - 2));
                            }
                        }
                    } else { // GMs invisible within the check
                        for (ChannelServer ch : LoginServer.getInstance().getWorld(c.getWorld()).getChannels()) {
                            String s = "Current Characters Online [Channel " + ch.getChannel() + " Online: " + ch.getPlayerStorage().getAllCharacters().size() + "] : ";
                            if (ch.getPlayerStorage().getAllCharacters().size() < 999) {
                                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                                    if (!chr.isGM())
                                        s += MapleCharacter.makeMapleReadable(chr.getName()) + ", ";
                                }
                                player.dropMessage(s.substring(0, s.length() - 2));
                            }
                        }
                    }
                 return true;
                case "donor":
                    player.changeMap(91);
                    player.dropMessage(-1, "Welcome to Donor Island!");
                    return true;
                case "dnotice":
                case "dsay":
                    if (splitted.length > 1) {
                        World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, "[Donator] " + player.getName() + " : " + StringUtil.joinStringFrom(splitted, 1)));
                    } else {
                        player.dropMessage(5, "Syntax: !" + splitted[0] + " <message>");
                    }
                     return true;
                case "dbuff":
                    int[] array = {1001003, 2001002, 1101006, 1101007, 1301007, 2201001, 2121004, 2111005, 2311003, 1121002, 4211005, 3121002, 1121000, 2311003, 1101004, 1101006, 4101004, 4111001, 2111005, 1111002, 2321005, 3201002, 4101003, 4201002, 5101006, 1321010, 1121002, 1120003};
                  for (int i = 0; i < array.length; i++) {
                       SkillFactory.getSkill(array[i]).getEffect(SkillFactory.getSkill(array[i]).getMaxLevel()).applyTo(player);
                   }
                case "dvac": 
                    if (!player.Spam(600000, 17)) {
                        List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
                        for (MapleMapObject item : items) {
                            MapleMapItem mapItem = (MapleMapItem) item;
                            if (mapItem.getMeso() > 0) {
                                player.gainMeso(mapItem.getMeso(), true);
                            } else {
                                MapleInventoryManipulator.addFromDrop(c, mapItem.getItem(), true);
                            }
                            mapItem.setPickedUp(true);
                            player.getMap().removeMapObject(item); 
                            player.getMap().broadcastMessage(CField.removeItemFromMap(mapItem.getObjectId(), 2, player.getId()), mapItem.getPosition());
                        }
                    } else {
                        player.dropMessage("Donator's can only vac every minute, calm down!");
                    }
                    return true;
                case "cgender":
                    String req = splitted[1];
                     if (req.equalsIgnoreCase("male") || req.equalsIgnoreCase("female") || req.equalsIgnoreCase("shemale")) {
                        switch(req) {
                            case "male":
                                player.setGender((byte)0);
                                break;
                            case "female":
                                player.setGender((byte)1);
                                break;
                            case "shemale":
                                player.setGender((byte)2);
                                break;
                        }
                        player.dropMessage(5, "You are now a " + req);
                        player.fakeRelog(); // is this needed? why not. :P
                     } else {
                         player.dropMessage("Invalid syntax. Syntax: !cgender <male/female/shemale>");
                         return true;
                     }
                     return true;
                case "dcommands":
                case "dcommand":
                    player.dropNPC("[" + ServerConstants.SERVER_NAME + "'s #rDonor#k Commands]\r\n!dnotice <message> - A world message with [Donor]\r\n!dbuff - Gives you donator buffs\r\n!donline - Advanced @online command showing all channels\r\n!donor - Warps you to Donator Island!\r\n!tickle - Notifies you when a player clicks on you.\r\n!dvac - Item vacs. Cooldown: 10 minutes\r\n!cgender <male/female/shemale> - Change your gender <3");
                    return true;
                default:
                    if (c.getPlayer().gmLevel() >= 2) {
                        return SuperDonatorCommand.executeSuperDonatorCommand(c, splitted);
                    } else {
                        return PlayerCommand.executePlayerCommands(c, splitted);
                    }
            }
        } else {
            return true;
        }
    }
}
