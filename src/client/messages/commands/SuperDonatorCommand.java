package client.messages.commands;

import client.MapleCharacter;
import client.MapleClient;
import constants.GameConstants;
import constants.ServerConstants.PlayerGMRank;
import server.Timer.EventTimer;
import tools.FileoutputUtil;
import tools.packet.CWvsContext;
import tools.packet.MobPacket;

/**
 *
 * @author Eric
 */
public class SuperDonatorCommand {
    static boolean usedCommandSDonator;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERDONATOR;
    }

    public static boolean executeSuperDonatorCommand (MapleClient c, String[] splitted) {
        MapleCharacter player = c.getPlayer();
        if (!player.isGM() && GameConstants.isJail(c.getPlayer().getMapId())) {
            c.getPlayer().dropMessage(1, "You may not use commands in this map.");
            return true;
        }
        if (c.getPlayer().gmLevel() >= 2) {
             if (player.gmLevel() < 3 && usedCommandSDonator == false) {
                    FileoutputUtil.log("DonorLog.txt", "\r\nIGN: " + player.getName() + " || Command: " + InternCommand.joinStringFrom(splitted, 0) + " \r\n");
                    usedCommandSDonator = true;
                    EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                           usedCommandSDonator = false;  
                        }
                    }, 10);
                }
            switch (splitted[0].substring(1).toLowerCase()) {
                case "null":
                case "fixme":
                    c.announce(CWvsContext.enableActions());
                    return true;
                case "gunslinger":
                    player.changeJob(532);
                    return true;
                case "box":
                    if (splitted[1].equalsIgnoreCase("s") || splitted[1].equalsIgnoreCase("safe")) {
                        player.spawnMonster(9400567, 1, 1, 1);
                        player.dropMessage("Safe BoX spawned.");
                    } else if (splitted[1].equalsIgnoreCase("t") || splitted[1].equalsIgnoreCase("trick")) {
                        player.spawnMonster(9400566, 1, 1, 1);
                        player.dropMessage("Trick BoX spawned.");
                    } else {
                        player.dropMessage("Invalid syntax. Syntax: !box s | t, where s = safe and t = trap");
                    }
                    return true;
                default:
                    if (c.getPlayer().gmLevel() >= 3) {
                        return InternCommand.executeInternCommand(c, splitted);
                    } else {
                        return DonatorCommand.executeDonatorCommand(c, splitted);
                    }
            }
        } else {
            return true;
        }
    }
}