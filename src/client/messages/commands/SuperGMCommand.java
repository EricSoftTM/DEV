package client.messages.commands;

import client.*;
import client.inventory.*;
import client.messages.CommandProcessorUtil;
import constants.GameConstants;
import constants.Occupations;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.MapleServerHandler;
import handling.RecvPacketOpcode;
import handling.SendPacketOpcode;
import handling.channel.ChannelServer;
import handling.channel.handler.ChatHandler;
import handling.login.LoginServer;
import handling.login.handler.CharLoginHandler;
import handling.world.World;
import java.awt.Point;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import scripting.PortalScriptManager;
import scripting.ReactorScriptManager;
import server.*;
import server.Timer;
import server.Timer.BuffTimer;
import server.Timer.EtcTimer;
import server.Timer.EventTimer;
import server.Timer.MapTimer;
import server.Timer.WorldTimer;
import server.life.*;
import server.maps.*;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CField.NPCPacket;
import tools.packet.CWvsContext;
import tools.packet.MobPacket;

/**
 * @author: Eric
 * @rev: 3.9 - Moved several commands to GMCommand
 * 
 */
public class SuperGMCommand {
    
    static boolean usedCommandSuperGM = false;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERGM;
    }
    
    public static boolean executeSuperGMCommand(MapleClient c, String[] splitted) {
        if (c.getPlayer().getGMLevel() >= PlayerGMRank.SUPERGM.getLevel()) {
            StringBuilder builder = new StringBuilder();
            MapleCharacter player = c.getPlayer();
            MapleCharacter chrs;
            MapleCharacter victim;
            MapleCharacter target;
            MapleMonster mob;
            double range = Double.POSITIVE_INFINITY;
            int damage;
            MapleMap map = c.getPlayer().getMap();
            Thread[] threads = new Thread[Thread.activeCount()];
            Skill skill;
            byte ret;
            if (player.gmLevel() < 6 && usedCommandSuperGM == false) {
                    FileoutputUtil.log("GMLog.txt", "\r\nIGN: " + player.getName() + " || Command: " + InternCommand.joinStringFrom(splitted, 0) + " \r\n");
                    usedCommandSuperGM = true;
                    EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                           usedCommandSuperGM = false;  
                        }
                    }, 10);
                }
            switch (splitted[0].substring(1).toLowerCase()) {
                // Start of Eric's Commands
                case "proitem":
                    try {
                        int itemid = Integer.parseInt(splitted[1]);
                        int stats = Integer.parseInt(splitted[2]);
                        Equip equip = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
                        if (equip == null) {
                            c.getPlayer().showMessage("Item does not exist.");
                        } else {
                            equip.makeProItem(c.getPlayer().getName(), (short) stats, false);
                            MapleInventoryManipulator.addbyItem(c, (Item) equip);
                            c.getPlayer().showMessage("You just got a " + MapleItemInformationProvider.getInstance().getName(itemid) + "!");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return true;
                case "fakeban":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    final MapleCharacter fakeBan = victim;
                    CharLoginHandler.fakeBan = victim.getClient().getAccountName();
                    victim.dropNPC("You have been blocked by #bPolice GMMapleStory for the HACK reason.#k");
                    EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            fakeBan.getClient().disconnect(true, false);
                            fakeBan.getClient().getSession().close();
                        }
                    }, 2 * 1000);
                    World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(6, "<Development Ban> : " + player.getName() + " has banned " + victim.getName() + " for " + InternCommand.joinStringFrom(splitted, 2)));
                    return true;
                case "unfakeban":
                    CharLoginHandler.fakeBan = "";
                    player.dropMessage("Reset Fake Ban.");
                    return true;
                case "flyperson":
                        victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                        victim.fly(victim);
                    /*if (victim.isFlying() == false) {
                        SkillFactory.getSkill(80001069).getEffect(1).applyTo(victim);
                      if (victim.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                        victim.setFlying(true);
                        SkillFactory.getSkill(80001089).getEffect(1).applyTo(victim);
                        victim.cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
                        victim.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                        victim.dropMessage(5, "Fly Mode : Active.");
                        victim.dropMessage(5, "To fly, simply jump and then use the arrow keys to control your flight.");
                   }
                 } else if (victim.isFlying() == true) {
                     victim.cancelBuffStats(MapleBuffStat.SOARING);
                     victim.cancelEffectFromBuffStat(MapleBuffStat.SOARING);
                     victim.setFlying(false);
                     victim.dropMessage(5, "Fly Mode : Inactive.");
                 }*/
                    return true;
                case "supermax":
                    HashMap sa = new HashMap();
      for (Skill skil : SkillFactory.getAllSkills()) {
        if ((GameConstants.isApplicableSkill(skil.getId())) && (skil.getId() < 90000000)) {
          if ((skil.getId() != 3101003) && (skil.getId() != 3201003) && (skil.getId() != 13101004))
            sa.put(skil, new SkillEntry((byte)skil.getMaxLevel(), (byte)skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
          else {
            sa.put(skil, new SkillEntry(0, (byte)skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
          }
        }
      }
      player.changeSkillsLevel(sa);
                    return true;
                case "maxtraits":
                    player.traits.get(MapleTrait.MapleTraitType.charm).setExp(93596);
                    player.traits.get(MapleTrait.MapleTraitType.charisma).setExp(93596);
                    player.traits.get(MapleTrait.MapleTraitType.craft).setExp(93596);
                    player.traits.get(MapleTrait.MapleTraitType.insight).setExp(93596);
                    player.traits.get(MapleTrait.MapleTraitType.sense).setExp(93596);
                    player.traits.get(MapleTrait.MapleTraitType.will).setExp(93596);
                    c.getSession().write(CField.getCharInfo(player));
                    player.getMap().removePlayer(player);
                    player.getMap().addPlayer(player);
                    player.dropMessage(5, "Maxed your traits to 93596! If you don't see it, relog!");
                        return true;
                case "rank1":
                        player.pvpExp = 10001562;
                        player.savePlayer();
                        c.getSession().write(CField.getCharInfo(player));
                        player.getMap().removePlayer(player);
                        player.getMap().addPlayer(player);
                        player.dropMessage(5, "You are now PvP Rank 1! If you don't see it, try doing a relog!");
                        return true;
                case "maxmeso":
                case "maxmesos":
                    player.gainMeso(Integer.MAX_VALUE - player.getMeso(), true);
                    return true;
                case "superzak":
                    player.getMap().broadcastMessage(CWvsContext.serverNotice(1, "You're about to battle against a monster with such high HP it can't even be valued.\r\n\r\nCalculation of total HP: \\7.28e+13\\"));
                    player.spawnMonster(8800000, 9100000000000L, 9999999, 1);
                    for (int x = 8800003; x < 8800011; x++) {
                       player.spawnMonster(x, 9100000000000L, 9999999, 1);
                    }
                    AdminCommand.superBaal = true; // umad luke?
                    return true;
                case "1337bob":
                    player.dropMessage(1, "Bob is sexy ok\r\nhe is 1337 yep");
                    player.dropMessage(1, "Some facts about bob:\r\nBob's MAXHP: 9223372036854775807\r\nHow much HP is that?: Over a quintillion.\r\nCan I hit max damage?: Your damage is fixed at 1337.\r\nHow long will it take to kill bob?: About 3.449279e+15 days.");
                    MapleMonster npcmob = MapleLifeFactory.getMonster(100100);
                    MapleMonsterStats stats = new MapleMonsterStats(100100);
                    OverrideMonsterStats newStats = new OverrideMonsterStats();
                    newStats.setOHp(Long.MAX_VALUE);
                    newStats.setOMp(9999999);
                    stats.setFixedDamage(1337);
                    npcmob.setOverrideStats(newStats);
                    npcmob.setHp(npcmob.getMobMaxHp());
                    npcmob.setMp(npcmob.getMobMaxMp());
                    player.getMap().spawnMonsterOnGroundBelow(npcmob, player.getPosition());
                    return true;
                case "str":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    short amount = Short.parseShort(splitted[2]);
                    if (victim != null) {
                        if (victim.getStat().getStr() + amount < 32767) {
                            victim.getStat().setStr(amount, victim);
                            player.dropMessage(5, "Added " + amount + " AP to " + victim.getName() + "'s " + splitted[0]);
                        } else
                            player.dropMessage(5, victim.getName() + "'s " + splitted[0] + " can't exceed 32767.");
                    } else
                        player.dropMessage(5, "Unable to find the player.");
                    return true;
                case "dex":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    amount = Short.parseShort(splitted[2]);
                    if (victim != null) {
                        if (victim.getStat().getDex() + amount < 32767) {
                            victim.getStat().setDex(amount, victim);
                            player.dropMessage(5, "Added " + amount + " AP to " + victim.getName() + "'s " + splitted[0]);
                        } else
                            player.dropMessage(5, victim.getName() + "'s " + splitted[0] + " can't exceed 32767.");
                    } else
                        player.dropMessage(5, "Unable to find the player.");
                    return true;
                case "int":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    amount = Short.parseShort(splitted[2]);
                    if (victim != null) {
                        if (victim.getStat().getInt() + amount < 32767) {
                            victim.getStat().setInt(amount, victim);
                            player.dropMessage(5, "Added " + amount + " AP to " + victim.getName() + "'s " + splitted[0]);
                        } else
                            player.dropMessage(5, victim.getName() + "'s " + splitted[0] + " can't exceed 32767.");
                    } else
                        player.dropMessage(5, "Unable to find the player.");
                    return true;
                case "luk":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    amount = Short.parseShort(splitted[2]);
                    if (victim != null) {
                        if (victim.getStat().getLuk() + amount < 32767) {
                            victim.getStat().setLuk(amount, victim);
                            player.dropMessage(5, "Added " + amount + " AP to " + victim.getName() + "'s " + splitted[0]);
                        } else
                            player.dropMessage(5, victim.getName() + "'s " + splitted[0] + " can't exceed 32767.");
                    } else
                        player.dropMessage(5, "Unable to find the player.");
                    return true;
                case "apstorage":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    int apamount = Integer.parseInt(splitted[2]);
                    if (victim != null) {
                        victim.gainAPS(apamount); // should add a check but nobodys going to be giving a player 2.147b ap. :|
                        player.dropMessage(5, "Added " + apamount + " AP to " + victim.getName() + "'s AP Storage.");
                    } else
                        player.dropMessage(5, "Unable to find the player.");
                    return true;
                case "worldtrip":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim.isAdmin()) {
                    player.dropMessage("Don't world trip owners!");
                } else {
                if (victim != null) {
                    for (int i = 0; i < 4; i++) {
                        victim.changeMap(100000000 + 1000000 * i);
                    }
                } else {
                    player.dropMessage(splitted[1] + " either doesn't exist or is offline");
                }
                }
                return true;
                case "whatsmyid":
                    player.dropMessage(6, "Your Current Player ID In The Database Is : " + player.getId());
                    return true;
                case "mypos":
                    player.dropMessage(6, "X = " + player.getPosition().x + ", Y = " + player.getPosition().y);
                    return true;
                case "giftnx":
                    c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]).modifyCSPoints(1, Integer.parseInt(splitted[2]) * 2);
                    player.dropMessage("Gifted " + Integer.parseInt(splitted[2]) + " NX to " + c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]).getName());
                    c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]).dropMessage(5, player.getName() + " has gifted you " + Integer.parseInt(splitted[1]) + " NX.");
                    return true;
                case "bombwholemap":
                    for (MapleCharacter map_ : player.getMap().getCharacters()) {
                if (map_ != null && map_ != player) {
                    map_.spawnBomb();
                    return true;
                }
            }
                return true;
                    // End of Eric's Commands
                case "unb":
                case "unban":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "[Syntax] !Unban <IGN>");
                        return true;
                    }
                    ret = MapleClient.unban(splitted[1]);
                    if (ret == -2) {
                        c.getPlayer().dropMessage(6, "[Unban] SQL error.");
                        return true;
                    } else if (ret == -1) {
                        c.getPlayer().dropMessage(6, "[Unban] The character does not exist.");
                        return true;
                    } else {
                        c.getPlayer().dropMessage(6, "[Unban] Successfully unbanned!");

                    }
                    byte ret_ = MapleClient.unbanIPMacs(splitted[1]);
                    if (ret_ == -2) {
                        c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
                    } else if (ret_ == -1) {
                        c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
                    } else if (ret_ == 0) {
                        c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
                    } else if (ret_ == 1) {
                        c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
                    } else if (ret_ == 2) {
                        c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
                    }
                    return true;
                case "unbanip":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "[Syntax] !unbanip <IGN>");
                        return true;
                    }
                    ret = MapleClient.unbanIPMacs(splitted[1]);
                    if (ret == -2) {
                        c.getPlayer().dropMessage(6, "[UnbanIP] SQL error.");
                    } else if (ret == -1) {
                        c.getPlayer().dropMessage(6, "[UnbanIP] The character does not exist.");
                    } else if (ret == 0) {
                        c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
                    } else if (ret == 1) {
                        c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
                    } else if (ret == 2) {
                        c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
                    }
                    if (ret > 0) {
                        return true;
                    }
                    return true;
                case "giveskill":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    skill = SkillFactory.getSkill(Integer.parseInt(splitted[2]));
                    byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);
                    byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 4, 1);

                    if (level > skill.getMaxLevel()) {
                        level = (byte) skill.getMaxLevel();
                    }
                    if (masterlevel > skill.getMaxLevel()) {
                        masterlevel = (byte) skill.getMaxLevel();
                    }
                    victim.changeSingleSkillLevel(skill, level, masterlevel);
                    return true;
                case "unlockinv":
                    java.util.Map<Item, MapleInventoryType> eqs = new HashMap<>();
                    boolean add = false;
                    if (splitted.length < 2 || splitted[1].equals("all")) {
                        for (MapleInventoryType type : MapleInventoryType.values()) {
                            for (Item item : c.getPlayer().getInventory(type)) {
                                if (ItemFlag.LOCK.check(item.getFlag())) {
                                    item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                                    add = true;
                                    //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                                }
                                if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                                    item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                                    add = true;
                                    //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                                }
                                if (add) {
                                    eqs.put(item, type);
                                }
                                add = false;
                            }
                        }
                    } else if (splitted[1].equals("eqp")) {
                        for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).newList()) {
                            if (ItemFlag.LOCK.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (add) {
                                eqs.put(item, MapleInventoryType.EQUIP);
                            }
                            add = false;
                        }
                    } else if (splitted[1].equals("eq")) {
                        for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                            if (ItemFlag.LOCK.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (add) {
                                eqs.put(item, MapleInventoryType.EQUIP);
                            }
                            add = false;
                        }
                    } else if (splitted[1].equals("u")) {
                        for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                            if (ItemFlag.LOCK.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (add) {
                                eqs.put(item, MapleInventoryType.USE);
                            }
                            add = false;
                        }
                    } else if (splitted[1].equals("s")) {
                        for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                            if (ItemFlag.LOCK.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (add) {
                                eqs.put(item, MapleInventoryType.SETUP);
                            }
                            add = false;
                        }
                    } else if (splitted[1].equals("e")) {
                        for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                            if (ItemFlag.LOCK.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (add) {
                                eqs.put(item, MapleInventoryType.ETC);
                            }
                            add = false;
                        }
                    } else if (splitted[1].equals("c")) {
                        for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                            if (ItemFlag.LOCK.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.LOCK.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (ItemFlag.UNTRADEABLE.check(item.getFlag())) {
                                item.setFlag((byte) (item.getFlag() - ItemFlag.UNTRADEABLE.getValue()));
                                add = true;
                                //c.getSession().write(CField.updateSpecialItemUse(item, type.getType()));
                            }
                            if (add) {
                                eqs.put(item, MapleInventoryType.CASH);
                            }
                            add = false;
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "[all/eqp/eq/u/s/e/c]");
                    }

                    for (Entry<Item, MapleInventoryType> eq : eqs.entrySet()) {
                        c.getPlayer().forceReAddItem_NoUpdate(eq.getKey().copy(), eq.getValue());
                    }
                    return true;
                case "pap":
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8500001), player.getPosition());
                    return true;
                case "pianus":
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8510000), player.getPosition());
                    return true;
                case "occupations":
            player.dropMessage("None - 0");
            player.dropMessage("Noob - 1");
            player.dropMessage("Sniper - 100");
            player.dropMessage("Leprechaun - 200");
            player.dropMessage("NX Addict - 300");
            player.dropMessage("Hacker - 400");
            player.dropMessage("Eric IdoL - 500");
            player.dropMessage("The Transformers AutoBots - 600");
            player.dropMessage("Smega Whore - 700");
            player.dropMessage("Terrorist - 800");
            // player.dropMessage("I liek teh 1337 - 1337");
            return true;
                case "occ":
                case "occupation":
                    if (splitted.length != 2) {
                        player.dropMessage("Occupation ID List - !occupations");
                        return true;
                    }
                    player.setOccId(Integer.parseInt(splitted[1]));
                    player.setOccupation(Integer.parseInt(splitted[1]));
                    player.dropMessage("You have changed your occupation to " + Occupations.getById(Integer.parseInt(splitted[1])));
            return true;
                case "occplayer":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.setOccId(Integer.parseInt(splitted[2]));
                    victim.setOccupation(Integer.parseInt(splitted[2]));
                    player.dropMessage(victim.getName() + " is now a " + Occupations.getById(Integer.parseInt(splitted[2])));
                    return true;
                case "makeitem":
                    int itemId = Integer.parseInt(splitted[1]);
                    short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
                    MapleItemInformationProvider ii_ = MapleItemInformationProvider.getInstance();
                    if (splitted.length > 3) {
                        return true;
                    }
                    try {
                        itemId = Integer.parseInt(splitted[1]);
                    } catch (Exception e) {
                        itemId = 0;
                        c.getPlayer().showMessage("Error while creating item.");
                    }
                    try {
                        quantity = Short.parseShort(splitted[2]);
                    } catch (Exception e) {
                        quantity = 1;
                    }
                    if (!c.getPlayer().isAdmin()) {
                        for (int i : GameConstants.itemBlock) {
                            if (itemId == i) {
                                c.getPlayer().dropMessage(5, "Sorry but this item is blocked for your GM level.");
                                return true;
                            }
                        }
                    }
                    if (MapleItemInformationProvider.getInstance().itemExists(itemId)) {
                        if (itemId != 0) {
                            if (itemId >= 5000000 && itemId < 5000065) {
                                MaplePet.createPet(itemId, -1);
                            } else {
                                MapleInventoryManipulator.addById(c, itemId, (short) quantity, c.getPlayer().getName() + " used !item to get " + itemId);
                            }
                        }
                    } else {
                    c.getPlayer().showMessage("Item does not exist.");
                    }
                    return true;
                case "marry":
                    if (splitted.length < 3) {
                        c.getPlayer().dropMessage(6, "Need <name> <itemid>");
                        return true;
                    }
                    itemId = Integer.parseInt(splitted[2]);
                    if (!GameConstants.isEffectRing(itemId)) {
                        c.getPlayer().dropMessage(6, "Invalid itemID.");
                    } else {
                        MapleCharacter fff = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                        if (fff == null) {
                            c.getPlayer().dropMessage(6, "Player must be online");
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
                                    chrz[i].dropMessage(6, "Successfully married with " + chrz[i == 0 ? 1 : 0].getName());
                                }
                                MapleRing.addToDB(itemId, c.getPlayer(), fff.getName(), fff.getId(), ringID);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return true;
                case "vac":
                    if (!c.getPlayer().isHidden()) {
                        c.getPlayer().dropMessage(6, "You can only vac monsters while in hide.");
                        return true;
                    } else {
                        for (final MapleMapObject mmo : c.getPlayer().getMap().getAllMonstersThreadsafe()) {
                            final MapleMonster monster = (MapleMonster) mmo;
                            c.getPlayer().getMap().broadcastMessage(MobPacket.moveMonster(false, -1, 0, monster.getObjectId(), monster.getTruePosition(), c.getPlayer().getLastRes()));
                            monster.setPosition(c.getPlayer().getPosition());
                        }
                    }
                    return true;
                case "givevpoint":
                    if (splitted.length < 3) {
                        c.getPlayer().dropMessage(6, "Need playername and amount.");
                        return true;
                    }
                    chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (chrs == null) {
                        c.getPlayer().dropMessage(6, "Make sure they are in the correct channel");
                    } else {
                        chrs.setVPoints(chrs.getVPoints() + Integer.parseInt(splitted[2]));
                        c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getVPoints() + " vpoints, after giving " + splitted[2] + ".");
                    }
                    return true;
                case "speakmap":
                    for (MapleCharacter victim2 : c.getPlayer().getMap().getCharactersThreadsafe()) {
                        if (player.isOwner()) {
                            if (victim2.getId() != c.getPlayer().getId()) {
                                ChatHandler.GeneralChat(InternCommand.joinStringFrom(splitted, 1), (byte) 0, victim2.getClient(), victim2);
                            }
                        } else {
                            if (victim2.getId() != c.getPlayer().getId()) {
                                victim2.getMap().broadcastMessage(CField.getChatText(victim2.getId(), StringUtil.joinStringFrom(splitted, 1), victim2.isGM(), 0));
                            }
                        }
                    }
                    return true;
                case "speakchannel":
                    for (MapleCharacter victim2 : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                        if (victim2.getId() != c.getPlayer().getId()) {
                            victim2.getMap().broadcastMessage(CField.getChatText(victim2.getId(), StringUtil.joinStringFrom(splitted, 1), victim2.isGM(), 0));
                        }
                    }
                    return true;
                case "speakworld":
                    for (ChannelServer cserv : LoginServer.getInstance().getWorld(c.getWorld()).getChannels()) {
                        for (MapleCharacter victim2 : cserv.getPlayerStorage().getAllCharacters()) {
                            if (victim2.getId() != c.getPlayer().getId()) {
                                victim2.getMap().broadcastMessage(CField.getChatText(victim2.getId(), StringUtil.joinStringFrom(splitted, 1), victim2.isGM(), 0));
                            }
                        }
                    }
                    return true;
                
                case "resetother":
                    MapleQuest.getInstance(Integer.parseInt(splitted[2])).forfeit(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]));
                    return true;
                case "fstartother":
                    MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceStart(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]), splitted.length > 4 ? splitted[4] : null);
                    return true;
                case "fcompleteother":
                    MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceComplete(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]));
                    return true;
                case "threads":
                    Thread.enumerate(threads);
                    String filter = "";
                    if (splitted.length > 1) {
                        filter = splitted[1];
                    }
                    for (int i = 0; i < threads.length; i++) {
                        String tstring = threads[i].toString();
                        if (tstring.toLowerCase().indexOf(filter.toLowerCase()) > -1) {
                            c.getPlayer().dropMessage(6, i + ": " + tstring);
                        }
                    }
                    return true;
                case "showtrace":
                    if (splitted.length < 2) {
                        throw new IllegalArgumentException();
                    }
                    Thread.enumerate(threads);
                    Thread t = threads[Integer.parseInt(splitted[1])];
                    c.getPlayer().dropMessage(6, t.toString() + ":");
                    for (StackTraceElement elem : t.getStackTrace()) {
                        c.getPlayer().dropMessage(6, elem.toString());
                    }
                    return true;
               
                case "tmegaphone":
                    World.toggleMegaphoneMuteState(c.getWorld());
                    c.getPlayer().dropMessage(6, "Megaphone state : " + (c.getChannelServer().getMegaphoneMuteState() ? "Enabled" : "Disabled"));
                    return true;
                case "sreactor":
                    MapleReactor reactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(splitted[1])), Integer.parseInt(splitted[1]));
                    reactor.setDelay(-1);
                    c.getPlayer().getMap().spawnReactorOnGroundBelow(reactor, new Point(c.getPlayer().getTruePosition().x, c.getPlayer().getTruePosition().y - 20));
                    return true;
                case "clearsquads":
                    final Collection<MapleSquad> squadz = new ArrayList<>(c.getChannelServer().getAllSquads().values());
                    for (MapleSquad squads : squadz) {
                        squads.clear();
                    }
                    return true;
                case "hitmonsterbyoid":
                    int targetId = Integer.parseInt(splitted[1]);
                    damage = Integer.parseInt(splitted[2]);
                    MapleMonster monster = map.getMonsterByOid(targetId);
                    if (monster != null) {
                        map.broadcastMessage(MobPacket.damageMonster(targetId, damage));
                        monster.damage(c.getPlayer(), damage, false);
                    }
                    return true;
                case "hitall":
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
                    damage = Integer.parseInt(splitted[1]);
                    for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                        mob = (MapleMonster) monstermo;
                        map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                        mob.damage(c.getPlayer(), damage, false);
                    }
                    return true;
                case "hitmonster":
                    damage = Integer.parseInt(splitted[1]);
                    for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                        mob = (MapleMonster) monstermo;
                        if (mob.getId() == Integer.parseInt(splitted[2])) {
                            map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                            mob.damage(c.getPlayer(), damage, false);
                        }
                    }
                    return true;
                case "killmonster":
                    for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                        mob = (MapleMonster) monstermo;
                        if (mob.getId() == Integer.parseInt(splitted[1])) {
                            mob.damage(c.getPlayer(), (int) mob.getHp(), false);
                        }
                    }
                    return true;
                case "killalldrops":
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
                    for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                        mob = (MapleMonster) monstermo;
                        map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
                    }
                    return true;
                case "killallexp":
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
                    for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                        mob = (MapleMonster) monstermo;
                        mob.damage(c.getPlayer(), (int) mob.getHp(), false);
                    }
                    return true;
                case "npc":
                    int npcId = Integer.parseInt(splitted[1]);
                    MapleNPC npc = MapleLifeFactory.getNPC(npcId);
                    if (npc != null && !npc.getName().equals("MISSINGNO")) {
                        npc.setPosition(c.getPlayer().getPosition());
                        npc.setCy(c.getPlayer().getPosition().y);
                        npc.setRx0(c.getPlayer().getPosition().x + 50);
                        npc.setRx1(c.getPlayer().getPosition().x - 50);
                        npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                        npc.setCustom(true);
                        c.getPlayer().getMap().addMapObject(npc);
                        c.getPlayer().getMap().broadcastMessage(NPCPacket.spawnNPC(npc, true));
                    } else {
                        c.getPlayer().dropMessage(6, "You have entered an invalid Npc-Id");
                        return true;
                    }
                    return true;
                case "playernpc":
                    try {
                        c.getPlayer().dropMessage(6, "Making playerNPC...");
                        MapleCharacter chhr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                        if (chhr == null) {
                            c.getPlayer().dropMessage(6, splitted[1] + " is not online");
                            return true;
                        }
                        PlayerNPC pnpc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
                        pnpc.addToServer();
                        c.getPlayer().dropMessage(6, "Done");
                    } catch (Exception e) {
                        c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
                        e.printStackTrace();
                    }
                    return true;
                case "destroynpc":
                    try {
                        c.getPlayer().dropMessage(6, "Destroying playerNPC...");
                        MapleNPC dnpc = c.getPlayer().getMap().getNPCByOid(Integer.parseInt(splitted[1]));
                        if (dnpc instanceof PlayerNPC) {
                            ((PlayerNPC) dnpc).destroy(true);
                            c.getPlayer().dropMessage(6, "Done");
                        } else {
                            c.getPlayer().dropMessage(6, "!destroypnpc [objectid]");
                        }
                    } catch (Exception e) {
                        c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
                        e.printStackTrace();
                    }
                    return true;
                case "servermessage":
                    String outputMessage = StringUtil.joinStringFrom(splitted, 1);
                    for (World worlds : LoginServer.getInstance().getWorlds()) {
                        for (ChannelServer cserv : worlds.getChannels()) {
                            cserv.setServerMessage(outputMessage);
                        }
                    }
                    return true;
                case "respawn":
                    c.getPlayer().getMap().respawn(true);
                    return true;
                case "testtimer":
                    Timer toTest = null;
                    switch (splitted[1]) {
                        case "event":
                            toTest = EventTimer.getInstance();
                        case "etc":
                            toTest = EtcTimer.getInstance();
                        case "map":
                            toTest = MapTimer.getInstance();
                        case "world":
                            toTest = WorldTimer.getInstance();
                        case "buff":
                            toTest = BuffTimer.getInstance();
                    }
                    final int sec = Integer.parseInt(splitted[2]);
                    c.getPlayer().dropMessage(5, "Message will pop up in " + sec + " seconds.");
                    c.getPlayer().dropMessage(5, "Active: " + toTest.getSES().getActiveCount() + " Core: " + toTest.getSES().getCorePoolSize() + " Largest: " + toTest.getSES().getLargestPoolSize() + " Max: " + toTest.getSES().getMaximumPoolSize() + " Current: " + toTest.getSES().getPoolSize() + " Status: " + toTest.getSES().isShutdown() + toTest.getSES().isTerminated() + toTest.getSES().isTerminating());
                    return true;
                case "fillbook":
                    for (int e : MapleItemInformationProvider.getInstance().getMonsterBook().keySet()) {
                        c.getPlayer().getMonsterBook().getCards().put(e, 2);
                    }
                    c.getPlayer().getMonsterBook().changed();
                    c.getPlayer().dropMessage(5, "Done.");
                    return true;
                case "listbook":
                    final List<Entry<Integer, Integer>> mbList = new ArrayList<>(MapleItemInformationProvider.getInstance().getMonsterBook().entrySet());
                    Collections.sort(mbList, new BookComparator());
                    final int page = Integer.parseInt(splitted[1]);
                    for (int e = (page * 8); e < Math.min(mbList.size(), (page + 1) * 8); e++) {
                        c.getPlayer().dropMessage(6, e + ": " + mbList.get(e).getKey() + " - " + mbList.get(e).getValue());
                    }
                    return true;
                case "clearpokedex":
                    c.getPlayer().getMonsterBook().getCards().clear();
                    c.getPlayer().getMonsterBook().changed();
                    c.getPlayer().dropMessage(5, "Done.");
                    return true;
                case "subcategory":
                    c.getPlayer().setSubcategory(Byte.parseByte(splitted[1]));
                    return true;
                case "gaincash":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(5, "Need amount.");
                        return true;
                    }
                    c.getPlayer().modifyCSPoints(1, Integer.parseInt(splitted[1]), true);
                    return true;
                case "gainmp":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(5, "Need amount.");
                        return true;
                    }
                    c.getPlayer().modifyCSPoints(2, Integer.parseInt(splitted[1]), true);
                    return true;
                case "gainp":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(5, "Need amount.");
                        return true;
                    }
                    c.getPlayer().setPoints(c.getPlayer().getPoints() + Integer.parseInt(splitted[1]));
                    return true;
                case "gainvp":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(5, "Need amount.");
                        return true;
                    }
                    c.getPlayer().setVPoints(c.getPlayer().getVPoints() + Integer.parseInt(splitted[1]));
                    return true;
                case "reloadops":
                    SendPacketOpcode.reloadValues();
                    RecvPacketOpcode.reloadValues();
                    return true;
                case "reloaddrops":
                    MapleMonsterInformationProvider.getInstance().clearDrops();
                    ReactorScriptManager.getInstance().clearDrops();
                    return true;
                case "reloadportals":
                    PortalScriptManager.getInstance().clearScripts();
                    return true;
                case "reloadshops":
                    MapleShopFactory.getInstance().clear();
                    return true;
                case "reloadevents":
                    for (ChannelServer ch : LoginServer.getInstance().getWorld(c.getWorld()).getChannels()) {
                        ch.reloadEvents();
                    }
                    return true;
                case "resetmap":
                    c.getPlayer().getMap().resetFully();
                    return true;
                case "resetquest":
                    MapleQuest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
                    return true;
                case "startquest":
                    MapleQuest.getInstance(Integer.parseInt(splitted[1])).start(c.getPlayer(), Integer.parseInt(splitted[2]));
                    return true;
                case "completequest":
                    MapleQuest.getInstance(Integer.parseInt(splitted[1])).complete(c.getPlayer(), Integer.parseInt(splitted[2]), Integer.parseInt(splitted[3]));
                    return true;
                case "fstartquest":
                    MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(), Integer.parseInt(splitted[2]), splitted.length >= 4 ? splitted[3] : null);
                    return true;
                case "fcompletequest":
                    MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceComplete(c.getPlayer(), Integer.parseInt(splitted[2]));
                    return true;
                case "hreactor":
                    c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
                    return true;
                case "fhreactor":
                    c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).forceHitReactor(Byte.parseByte(splitted[2]));
                    return true;
                case "dreactor":
                    List<MapleMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.REACTOR));
                    if (splitted[1].equals("all")) {
                        for (MapleMapObject reactorL : reactors) {
                            MapleReactor reactor2l = (MapleReactor) reactorL;
                            c.getPlayer().getMap().destroyReactor(reactor2l.getObjectId());
                        }
                    } else {
                        c.getPlayer().getMap().destroyReactor(Integer.parseInt(splitted[1]));
                    }
                    return true;
                case "setreactor":
                    c.getPlayer().getMap().setReactorState(Byte.parseByte(splitted[1]));
                    return true;
                case "resetreactor":
                    c.getPlayer().getMap().resetReactors();
                    return true;
                case "sendallnote":
                    if (splitted.length >= 1) {
                        String text = StringUtil.joinStringFrom(splitted, 1);
                        for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                            c.getPlayer().sendNote(mch.getName(), text);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "Use it like this, !sendallnote <text>");
                        return true;
                    }
                    return true;
                case "sendnote":
                    if (splitted.length >= 2) { // raaaight? :(
                        String text = StringUtil.joinStringFrom(splitted, 2);
                        victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                        c.getPlayer().sendNote(victim.getName(), text);
                    } else
                        c.getPlayer().dropMessage(6, "Use it like this, !sendnote <ign> <text>");
                    return true;
                case "buffskill":
                    SkillFactory.getSkill(Integer.parseInt(splitted[1])).getEffect(Integer.parseInt(splitted[2])).applyTo(c.getPlayer());
                    return true;
                case "buffitem":
                    MapleItemInformationProvider.getInstance().getItemEffect(Integer.parseInt(splitted[1])).applyTo(c.getPlayer());
                    return true;
                case "buffitemex":
                    MapleItemInformationProvider.getInstance().getItemEffectEX(Integer.parseInt(splitted[1])).applyTo(c.getPlayer());
                    return true;
                case "itemsize":
                    c.getPlayer().dropMessage(6, "Number of items: " + MapleItemInformationProvider.getInstance().getAllItems().size());
                    return true;
                default:
                    if (c.getPlayer().getGMLevel() >= 6) {
                        return AdminCommand.executeAdminCommand(c, splitted);
                    } else {
                        return GMCommand.executeGMCommand(c, splitted);
                    }
            }
        } else {
            c.getPlayer().showMessage("You are not a GM Level 5 (Super Game Master), how the fuck did you get this far?!");
            return true;
        }
    }
        
    public static class BookComparator implements Comparator<Entry<Integer, Integer>>, Serializable {

            @Override
            public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
                if (o1.getValue() > o2.getValue()) {
                    return 1;
                } else if (o1.getValue() == o2.getValue()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    
    public static int getOptionalIntArg(String splitted[], int position, int def) {
        if (splitted.length > position) {
            try {
                return Integer.parseInt(splitted[position]);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }
}
