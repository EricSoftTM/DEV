package client.messages.commands;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleDisease;
import client.MapleStat;
import client.Skill;
import client.SkillFactory;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import client.messages.CommandProcessorUtil;
import static client.messages.commands.SuperGMCommand.getOptionalIntArg;
import constants.GameConstants;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.handler.ChatHandler;
import handling.login.LoginServer;
import handling.world.World;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.logging.Logger;
import scripting.EventInstanceManager;
import scripting.EventManager;
import scripting.NPCScriptManager;
import server.MapleCarnivalChallenge;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.MapleShopFactory;
import server.Timer.EventTimer;
import server.events.InsultBot;
import server.events.MLIABot;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.events.MapleFML;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkillFactory;
import server.life.OverrideMonsterStats;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.packet.CField;
import tools.StringUtil;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.InventoryPacket;

/**
 *
 * @author Eric
 * @rev: 3.4 - Commands from SuperGMCommand transferred. 
 * @rev: 3.4 - Fixed setOwner function disabled for GMLevel >= 5
 */
public class GMCommand {

    static boolean usedCommandGM;
    static int marriage_prompter = 0;
    static int marriage_prompter_vic = 0;
    static int promptMarriage;
    
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.GM;
    }

    public static boolean executeGMCommand(MapleClient c, String[] splitted) {
        if (c.getPlayer().getGMLevel() >= PlayerGMRank.GM.getLevel()) {
            MapleCharacter player = c.getPlayer();
            final MapleCharacter playerf = player;
            MapleCharacter victim;// = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            MapleCharacter chr;// = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            EventManager em;// = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
            MapleMap map = c.getPlayer().getMap();
            MapleCharacter target;
            MapleMonster mob;
            StringBuilder sb = new StringBuilder();
            Skill skill;
            if (player.gmLevel() < 6 && usedCommandGM == false) {
                    FileoutputUtil.log("GMLog.txt", "\r\nIGN: " + player.getName() + " || Command: " + InternCommand.joinStringFrom(splitted, 0) + " \r\n");
                    usedCommandGM = true;
                    EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                           usedCommandGM = false;  
                        }
                    }, 10);
                }
            switch (splitted[0].substring(1).toLowerCase()) {
                case "getskill":
                    skill = SkillFactory.getSkill(Integer.parseInt(splitted[1]));
                    byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
                    byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);

                    if (level > skill.getMaxLevel()) {
                        level = (byte) skill.getMaxLevel();
                    }
                    if (masterlevel > skill.getMaxLevel()) {
                        masterlevel = (byte) skill.getMaxLevel();
                    }
                    c.getPlayer().changeSingleSkillLevel(skill, level, masterlevel);
                     return true;
                    // Start of Eric's Commands
                case "texttest":
                    ChatHandler.GeneralChat(".@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@", (byte)0, c, player);
                    return true;
                case "marryplayer":
                    if (splitted.length > 1) {
                victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                      if (victim != null) {
                          if (player.getMarriageId() == 0 && victim.getMarriageId() == 0) {
                              if (marriage_prompter == 0) {
                                  if (splitted[1] != null) {
                                        promptMarriage = 1; // what do we even..
                                        marriage_prompter = player.getId();
                                        marriage_prompter_vic = victim.getId();
                                        victim.dropMessage(5, "[" + player.getName() + "]: -gets on one knee- " + victim.getName() + ", will you marry me? Type @acceptmarriage or @declinemarriage.");
                                        player.dropMessage("You have asked " + victim.getName() + " to be your marriage partner!");
                                  } else
                                      player.dropMessage(5, "Invalid syntax. Syntax: !marryplayer <ign>");
                              } else
                                  player.dropMessage(5, "A proposal is already in-progress, please wait for it to be finished.");
                          } else
                              player.dropMessage(5, "You or the player you're trying to marry is already married.");
                      } else 
                          player.dropMessage(5, "Unable to find '" + splitted[1] + "' in Channel " + c.getChannel() + ". Please try again.");
                    } else
                        player.dropMessage(5, "Invalid syntax. Syntax: !marryplayer <ign>");
                    return true;
                case "gmshop":
                   MapleShopFactory.getInstance().getShop(1337).sendShop(c);
                     return true;
                case "fml":
                    World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, MapleFML.getFML()));
                    return true;
                case "mlia":
                    World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, MLIABot.findMLIA()));
                    return true;
                case "insult":
                    c.getChannelServer().broadcastPacket(CWvsContext.serverNotice(6, player.getName() + " : " + InsultBot.getInsult()));
                    return true;
                case "mute":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                        if (!victim.isGM()) {
                            victim.setMuteLevel(1); // On
                            player.dropMessage(5, victim.getName() + " has been muted.");
                        } else {
                            player.dropMessage("Don't mute GMs!");
                        }
                    // victim.dropMessage(6, "You have been muted."); // should we? ;P
                     return true;
                case "unmute":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.setMuteLevel(0); // Off
                    player.dropMessage(5, victim.getName() + " has been unmuted.");
                    victim.dropMessage(6, "You have been unmuted.");
                     return true;
                case "mutemap":
                    for (MapleCharacter muted : player.getMap().getCharacters()) {
                        if (!muted.isGM()) {
                            muted.setMuteLevel(1);
                            muted.dropMessage("The map has been muted.");
                        }
                    }
                    player.dropMessage("You have muted the map.");
                    return true;
                case "unmutemap":
                    for (MapleCharacter muted : player.getMap().getCharacters()) {
                        if (!muted.isGM()) {
                            muted.setMuteLevel(0);
                            muted.dropMessage("The map has been unmuted.");
                        }
                    }
                    player.dropMessage("You have unmuted the map.");
                    return true;
                case "stripperson":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (!victim.isGM()) {
                        victim.unequipEverything();
                    }
                     return true;
                case "msg":
                    player.getClient().announce(CField.sendHint(InternCommand.joinStringFrom(splitted, 1), 350, 5));
                    for (MapleCharacter nsg : player.getMap().getCharacters()) {
                        nsg.getClient().announce(CField.sendHint(InternCommand.joinStringFrom(splitted, 1), 350, 5));
                    }
                     return true;
                case "charview":
                    if (player.getCharToggle() == 1) {
                        player.setCharToggle(0);
                        player.dropMessage(6, "You have set your Char Toggle OFF. Players can click you now.");
                    } else if (player.getCharToggle() == 0) {
                        player.setCharToggle(1);
                        player.dropMessage(6, "You have set your Char Toggle ON. Players can't click you now.");
                    } else {
                        player.dropMessage("chartoggle=null;");
                    }
                 return true;
                case "disableantiks":
                    if (player.getMap().getBlockedMap() == false) {
                        player.getMap().blockMap(player.getMapId());
                        player.dropMessage("Blocking map " + player.getMapId() + "...");
                    } else {
                        player.getMap().unblockMap(player.getMapId());
                        player.dropMessage("Unblocking map " + player.getMapId() + "...");
                    }
                    EventTimer.getInstance().schedule(new Runnable() {
                       @Override
                            public void run() {
                                playerf.dropMessage(5, "Map's AntiKS has been " + (playerf.getMap().getBlockedMap() ? "enabled" : "disabled") + ".");
                            }
                    }, 1000);
                     return true;
                case "enableantiks":
                    if (player.warning[2] == false) {
                        player.dropMessage(5, "[Warning]: To confirm unlock of AntiKS, please type the command again.");
                        player.warning[2] = true;
                    }
                    player.getMap().unblockMap(player.getMapId());
                    player.getMap().unblockMap(player.getMapId()); // zzz
                    player.getMap().unblockMap(player.getMapId()); // zzz
                    player.dropMessage("Unblocking map " + player.getMapId() + "...");
                    EventTimer.getInstance().schedule(new Runnable() {
                       @Override
                            public void run() {
                                playerf.dropMessage(5, "Map's AntiKS has been " + (playerf.getMap().getBlockedMap() ? "enabled" : "disabled") + ".");
                            }
                    }, 1000);
                     return true;
                    case "addpvpmap":
                    if (player.getMap().pvpEnabled() == false) {
                        player.getMap().addPvPMap(player.getMapId());
                        player.dropMessage("Enabling PvP on map " + player.getMapId() + "...");
                    } else {
                        player.getMap().removePvPMap(player.getMapId());
                        player.dropMessage("Disabling pvp on map " + player.getMapId() + "...");
                    }
                    EventTimer.getInstance().schedule(new Runnable() {
                       @Override
                            public void run() {
                                playerf.getMap().broadcastMessage(CWvsContext.serverNotice(5, "PvP on this map has been " + (playerf.getMap().pvpEnabled() ? "enabled" : "disabled") + "."));
                            }
                    }, 1000);
                     return true;
                case "removepvpmap":
                case "rpvpmap":
                    if (player.warning[2] == false) {
                        player.dropMessage(5, "[Warning]: To confirm removal of PvP within the map, please type the command again.");
                        player.warning[2] = true;
                    }
                    player.getMap().removePvPMap(player.getMapId());
                    player.getMap().removePvPMap(player.getMapId()); // zzz
                    player.getMap().removePvPMap(player.getMapId()); // zzz
                    player.dropMessage("Removing PvP in map " + player.getMapId() + "...");
                    EventTimer.getInstance().schedule(new Runnable() {
                       @Override
                            public void run() {
                                playerf.getMap().broadcastMessage(CWvsContext.serverNotice(5, "PvP on this map has been " + (playerf.getMap().pvpEnabled() ? "enabled" : "disabled") + "."));
                                playerf.warning[2] = false;
                            }
                    }, 1000);
                     return true;
              //  case "elf":
              //      victim = c.getPlayer();
              //      if (splitted.length > 0) {
              //          victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
              //      }
              //          victim.setElf(victim.getElf() == 0 ? 1 : 0);
              //   return true;
                case "spawn":
                    final int mid = Integer.parseInt(splitted[1]);
                    final int num = Math.min(CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1), 500);
                    Integer lvl = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lvl");
                    Long hp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "hp");
                    Integer exp = CommandProcessorUtil.getNamedIntArg(splitted, 1, "exp");
                    Double php = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "php");
                    Double pexp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pexp");
                    MapleMonster onemob;
                    try {
                        onemob = MapleLifeFactory.getMonster(mid);
                    } catch (RuntimeException e) {
                        c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                         return true;
                    }
                    if (onemob == null) {
                        c.getPlayer().dropMessage(5, "Mob does not exist");
                         return true;
                    }
                    long newhp;
                    int newexp;
                    if (hp != null) {
                        newhp = hp.longValue();
                    } else if (php != null) {
                        newhp = (long) (onemob.getMobMaxHp() * (php.doubleValue() / 100));
                    } else {
                        newhp = onemob.getMobMaxHp();
                    }
                    if (exp != null) {
                        newexp = exp.intValue();
                    } else if (pexp != null) {
                        newexp = (int) (onemob.getMobExp() * (pexp.doubleValue() / 100));
                    } else {
                        newexp = onemob.getMobExp();
                    }
                    if (newhp < 1) {
                        newhp = 1;
                    }
                    if (!player.isAdmin() && (num >= 50 || player.getMap().getMobsSize() >= 50)) {
                        player.dropMessage(5, "You may not spawn more than 50 mobs!");
                        return true;
                    }

                    final OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);
                    for (int i = 0; i < num; i++) {
                        mob = MapleLifeFactory.getMonster(mid);
                        mob.setHp(newhp);
                        if (lvl != null) {
                            mob.changeLevel(lvl.intValue(), false);
                        } else {
                            mob.setOverrideStats(overrideStats);
                        }
                        c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
                    }
                     return true;
                case "weaponlevel":
                    int equipId = Integer.parseInt(splitted[1]); // TODO LOL
                    Equip equip = (Equip) MapleItemInformationProvider.getInstance().getEquipById(equipId);
                    player.dropNPC("#r-- Equipment Upgrading System --#k\r\n\r\n#bEquipment Level#k : #r" + equip.getEquipmentLevel() + "#k\r\n#bEquipment Exp#k : #r" + equip.getEquipmentExp() + "#k");
                    return true;
                case "heal":
                    if (splitted.length != 2) {
                        player.getStat().heal(player);
                        return true;
                    }
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.getStat().heal(victim);
                    return true;
                case "morphperson":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    ii.getItemEffect(Integer.parseInt("22100" + splitted[2])).applyTo(victim);
                    return true;
                case "seduce":
                    int level_ = Integer.parseInt(splitted[2]);
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim.getGMLevel() >= player.gmLevel()) {
                         player.dropMessage(5, "Don't seduce GMs!");
                         return true;
                    }
                    victim.enableSeduce();
                    victim.setChair(0);
                    victim.getClient().getSession().write(CField.cancelChair(-1));
                    victim.getMap().broadcastMessage(victim, CField.showChair(victim.getId(), 0), false);
                    victim.giveDebuff(MapleDisease.SEDUCE,MobSkillFactory.getMobSkill(128,level_));
                    return true;
                case "zakum":
                 player.getMap().spawnFakeMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800000), player.getPosition());
                    for (int x = 8800003; x < 8800011; x++) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(x), player.getPosition());
            }
                 return true;
                case "smega1":
                    World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(3, c.getChannel(), c.getPlayer().getName() + " : " + StringUtil.joinStringFrom(splitted, 1)));
                     return true;
                case "pvpgodmode":
                case "pgm":
                    if (player.wantHit()) {
                        player.toggleHit(false);
                        player.dropMessage(5, "You can't get hit in PvP.");
                    } else {
                        player.toggleHit(true);
                        player.dropMessage(5, "You can get hit in PvP.");
                    }
                    return true;
                case "pvpgodmodep":
                case "pgmp":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim.wantHit()) {
                        victim.toggleHit(false);
                        victim.dropMessage(5, "You can't get hit in PvP.");
                        player.dropMessage(5, victim.getName() + " can't get hit in PvP.");
                    } else {
                        victim.toggleHit(true);
                        victim.dropMessage(5, "You can get hit in PvP.");
                        player.dropMessage(5, victim.getName() + " can get hit in PvP.");
                    }
                    return true;
                case "bomb":
                    if (splitted.length > 1) {
                    for (int i = 0; i < Integer.parseInt(splitted[1]); i++) {
                        player.spawnBomb();
                    }
                       player.dropMessage("Planted " + splitted[1] + " bombs.");
                        return true;
                } else {
                       player.spawnBomb();
                       player.dropMessage("Planted a bomb.");
                        return true;
                }
                case "morph":
                    MapleItemInformationProvider ii1 = MapleItemInformationProvider.getInstance();
                    ii1.getItemEffect(Integer.parseInt("22100" + splitted[1])).applyTo(player);
                     return true;
                case "unjail":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    MapleMap target_ = c.getChannelServer().getMapFactory().getMap(100000000);
                    c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]).changeMap(target_, target_.getPortal(0));
                    player.dropMessage(victim.getName() + " has been unjailed.");
                     return true;
                case "spy":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    String sendText = new StringBuilder().append("Stats for ").append(victim.getName()).append(":\r\n").toString();
      if (victim != null) {
        sendText = new StringBuilder().append(sendText).append("Account Date Created: ").append(MapleCharacter.getAccountDateById(victim.getAccountID())).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("Character Date Created: ").append(MapleCharacter.getCharDateById(victim.getId())).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("Username: ").append(victim.getClient().getAccountName()).append("\r\n").toString();
        //sendText = new StringBuilder().append(sendText).append("Password: ").append(victim.isGM() ? "<null>" : victim.getClient().getAccountPass()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("STR: ").append(victim.getStat().getStr()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("DEX: ").append(victim.getStat().getDex()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("LUK: ").append(victim.getStat().getLuk()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("INT : ").append(victim.getStat().getInt()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("HP: ").append(victim.getStat().getHp()).append("/").append(victim.getStat().getMaxHp()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("MP: ").append(victim.getStat().getMp()).append("/").append(victim.getStat().getMaxMp()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("AP: ").append(victim.getRemainingAp()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("SP: ").append(victim.getRemainingSp()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("MapID: ").append(victim.getMapId()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("Level: ").append(victim.getLevel()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("Reborns: ").append(victim.getReborns()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("Account ID:").append(victim.getClient().getAccID()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("Player ID:").append(victim.getId()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("Vote Points: ").append(victim.getVPoints()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("Wiz Coins: ").append(victim.getItemQuantity(ServerConstants.Currency, false)).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("Mesos: ").append(victim.getMeso()).append("\r\n").toString();
        sendText = new StringBuilder().append(sendText).append("NX: ").append(victim.getCSPoints(1)).append("\r\n").toString();
        if (victim.getGMLevel() > player.getGMLevel()) {
        sendText = new StringBuilder().append(sendText).append("IP Adress: ").append("#rUnavailable#k because player is higher #bGM Level#k.").append("\r\n").toString();
      } else {
        sendText = new StringBuilder().append(sendText).append("IP Adress: ").append(victim.getClient().getLiteralIP()).append("\r\n").toString();
      }
        short watk = 0;
        for (Item e : victim.getInventory(MapleInventoryType.EQUIPPED)) {
          Equip eq = (Equip)e;
          watk = (short)(watk + eq.getWatk());
        }
        sendText = new StringBuilder().append(sendText).append("WATK: ").append(watk).append("\r\n").toString();
        player.getClient().announce(CField.getNPCTalk(9010000, (byte)0, sendText, "00 00"));
      } else {
        player.dropMessage(6, " The username you entered doesn't exist or is not on your channel.");
      }
                 return true;
                case "reloadmap":
                     c.getChannelServer().getMapFactory().disposeMap(player.getMapId());
                     player.dropMessage("The map has been reloaded and disposed.");
                     return true;
                case "sealmap":
                    for (MapleCharacter chr_ : player.getMap().getCharacters()) {
                if (!chr_.isGM()) {
                    chr_.giveDebuff(MapleDisease.getType(120), MobSkillFactory.getMobSkill(120, 1));
                }
            }
                 return true;
                case "clock":
                  player.getMap().broadcastMessage(CField.getClock(getOptionalIntArg(splitted, 1, 60)));
                     return true;
                case "buffme":
                    final int[] array = {9001000, 9101002, 9101003, 9101008, 2001002, 1101007, 1005, 2301003, 5121009, 1111002, 4111001, 4111002, 4211003, 4211005, 1321000, 2321004, 3121002};
            for (int iahhgs : array) {
                SkillFactory.getSkill(iahhgs).getEffect(SkillFactory.getSkill(iahhgs).getMaxLevel()).applyTo(player);
            }
                 return true;
                case "startevent":
                    LoginServer.getInstance().getWorld(player.getWorld()).setEventOn(true);
                    LoginServer.getInstance().getWorld(player.getWorld()).setEventMap(109040000);
                    LoginServer.getInstance().getWorld(player.getWorld()).getAutoJQ().openAutoJQ();
                    World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, c.getChannel(), "[AutoJQ Event] " + player.getName() + " has just created an Automatic Jump Quest Event on Channel " + c.getChannel() + "! Use @join to join it."));
                 return true;
                    case "healmap":
                    for (MapleCharacter mch : player.getMap().getCharacters()) {
                        if (mch != null) {
                           mch.getStat().heal(mch);
                        }
                    }
                     return true;
                    case "ban":
                    if (splitted.length < 3) {
                         c.getPlayer().dropMessage(5, "[Syntax] !ban <IGN> <Reason>");
                         return true;
                    }
                    sb.append(c.getPlayer().getName()).append(" banned ").append(splitted[1]).append(": ").append(StringUtil.joinStringFrom(splitted, 2));
                    target = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (target != null) {
                        if (target.getAccountID() == ServerConstants.ERIC_ACC_ID) {
                            player.getClient().getSession().write(HexTool.getByteArrayFromHexString("1A 00"));
                            return true; // hehe
                        }
                        if (c.getPlayer().getGMLevel() > target.getGMLevel() || c.getPlayer().isAdmin()) {
                            sb.append(" (IP: ").append(target.getClient().getSessionIPAddress()).append(")");
                            if (target.ban(sb.toString(), false || false, false, false)) {
                                 World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, "<Development Ban> : " + player.getName() + " has banned " + splitted[1] + " for " + InternCommand.joinStringFrom(splitted, 2)));
                                 c.getPlayer().dropMessage(6, "[Ban] Successfully banned " + splitted[1] + ".");
                                 return true;
                            } else {
                                c.getPlayer().dropMessage(6, "[Ban] Failed to ban.");
                                 return true;
                            }
                        } else {
                            c.getPlayer().dropMessage(6, "[Ban] May not ban GMs...");
                             return true;
                        }
                    } else {
                        if (MapleCharacter.ban(splitted[1], sb.toString(), false, c.getPlayer().isAdmin() ? 250 : c.getPlayer().getGMLevel(), false)) {
                            World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, "<Development Ban> : " + player.getName() + " has banned " + splitted[1] + " for " + InternCommand.joinStringFrom(splitted, 2)));
                            c.getPlayer().dropMessage(6, "[Ban] Successfully offline banned " + splitted[1] + ".");
                             return true;
                        } else {
                            c.getPlayer().dropMessage(6, "[Ban] Failed to ban " + splitted[1]);
                             return true;
                        }
                    }
                   case "ipban":
                    if (splitted.length < 3) {
                        c.getPlayer().dropMessage(5, "[Syntax] !ban <IGN> <Reason>");
                         return true;
                    }
                    sb.append(c.getPlayer().getName()).append(" banned ").append(splitted[1]).append(": ").append(StringUtil.joinStringFrom(splitted, 2));
                    target = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (target != null) {
                        if (target.getAccountID() == ServerConstants.ERIC_ACC_ID) {
                            player.getClient().getSession().write(HexTool.getByteArrayFromHexString("1A 00"));
                            return true; // hehe
                        }
                        if (c.getPlayer().getGMLevel() > target.getGMLevel() || c.getPlayer().isAdmin()) {
                            sb.append(" (IP: ").append(target.getClient().getSessionIPAddress()).append(")");
                            if (target.ban(sb.toString(), false || true, false, false)) {
                                World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, "<Development Ban> : " + player.getName() + " has banned " + splitted[1] + " for " + InternCommand.joinStringFrom(splitted, 2)));
                                c.getPlayer().dropMessage(6, "[Ban] Successfully banned " + splitted[1] + ".");
                                 return true;
                            } else {
                                c.getPlayer().dropMessage(6, "[Ban] Failed to ban.");
                                 return true;
                            }
                        } else {
                            c.getPlayer().dropMessage(6, "[Ban] May not ban GMs...");
                             return true;
                        }
                    } else {
                        if (MapleCharacter.ban(splitted[1], sb.toString(), false, c.getPlayer().isAdmin() ? 250 : c.getPlayer().getGMLevel(), false)) {
                            World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, "<Development Ban> : " + player.getName() + " has banned " + splitted[1] + " for " + InternCommand.joinStringFrom(splitted, 2)));
                            c.getPlayer().dropMessage(6, "[Ban] Successfully offline banned " + splitted[1] + ".");
                             return true;
                        } else {
                            c.getPlayer().dropMessage(6, "[Ban] Failed to ban " + splitted[1]);
                             return true;
                        }
                    }
                case "tempban":
                    boolean ipBan = false;
                    String[] types = {"HACK", "BOT", "AD", "HARASS", "CURSE", "SCAM", "MISCONDUCT", "SELL", "ICASH", "TEMP", "GM", "IPROGRAM", "MEGAPHONE"};
                    if (splitted.length < 4) {
                        c.getPlayer().dropMessage(6, "Tempban [name] [REASON] [days]");
                        StringBuilder s = new StringBuilder("Tempban reasons: ");
                        for (int i = 0; i < types.length; i++) {
                            s.append(i + 1).append(" - ").append(types[i]).append(", ");
                        }
                        c.getPlayer().dropMessage(6, s.toString());
                         return true;
                    }
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    int reason = Integer.parseInt(splitted[2]);
                    int numDay = Integer.parseInt(splitted[3]);

                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DATE, numDay);
                    DateFormat df = DateFormat.getInstance();

                    if (victim == null || reason < 0 || reason >= types.length) {
                        c.getPlayer().dropMessage(6, "Unable to find character or reason was not valid, type tempban to see reasons");
                         return true;
                    }
                    if (victim.getAccountID() == ServerConstants.ERIC_ACC_ID) {
                            player.getClient().getSession().write(HexTool.getByteArrayFromHexString("1A 00"));
                            return true; // hehe
                        }
                    victim.tempban("Temp banned by " + c.getPlayer().getName() + " for " + types[reason] + " reason", cal, reason, ipBan);
                    c.getPlayer().dropMessage(6, "The character " + splitted[1] + " has been successfully tempbanned till " + df.format(cal.getTime()));
                     return true;
                case "jail":
                  int mapid = 30;
                  int free = 100000000;
                  int hour = 3600000;
                  final MapleCharacter criminal = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                  MapleMap jail = c.getChannelServer().getMapFactory().getMap(mapid);
                  final MapleMap freedom = c.getChannelServer().getMapFactory().getMap(free);
                      criminal.changeMap(jail, jail.getPortal(0));
                      criminal.getClient().getSession().write(CField.getClock(3600));
                      criminal.getClient().getSession().write(CWvsContext.serverNotice(1, "You have been jailed for not listening to " + player.getName() + ".\r\n\r\nBitch, get at me."));

                EventTimer.getInstance().schedule(new Runnable() {
                   public void run() {
                      criminal.changeMap(freedom, freedom.getPortal(0));
                   }
                }
                , hour);
                     return true;
                case "ccplayer":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.changeChannel(World.Find.findChannel(splitted[2]));
                    player.dropMessage(victim.getName() + " has been CC'd to Ch. " + splitted[2]);
                     return true;
                case "unregister":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.getClient().getChannelServer().getPlayerStorage().deregisterPlayer(victim);
                    player.dropMessage("Deregistered");
                    victim.getClient().getChannelServer().getPlayerStorage().deregisterPendingPlayer(victim.getId());
                    player.dropMessage("Deregistered charid and name");
                    victim.getClient().getSession().close();
                    player.dropMessage("Closed session");
                    victim.getClient().disconnect(true, false, true);
                    player.dropMessage("Disconnected");
                    return true;
                case "unregister2":
                    victim = CashShopServer.getPlayerStorage().getCharacterByName(splitted[1]);
                    CashShopServer.getPlayerStorage().deregisterPlayer(victim);
                    CashShopServer.getPlayerStorage().deregisterPendingPlayer(victim.getId());
                    CashShopServer.getPlayerStorage().getCharacterById(victim.getId()).getClient().getSession().close();
                    player.dropMessage("De-registered from Cash Shop");
                    return true;
                case "checknull":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim == null) {
                        player.dropMessage("Player is null!");
                    }
                    try {
                        victim.changeMap(910000000, 0);
                        victim.dropMessage("test");
                        victim.getClient().getSession().write(CWvsContext.enableActions());
                        victim.getClient().getSession().close();
                    } catch (Exception e) {
                        System.out.println("Error of null: " + e.toString());
                        System.out.println("-----------------------------\r\nERROR: " + e + "\r\n-----------------------------");
                    }
                    return true;
                case "dc":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[splitted.length - 1]);
                    if (victim != null && victim.getAccountID() != ServerConstants.ERIC_ACC_ID && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                        victim.getClient().getSession().close();
                        victim.getClient().disconnect(true, false);
                         return true;
                    } else {
                        c.getPlayer().dropMessage(6, "The victim does not exist.");
                         return true;
                    }
                case "kill":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "Syntax: !kill <list player names>");
                         return true;
                    }
                    victim = null;
                    for (int i = 1; i < splitted.length; i++) {
                        try {
                            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[i]);
                        } catch (Exception e) {
                            c.getPlayer().dropMessage(6, "Player " + splitted[i] + " not found.");
                        }
                        if (player.allowedToTarget(victim) && player.getGMLevel() >= victim.getGMLevel()) {
                            victim.getStat().setHp((short) 0, victim);
                            victim.getStat().setMp((short) 0, victim);
                            victim.updateSingleStat(MapleStat.HP, 0);
                            victim.updateSingleStat(MapleStat.MP, 0);
                        }
                    }
                     return true;
                    case "permweather":
                    if (c.getPlayer().getMap().getPermanentWeather() > 0) {
                        c.getPlayer().getMap().setPermanentWeather(0);
                        c.getPlayer().getMap().broadcastMessage(CField.removeMapEffect());
                        c.getPlayer().dropMessage(5, "Map weather has been disabled.");
                    } else {
                        final int weather = CommandProcessorUtil.getOptionalIntArg(splitted, 1, 5120000);
                        if (!MapleItemInformationProvider.getInstance().itemExists(weather) || weather / 10000 != 512) {
                            c.getPlayer().dropMessage(5, "Invalid ID.");
                        } else {
                            c.getPlayer().getMap().setPermanentWeather(weather);
                            c.getPlayer().getMap().broadcastMessage(CField.startMapEffect("", weather, false));
                            c.getPlayer().dropMessage(5, "Map weather has been enabled.");
                        }
                    }
                     return true;
                        case "resetstats":
                    c.getPlayer().getStat().setStr((short)4, c.getPlayer());
                    c.getPlayer().updateSingleStat(MapleStat.STR, c.getPlayer().getStat().getStr());
                    c.getPlayer().getStat().setDex((short)4, c.getPlayer());
                    c.getPlayer().updateSingleStat(MapleStat.DEX, c.getPlayer().getStat().getDex());
                    c.getPlayer().getStat().setInt((short)4, c.getPlayer());
                    c.getPlayer().updateSingleStat(MapleStat.INT, c.getPlayer().getStat().getInt());
                    c.getPlayer().getStat().setLuk((short)4, c.getPlayer());
                    c.getPlayer().updateSingleStat(MapleStat.LUK, c.getPlayer().getStat().getLuk());
                     return true;
                case "maxstats":
                case "maxall":
                    c.getPlayer().getStat().setStr((short)32767, c.getPlayer());
                    c.getPlayer().updateSingleStat(MapleStat.STR, c.getPlayer().getStat().getStr());
                    c.getPlayer().getStat().setDex((short)32767, c.getPlayer());
                    c.getPlayer().updateSingleStat(MapleStat.DEX, c.getPlayer().getStat().getDex());
                    c.getPlayer().getStat().setInt((short)32767, c.getPlayer());
                    c.getPlayer().updateSingleStat(MapleStat.INT, c.getPlayer().getStat().getInt());
                    c.getPlayer().getStat().setLuk((short)32767, c.getPlayer());
                    c.getPlayer().updateSingleStat(MapleStat.LUK, c.getPlayer().getStat().getLuk());
                     return true;
                case "startpvp": // TODO: AutoPvP. o-o
//                    player.getClient().getChannelServer().autoPvpMap = 960010104;
  //                  player.getClient().getChannelServer().autoPvpEventOn = true;
                    World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, c.getChannel(), "[AutoPvP Event] " + player.getName() + " has just started an Automatic Player vs. Player Event! Use @joinpvp to join it."));
                 return true;
                case "startjq":
                    int autojq = Integer.parseInt(splitted[1]);
                    LoginServer.getInstance().getWorld(player.getWorld()).setEventOn(true);
                    LoginServer.getInstance().getWorld(player.getWorld()).setEventMap(autojq);
                    LoginServer.getInstance().getWorld(player.getWorld()).setJQChannel(c.getChannel());
                    LoginServer.getInstance().getWorld(player.getWorld()).getAutoJQ().openAutoJQ();
                
                    World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, c.getChannel(), "[AutoJQ Event] " + player.getName() + " has just created an Automatic Jump Quest Event On Channel " + c.getChannel() + "! Use @join to join it."));
                 return true;
                case "autojqmaps":
                    player.dropMessage(6, "220000006 - Ludi Pet Park");
                    player.dropMessage(6, "100000202 - Henesys Pet Park");
                    player.dropMessage(6, "922020000 - Scary Invisible JQ");
                    player.dropMessage(6, "682000200 - Chimney JQ");
                    // player.dropMessage(6, "105040310 - The JQ With Red Balls");
                     return true;
                case "horntail":
                    MapleMonster ht = MapleLifeFactory.getMonster(8810026);
            player.getMap().spawnMonsterOnGroudBelow(ht, player.getPosition());
            player.getMap().killMonster(8810026);
            player.getMap().broadcastMessage(CWvsContext.serverNotice(0, "As the cave shakes and rattles, here comes Horntail."));
            for (int i = 8810002; i < 8810010; i++) {
                player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(i), player.getPosition());
            }
             case "text":
                  int gmtext = Integer.parseInt(splitted[1]);
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("UPDATE characters SET gmtext = ? WHERE name = ?");
                    ps.setString(2, player.getName());
                    ps.setInt(1, gmtext);
                    ps.executeUpdate();
                    ps.close();
                    player.setGMText(gmtext);
                } catch (SQLException e) {
                }
                    return true;
            case "gmtext":
                  int text;
            //RegularChat
            if (splitted[1].equalsIgnoreCase("normal")) {
                text = 10;
            //MultiChat
            } else if (splitted[1].equalsIgnoreCase("orange")) {
                text = 1;
            } else if (splitted[1].equalsIgnoreCase("pink")) {
                text = 2;
            } else if (splitted[1].equalsIgnoreCase("purple")) {
                text = 3;
            } else if (splitted[1].equalsIgnoreCase("green")) {
                text = 4;
            //ServerNotice
            } else if (splitted[1].equalsIgnoreCase("red")) {
                text = 5;
            } else if (splitted[1].equalsIgnoreCase("blue")) {
                text = 6;
            //RegularChat
            } else if (splitted[1].equalsIgnoreCase("whitebg")) {
                text = 7;
            //Whisper
            } else if (splitted[1].equalsIgnoreCase("lightinggreen")) {
                text = 8;
            //Eric's Troll Text
            } else if (splitted[1].equalsIgnoreCase("ericstrolltext")){
                text = 1337;
            //Yellow<3 (Eric's fav hehes)
           } else if (splitted[1].equalsIgnoreCase("yellow")) {
                text = 9;
           } else if (splitted[1].equalsIgnoreCase("spouse")) {
                text = 102;
           } else if (splitted[1].equalsIgnoreCase("mega")) {
                text = 100;
           } else if (splitted[1].equalsIgnoreCase("smega")) {
                text = 103;
           } else if (splitted[1].equalsIgnoreCase("pie")) {
                text = 99;
           } else if (splitted[1].equalsIgnoreCase("god")) {
                text = 90;
           } else if (splitted[1].equalsIgnoreCase("cake")) {
                text = 98;
           } else if (splitted[1].equalsIgnoreCase("avi")) {
                text = 101;
            } else {
                player.dropMessage("Wrong syntax: use !gmtext normal/orange/pink/purple/green/blue/red/whitebg/lightinggreen/yellow/spouse");
                 return true;
            }
            try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET gmtext = ? WHERE name = ?");
            ps.setString(2, player.getName());
            ps.setInt(1, text);
            ps.executeUpdate();
            ps.close();
            player.setGMText(text);
            } catch (SQLException e) {
            }
             return true;
                    // End of Eric's Commands
                    // -------------------------
                    // Start of Eric's OX Event System
                case "ox":
                    if (splitted.length == 2) {
                        if (splitted[1].equalsIgnoreCase("me")){
                            c.getPlayer().changeMap(109020001);
                        } else if (splitted[1].equalsIgnoreCase("map")) {
                            for (MapleCharacter mcha1 : player.getMap().getCharacters()) {
                                if (!mcha1.isGM() && mcha1 != null) {
                                    mcha1.changeMap(109020001);
                                }
                            }
                        } else if (splitted[1].equalsIgnoreCase("true")){
                            for (MapleCharacter mcha2 : player.getMap().getCharacters()) {
                                if (mcha2.getPosition().x > -240 && mcha2.gmLevel() < 3){
                                    mcha2.getStat().setHp(0, mcha2);
                                    mcha2.getStat().setMp(0, mcha2);
                                    mcha2.updateSingleStat(MapleStat.HP, 0);
                                    if (mcha2.getLevel() > 199){
                                        mcha2.setLevel((short)199);
                                    }
                                }
                             }
                         } else if (splitted[1].equalsIgnoreCase("close")) {
                           player.getClient().getChannelServer().eventOn = false;
                           player.getClient().getChannelServer().eventMap = 0;
                           World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, c.getChannel(), "[OX Event] " + player.getName() + " has closed the entrance to the OX Event on Channel " + c.getChannel() + "."));
                        } else if (splitted[1].equalsIgnoreCase("out") || splitted[1].equalsIgnoreCase("warp")) { 
                            for (MapleCharacter outz : player.getMap().getCharacters()) {
                                  if (!outz.isAlive()) {
                                        outz.changeMap(100000000, 0);
                                        outz.dropMessage(5, "Thanks for playing! Good luck next time!");
                                    }
                                }
                           player.dropMessage(5, "All dead players have been warped out.");
                       } else if (splitted[1].equalsIgnoreCase("notice") || splitted[1].equalsIgnoreCase("open")) {
                           int ox = 109020001; //bit faster then direct - and neat.
                           player.getClient().getChannelServer().eventOn = true;
                           player.getClient().getChannelServer().eventMap = ox;
                           player.getClient().getChannelServer().eventChannel = c.getChannel();
                               World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, c.getChannel(), "[OX Event] " + player.getName() + " has just created an OX Event On Channel " + c.getChannel() + ", Type @joinox to join!"));
                       } else if (splitted[1].equalsIgnoreCase("directions")) {
                           for (MapleCharacter mcha4_ : player.getMap().getCharacters()) {
                               mcha4_.getClient().getSession().write(CWvsContext.getMidMsg("<----- True = O, False = X ----->", true, 0));
                               mcha4_.getClient().getSession().write(CField.showEventInstructions());
                           }
                        } else if (splitted[1].equalsIgnoreCase("false")){
                            for (MapleCharacter mcha4 : player.getMap().getCharacters()) {
                                if (mcha4.getPosition().x < -240 && mcha4.gmLevel() < 2){
                                    mcha4.getStat().setHp(0, mcha4);
                                    mcha4.getStat().setMp(0, mcha4);
                                    mcha4.updateSingleStat(MapleStat.HP, 0);
                                    if (mcha4.getLevel() > 199){
                                        mcha4.setLevel((short)199);
                                    }
                                }
                            }
                        } else {
                            player.dropMessage("Incorrect Syntax: !ox true, !ox false, !ox me, !ox map, or !ox");
                        }
                    } else {
                        for (MapleCharacter chr1 : player.getMap().getCharacters()) {
                            chr1.dropMessage("<----- True = O, False = X ----->");
                            chr1.dropMessage("<----- True = O, False = X ----->");
                            chr1.dropMessage("<----- True = O, False = X ----->");
                            chr1.dropMessage("<----- True = O, False = X ----->");
                            chr1.dropMessage("<----- True = O, False = X ----->");
                            chr1.dropMessage("<----- True = O, False = X ----->");
                            chr1.dropMessage("<----- True = O, False = X ----->");
                            chr1.getStat().setMp(0, chr1); //So people can't use skills xD
                        }
                    }
                     return true;
                    // End of Eric's OX Event System
                case "fame":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "Syntax: !fame <player> <amount>");
                         return true;
                    }
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    int fame;
                    try {
                        fame = Integer.parseInt(splitted[2]);
                    } catch (NumberFormatException nfe) {
                        c.getPlayer().dropMessage(6, "Invalid Number...");
                         return true;
                    }
                    if (victim != null && player.allowedToTarget(victim)) {
                        victim.setFame(fame); // what idiot adds? it's set :(
                        victim.updateSingleStat(MapleStat.FAME, victim.getFame());
                    }
                     return true;
                case "ap":
                    player.setRemainingAp(Integer.parseInt(splitted[1]));
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                     return true;
                case "spyinv":
                    sb = new StringBuilder();
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    sb.append("#eInventory Items#n\r\n");
                for (Item item : victim.getInventory(MapleInventoryType.EQUIP).list()) {
                    sb.append("#L").append(item.getPosition()).append("##i").append(item.getItemId()).append("# #b#t").append(item.getItemId()).append("##l\r\n-ID:").append(item.getItemId()).append(".\r\n");
                }
                    sb.append("#eEquiped Items#n\r\n");
                for (Item item : victim.getInventory(MapleInventoryType.EQUIPPED).list()) {
                    sb.append("#L").append(item.getPosition()).append("##i").append(item.getItemId()).append("# #b#t").append(item.getItemId()).append("##l\r\n-ID:").append(item.getItemId()).append(".\r\n");
                }
                    player.dropNPC(sb.toString());
                    return true;
                case "sp":
                    c.getPlayer().setRemainingSp(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1));
                    c.getPlayer().updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
                     return true;
                case "warpportal":
                    player.changeMap(Integer.parseInt(splitted[1]), Integer.parseInt(splitted[2]));
                    return true;
                case "item":
                    final int itemId = Integer.parseInt(splitted[1]);
                    final short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);

                    if (!c.getPlayer().isAdmin()) {
                        for (int i : GameConstants.itemBlock) {
                            if (itemId == i) {
                                c.getPlayer().dropMessage(5, "Sorry but this item is blocked for your GM level.");
                                 return true;
                            }
                        }
                    }
                    ii = MapleItemInformationProvider.getInstance();
                    if (GameConstants.isPet(itemId)) {
                        c.getPlayer().dropMessage(5, "Please purchase a pet from the cash shop instead.");
                    } else if (!ii.itemExists(itemId)) {
                        c.getPlayer().dropMessage(5, itemId + " does not exist");
                    } else {
                        Item item;
                        short flag = (short) ItemFlag.LOCK.getValue();

                        if (GameConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                            item = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                        } else {
                            item = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);

                        }
                        if (!c.getPlayer().isSuperGM()) {
                            item.setFlag(flag);
                        }
                        if (!c.getPlayer().isAdmin()) {
                            item.setGMLog(c.getPlayer().getName() + " used !getitem");
                        }
                        item.setOwner(player.getName());
                        MapleInventoryManipulator.addbyItem(c, item);
                    }
                     return true;
                case "job":
                    if (MapleCarnivalChallenge.getJobNameById(Integer.parseInt(splitted[1])).length() == 0) { //wut?
                        c.getPlayer().dropMessage(5, "Invalid Job");
                         return true;
                    }
                    c.getPlayer().changeJob((short)Integer.parseInt(splitted[1]));
                     return true;
                case "shop":
                    MapleShopFactory shop = MapleShopFactory.getInstance();
                    int shopId = Integer.parseInt(splitted[1]);
                    if (shop.getShop(shopId) != null) {
                        shop.getShop(shopId).sendShop(c);
                    }
                     return true;
                case "level":
                    c.getPlayer().setLevel((short)(Short.parseShort(splitted[1])-1));
                    c.getPlayer().levelUp();
                    c.getPlayer().levelUp();
                    if (c.getPlayer().getExp() < 0) {
                        c.getPlayer().gainExp(-c.getPlayer().getExp(), false, false, true);
                    }
                     return true;
                case "startautoevent":
                    em = c.getChannelServer().getEventSM().getEventManager("AutomatedEvent");
                    if (em != null) {
                        em.scheduleRandomEvent();
                    }
                     return true;
                case "setevent":
                    MapleEvent.onStartEvent(c.getPlayer());
                     return true;
                case "openevent":
                    if (c.getChannelServer().getEvent() == c.getPlayer().getMapId()) {
                        MapleEvent.setEvent(c.getChannelServer(), false);
                        c.getPlayer().dropMessage(5, "Started the event and closed off");
                         return true;
                    } else {
                        c.getPlayer().dropMessage(5, "!scheduleevent must've been done first, and you must be in the event map.");
                         return true;
                    }
                case "scheduleevent":
                    MapleEventType eventType = MapleEventType.getByString(splitted[1]);
                    if (eventType == null) {
                        sb = new StringBuilder("Wrong syntax: ");
                        for (MapleEventType t : MapleEventType.values()) {
                            sb.append(t.name()).append(",");
                        }
                        c.getPlayer().dropMessage(5, sb.toString().substring(0, sb.toString().length() - 1));
                         return true;
                    }
                    final String msg = MapleEvent.scheduleEvent(eventType, c.getChannelServer());
                    if (msg.length() > 0) {
                        c.getPlayer().dropMessage(5, msg);
                         return true;
                    }
                     return true;
                case "removeitem":
                    if (splitted.length < 3) {
                        c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                         return true;
                    }
                    chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (chr == null) {
                        c.getPlayer().dropMessage(6, "This player does not exist");
                         return true;
                    }
                    chr.removeAll(Integer.parseInt(splitted[2]), false);
                    c.getPlayer().dropMessage(6, "All items with the ID " + splitted[2] + " has been removed from the inventory of " + splitted[1] + ".");
                     return true;
                case "lockitem":
                    if (splitted.length < 3) {
                        c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                         return true;
                    }
                    chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (chr == null) {
                        c.getPlayer().dropMessage(6, "This player does not exist");
                         return true;
                    }
                    int itemid = Integer.parseInt(splitted[2]);
                    MapleInventoryType invType = GameConstants.getInventoryType(itemid);
                    for (Item item : chr.getInventory(invType).listById(itemid)) {
                        item.setFlag((byte) (item.getFlag() | ItemFlag.LOCK.getValue()));
                        chr.getClient().getSession().write(InventoryPacket.updateSpecialItemUse(item, invType.getType(), item.getPosition(), true, chr));
                    }
                    if (invType == MapleInventoryType.EQUIP) {
                        invType = MapleInventoryType.EQUIPPED;
                        for (Item item : chr.getInventory(invType).listById(itemid)) {
                            item.setFlag((byte) (item.getFlag() | ItemFlag.LOCK.getValue()));
                            //chr.getClient().getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                        }
                    }
                    c.getPlayer().dropMessage(6, "All items with the ID " + splitted[2] + " has been locked from the inventory of " + splitted[1] + ".");
                     return true;
                case "killmap":
                    for (MapleCharacter map2 : c.getPlayer().getMap().getCharacters()) {
                        if (map2 != null && map2 != player) {
                            if (map2.isEric() || (!player.isAdmin() && map2.isGM())) {
                                 player.dropMessage(map2.getName() + " is a GM.");
                            } else {
                                map2.getStat().setHp((short) 0, c.getPlayer());
                                map2.getStat().setMp((short) 0, c.getPlayer());
                                map2.updateSingleStat(MapleStat.HP, 0);
                                map2.updateSingleStat(MapleStat.MP, 0);
                            }
                        }
                    }
                     return true;
                case "smegaplayer":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    World.Broadcast.broadcastSmega(player.getWorld(), CWvsContext.serverNotice(3, victim == null ? c.getChannel() : victim.getClient().getChannel(), victim == null ? splitted[1] : victim.getName() + " : " + StringUtil.joinStringFrom(splitted, 2), true));
                     return true;
                case "elf": // TODO: make donor? o.o
                    if (splitted.length > 1) {
                            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                        if (victim != null) {
                            victim.changeElf();
                            player.dropMessage("You've  " + victim.getName() + "'s Elf Ears.");
                        }
                    } else {
                            player.changeElf();
                            player.dropMessage("You now have Elf Ears.");
                    }  
                    return true;
                case "speak":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (player.gmLevel() > 5) {
                        if (victim == null) {
                            c.getPlayer().dropMessage(5, "unable to find '" + splitted[1]);
                             return true;
                        }
                        ChatHandler.GeneralChat(InternCommand.joinStringFrom(splitted, 2), (byte) 0, victim.getClient(), victim);
                         return true;
                    } else {
                    if (victim.gmLevel() >= 6 || victim == null) { //MAYBE ADMINS DONT WANNA GET !SPEAK'D, YAH. :(
                        c.getPlayer().dropMessage(5, "Unable to find '" + splitted[1]);
                         return true;
                    } else {
                        victim.getMap().broadcastMessage(CField.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 2), victim.isGM(), 0));
                      }
                    }
                         return true;
                  case "drop":
                    int aitemId = Integer.parseInt(splitted[1]);
                    int aquantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
                    MapleItemInformationProvider ii_ = MapleItemInformationProvider.getInstance();
                    if (GameConstants.isPet(aitemId)) {
                        c.getPlayer().dropMessage(5, "Please purchase a pet from the cash shop instead.");
                    } else if (!ii_.itemExists(aitemId)) {
                        c.getPlayer().dropMessage(5, aitemId + " does not exist");
                    } else {
                        Item toDrop;
                        if (GameConstants.getInventoryType(aitemId) == MapleInventoryType.EQUIP) {

                            toDrop = ii_.randomizeStats((Equip) ii_.getEquipById(aitemId));
                        } else {
                            toDrop = new client.inventory.Item(aitemId, (byte) 0, (short) aquantity, (byte) 0);
                        }
                         if (!c.getPlayer().isAdmin()) {
                        for (int i : GameConstants.itemBlock) {
                            if (aitemId == i) {
                                c.getPlayer().dropMessage(5, "Sorry but this item is blocked for your GM level.");
                                 return true;
                            }
                        }
                    }
                        toDrop.setOwner(player.getName());
                        if (!c.getPlayer().isAdmin()) {
                            toDrop.setGMLog(c.getPlayer().getName() + " used !drop");
                        }
                        toDrop.setOwner(c.getPlayer().getName());
                        c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
                    }
                     return true;
                case "gainmeso":
                case "meso":
                    c.getPlayer().gainMeso(Integer.parseInt(splitted[1]), true);
                     return true;
                case "disease":
                    if (splitted.length < 3) {
                        c.getPlayer().dropMessage(6, "!disease <type> [charname] <level> where type = SEAL/DARKNESS/WEAKEN/STUN/CURSE/POISON/SLOW/SEDUCE/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                         return true;
                    }
                    int type;
                    if (splitted[1].equalsIgnoreCase("SEAL")) {
                        type = 120;
                    } else if (splitted[1].equalsIgnoreCase("DARKNESS")) {
                        type = 121;
                    } else if (splitted[1].equalsIgnoreCase("WEAKEN")) {
                        type = 122;
                    } else if (splitted[1].equalsIgnoreCase("STUN")) {
                        type = 123;
                    } else if (splitted[1].equalsIgnoreCase("CURSE")) {
                        type = 124;
                    } else if (splitted[1].equalsIgnoreCase("POISON")) {
                        type = 125;
                    } else if (splitted[1].equalsIgnoreCase("SLOW")) {
                        type = 126;
                    } else if (splitted[1].equalsIgnoreCase("SEDUCE")) {
                        type = 128;
                    } else if (splitted[1].equalsIgnoreCase("REVERSE")) {
                        type = 132;
                    } else if (splitted[1].equalsIgnoreCase("ZOMBIFY")) {
                        type = 133;
                    } else if (splitted[1].equalsIgnoreCase("POTION")) {
                        type = 134;
                    } else if (splitted[1].equalsIgnoreCase("SHADOW")) {
                        type = 135;
                    } else if (splitted[1].equalsIgnoreCase("BLIND")) {
                        type = 136;
                    } else if (splitted[1].equalsIgnoreCase("FREEZE")) {
                        type = 137;
                    } else if (splitted[1].equalsIgnoreCase("POTENTIAL")) {
                        type = 138;
                    } else {
                        c.getPlayer().dropMessage(6, "!disease <type> [charname] <level> where type = SEAL/DARKNESS/WEAKEN/STUN/CURSE/POISON/SLOW/SEDUCE/REVERSE/ZOMBIFY/POTION/SHADOW/BLIND/FREEZE/POTENTIAL");
                         return true;
                    }
                    if (splitted.length == 4) {
                        victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[2]);
                        if (victim == null) {
                            c.getPlayer().dropMessage(5, "Not found.");
                             return true;
                        }
                        victim.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1));
                    } else {
                        for (MapleCharacter victim2 : c.getPlayer().getMap().getCharactersThreadsafe()) {
                            victim2.disease(type, CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1));
                        }
                    }
                     return true;
                /*case "setinstanceproperty":
                    if (em == null || em.getInstances().size() <= 0) {
                        c.getPlayer().dropMessage(5, "none");
                    } else {
                        em.setProperty(splitted[2], splitted[3]);
                        for (EventInstanceManager eim : em.getInstances()) {
                            eim.setProperty(splitted[2], splitted[3]);
                        }
                    }
                     return true;
                case "listinstanceproperty":
                    if (em == null || em.getInstances().size() <= 0) {
                        c.getPlayer().dropMessage(5, "none");
                    } else {
                        for (EventInstanceManager eim : em.getInstances()) {
                            c.getPlayer().dropMessage(5, "Event " + eim.getName() + ", eventManager: " + em.getName() + " iprops: " + eim.getProperty(splitted[2]) + ", eprops: " + em.getProperty(splitted[2]));
                        }
                    }
                     return true;*/
                case "checkinv":
                    NPCScriptManager.getInstance().start(c, 2081005);
                    return true;
                case "leaveinstance":
                    if (c.getPlayer().getEventInstance() == null) {
                        c.getPlayer().dropMessage(5, "You are not in one");
                    } else {
                        c.getPlayer().getEventInstance().unregisterPlayer(c.getPlayer());
                    }
                     return true;
                case "whosthere":
                    StringBuilder builder = new StringBuilder("Players on Map: ").append(c.getPlayer().getMap().getCharactersThreadsafe().size()).append(", ");
                    for (MapleCharacter chr2 : c.getPlayer().getMap().getCharactersThreadsafe()) {
                        if (builder.length() > 150) { // wild guess :o
                            builder.setLength(builder.length() - 2);
                            c.getPlayer().dropMessage(6, builder.toString());
                            builder = new StringBuilder();
                        }
                        builder.append(MapleCharacterUtil.makeMapleReadable(chr2.getName()));
                        builder.append(", ");
                    }
                    builder.setLength(builder.length() - 2);
                    c.getPlayer().dropMessage(6, builder.toString());
                     return true;
                /*case "startinstance":
                    if (c.getPlayer().getEventInstance() != null) {
                        c.getPlayer().dropMessage(5, "You are in one");
                    } else if (splitted.length > 2) {
                        if (em == null || em.getInstance(splitted[2]) == null) {
                            c.getPlayer().dropMessage(5, "Not exist");
                        } else {
                            em.getInstance(splitted[2]).registerPlayer(c.getPlayer());
                        }
                    } else {
                        c.getPlayer().dropMessage(5, "!startinstance [eventmanager] [eventinstance]");
                    }
                     return true;*/
                case "resetmobs":
                    c.getPlayer().getMap().killAllMonsters(false);
                     return true;
                case "killmonsterbyoid":
                    int targetId = Integer.parseInt(splitted[1]);
                    MapleMonster monster = map.getMonsterByOid(targetId);
                    if (monster != null) {
                        map.killMonster(monster, c.getPlayer(), false, false, (byte) 1);
                    }
                     return true;
                case "removenpcs":
                    c.getPlayer().getMap().resetNPCs();
                     return true;
                case "notice":
                    int joinmod = 1;
                    int range = -1;
                    switch (splitted[1]) {
                        case "m":
                            range = 0;
                            break;
                        case "c":
                            range = 1;
                            break;
                        case "w":
                            range = 2;
                            break;
                    }
                    int tfrom = 2;
                    if (range == -1) {
                        range = 2;
                        tfrom = 1;
                    }
                    int color = getNoticeType(splitted[tfrom]);
                    if (color == -1) {
                        color = 0;
                        joinmod = 0;
                    }
                    sb = new StringBuilder();
                    if (splitted[tfrom].equals("nv")) {
                        sb.append("[Notice]");
                    } else {
                        sb.append("");
                    }
                    joinmod += tfrom;
                    sb.append(StringUtil.joinStringFrom(splitted, joinmod));

                    byte[] packet = CWvsContext.serverNotice(color, sb.toString());
                    if (range == 0) {
                        c.getPlayer().getMap().broadcastMessage(packet);
                    } else if (range == 1) {
                        ChannelServer.getInstance(player.getWorld(), c.getChannel()).broadcastPacket(packet);
                    } else if (range == 2) {
                        World.Broadcast.broadcastMessage(player.getWorld(), packet);
                    }
                     return true;
                case "y":
                case "yellow":
                    range = -1;
                    switch (splitted[1]) {
                        case "m":
                            range = 0;
                            break;
                        case "c":
                            range = 1;
                            break;
                        case "w":
                            range = 2;
                            break;
                    }
                    if (range == -1) {
                        range = 2;
                    }
                    packet = CWvsContext.yellowChat((splitted[0].equals("!y") ? ("[" + c.getPlayer().getName() + "] ") : "") + StringUtil.joinStringFrom(splitted, 2));
                    if (range == 0) {
                        c.getPlayer().getMap().broadcastMessage(packet);
                    } else if (range == 1) {
                        ChannelServer.getInstance(player.getWorld(), c.getChannel()).broadcastPacket(packet);
                    } else if (range == 2) {
                        World.Broadcast.broadcastMessage(player.getWorld(), packet);
                    }
                     return true;
                case "whatsmyip":
                    c.getPlayer().dropMessage(5, "IP: " + c.getSession().getRemoteAddress().toString().split(":")[0]);
                     return true;
                case "tdrops":
                    c.getPlayer().getMap().toggleDrops();
                     return true;
                default:
                    if (c.getPlayer().getGMLevel() >= 5) {
                        return SuperGMCommand.executeSuperGMCommand(c, splitted);
                    } else {
                        return InternCommand.executeInternCommand(c, splitted);
                    }
            }
        } else {
            c.getPlayer().showMessage("You are not a GM Level 4 (Game Master), how the fuck did you get this far?!");
            return true;
        }
    }
    
    static int getNoticeType(String typestring) {
        switch (typestring) {
            case "n":
                return 0;
            case "p":
                return 1;
            case "l":
                return 2;
            case "nv":
                return 5;
            case "v":
                return 5;
            case "b":
                return 6;
            default:
                return -1;
        }
    }
}
