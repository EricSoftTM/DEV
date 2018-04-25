package client.messages.commands;

import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConstants.PlayerGMRank;
import handling.channel.ChannelServer;
import handling.world.World;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CWvsContext;

/**
 *
 * @author Eric
 * @rev: 1.0 - Initial revision
 * @desc: Decided to make this incase anybody gets Admin, I can hide commands here.
 */
public class GodCommand {
    
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.GOD; // 100+ :|
    }
    
    public static boolean executeGodCommand(MapleClient c, String[] splitted) {
        MapleCharacter player = c.getPlayer();
        MapleCharacter victim;
        ChannelServer cserv = c.getChannelServer();
        if (player.isGod()) {
            switch (splitted[0].substring(1).toLowerCase()) {
                case "godself":
                    player.setGmLevel((byte)100);
                    player.dropMessage(5, "You are now a God.");
                    return true;
                case "godperson":
                    victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.setGmLevel((byte)100);
                    player.dropMessage(5, victim.getName() + " has been made a God.");
                    return true;
                 case "hair":
                    player.setHair(Integer.parseInt(splitted[1]));
                    player.getClient().getSession().write(CField.getCharInfo(player));
                    player.getMap().removePlayer(player);
                    player.getMap().addPlayer(player);
                    player.dropMessage(5, "Your hair ID has been changed to " + splitted[1]);
                    return true;
                case "eyes":
                    player.setFace(Integer.parseInt(splitted[1]));
                    player.getClient().getSession().write(CField.getCharInfo(player));
                    player.getMap().removePlayer(player);
                    player.getMap().addPlayer(player);
                    player.dropMessage(5, "Your face ID has been changed to " + splitted[1]);
                    return true;
                case "yn":
                    if (splitted.length > 1) {
                        World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.yellowChat("[God of Trolls] " + c.getPlayer().getName() + " : " + StringUtil.joinStringFrom(splitted, 1)));
                    } else 
                        player.dropMessage(6, "Syntax: !yn <message>");
                    return true;
                default:
                    return AdminCommand.executeAdminCommand(c, splitted);
            }
        } else {
            c.getPlayer().showMessage("You are not a God, how the fuck did you get this far?!");
            return true;
        }
    }
}
