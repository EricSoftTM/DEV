package client.messages.commands;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MapleRing;
import client.messages.MessageCallback;
import client.messages.ServernoticeMapleClientMessageCallback;
import constants.GameConstants;
import constants.JQLevels;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.channel.handler.NPCHandler;
import handling.login.LoginServer;
import handling.world.World;
import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import scripting.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleShopFactory;
import server.RankingWorker;
import server.RankingWorker.RankingInformation;
import server.Timer.EventTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.OverrideMonsterStats;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.SavedLocationType;
import tools.FileoutputUtil;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.MTSCSPacket;

public class PlayerCommand {
    
    /**
     * 
     * @author: Eric
     * @param: <All commands updated and/or coded by Eric.
     * @return: Use only for Development v117.2
     */ 
   
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.NORMAL;
    }
    
    private static ResultSet ranking(boolean gm) {
    try {
      Connection con = DatabaseConnection.getConnection();
      PreparedStatement ps;
      if (!gm)
        ps = con.prepareStatement("SELECT reborns, level, name, job FROM characters WHERE gm < 3 ORDER BY reborns DESC, level DESC LIMIT 10");
      else {
        ps = con.prepareStatement("SELECT name, gm FROM characters WHERE gm >= 3");
      }
      return ps.executeQuery(); 
    } catch (SQLException ex) {
    }
    return null;
  }
    
    private static ResultSet JQranking(boolean gm) {
    try {
      Connection con = DatabaseConnection.getConnection();
      PreparedStatement ps;
      if (!gm)
        ps = con.prepareStatement("SELECT jqlevel, jqexp, name FROM characters WHERE gm < 3 ORDER BY jqlevel DESC, jqexp DESC LIMIT 10");
      else {
        ps = con.prepareStatement("SELECT name, gm FROM characters WHERE gm >= 3");
      }
      return ps.executeQuery(); 
    } catch (SQLException ex) {
    }
    return null;
  }

    
    public static boolean executePlayerCommands(MapleClient c, String[] splitted) {
        final MapleCharacter player = c.getPlayer();
        MapleCharacter victim;
        MessageCallback mc = new ServernoticeMapleClientMessageCallback(player.getClient()); // #v62dayz
        if (!player.isGM() && GameConstants.isJail(c.getPlayer().getMapId())) {
            mc.dropMessage(1, "You may not use commands in this map.");
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
        switch (splitted[0].substring(1).toLowerCase()) {
            // Start of Eric's Commands
            case "commands":
            case "helpmeplz":
            case "help":
//                player.dropNPC("[" + ServerConstants.SERVER_NAME + " Player Commands] :\r\n\r\nFun Commands - @funcommands\r\nUseful Commands - @listcommands");
//                return true;
            case "listcommands":
                player.dropNPC("[" + ServerConstants.SERVER_NAME + "'s #eOfficial#n Command List] :\r\n\r\n#b@relog#k - #rGlitched, frozen, or bugged? Try this.#k\r\n#b@keyfix#k - #rKeys reset? Try this.#k\r\n#b@afk#k - #rGoing afk? Feel free to use this.#k\r\n#b@expfix#k - #rEXP passed your screen? Try this.#k\r\n#b@spy <ign>#k - #rLists basic stats of a player.#k\r\n#b@checkme#k - #rShows your character's stats and information.#k\r\n#b@credits#k - #rCredits to " + ServerConstants.SERVER_NAME + "'s Staff Team.#k\r\n#b@clear#k - #rClears all of the drops within the map.#k\r\n#b@kin/#b@nimakin#k - #r" + ServerConstants.SERVER_NAME + "'s Stylers.#k\r\n#b@unstuck <ign>#k - #rFixes a character that's stuck on a d/cing map.#k\r\n#b@save#k - #rSaves your character.#k\r\n#b@fm#k - #rDirectly warp to FM.#k\r\n#b@dispose#k - #rCan't click a NPC? Try this.#k\r\n#b@ranking#k - #r" + ServerConstants.SERVER_NAME + "'s Rankings.#k\r\n#b@gm <msg>#k - #rContact Staff Member's for help.#k\r\n#b@dev#k - #rAll-In-One NPC. Has basic stuff and shops.#k\r\n#b@online#k - #rLists all online players.#k\r\n#b@go <map>#k - #rDirectly warp to a map!#k\r\n#b@pvpranks#k - #rDisplay PvP leaderboards!#k\r\n#b@ring <id/info> <ign>#k - #rGet a ring with another player!#k\r\n#b@shop#k - #rOur all-in-one potion shop!#k\r\n#b@dps#k - #rTest out your Damage Per Second!#k\r\n#b@android#k - #rAccess the Android Styler NPC!#k\r\n#b@storage#k - #rDirect access to storage anywhere!#k\r\n#b@news#k - #r" + ServerConstants.SERVER_NAME + "'s Official Newsboard!#k\r\n#b@pvp#k - #rWarps you to our very own Battle Square!#k\r\n#b@advance#k - #rJob advances you if you're an explorer.#k");
                return true;
           // case "funcommands":
           //     player.dropNPC("[" + ServerConstants.SERVER_NAME + "'s #eFun#n Commands] :\r\n\r\n#b@chalk <message>#k - #rChalk talk! #k\r\n#b@leet#k - #rType #b@leet to toggle typing in 1337.#k");
           //     return true;
            case "serveruptime":
            case "uptime":
                c.getSession().write(CWvsContext.yellowChat(ServerConstants.SERVER_NAME + " + has been online for " + StringUtil.getReadableMillis(ChannelServer.serverStartTime, System.currentTimeMillis()) + " without a restart!"));
                return true;
            case "storage":
                player.getStorage().sendStorage(c, 9930100);
                return true;
            case "dps": // Damage Per Second System
                if(!player.isTestingDPS()) {
                    player.toggleTestingDPS();
                    player.dropMessage("Attack the Rock for 15 seconds to test your Damage Per Second.");
                final MapleMonster mm = MapleLifeFactory.getMonster(9400410);
                int distance = ((player.getJobId() >= 300 && player.getJobId() < 413) || (player.getJobId() >= 1300 && player.getJobId() < 1500) || (player.getJobId() >= 520 && player.getJobId() < 600)) ? 125 : 50;
                Point p = new Point(player.getPosition().x - distance, player.getPosition().y);
                mm.setBelongTo(player);
                final long newhp = Long.MAX_VALUE;
                OverrideMonsterStats overrideStats = new OverrideMonsterStats();
                overrideStats.setOHp(newhp);
                mm.setHp(newhp);
                mm.setOverrideStats(overrideStats);
                player.getMap().spawnMonsterOnGroudBelow(mm, p);
                    EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            long health = mm.getHp();
                            player.getMap().killMonster(mm);
                            long dps = (newhp - health) / 15;
                            if (dps > player.getDPS()) {
                                player.dropMessage("Your DPS is " + dps + ". That's a new record!");
                                player.setDPS(dps);
                                player.savePlayer();
                                player.toggleTestingDPS();
                            } else {
                                player.dropMessage("Your DPS is " + dps + ". Your record is " + player.getDPS() + ".");
                                player.toggleTestingDPS();
                            }
                        }
                    }, 15000);
                }
                return true;
            case "dbomb":
                    if (player.getOccId() == 800 || player.isDonator()) {
                        if (!player.Spam((player.isDonator() ? 60000 : 69696969), 13)) {
                            player.spawnBomb(); // todo: fix bombs from sticking after detonate
                        } else 
                            player.dropMessage("Please don't spam this command, you have a wait time of " + (player.isDonator() ? "1 minute." : "5 minutes."));
                    } else
                        player.dropMessage("What do you think you're doing! Only Terrorists can spawn bombs!");
                    return true;
            case "top10":
            case "ranking":
                ResultSet rs;
                rs = ranking(false);
                String top10msg = "Top 10 Players of " + ServerConstants.SERVER_NAME + ": ";
                int zzz = 1;
                    try {
                while (rs.next()) {
                    String job = getJobyNameById(rs.getInt("job"));
                    top10msg += ("\r\n#e" + zzz + "#n. #b" + rs.getString("name") + "#k\r\nJob: " + job + "  ||  Rebirths: #r" + rs.getInt("reborns") + "#k  ||  Level: #d" + rs.getInt("level") + "#k");
                    zzz++;
                }
                player.dropNPC(top10msg);
                } catch (SQLException e) {
                }
                return true;
        /*    case "jqrank":
            case "jqranks":
                rs = JQranking(false);
                String jq_onload = "Jump Quest Leaderboards: ";
                int lb = 1;
                    try {
                        while (rs.next()) {
                            jq_onload += "\r\n#e" + lb + "#n. #b" + rs.getString("name") + "#k - JQ Level: #r" + rs.getInt("jqlevel") + "#k || JQ Exp: #g" + rs.getInt("jqexp") + "#k";
                            lb++;
                        }
                    player.dropNPC(jq_onload);
                    } catch (SQLException e) {
                    }
                return true;*/
            case "pvprank":
            case "pvpranks":
              try {
               PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name, pvpKills, pvpDeaths FROM characters WHERE gm < 3 ORDER BY pvpKills desc LIMIT 10");
               ps.executeQuery();
               rs = ps.executeQuery();
            String msg = "Your Kills: #g" + c.getPlayer().getPvpKills() + "#k || Your Deaths: #r" + c.getPlayer().getPvpDeaths() + "#k\r\nPlayer vs. Player Top 10:";
            int aaa = 1;
                while (rs.next()) {
                    int kills = rs.getInt("pvpKills");
                    int deaths = rs.getInt("pvpDeaths");
                    double kd = ((double)kills / (double)deaths);
                    msg += "\r\n#e" + aaa + "#n. #b" + rs.getString("name") + "#k  -  Kills : #g" + kills + "#k  ||  Deaths : #r" + deaths + "#k || K/D : #d" + kd + "#k";
                    aaa++;
                }
                    player.dropNPC(msg);
                    rs.close();
                    ps.close();
                } catch (Exception ex) {
                }
                    return true;
            case "partyfix":
                player.setParty(null); 
                player.dropMessage("Please Relog or CC to finish changes."); 
                return true;
            case "fixmesos":
            if (player.getMeso() < 0) {
                player.setMeso(0);
                player.dropMessage("Reset your mesos to 0.");
            } else
                player.dropMessage("You don't have negative mesos.");
            return true;
           case "news":
               NPCHandler.openNpc(9040011, c);
               return true;
            case "joinox":
                if (player.getClient().getChannelServer().getEvent() == 109020001) {
                    player.changeMap(109020001);
                    // player.changeMap(player.getClient().getChannelServer().getEvent() == 109020001 ? 109020001 : 910000000);
                } else
                    player.dropMessage(5, "There is no open OX Event to join, please try again later.");
                return true;
            case "relog":
                c.getSession().write(CField.getCharInfo(player));
                player.getMap().removePlayer(player);
                player.getMap().addPlayer(player);
                return true;
            case "keyfix":
                c.getPlayer().sendKeymap();
                player.dropMessage(6, "Your Key Configuration has been fixed.");
                return true;
            case "bosshp":
                List<MapleMapObject> mobs = c.getPlayer().getMap().getMapObjectsInRange(new Point(0,0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
            for (MapleMapObject mob : mobs) {
                MapleMonster m = (MapleMonster) mob;
                if (m.isBoss()) {
                    player.dropMessage("Boss: " + m.getName() + " | HP: " + m.getHp() + "/" + m.getMobMaxHp() + "");
                }
            }
                return true;
            case "mobhp":
                MapleMap map = player.getMap();
            List<MapleMapObject> monsters = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
            for (MapleMapObject monstermo : monsters) {
                MapleMonster monster = (MapleMonster) monstermo;
                player.dropMessage(6, monster.toString_());
            }
            return true;
            case "afk":
                player.setChalkboard(player.getName() + " is AFK~");
                return true;
            case "emo":
                if (!player.isSeduced()) {
                    player.getStat().setHp(0, player);
                    player.updateSingleStat(MapleStat.HP, 0);
                    c.getPlayer().dropMessage(0, "Congratulations, You have just gone Emo.");
                }
                return true;
            case "changechannel":
            case "cc":
                int channel = Integer.parseInt(splitted[1]);
                if (channel < 1 || channel > 4) { // TODO: max channel var in case we ever pass 4
                    player.dropMessage(5, "Please enter a channel number from 1~4.");
                    return true;
                }
                player.getClient().getSession().write(CWvsContext.changeChannelMsg((channel-1), "You are now switching channels to Channel " + channel + "."));
                return true;
            case "str":
            case "dex":
            case "int":
            case "luk":
                int amount = Integer.parseInt(splitted[1]);
                boolean str = splitted[0].equalsIgnoreCase("str");
                boolean Int = splitted[0].equalsIgnoreCase("int");
                boolean luk = splitted[0].equalsIgnoreCase("luk");
                boolean dex = splitted[0].equalsIgnoreCase("dex");
        if (((amount > 0) && (amount <= player.getRemainingAp()) && (amount <= 32763)) || ((amount < 0) && (amount >= -32763) && (Math.abs(amount) + player.getRemainingAp() <= 32767))) {
          if ((str) && (amount + player.getStat().getStr() <= 32767) && (amount + player.getStat().getStr() >= 4)) {
            player.getStat().setStr((short)(player.getStat().getStr() + amount), player);
            player.updateSingleStat(MapleStat.STR, player.getStat().getStr());
            player.setRemainingAp(player.getRemainingAp() - amount);
            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
          } else if ((Int) && (amount + player.getStat().getInt() <= 32767) && (amount + player.getStat().getInt() >= 4)) {
            player.getStat().setInt((short)(player.getStat().getInt() + amount), player);
            player.updateSingleStat(MapleStat.INT, player.getStat().getInt());
            player.setRemainingAp(player.getRemainingAp() - amount);
            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
          } else if ((luk) && (amount + player.getStat().getLuk() <= 32767) && (amount + player.getStat().getLuk() >= 4)) {
            player.getStat().setLuk((short)(player.getStat().getLuk() + amount), player);
            player.updateSingleStat(MapleStat.LUK, player.getStat().getLuk());
            player.setRemainingAp(player.getRemainingAp() - amount);
            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
          } else if ((dex) && (amount + player.getStat().getDex() <= 32767) && (amount + player.getStat().getDex() >= 4)) {
            player.getStat().setDex((short)(player.getStat().getDex() + amount), player);
            player.updateSingleStat(MapleStat.DEX, player.getStat().getDex());
            player.setRemainingAp(player.getRemainingAp() - amount);
            player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
          } else {
            player.dropMessage("Please make sure the stat you are trying to raise is not over 32,767 or under 4.");
          }
        } else {
             player.dropMessage("Please make sure your AP is not over 32,767 and you have enough to distribute.");
         }
            return true;
           case "pvp":
                player.changeMap(960000000);
                return true;
           case "rates":
                int exp = c.getWorldServer().getExpRate();
                int meso = c.getWorldServer().getMesoRate();
                int drop = c.getWorldServer().getDropRate();
                player.dropNPC("[" + ServerConstants.SERVER_NAME + "]: Your rates are as follows:\r\n\r\nEXP: #b" + exp + "#k\r\nMESO: #b" + meso + "#k\r\nDROP: #b" + drop + "#k");
                return true;
            case "chalk":
                if (splitted.length < 2) {
            player.dropMessage(6, "Please use @chalk <message>");
            return true;
          } else {
            player.setChalkboard("" + StringUtil.joinStringFrom(splitted, 1) + "");
            player.getMap().broadcastMessage(MTSCSPacket.useChalkboard(player.getId(), StringUtil.joinStringFrom(splitted, 1)));
            player.getClient().getSession().write(CWvsContext.enableActions());
          }
            return true;
            case "sex":
                player.dropMessage(5, "Smexy you!");
                player.unequipEverything();
                return true;
            case "home":
                player.changeMap(100000000, 0);
                player.dropMessage(-1, "Welcome home, " + player.getName() + "!");
                return true;
            case "serverinfo":
                player.dropNPC("Version: 117.2\r\nRates: " + c.getWorldServer().getExpRate() + "x EXP | " + c.getWorldServer().getMesoRate() + "x MESO | " + c.getWorldServer().getDropRate() + "x DROP\r\nAIO Commands: @" + ServerConstants.SERVER_NAME.substring(0, 3).toLowerCase() + "\r\nWZ Revision: " + ServerConstants.WzRevision + "\r\nSource Revision: " + ServerConstants.SourceRev + "\r\nWebsite: http://maple.development.net/\r\n\r\n#bHave fun in " + ServerConstants.SERVER_NAME + "!#k");
                return true;
            case "expfix":
                player.setExp(0);
                player.updateSingleStat(MapleStat.EXP, player.getExp());
                player.dropMessage(6, "Your EXP has been reset to 0% - you're now fixed!");
                return true;
            case "spy":
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(InternCommand.joinStringFrom(splitted,1 )); 
                StringBuilder sendText = new StringBuilder();
                sendText.append(victim.getName()).append("'s Stats Are:").append("\r\n");
                sendText.append("Str: ").append(victim.getStat().getTotalStr()).append(", Dex: ").append(victim.getStat().getTotalDex()).append(", Int: ").append(victim.getStat().getTotalInt()).append(", Luk: ").append(victim.getStat().getTotalLuk()).append("\r\n");
                sendText.append("Mesos: ").append(victim.getMeso()).append("\r\n");
                sendText.append("NX: ").append(victim.getCSPoints(1)).append("\r\n");
                sendText.append("Health(HP): ").append(victim.getStat().getHp()).append("/").append(victim.getStat().getMaxHp()).append("\r\n");
                sendText.append("Mana Potion(MP): ").append(victim.getStat().getMp()).append("/").append(victim.getStat().getMaxMp()).append("\r\n");
                sendText.append("Rebirths: ").append(victim.getReborns()).append("\r\n");
                sendText.append("Rank: ").append(victim.getPlayerRank(victim)).append("\r\n");
                sendText.append("Marriage Partner: ").append((victim.getPartner())).append("\r\n");
                sendText.append("Damage Per Second: ").append(victim.getDPS()).append("\r\n");
                sendText.append("Weapon Attack: ").append(victim.getStat().getTotalWatk()).append("\r\n");
                sendText.append("Magic Attack: ").append(victim.getStat().getTotalMagic()).append("\r\n");
                sendText.append("----- Player vs. Player -----").append("\r\n");
                sendText.append("Pvp Kills: ").append(victim.getPvpKills()).append("\r\n");
                sendText.append("Pvp Deaths: ").append(victim.getPvpDeaths()).append("\r\n");
                sendText.append("K/D Ratio: ").append(victim.getPvpRatio()).append("\r\n");
                sendText.append("----- Currency/Points -----").append("\r\n");
                sendText.append("Munny: ").append(victim.getItemQuantity(ServerConstants.Currency, false)).append("\r\n");
                sendText.append("Vote Points: ").append(victim.getVPoints()).append("\r\n");
                sendText.append("Donator Points: ").append(victim.getPoints()).append("\r\n");
                sendText.append("----- JumpQuest Information -----").append("\r\n");
                sendText.append("JQ Rank: ").append(JQLevels.getNameById(victim.getJQLevel())).append(" (Level ").append(victim.getJQLevel()).append(")").append("\r\n");
                sendText.append("JQ Exp: ").append(victim.getJQExp()).append("/").append(victim.getJQExpNeeded()).append("\r\n");
              //  sendText.append("----- Occupation Information -----").append("\r\n");
              //  sendText.append("Occupation: ").append(Occupations.getNameById(victim.getOccId())).append("\r\n");
              //  sendText.append("Occupation Lv.: ").append(victim.getOccLevel()).append(" (").append(victim.getOccEXP()).append("/").append(victim.getOccExpNeeded()).append(")").append("\r\n");
                player.dropNPC(sendText.toString());
                return true;
            case "checkme":
            case "check":
            case "checkstats":
                victim = player;  // fuck im too lazy that i'll copy @spy ;)
                sendText = new StringBuilder();
                sendText.append("Your current stats as of [").append(FileoutputUtil.CurrentReadable_TimeGMT()).append(" GMT]: \r\n");
                sendText.append("Str: ").append(victim.getStat().getTotalStr()).append(", Dex: ").append(victim.getStat().getTotalDex()).append(", Int: ").append(victim.getStat().getTotalInt()).append(", Luk: ").append(victim.getStat().getTotalLuk()).append("\r\n");
                sendText.append("Mesos: ").append(victim.getMeso()).append("\r\n");
                sendText.append("NX: ").append(victim.getCSPoints(1)).append("\r\n");
                sendText.append("Health(HP): ").append(victim.getStat().getHp()).append("/").append(victim.getStat().getMaxHp()).append("\r\n");
                sendText.append("Mana Potion(MP): ").append(victim.getStat().getMp()).append("/").append(victim.getStat().getMaxMp()).append("\r\n");
                sendText.append("Rebirths: ").append(victim.getReborns()).append("\r\n");
                sendText.append("Rank: ").append(victim.getPlayerRank(victim)).append("\r\n");
                sendText.append("Marriage Partner: ").append((victim.getPartner())).append("\r\n");
                sendText.append("Damage Per Second: ").append(victim.getDPS()).append("\r\n");
                sendText.append("Weapon Attack: ").append(victim.getStat().getTotalWatk()).append("\r\n");
                sendText.append("Magic Attack: ").append(victim.getStat().getTotalMagic()).append("\r\n");
                sendText.append("----- Player vs. Player -----").append("\r\n");
                sendText.append("Pvp Kills: ").append(victim.getPvpKills()).append("\r\n");
                sendText.append("Pvp Deaths: ").append(victim.getPvpDeaths()).append("\r\n");
                sendText.append("K/D Ratio: ").append(victim.getPvpRatio()).append("\r\n");
                sendText.append("----- Currency/Points -----").append("\r\n");
                sendText.append("Munny: ").append(victim.getItemQuantity(ServerConstants.Currency, false)).append("\r\n");
                sendText.append("Vote Points: ").append(victim.getVPoints()).append("\r\n");
                sendText.append("Donator Points: ").append(victim.getPoints()).append("\r\n");
                player.dropNPC(sendText.toString());
                return true;
            case "leet":
                player.setLeetness(true);
                player.dropMessage(6, "1337 Language Enabled.");
                return true;
            case "1337":
                player.setLeetness(false);
                player.dropMessage(6, "1337 Language Disabled.");
                return true; 
            case "clear":
                if (!player.Spam(60000, 55) || player.isGM()) { // I like @clear better so let's skip check for GMs
                    player.getMap().removeDrops();
                    player.dropMessage("Drops Cleared.");
                } else
                    player.dropMessage(5, "You may only use this command every minute.");
                return true;
            case "credits":
                player.dropMessage(6, ServerConstants.SERVER_NAME + "'s Credits: ");
                player.dropMessage(6, "Paul - Creator of this world");
                player.dropMessage(6, "Eric - Head Coder / WZ Editor");
                player.dropMessage(6, "Kevin - WZ Editor / Graphic Designer");
                return true;
            case "kin":
                NPCScriptManager.getInstance().start(c, 9900000);
                return true;
            case "nimakin":
                NPCScriptManager.getInstance().start(c, 9900001);
                return true;
            case "advance":
            case "job":
                NPCScriptManager.getInstance().start(c, 9900002);
                return true;
            case "gmplz":
            case "makemeagm":
                if (player.isSeduced()) {
                    player.dropMessage("y u try to die when getting seduced?");
                    return true;
                }
                c.getPlayer().getStat().setHp(0, player);
                c.getPlayer().updateSingleStat(MapleStat.HP, 0);
                c.getPlayer().dropMessage(1, "-TrollFace-");
                c.getPlayer().dropMessage(1, "CAUSE I KEEL JOO!");
                c.getPlayer().dropMessage(1, "That's too bad,");
                c.getPlayer().dropMessage(1, "Not even a little bit?");
                c.getPlayer().dropMessage(1, "You sure?");
                c.getPlayer().dropMessage(1, "Still not mad?!");
                c.getPlayer().dropMessage(1, "UMadYet?");
                c.getPlayer().dropMessage(1, "UMad?");
                c.getPlayer().dropMessage(1, "NOOB!!!");
                c.getPlayer().dropMessage(1, "NOOB!!!");
                c.getPlayer().dropMessage(1, "Wait.. you like JB and are my bitch? AW HEEEEEELLL NO!");
                c.getPlayer().dropMessage(1, player.getName() + " is Eric's bitch."); // yep :)
                c.getPlayer().dropMessage(1, "Lol, you like JB.. nub. ");
                c.getPlayer().dropMessage(1, player.getName() + " loves Justin Bieber!");
                c.getPlayer().dropMessage(1, "You have to press 'OK' to my jokes, hahaha!");
                c.getPlayer().dropMessage(1, "Eric was a Troll Master, is he still?");
                c.getPlayer().dropMessage(1, "Do you like being called a noob?");
                c.getPlayer().dropMessage(1, "NOOB!!!");
                c.getPlayer().dropMessage(1, "NOOB!!!");
                c.getPlayer().dropMessage(1, "You think you had a chance? hahaha ouch!");
                c.getPlayer().dropMessage(1, "Like, really.. You typed this<3");
                c.getPlayer().dropMessage(1, "so iherduliekmudkipz");
                c.getPlayer().dropMessage(1, "YUISWANTGM?");
                c.getPlayer().dropMessage(1, "NOOB!!!");
                c.getPlayer().dropMessage(1, "#YoloSwag420"); //LOL.
                return true;
            case "gm":
                if (splitted.length < 2) {
                   return true;
                }
          if (!player.Spam(300000, 1)) {
              World.Broadcast.broadcastGMMessage(player.getWorld(), CWvsContext.serverNotice(6, "Channel: " + c.getChannel() + "  " + player.getName() + ": " + StringUtil.joinStringFrom(splitted, 1)));
              player.dropMessage("Message sent.");
              return true;
          } else {
            player.dropMessage(1, "Please don't flood GMs with your messages.");
          }
            return true;
            case "unstuck":
                if (splitted.length < 2) {
                player.dropMessage("Syntax: @unstuck <ign> - The character MUST be on your account!");
                return true;
            }
            String chrName = splitted[1];
            if (chrName.equalsIgnoreCase(c.getPlayer().getName())) {
                player.dropMessage("You are already logged onto " + c.getPlayer().getName() + "!");
                return true;
            }
            int accId = c.getAccID();
            int charId = -999;
            int chrAccId = -999;
            try {
                java.sql.Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE name = ?");
                ps.setString(1, chrName);
                rs = ps.executeQuery();
                if (rs.next()) {
                    charId = rs.getInt("id");
                    chrAccId = rs.getInt("accountid");
                } else {
                    player.dropMessage("Character not found!");
                    return true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (chrAccId != accId /*&& !c.getPlayer().isInvincible()*/) {
                player.dropMessage("The character is not on your account!");
                return true;
            }
            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                player.dropMessage("The character is already online. Disconnect the character first. The character is on channel " + victim.getClient().getChannel() + ".");
                return true;
            }
            try {
            java.sql.Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET map = ? WHERE name = ?");
            ps.setInt(1, 100000000);
            ps.setString(2, chrName);
            ps.executeUpdate();
            ps.close();
            } catch (SQLException e) {
            }
            player.dropMessage("Sucessfully warped " + splitted[1] + " to Henesys.");
            return true;
            case "save":
                if (player.Spam(60000, 1)) { // save every minute sounds nice
                    player.dropMessage("Please don't flood our database.");
                } else {
                    player.savePlayer();
                    player.dropMessage("Save Complete.");
                }
                return true;
            case "ring":
            case "marry": // just incase?
                  if (splitted[1].equalsIgnoreCase("info")) { // allowance of INFO | iNfO
                      String ring_help = "It should go like: @ring <ringid> <partner> \r\n";
                      ring_help += "#r===== Crush Ring ID's: =====#k\r\n";
                      ring_help += "1112001 - Crush Ring\r\n";
                      ring_help += "1112002 - Cloud Ring\r\n";
                      ring_help += "1112003 - Cupid Ring\r\n";
                      ring_help += "1112005 - Venus Fireworks\r\n";
                      ring_help += "1112006 - Crossed Hearts\r\n";
                      ring_help += "1112007 - Mistletoe Crush Ring\r\n";
                      ring_help += "#d===== Friendship Ring ID's =====#k\r\n";
                      ring_help += "1112800 - Friendship Ring : Clover\r\n";
                      ring_help += "1112801 - Friendship Ring : Flower Petal\r\n";
                      ring_help += "1112802 - Friendship Ring : Star\r\n";
                      ring_help += "1112810 - Christmas Night Bells\r\n";
                      ring_help += "1112811 - Christmas Party\r\n";
                      ring_help += "1112812 - Shared Umbrella Ring\r\n";
                      ring_help += "1112816 - Couple Snow Dome\r\n";
                      ring_help += "#d===== Shirt ID's =====#k\r\n";
                      ring_help += "1048000 - Friendship Shirt\r\n";
                      ring_help += "1049000 - Couple Shirt\r\n\r\n";
                      ring_help += "#bCreating a ring will cost #e10#n vote points, shirts cost #e15#n.#k";
                      player.dropNPC(ring_help);
                      return true;
                  } else if (splitted.length < 3) {
                    c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                    return true;
                }
		int itemId = Integer.parseInt(splitted[2]);
                  if ((itemId == 1048000 && player.getVPoints() >= 15 || itemId == 1049000 && player.getVPoints() >= 15) || player.getVPoints() >= 10) {
                if (!GameConstants.isEffectRing(itemId)) {
                    c.getPlayer().dropMessage(6, "Invalid itemID.");
                    return true;
                } else if (itemId == 1049000 && player.getVPoints() < 15 || itemId == 1048000 && player.getVPoints() < 15) {
                    c.getPlayer().dropMessage(6, "You must have 15 Vote Points to buy a Friendship/Couple Shirt.");
                    return true;
                } else {
                    MapleCharacter fff = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (fff == null) {
                        c.getPlayer().dropMessage(6, "Player must be online");
                        return true;
                    } else {
                        int[] ringID = {MapleInventoryIdentifier.getInstance(), MapleInventoryIdentifier.getInstance()};
                        try {
                            MapleCharacter[] chrz = {fff, c.getPlayer()};
                            for (int i = 0; i < chrz.length; i++) {
                                Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId, ringID[i]);
                                if (eq == null) {
                                    c.getPlayer().dropMessage(6, "Invalid itemID.");
                                    return true;
                                }
                                MapleInventoryManipulator.addbyItem(chrz[i].getClient(), eq.copy());
                                chrz[i].dropMessage(6, "Successfully ring'd with " + chrz[i == 0 ? 1 : 0].getName());
                            }
                            MapleRing.addToDB(itemId, c.getPlayer(), fff.getName(), fff.getId(), ringID);
                        } catch (SQLException e) {
                        }
                        player.gainVotePoints(itemId == 1049000 || itemId == 1048000 ? -15 : -10); // shirt=15,ring=10
                    }
                }
            } else {
                c.getPlayer().dropMessage(5, "A ring costs 10 Vote Points! It must be a crush, friend or a marriage ring!");       
            }
            return true;
        case "joinevent":
            if (c.getPlayer().getClient().getChannelServer().eventOn && c.getChannelServer().getEvent() != 109020001) {
                try {
                    if (player.getClient().getChannel() != c.getChannelServer().eventChannel) {
                        c.getPlayer().dropMessage(5, "Please go to the channel where it's being hosted on before trying to warp there.");
                    } else {
                        int mapId = c.getChannelServer().eventMap;
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(mapId);
                        c.getPlayer().changeMap(target, target.getPortal(0));
                    }
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "Something went wrong " + e.getMessage());
                }
            } else {
                c.getPlayer().dropMessage(6, "There is no event currently on.");
            }
                return true;
        case "fm":
            c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET, c.getPlayer().getMap().getReturnMap().getId());
            map = c.getChannelServer().getMapFactory().getMap(910000000);
            c.getPlayer().changeMap(map, map.getPortal(0));
            return true;
        case "dispose":
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(CWvsContext.enableActions());
            player.dropMessage("Done.");
            return true;
        case "tsmega":
            c.getPlayer().setSmega();
            return true;
        case "android": // Android styler
            NPCScriptManager.getInstance().start(c, 1012123);
            return true;
        case "rank":
        case "ranks":
            if (splitted.length < 4) { //job start end
                c.getPlayer().dropMessage(5, "Use @ranking [job] [start number] [end number] where start and end are ranks of the players");
                final StringBuilder builder = new StringBuilder("JOBS: ");
                for (String b : RankingWorker.getJobCommands().keySet()) {
                    builder.append(b);
                    builder.append(" ");
                }
                c.getPlayer().dropMessage(5, builder.toString());
            } else {
                int start = 1, end = 20;
                try {
                    start = Integer.parseInt(splitted[2]);
                    end = Integer.parseInt(splitted[3]);
                } catch (NumberFormatException e) {
                    c.getPlayer().dropMessage(5, "You didn't specify start and end number correctly, the default values of 1 and 20 will be used.");
                }
                if (end < start || end - start > 20) {
                    c.getPlayer().dropMessage(5, "End number must be greater, and end number must be within a range of 20 from the start number.");
                } else {
                    final Integer job = RankingWorker.getJobCommand(splitted[1]);
                    if (job == null) {
                        c.getPlayer().dropMessage(5, "Please use @ranking to check the job names.");
                    } else {
                        final List<RankingInformation> ranks = RankingWorker.getRankingInfo(job.intValue());
                        if (ranks == null || ranks.size() <= 0) {
                            c.getPlayer().dropMessage(5, "Please try again later.");
                        } else {
                            int num = 0;
                            for (RankingInformation rank : ranks) {
                                if (rank.rank >= start && rank.rank <= end) {
                                    if (num == 0) {
                                        c.getPlayer().dropMessage(6, "Rankings for " + splitted[1] + " - from " + start + " to " + end);
                                        c.getPlayer().dropMessage(6, "--------------------------------------");
                                    }
                                    c.getPlayer().dropMessage(6, rank.toString());
                                    num++;
                                }
                            }
                            if (num == 0) {
                                c.getPlayer().dropMessage(5, "No ranking was returned.");
                            }
                        }
                    }
                }
            }
            return true;
        case "maxskills": 
            if (player.isSuperDonor()) { // gm lv 2
                c.getPlayer().maxAllSkills();
            } else
                player.dropMessage(5, "You must be a GM to max your skills.");
            return true;
        case "dev": // aio npc
            NPCHandler.openNpc(9270035, c);
            return true;
        case "shop":
            MapleShopFactory.getInstance().getShop(61).sendShop(c);
            return true;
        case "go":
            if (!player.isGM() && player.getReborns() < 3) {
                player.dropMessage(5, "You are still weak. Finish your training first and come back when you have fullfilled your 3 rebirths.");
                return true;
            }
            HashMap<String, Integer> maps = new HashMap<String, Integer>();
                    maps.put("henesys", 100000000);
                    maps.put("ellinia", 101000000);
                    maps.put("perion", 102000000);
                    maps.put("kerning", 103000000);
                    maps.put("lith", 104000000);
                    maps.put("amoria", 680000000);
                    maps.put("sleepywood", 105000000); //105040300 - old maple o-o
                    maps.put("florina", 110000000);
                    maps.put("orbis", 200000000);
                    maps.put("happy", 209000000);
                    maps.put("elnath", 211000000);
		    maps.put("ereve", 130000000);
                    maps.put("ludi", 220000000);
                    maps.put("omega", 221000000);
                    maps.put("korean", 222000000);
                    maps.put("aqua", 230000000);
                    maps.put("maya", 100000001);
                    maps.put("leafre", 240000000);
                    maps.put("mulung", 250000000);
                    maps.put("herb", 251000000);
                    maps.put("nlc", 600000000);
                    maps.put("shrine", 800000000);
                    maps.put("showa", 801000000);
                    maps.put("fm", 910000000);
		    maps.put("guild", 200000301);
		    maps.put("fog", 105040306);
                    maps.put("kaede", 800040000);
                    maps.put("ellin", 300000000);
                    maps.put("coke", 219000000);
                    maps.put("kampung", 551000000);
                    maps.put("tot", 270000100);
                    maps.put("nautilus", 120000000);
                    maps.put("magatia", 261000000);
                    maps.put("future", 271000000);
                    maps.put("ariant", 260000000);
                if (splitted.length != 2) {
                StringBuilder builder = new StringBuilder("Syntax: @go/@goto <mapname>");
                int i = 0;
                for (String mapss : maps.keySet()) {
                    if (1 % 10 == 0) {// 10 maps per line
                        player.dropMessage(builder.toString());
                    } else {
                            builder.append(mapss).append(", ");
                    }
                }
                player.dropMessage(builder.toString());
            } else if (maps.containsKey(splitted[1])) {
                int map_ = maps.get(splitted[1]);
                if (map_ == 910000000) {
                    player.saveLocation(SavedLocationType.FREE_MARKET);
                }
                if (player.getMapId() == 502) {
                    c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET, c.getPlayer().getMap().getReturnMap().getId());
                }
                player.changeMap(map_);
            } else {
                StringBuilder builder = new StringBuilder("Syntax: @go/@goto <mapname>");
                int i = 0;
                for (String mapss : maps.keySet()) {
                    if (1 % 10 == 0) {// 10 maps per line 
                        player.dropMessage(builder.toString());
                    } else {
                            builder.append(mapss).append(", ");
                    }
                }
                player.dropMessage(builder.toString());
                }
                maps.clear();
            return true;
        case "online":
        case "connected":
            for (ChannelServer cs : LoginServer.getInstance().getWorld(player.getWorld()).getChannels()) {
                c.getPlayer().dropMessage(6, "Total Players Online: " + cs.getPlayerStorage().getOnlinePlayers(false));
            }
            c.getPlayer().dropMessage(6, "Characters on Channel " + c.getChannel() + ":");
            c.getPlayer().dropMessage(6, ChannelServer.getInstance(player.getWorld(), c.getChannel()).getPlayerStorage().getOnlinePlayers(false));
            return true;
        default:
            c.getPlayer().showMessage(splitted[0].substring(1) + " does not exist.");
            return false;
        }
    }
    
    /**
     * @author: Eric
     * @param id - Gets the JOBID
     * @return - Returns the String of the Job rather than an enum.
     */

    public static String getJobyNameById(int id) {
        switch(id) {
            case 0:
                return "Beginner"; // Beginner
            case 100:
                return "Warrior"; // Warrior
            case 110:
                return "Fighter";
            case 111:
                return "Crusader";
            case 112:
                return "Hero";
            case 120:
                return "Page";
            case 121:
                return "White Knight";
            case 122:
                return "Paladin";
            case 130:
                return "Spearman";
            case 131:
                return "Dragon Knight";
            case 132:
                return "Dark Knight";
            case 200:
                return "Magician"; // Magician
            case 210:
                return "Wizard (Fire, Poison)";
            case 211:
                return "Mage (Fire, Poison)"; 
            case 212:
                return "Arch Mage (Fire, Poison)";
            case 220:
                return "Wizard (Ice, Lightninig)";
            case 221:
                return "Mage (Ice, Lightning)";
            case 222:
                return "Arch Mage (Ice, Lightning)";
            case 230:
                return "Cleric";
            case 231:
                return "Priest";
            case 232:
                return "Bishop";
            case 300:
                return "Archer"; // Bowman
            case 310:
                return "Hunter";
            case 311:
                return "Ranger";
            case 312:
                return "Bowmaster";
            case 320:
                return "Crossbowman";
            case 321:
                return "Sniper";
            case 322:
                return "Marksman";
            case 400: 
                return "Rogue"; // Thief
            case 410:
                return "Assassin";
            case 411:
                return "Hermit";
            case 412:
                return "Night Lord";
            case 420:
                return "Bandit";
            case 421:
                return "Chief Bandit";
            case 422:
                return "Shadower";
            case 430:
                return "Blade Recruit"; // Dual Blade (still Thief)*
            case 431:
                return "Blade Acolyte";
            case 432:
                return "Blade Specialist";
            case 433:
                return "Blade Lord";
            case 434:
                return "Blade Master";
            case 500:
                return "Pirate"; // Pirate
            case 501:
                return "Pirate"; // (Cannon Shooter)"; // Cannoneer
            case 508:
                return "Jett"; // Jett
            case 510:
                return "Brawler";
            case 511:
                return "Marauder";
            case 512:
                return "Buccaneer";
            case 520:
                return "Gunslinger";
            case 521:
                return "Outlaw";
            case 522:
                return "Corsair";
            case 530:
                return "Cannoneer"; // Cannoneer
            case 531:
                return "Cannon Trooper";
            case 532:
                return "Cannon Master";
            case 570:
                return "Jett"; // Jett
            case 571:
                return "Jett";
            case 572:
                return "Jett";
            case 800:
                return "Manager"; // MAPLE_LEAF_BRIDGADIER
            case 900:
                return "GM"; // GM
            case 910:
                return "SuperGM"; // SuperGM
            case 1000: 
                return "Noblesse"; // Cygnus
            case 1100:
                return "Dawn Warrior";
            case 1110:
                return "Dawn Warrior";
            case 1111: 
                return "Dawn Warrior";
            case 1112:
                return "Dawn Warrior";
            case 1200:
                return "Blaze Wizard";
            case 1210:
                return "Blaze Wizard";
            case 1211:
                return "Blaze Wizard";
            case 1212:
                return "Blaze Wizard";
            case 1300:
                return "Wind Archer";
            case 1310:
                return "Wind Archer";
            case 1311:
                return "Wind Archer";
            case 1312:
                return "Wind Archer";
            case 1400:
                return "Night Walker";
            case 1410:
                return "Night Walker";
            case 1411:
                return "Night Walker";
            case 1412:
                return "Night Walker";
            case 1500:
                return "Thunder Breaker";
            case 1510:
                return "Thunder Breaker";
            case 1511:
                return "Thunder Breaker";
            case 1512:
                return "Thunder Breaker";
            case 2000:
                return "Legend"; // Aran
            case 2001:
                return "Evan"; // Evan
            case 2100:
                return "Aran"; // Aran
            case 2110:
                return "Aran";
            case 2111:
                return "Aran";
            case 2112:
                return "Aran";
            case 2200:
                return "Evan"; // Evan
            case 2210:
                return "Evan";
            case 2211:
                return "Evan";
            case 2212:
                return "Evan";
            case 2213:
                return "Evan";
            case 2214:
                return "Evan";
            case 2215:
                return "Evan";
            case 2216:
                return "Evan";
            case 2217:
                return "Evan";
            case 2218:
                return "Evan";
            case 2002:
                return "Mercedes"; // Mercedes
            case 2300:
                return "Mercedes";
            case 2310:
                return "Mercedes";
            case 2311:
                return "Mercedes";
            case 2312:
                return "Mercedes";
            case 2003:
                return "Phantom"; // Phantom
            case 2400:
                return "Phantom";
            case 2410:
                return "Phantom";
            case 2411:
                return "Phantom";
            case 2412:
                return "Phantom";
            case 3000:
                return "Citizen"; // Resistance
            case 3001:
                return "Demon Slayer"; 
            case 3100:
                return "Demon Slayer";
            case 3110:
                return "Demon Slayer";
            case 3111:
                return "Demon Slayer";
            case 3112:
                return "Demon Slayer";
            case 3200:
                return "Battle Mage";
            case 3210:
                return "Battle Mage";
            case 3211:
                return "Battle Mage";
            case 3212:
                return "Battle Mage";
            case 3300:
                return "Wild Hunter";
            case 3310:
                return "Wild Hunter";
            case 3311:
                return "Wild Hunter";
            case 3312:
                return "Wild Hunter";
            case 3500:
                return "Mechanic";
            case 3510:
                return "Mechanic";
            case 3511:
                return "Mechanic";
            case 3512:
                return "Mechanic";
            case 5000:
                return "Mihile"; // Mihile || Mikhaill
            case 5100:
                return "Mihile";
            case 5110:
                return "Mihile";
            case 5111:
                return "Mihile";
            case 5112:
                return "Mihile";
            case 9000:
                return "Additional Skills";
            default:
                return "Beginner";
        }
    }
}
