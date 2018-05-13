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

import client.MapleClient;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.inventory.Item;
import client.messages.CommandProcessor;
import client.messages.commands.AbstractCommandScriptManager;
import client.messages.commands.DonatorCommand;
import client.messages.commands.GMCommand;
import client.messages.commands.InternCommand;
import client.messages.commands.PlayerCommand;
import client.messages.commands.SuperDonatorCommand;
import constants.ServerConstants;
import constants.ServerConstants.CommandType;
import constants.ServerConstants.PlayerGMRank;
import handling.channel.ChannelServer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.World;
import java.util.LinkedList;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.WordFilter;
import tools.StringUtil;
import tools.packet.CField;
import tools.data.LittleEndianAccessor;
import tools.packet.CWvsContext;

public class ChatHandler {

    public static final void GeneralChat(String text, final byte unk, final MapleClient c, final MapleCharacter chr) {
        text = WordFilter.illegalArrayCheck(text, c.getPlayer()); 
        if (c.getPlayer().getLeetness()) {
            String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
            String leet = "48(d3f9h1jk1mn0PQR57uvwxyz@6cD3F9hiJk|Mn0pqr$+uvWXy2";
        for (int i = 0; i < 52; i++) {
            text = text.replace(normal.charAt(i), leet.charAt(i));
        }
            text = text.replaceAll("y0u", "j00");
        }
        if ((StringUtil.countCharacters(text, '@') > 5) || (StringUtil.countCharacters(text, '!') > 4) || (StringUtil.countCharacters(text, '#') > 6) || (StringUtil.countCharacters(text, '%') > 6) || (StringUtil.countCharacters(text, '&') > 6) || (StringUtil.countCharacters(text, '~') > 6)){
            text = "Development FTW! Trolls Rule!!!!!!!!1111111one111111\r\n-Eric";
        }
        if (!c.getPlayer().isAdmin() && c.getPlayer().getMuteLevel() == 1) { // we don't mute admins :(
            c.getPlayer().dropMessage(5, "You are muted, therefore you cannot talk.");
            return;
        }
        if (c.getPlayer().getWatcher() != null) {
            c.getPlayer().getWatcher().dropMessage(5, "[" + c.getPlayer().getName() + "] [Chat Type: All] : " + text);
        }
        if (text.contains("Eric sucks") || text.contains("eric sucks"))
            text = "I eat dogshit.";
        int tryingToTalk = 0;
        if (text.startsWith("@@") || text.startsWith("!!")) {
            tryingToTalk = 1; // they tried
            //chr.getMap().broadcastMessage(CField.getChatText(chr.getId(), text, c.getPlayer().isSuperGM(), unk), c.getPlayer().getTruePosition());
	}
            boolean commandWorked = false;
            // @ for players, and ! allowed if isGM(), allow SKIP if a player goes "!!" or "@@"++ for fun. :)
            if (tryingToTalk == 1) {
                tryingToTalk = 2; // looks like they're being silly, we'll skip using commands to allow them to say !!!111 or @@@
            } else if ((text.startsWith("@")) || (text.startsWith("!") && c.getPlayer().isGM())) { // this way players can use ! and not access commands. :)
                boolean allowed = true;
                String[] args = text.split(" ");
                int domain;
                String commandType;
                if (args[0].charAt(0) == '@') { // player
                    domain = PlayerGMRank.NORMAL.getLevel();
                    commandType = "player";
                } else if (args[0].charAt(0) == '!') { // donor and above = ! 
                    domain = PlayerGMRank.DONATOR.getLevel();
                    commandType = "donor";
                    if (c.getPlayer().getGMLevel() < 1) { // this isn't fucking points omfg.
                        allowed = false;
                    }
                } else if (args[0].charAt(0) == '!') {
                    domain = PlayerGMRank.SUPERDONATOR.getLevel();
                    commandType = "sdonor";
                    if (c.getPlayer().getGMLevel() < 2) { // ./rage -.-
                        allowed = false;
                    }
                } else if (args[0].charAt(0) == '!') { // intern = 3, right? o-o
                    domain = PlayerGMRank.INTERN.getLevel();
                    commandType = "intern";
                    if (c.getPlayer().getGMLevel() < 3) {
                        allowed = false;
                    }
                } else if (args[0].charAt(0) == '!' && c.getPlayer().getGMLevel() >= 4) { // GM = 4? i think so anyways lol
                    domain = PlayerGMRank.GM.getLevel();
                    commandType = "gm";
                    if (c.getPlayer().getGMLevel() < 4) {
                        allowed = false;
                    }
                } else { //impossible but just in case
                    return;
                }
                //String[] commandTypes = {"player", "donor", "sdonor", "intern", "gm"};
                //String prefix = "@#$%!";
                //int domain = prefix.indexOf(args[0].substring(0, 1));
                String name = args[0].replace(args[0].substring(0, 1), "");
                //String commandType = commandTypes[domain];
                if (!chr.hasGmLevel(domain)) {
                    //chr.showMessage("You do not have the privileges to use that command.");
                    //return;
                    allowed = false;
                }
                if (allowed) {
                    c.getPlayer().setCommandArgs(args);
                    Invocable iv = AbstractCommandScriptManager.getInvocableCommand(commandType, name, c);
                    final ScriptEngine scriptengine = (ScriptEngine) iv;
                    final AbstractCommandScriptManager acm = new AbstractCommandScriptManager();
                    try {
                        if (iv != null) {
                            acm.putCms(c, acm);
                            scriptengine.put("c", c);
                            scriptengine.put("acm", acm);
                            scriptengine.put("args", args);
                            iv.invokeFunction("start");
                            commandWorked = true;
                        } else {
                            commandWorked = false;
                            iv = AbstractCommandScriptManager.getInvocableCommand(commandType, "nocommand", c); //safe disposal

                        }
                    } catch (final Exception e) {
                        System.err.println("Error executing Command script, Command : " + name + "." + e);
                    } finally {
                        acm.dispose(c, commandType, name);
                    }
                }
            }
            if (tryingToTalk > 0)
                commandWorked = false;
            if (!commandWorked) {
                switch (text.charAt(0)) {
                    case '@':
                        PlayerCommand.executePlayerCommands(c, text.split(" "));
                        commandWorked = true;
                        break;
                    case '!': // should make Donor+ all ! o-o
                        DonatorCommand.executeDonatorCommand(c, text.split(" "));
                        commandWorked = true;
                        break;
                   /* case '$':
                        SuperDonatorCommand.executeSuperDonatorCommand(c, text.split(" "));
                        commandWorked = true;
                        break;
                    case '%':
                        InternCommand.executeInternCommand(c, text.split(" "));
                        commandWorked = true;
                        break;
                    case '!':
                        GMCommand.executeGMCommand(c, text.split(" "));
                        commandWorked = true;
                        break;*/
                    default:
                        commandWorked = false;
                        break;
                }
            }
            if (tryingToTalk > 0)
                commandWorked = false;
            if (!commandWorked || tryingToTalk > 0) {
                if (text.length() > 0 && chr != null && chr.getMap() != null) {
                    if (!chr.isIntern() && text.length() >= 80) {
                        return;
                    }
                       if (chr.getCanTalk() || chr.isStaff()) {
                            //Note: This patch is needed to prevent chat packet from being broadcast to people who might be packet sniffing.
                            if (chr.isHidden()) {
                                if (chr.isIntern() && !chr.isSuperGM() && unk == 0) {
                                    chr.getMap().broadcastGMMessage(chr, CField.getChatText(chr.getId(), text, false, (byte) 1), true);
                                    if (unk == 0) {
                                        chr.getMap().broadcastGMMessage(chr, CWvsContext.serverNotice(2, chr.getName() + " : " + text), true);
                                    }
                                } else {
                                    chr.getMap().broadcastGMMessage(chr, CField.getChatText(chr.getId(), text, c.getPlayer().isSuperGM(), unk), true);
                                }
                            } else {
                                if ((c.getPlayer().getGMLevel() == 1 && c.getPlayer().getGMText() == 0) && unk == 0) {
                                    c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                  if (unk == 0) {
                                    chr.getMap().broadcastMessage(CWvsContext.yellowChat("[Donor]" + c.getPlayer().getName() + ": " + text));
                                }
                       } else if (chr.getGMText() > 1) {
                            switch (c.getPlayer().getGMText()) {
                                case 1:
                                    c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                if (unk == 0) {
                                    c.getPlayer().getMap().broadcastMessage(CWvsContext.getBuddy(c.getPlayer().getName(), text));
                                }
                                    break;
                                case 2:
                                case 3:
                                case 4:
                                    c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                if (unk == 0) {
                                    c.getPlayer().getMap().broadcastMessage(CWvsContext.multiChat(c.getPlayer().getName(), text, c.getPlayer().getGMText() - 1));
                                }
                                    break;
                                case 5:
                                case 6:
                                    c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                if (unk == 0) {
                                    c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(c.getPlayer().getGMText(), c.getPlayer().getName() + " Says : " + text));
                                }
                                    break;
                                case 7: // gm text 
                                     chr.getMap().broadcastMessage(CField.getChatText(chr.getId(), text, true, (byte) 1));
                                         if (unk == 0) {
                                              chr.getMap().broadcastMessage(CField.getChatText(chr.getId(), text, c.getPlayer().isSuperGM(), unk), c.getPlayer().getTruePosition());
                                          }
                                       // c.getPlayer().dropMessage(5, "Please use !gmtext normal. ");
                                        break;
                                case 8:
                                    c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                if (unk == 0) {
                                    c.getPlayer().getMap().broadcastMessage(CWvsContext.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                                }
                                    break;
                                case 9:
                                        if (c.getPlayer().getAccountID() == ServerConstants.ERIC_ACC_ID) {
                                            c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        if (unk == 0) {
                                            c.getPlayer().getMap().broadcastMessage(CWvsContext.yellowChat(c.getPlayer().getName() + " Tweets: " + text));   
                                          }
                                        } else {
                                            c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        if (unk == 0) {
                                               c.getPlayer().getMap().broadcastMessage(CWvsContext.yellowChat(c.getPlayer().getName() + " Says : " + text));
                                            }
                                        }
                                        break;
                                case 10: // normal text 
                                     chr.getMap().broadcastMessage(CField.getChatText(chr.getId(), text, true, (byte) 1));
                                         if (unk == 0) {
                                              chr.getMap().broadcastMessage(CField.getChatText(chr.getId(), text, false, unk), c.getPlayer().getTruePosition());
                                          }
                                        //c.getPlayer().dropMessage(5, "Please use !gmtext normal. ");
                                        break;
                                case 90: // only a chat bubble, no chat text broadcasts to map.
                                        c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        break;
                                case 94:
                                        c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        if (unk == 0) {
                                            Item test = MapleItemInformationProvider.getEquipById(1302000); // test for now :/
                                            byte rareness = 0; // this isn't needed because it doesn't use this in the packet
                                            c.getPlayer().getMap().broadcastMessage(CWvsContext.getGachaponMega(c.getPlayer().getName(), text, test, rareness, false, "Test"));
                                        }
                                        break;
                                case 95: // item mega
                                        c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        if (unk == 0) {
                                            c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(9, c.getChannel(), c.getPlayer().getName() + " : " + text, Math.random() > 0.5));
                                        }
                                        break;
                                case 96: // skull mega
                                        c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        if (unk == 0) {
                                            c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(22, c.getChannel(), c.getPlayer().getName() + " : " + text, Math.random() > 0.5));
                                        }
                                        break;
                                case 97: // orange text
                                        c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        if (unk == 0) {
                                            c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(14, c.getPlayer().getName() + " : " + text));
                                        }
                                        break;
                                case 98: // cake mega
                                        c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        if (unk == 0) {
                                            c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(25, c.getChannel(), c.getPlayer().getName() + " : " + text, Math.random() > 0.5));
                                        }
                                        break;
                                case 99: // pie mega
                                        c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        if (unk == 0) {
                                            c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(26, c.getChannel(), c.getPlayer().getName() + " : " + text, Math.random() > 0.5));
                                        }
                                        break;
                                case 100: //Megaphone
                                        c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                    if (unk == 0) {
                                        c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(2, c.getChannel(), c.getPlayer().getName() + " : " + text, Math.random() > 0.5));
                                    }
                                        break;
                                case 101: //Avatar Mega LOL
                                        String[] lines = {"", "", "", ""};
                                        if(text.length() > 30) {
                                            lines[0] = text.substring(0, 10);
                                            lines[1] = text.substring(10, 20);
                                            lines[2] = text.substring(20, 30);
                                            lines[3] = text.substring(30);
                                        } else if(text.length() > 20) {
                                            lines[0] = text.substring(0, 10);
                                            lines[1] = text.substring(10, 20);
                                            lines[2] = text.substring(20);
                                        } else if(text.length() > 10) {
                                            lines[0] = text.substring(0, 10);
                                            lines[1] = text.substring(10);
                                        } else if(text.length() <= 10) {
                                            lines[0] = text;
                                        }
                                        LinkedList<String> list = new LinkedList<String>();
                                        list.add(lines[0]);
                                        list.add(lines[1]);
                                        list.add(lines[2]);
                                        list.add(lines[3]);
                                        c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                    if (unk == 0) {
                                        int random = MapleCharacter.rand(0, 6);
                                        int smega;
                                        switch (random) {
                                            case 0:
                                                smega = 5390000;
                                                break;
                                            case 1:
                                                smega = 5390001;
                                                break;
                                            case 2:
                                                smega = 5390002;
                                                break;
                                            case 3:
                                                smega = 5390005;
                                                break;
                                            case 4:
                                                smega = 5390006;
                                                break;
                                            case 5:
                                                smega = 5390007;
                                                break;
                                            case 6:
                                                smega = 5390008;
                                                break;
                                            default: // no need for 0 lol
                                                smega = 5390000;
                                                break;
                                        }
                                            c.getPlayer().getMap().broadcastMessage(CWvsContext.getAvatarMega(c.getPlayer(), c.getChannel(), smega, list, Math.random() > 0.5));
                                    }
                                        break;
                                case 102: //Spouse chat (Y)
                                        c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                    if (unk == 0) {
                                        c.getPlayer().getMap().broadcastMessage(CWvsContext.toSpouse(c.getPlayer().getName(), text, 4));
                                    }
                                        break;
                                case 103: //Super Megaphone
                                        if (c.getPlayer().getAccountID() == ServerConstants.ERIC_ACC_ID || c.getPlayer().getId() == 1731) { // kevin
                                            c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        if (unk == 0) {
                                                c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(3, 99, "<Master Of Love> " + c.getPlayer().getName() + " : " + text, Math.random() > 0.5));  
                                            }
                                        } else {
                                            c.getPlayer().getMap().broadcastMessage(CField.getChatText(c.getPlayer().getId(), text, false, 1));
                                        if (unk == 0) {
                                                c.getPlayer().getMap().broadcastMessage(CWvsContext.serverNotice(3, c.getChannel(), c.getPlayer().getName() + " : " + text, Math.random() > 0.5));
                                            }
                                        }
                                            break;
                                        }
                            } else if (chr.isIntern() && !chr.isSuperGM() && unk == 0) {
                                    chr.getMap().broadcastMessage(CField.getChatText(chr.getId(), text, false, (byte) 1), c.getPlayer().getTruePosition());
                                    if (unk == 0) {
                                        chr.getMap().broadcastMessage(CWvsContext.serverNotice(2, chr.getName() + " : " + text), c.getPlayer().getTruePosition());
                                    }
                                } else {
                                    chr.getMap().broadcastMessage(CField.getChatText(chr.getId(), text, c.getPlayer().isSuperGM(), unk), c.getPlayer().getTruePosition());
                                }
                            }
                        } else {
                            c.getSession().write(CWvsContext.serverNotice(6, "You have been muted and are therefore unable to talk."));
                        }
                    }
               }
    }
    
    public static final String getChatType(int type) {
            switch (type) {
                case 0:
                    return "Buddy";
                case 1:
                    return "Party";
                case 2:
                    return "Guild";
                case 3:
                    return "Alliance";
                case 4:
                    return "Expedition";
            }
            return "Unknown";
    }
    
    public static final void Spouse_Chat(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        /* 
        02 00 // size of the spouse's name
        3C 33 // spouses name
        05 00 // length of message
        61 62 63 64 65 // message 
        */
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return;
        }
        final String spouse = slea.readMapleAsciiString();
        final String message = slea.readMapleAsciiString();
        final int channel = World.Find.findChannel(spouse);
        final int world = World.Find.findWorld(spouse);
        if (c.getPlayer().getMarriageId() == 0 || !c.getPlayer().getPartner().equalsIgnoreCase(spouse)) {
            c.getPlayer().dropMessage(5, "You are not married or your spouse is offline.");
            c.announce(CWvsContext.enableActions());
            return;
        }
        if (channel > 0) {
            MapleCharacter spouseChar = ChannelServer.getInstance(world, channel).getPlayerStorage().getCharacterByName(spouse);
            if (spouseChar == null) {
                c.getPlayer().dropMessage(5, "You are not married or your spouse is offline.");
                c.announce(CWvsContext.enableActions());
                return;
            }
            // TODO: code spouse-chat watch system: if (c.getPlayer().getWatcher() != null) { return; }
            spouseChar.getClient().getSession().write(CWvsContext.toSpouse(c.getPlayer().getName(), message, 5));
            c.getSession().write(CWvsContext.toSpouse(c.getPlayer().getName(), message, 5));
        } else {
            c.getPlayer().dropMessage(5, "You are not married or your spouse is offline.");
        }
    }

    public static final void Others(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int type = slea.readByte();
        final byte numRecipients = slea.readByte();
        if (numRecipients <= 0) {
            return;
        }
        int recipients[] = new int[numRecipients];

        for (byte i = 0; i < numRecipients; i++) {
            recipients[i] = slea.readInt();
        }
        final String chattext = slea.readMapleAsciiString();
        if (chr == null || !chr.getCanTalk()) {
            c.getSession().write(CWvsContext.serverNotice(6, "You have been muted and are therefore unable to talk."));
            return;
        }
        if (c.getPlayer().getMuteLevel() == 1) {
            c.getPlayer().dropMessage(5, "You have been muted, therefore you can't talk.");
            return;
        }
        
        if (c.getPlayer().getWatcher() != null) {
            c.getPlayer().getWatcher().dropMessage(5, "[" + c.getPlayer().getName() + " - Chat: " + getChatType(type) + "] : " + chattext);
        }
        
        if (chattext.length() <= 0 || CommandProcessor.processCommand(c, chattext, CommandType.NORMAL)) {
            return;
        }
        switch (type) {
            case 0:
                World.Buddy.buddyChat(recipients, chr.getId(), chr.getName(), chattext);
                break;
            case 1:
                if (chr.getParty() == null) {
                    break;
                }
                World.Party.partyChat(chr.getParty().getId(), chattext, chr.getName());
                break;
            case 2:
                if (chr.getGuildId() <= 0) {
                    break;
                }
                World.Guild.guildChat(chr.getGuildId(), chr.getName(), chr.getId(), chattext);
                break;
            case 3:
                if (chr.getGuildId() <= 0) {
                    break;
                }
                World.Alliance.allianceChat(chr.getGuildId(), chr.getName(), chr.getId(), chattext);
                break;
            case 4:
                if (chr.getParty() == null || chr.getParty().getExpeditionId() <= 0) {
                    break;
                }
                World.Party.expedChat(chr.getParty().getExpeditionId(), chattext, chr.getName());
                break;
        }
    }

    public static final void Messenger(final LittleEndianAccessor slea, final MapleClient c) {
		String input;
        MapleMessenger messenger = c.getPlayer().getMessenger();

        switch (slea.readByte()) {
            case 0x00: // open
                if (messenger == null) {
                    int messengerid = slea.readInt();
                    if (messengerid == 0) { // create
                        c.getPlayer().setMessenger(World.Messenger.createMessenger(new MapleMessengerCharacter(c.getPlayer())));
                    } else { // join
                        messenger = World.Messenger.getMessenger(messengerid);
                        if (messenger != null) {
                            final int position = messenger.getLowestPosition();
                            if (position > -1 && position < 4) {
                                c.getPlayer().setMessenger(messenger);
                                World.Messenger.joinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()), c.getPlayer().getName(), c.getPlayer().getWorld(), c.getChannel());
                            }
                        }
                    }
                }
                break;
            case 0x02: // exit
                if (messenger != null) {
                    final MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(c.getPlayer());
                    World.Messenger.leaveMessenger(messenger.getId(), messengerplayer);
                    c.getPlayer().setMessenger(null);
                }
                break;
            case 0x03: // invite

                if (messenger != null) {
                    final int position = messenger.getLowestPosition();
                    if (position <= -1 || position >= 4) {
                        return;
                    }
                    input = slea.readMapleAsciiString();
                    final MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(input);

                    if (target != null) {
                        if (target.getMessenger() == null) {
                            if (!target.isIntern() || c.getPlayer().isIntern()) {
                                c.getSession().write(CField.messengerNote(input, 4, 1));
                                target.getClient().getSession().write(CField.messengerInvite(c.getPlayer().getName(), messenger.getId()));
                            } else {
                                c.getSession().write(CField.messengerNote(input, 4, 0));
                            }
                        } else {
                            c.getSession().write(CField.messengerChat(c.getPlayer().getName(), " : " + target.getName() + " is already using Maple Messenger."));
                        }
                    } else {
                        if (World.isConnected(input)) {
                            World.Messenger.messengerInvite(c.getPlayer().getName(), messenger.getId(), input, c.getPlayer().getWorld(), c.getChannel(), c.getPlayer().isIntern());
                        } else {
                            c.getSession().write(CField.messengerNote(input, 4, 0));
                        }
                    }
                }
                break;
            case 0x05: // decline
                final String targeted = slea.readMapleAsciiString();
                final MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(targeted);
                if (target != null) { // This channel
                    if (target.getMessenger() != null) {
                        target.getClient().getSession().write(CField.messengerNote(c.getPlayer().getName(), 5, 0));
                    }
                } else { // Other channel
                    if (!c.getPlayer().isIntern()) {
                        World.Messenger.declineChat(targeted, c.getPlayer().getName());
                    }
                }
                break;
            case 0x06: // message
                if (messenger != null) {
                    final String charname = slea.readMapleAsciiString();
                    final String text = slea.readMapleAsciiString();
		    final String chattext = charname + "" + text;
                    if (c.getPlayer().getWatcher() != null) {
                        if (text.equals("0") || text.equals("1")) {
                            return;
                        } else {
                            c.getPlayer().getWatcher().dropMessage(5, "[" + c.getPlayer().getName() + " - Chat: Messenger]" + text);
                        }
                    }
                    World.Messenger.messengerChat(messenger.getId(), charname, text, c.getPlayer().getName());
                  
                }
                break;
        }
    }

    public static final void Whisper_Find(final LittleEndianAccessor slea, final MapleClient c) {
        final byte mode = slea.readByte();
        slea.readInt(); //ticks
        switch (mode) {
            case 68: //buddy
            case 5: { // Find

                final String recipient = slea.readMapleAsciiString();
                MapleCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (player != null) {
                    if (!player.isIntern() || c.getPlayer().isIntern() && player.isIntern()) {

                        c.getSession().write(CField.getFindReplyWithMap(player.getName(), player.getMap().getId(), mode == 68));
                    } else {
                        c.getSession().write(CField.getWhisperReply(recipient, (byte) 0));
                    }
                } else { // Not found
                    int ch = World.Find.findChannel(recipient);
                    int wl = World.Find.findWorld(recipient);
                    if (ch > 0) {
                        player = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(recipient);
                        if (player == null) {
                            break;
                        }
                        if (player != null) {
                            if (!player.isIntern() || (c.getPlayer().isIntern() && player.isIntern())) {
                                c.getSession().write(CField.getFindReply(recipient, (byte) ch, mode == 68));
                            } else {
                                c.getSession().write(CField.getWhisperReply(recipient, (byte) 0));
                            }
                            return;
                        }
                    }
                    if (ch == -10) {
                        c.getSession().write(CField.getFindReplyWithCS(recipient, mode == 68));
                    } else if (ch == -20) {
                        c.getPlayer().dropMessage(5, "'" + recipient + "' is at the MTS which doesnt exist so let's ban him/her."); //idfc
                    } else {
                        c.getSession().write(CField.getWhisperReply(recipient, (byte) 0));
                    }
                }
                break;
            }
            case 6: { // Whisper
                if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
                    return;
                }
                final String recipient = slea.readMapleAsciiString();
                final String text = slea.readMapleAsciiString();
                final int ch = World.Find.findChannel(recipient);
                final int wl = World.Find.findWorld(recipient);
                if (ch > 0) {
                    MapleCharacter player = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(recipient);
                    if (player == null) {
                        break;
                    }
                    if (c.getPlayer().getWatcher() != null) {
                       c.getPlayer().getWatcher().dropMessage(5, "[" + c.getPlayer().getName() + "] Whispered to [" + recipient + "] : " + text);
                    }
                    player.getClient().getSession().write(CField.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                    if (!c.getPlayer().isIntern() && player.isIntern()) {
                        c.getSession().write(CField.getWhisperReply(recipient, (byte) 0));
                    } else {
                        c.getSession().write(CField.getWhisperReply(recipient, (byte) 1));
                    }
                } else {
                    c.getSession().write(CField.getWhisperReply(recipient, (byte) 0));
                }
            }
            break;
        }
    }
}
