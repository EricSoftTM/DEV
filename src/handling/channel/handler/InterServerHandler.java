/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.SkillFactory;
import constants.GameConstants;
import constants.MapConstants;
import constants.ServerConstants;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.*;
import handling.world.exped.MapleExpedition;
import handling.world.guild.MapleGuild;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import server.maps.FieldLimitType;
import server.maps.MapleMap;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import tools.FileoutputUtil;
import tools.Pair;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.BuddylistPacket;
import tools.packet.CWvsContext.ExpeditionPacket;
import tools.packet.CWvsContext.FamilyPacket;
import tools.packet.CWvsContext.GuildPacket;
import tools.packet.LoginPacket;
import tools.packet.MTSCSPacket;

public class InterServerHandler {

    public static final void EnterCS(final MapleClient c, final MapleCharacter chr, final boolean mts) {
        if (chr.hasBlockedInventory() || chr.getMap() == null || chr.getEventInstance() != null || c.getChannelServer() == null) {
            c.getSession().write(CField.serverBlocked(2));
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (ServerConstants.BlockCS == true) {
            chr.dropMessage(1, "The Cash Shop has been temporarily disabled due to the amount of bugged players.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (chr.inJQ()) {
            chr.dropMessage(1, "You can't exit the AutoJQ unless you type @exit.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (chr.getMapId() == 502) {
            chr.dropMessage(1, "You can't enter the Cash Shop while in Fiesta.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (GameConstants.isJail(chr.getMapId())) {
            chr.dropMessage(1, "You can't enter the Cash Shop while in Jail.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (World.getPendingCharacterSize() >= 10) {
            chr.dropMessage(1, "The server is busy at the moment. Please try again in a minute or less.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        World.ChannelChange_Data(new CharacterTransfer(c.getPlayer()), c.getPlayer().getId(), c.getWorld(), 30);
        final String s = c.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        
        if (c.getPlayer().getMessenger() != null) {
            World.Messenger.silentLeaveMessenger(c.getPlayer().getMessenger().getId(), new MapleMessengerCharacter(c.getPlayer()));
        }
        c.getPlayer().changeRemoval();
        PlayerBuffStorage.addBuffsToStorage(c.getPlayer().getId(), c.getPlayer().getAllBuffs());
        PlayerBuffStorage.addDiseaseToStorage(c.getPlayer().getId(), c.getPlayer().getAllDiseases());
        PlayerBuffStorage.addCooldownsToStorage(c.getPlayer().getId(), c.getPlayer().getCooldowns());
        c.getPlayer().getMap().removePlayer(c.getPlayer());
        c.getChannelServer().removePlayer(c.getPlayer());
        c.getPlayer().saveToDB(false, false);
        c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());
        c.getSession().write(CField.getChannelChange(c, Integer.parseInt(CashShopServer.getIP().split(":")[1])));
        c.setPlayer(null);
        c.setReceiving(false);
    }
        
    public static final void Loggedin(final int playerid, final MapleClient c) {
        MapleCharacter player;
        CharacterTransfer transfer = c.getWorldServer().getPlayerStorage().getPendingCharacter(playerid);
        if (transfer == null) {
            transfer = CashShopServer.getPlayerStorage().getPendingCharacter(playerid);
            if (transfer == null) {
                player = MapleCharacter.loadCharFromDB(playerid, c, true);
            } else {
                player = MapleCharacter.ReconstructChr(transfer, c, true);
                player.setInCS(true);
            }
        } else {
            player = MapleCharacter.ReconstructChr(transfer, c, true);
        }
        
        c.setPlayer(player);
        c.setAccID(player.getAccountID());

        if (!c.CheckIPAddress()) { // Remote hack
            System.out.println(c.getAccountName() + " BUG?3");
            c.getSession().close(true);
            return;
        }

        final int state = c.getLoginState();
        boolean allowLogin = false;
        final ChannelServer channelServer = c.getChannelServer();

        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL || state == MapleClient.LOGIN_NOTLOGGEDIN) {
            allowLogin = !World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()));
        }

        if (state == MapleClient.LOGIN_SERVER_TRANSITION) {
            for (String charName : c.loadCharacterNames(c.getWorld())) {
                if (World.isConnected(charName)) {
                    System.err.print(charName + " has been unstuck from the login server.");
                    for (ChannelServer chan : LoginServer.getInstance().getWorld(c.getWorld()).getChannels()) {
                        for (MapleCharacter chr : chan.getPlayerStorage().getAllCharacters()) {
                            if (chr.getAccountID() == player.getAccountID()) {
                                chr.saveToDB(true, false);
                                chr.getClient().getSession().close(true);
                                chr.getMap().removePlayer(chr);
                            }
                        }
                    }
                    c.getSession().write(CWvsContext.serverNotice(1, "You were stuck.\r\n Please relog."));
                    break;
                }
            }
        }
        if (World.isCSConnected(c.loadCharacterIds(c.getWorld()))) {
            // this won't happen anymore actually, i managed to fix the cash shop glitch when doing multi-worlds.
            c.getSession().write(CWvsContext.serverNotice(1, "Uh-oh!\r\nLooks like you were stuck in the Cash shop!\r\n\r\nPlease exit back to the login as your character has been fixed."));
            MapleCharacter victim = CashShopServer.getPlayerStorage().getCharacterByName(player.getName());
            CashShopServer.getPlayerStorage().deregisterPlayer(victim);
            CashShopServer.getPlayerStorage().deregisterPendingPlayer(victim.getId());
            CashShopServer.getPlayerStorage().getCharacterById(victim.getId()).getClient().getSession().close();
            allowLogin = false;
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close(true);
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        LoginServer.getInstance().getWorld(player.getWorld()).addPlayer(player);
        player.giveCoolDowns(PlayerBuffStorage.getCooldownsFromStorage(player.getId()));
        player.silentGiveBuffs(PlayerBuffStorage.getBuffsFromStorage(player.getId()));
        player.giveSilentDebuff(PlayerBuffStorage.getDiseaseFromStorage(player.getId()));
        c.getSession().write(CField.getCharInfo(player));
        player.getMap().addPlayer(player);
        c.getSession().write(MTSCSPacket.enableCSUse());
        c.getSession().write(CWvsContext.temporaryStats_Reset()); //?
        
        if (player.inCS()) {
            player.setInCS(false); // exit them from CS enabling
        } else {
            c.getSession().write(CWvsContext.yellowChat("[Welcome] Welcome to " + ServerConstants.SERVER_NAME + " v117.2!"));
            c.getSession().write(CField.sendHint("" + ServerConstants.WELCOME_MESSAGE + "", 350, 5));
        }
        // GM Hide is a skill now, and auto-applies super hide. 
        if (player.isGM()) {
            if (player.isGod()) {
                player.setMegaHide(true); // on 
            }
            //SkillFactory.getSkill(9101004).getEffect(1).applyTo(c.getPlayer());
            player.toggleHide(false, !player.isHidden());
        }

        try {
            // Start of buddylist
            final int buddyIds[] = player.getBuddylist().getBuddyIds();
            World.Buddy.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
            if (player.getParty() != null) {
                final MapleParty party = player.getParty();
                World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));

                if (party != null && party.getExpeditionId() > 0) {
                    final MapleExpedition me = World.Party.getExped(party.getExpeditionId());
                    if (me != null) {
                        c.getSession().write(ExpeditionPacket.expeditionStatus(me, false));
                    }
                }
            }
            final CharacterIdChannelPair[] onlineBuddies = World.Find.multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                player.getBuddylist().get(onlineBuddy.getCharacterId()).setChannel(onlineBuddy.getChannel());
            }
            c.getSession().write(BuddylistPacket.updateBuddylist(player.getBuddylist().getBuddies()));

            // Start of Messenger
            final MapleMessenger messenger = player.getMessenger();
            if (messenger != null) {
                World.Messenger.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()));
                World.Messenger.updateMessenger(messenger.getId(), c.getPlayer().getName(), c.getWorld(), c.getChannel());
            }

            // Start of Guild and alliance
            if (player.getGuildId() > 0) {
                World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.getSession().write(GuildPacket.showGuildInfo(player));
                final MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                if (gs != null) {
                    final List<byte[]> packetList = World.Alliance.getAllianceInfo(gs.getAllianceId(), true);
                    if (packetList != null) {
                        for (byte[] pack : packetList) {
                            if (pack != null) {
                                c.getSession().write(pack);
                            }
                        }
                    }
                } else { //guild not found, change guild id
                    player.setGuildId(0);
                    player.setGuildRank((byte) 5);
                    player.setAllianceRank((byte) 5);
                    player.saveGuildStatus();
                }
            }

            if (player.getFamilyId() > 0) {
                World.Family.setFamilyMemberOnline(player.getMFC(), true, c.getChannel());
            }
            c.getSession().write(FamilyPacket.getFamilyData());
            c.getSession().write(FamilyPacket.getFamilyInfo(player));
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.Login_Error, e);
        }
        player.getClient().getSession().write(CWvsContext.serverMessage(channelServer.getServerMessage()));
        player.sendMacros();
        player.showNote();
        player.sendImp();
        player.updatePartyMemberHP();
        player.startFairySchedule(false);
        c.getSession().write(CField.getKeymap(player.getKeyLayout()));
        c.getSession().write(LoginPacket.enableReport());
        player.updatePetAuto();
        player.expirationTask(true, player == null);
        if (player.getJob() == 132) { // DARKKNIGHT
            player.checkBerserk();
        }
        player.spawnSavedPets();
        if (player.getStat().equippedSummon > 0) {
            SkillFactory.getSkill(player.getStat().equippedSummon).getEffect(1).applyTo(player);
        }
        player.loadQuests(c);
        c.getSession().write(CWvsContext.getFamiliarInfo(player));
        if (World.getShutdown()) {
            player.getClient().getSession().write(CWvsContext.getMidMsg("The server is preparing to shutdown, so don't get too comfortable!", true, 1));
        }
        if (player.getMap().getId() == MapConstants.STARTER_MAP) {
            World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.yellowChat("[" + c.getPlayer().getName() + "] Just Joined " + ServerConstants.SERVER_NAME + " - The Ultimate MapleStory Private Server!"));
            player.dropMessage(6, "Welcome to " + ServerConstants.SERVER_NAME + ", Player #" + player.getId() + "!");
        }
        if (player.haveItem(ServerConstants.Currency, 1000, false, true) && !player.isDonator() && player.getReborns() < 50 && !player.isSuperDonor() && !player.isGM()) {
            player.sendGMMessage(6, "[GM Notification]: " + player.getName() + " has more then 1000 Munny, and less then 50 rebirths.");  
        }
        if (player.haveItem(ServerConstants.Currency, 50000, false, true) && !player.isGM()) {
            player.sendGMMessage(6, "[GM Notification]: " + player.getName() + " has over 50,000 Munny. Check to see if they're hacking!");  
        }
        player.saveToDB(false, false);
        //final List<Pair<Integer, String>> ii = new LinkedList<>();
        //ii.add(new Pair<>(10000, "Pio"));
        //player.getClient().getSession().write(CField.NPCPacket.setNPCScriptable(ii));
    }

    public static void ChangeChannel(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr, final boolean room) {
        if (chr == null || chr.hasBlockedInventory() || chr.getEventInstance() != null || chr.getMap() == null || chr.isInBlockedMap() || FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())) {
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (World.getPendingCharacterSize() >= 10) {
            chr.dropMessage(1, "The server is busy at the moment. Please try again in a less than a minute.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        final int chc = slea.readByte() + 1;
        int mapid = 0;
        if (room) {
            mapid = slea.readInt();
        }
        slea.readInt();
        if (!World.isChannelAvailable(c.getWorld(), chc)) {
            chr.dropMessage(1, "The channel is full at the moment.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (room && (mapid < 910000001 || mapid > 910000022)) {
            chr.dropMessage(1, "The channel is full at the moment.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (chr.inJQ()) {
            chr.dropMessage(1, "You can't CC during a Jump Quest, try @exit.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (GameConstants.isJail(chr.getMapId())) {
            chr.dropMessage(1, "You can't Change Channels in Jail, fgt.");
            c.getSession().write(CWvsContext.enableActions());
            return;
        }
        if (room) {
            if (chr.getMapId() == mapid) {
                if (c.getChannel() == chc) {
                    chr.dropMessage(1, "You are already in " + chr.getMap().getMapName());
                    c.getSession().write(CWvsContext.enableActions());
                } else { // diff channel
                    chr.changeChannel(chc);
                }
            } else { // diff map
                if (c.getChannel() != chc) {
                    chr.changeChannel(chc);
                }
                final MapleMap warpz = ChannelServer.getInstance(c.getWorld(), c.getChannel()).getMapFactory().getMap(mapid);
                if (warpz != null) {
                    chr.changeMap(warpz, warpz.getPortal("out00"));
                } else {
                    chr.dropMessage(1, "The channel is full at the moment.");
                    c.getSession().write(CWvsContext.enableActions());
                }
            }
        } else {
            chr.changeChannel(chc);
        }
    }
}
