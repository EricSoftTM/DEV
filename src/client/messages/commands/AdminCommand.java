package client.messages.commands;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleDisease;
import client.MapleStat;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import server.*;
import server.Timer.EventTimer;
import server.events.InsultBot;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleReactor;
import tools.CPUSampler;
import tools.HexTool;
import tools.StringUtil;
import tools.packet.CField;
import tools.packet.CField.NPCPacket;
import tools.packet.CWvsContext;
import tools.packet.MTSCSPacket;
import tools.packet.MobPacket;

/**
 *
 * @author Eric
 */
public class AdminCommand {
    static boolean superBaal = false;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.ADMIN;
    }
    
    public static boolean executeAdminCommand(MapleClient c, String[] splitted) {
        MapleCharacter player = c.getPlayer();
        final MapleCharacter playerf = player;
        MapleCharacter victim; 
        ChannelServer cserv = c.getChannelServer();
        World world = c.getWorldServer();
        if (c.getPlayer().getGMLevel() >= PlayerGMRank.ADMIN.getLevel()) {
            switch (splitted[0].substring(1).toLowerCase()) {
                case "gc":
                    System.gc();
                    player.dropMessage("Free Memory = " + Runtime.getRuntime().freeMemory() + ".");
                    return true;
                case "ajob":
                    player.changeJob(Integer.parseInt(splitted[1]));
                    return true;
                case "news":
                  String title = (splitted[1]);
                  String msg = InternCommand.joinStringFrom(splitted, 2);
                try {
                    java.sql.Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("INSERT INTO wiz_news ( title, msg, date ) VALUES ( ?, ?, ? )");
                    ps.setString(1, title);
                    ps.setString(2, msg);
                    ps.setString(3, now("dd/MM/yy"));
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException e) {
                    player.dropMessage("[Error 46] : Unable to save to the database/wiz_news"); // lol 46 
                }
                    return true;
                case "shammos":
                    c.getSession().write(MobPacket.talkMonster(Integer.parseInt(splitted[1]), 2, 5000, "You fools!"));
                    return true;
                case "trip":
                    victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    if(!victim.isFiction())victim.worldTrip();
                    return true;
                case "superhide": // (angel)
                    if (player.isMegaHidden() == false) {
                        player.setMegaHide(true);
                        player.dropMessage(5, "Super Hide \\Enabled\\.");
                    } else if (player.isMegaHidden() == true) {
                        player.setMegaHide(false);
                        player.dropMessage(5, "Super Hide \\Disabled\\.");
                    } else
                        player.dropMessage(5, "Error with Super Hide.");
                    return true;
                case "crash":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                        victim.getClient().getSession().write(HexTool.getByteArrayFromHexString("1A 00")); //give_buff with no data :D
                        return true;
                    } else {
                        c.getPlayer().dropMessage(6, "The victim does not exist.");
                        return true;
                    }
                case "blind":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    int seconds = Integer.parseInt(splitted[2]);
                    if (seconds > 1) {
                        for (int i = 0; i < seconds; i++) { 
                            // should timer this or something, the skill itself is more then 1 seconds
                            victim.giveDebuff(MapleDisease.BLIND, MobSkillFactory.getMobSkill(136, seconds));
                        }
                    } else {
                        victim.giveDebuff(MapleDisease.BLIND, MobSkillFactory.getMobSkill(136, 1));
                    }
                    return true;
                case "blindmap":
                    seconds = Integer.parseInt(splitted[1]);
                        for (MapleCharacter blind_vics : player.getMap().getCharacters()) {
                    if (!blind_vics.isGM() && seconds > 1) {
                        for (int i = 0; i < seconds / 20; i++) // skill value = 10 seconds or 20 seconds.. 
                            blind_vics.giveDebuff(MapleDisease.BLIND, MobSkillFactory.getMobSkill(136, seconds));
                    } else {
                        if (!blind_vics.isGM())
                            blind_vics.giveDebuff(MapleDisease.BLIND, MobSkillFactory.getMobSkill(136, 1));
                    }
              }
                    return true;
                case "itemvac": 
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
                    return true;
                case "removeskill":
                    int[] skills = {20001142, 20001026, 80001089, 50001026, 1142, 1026, 30001026, 30001142, 20021026, 10001026, 10001142, 20031026, 20011026, 20011142, 30011026};
                        for (int i : skills) {
                            try {
                                Connection con = DatabaseConnection.getConnection();
                                PreparedStatement ps = con.prepareStatement("DELETE FROM skills WHERE skillid = " + i + ";"); // forlooping this eh ;)
                                ps.executeUpdate();
                                ps.close(); 
                            } catch (SQLException e) {
                            }
                        }
                    return true;
                case "warpallhere":
                    for (MapleCharacter chrs : ChannelServer.getInstance(c.getWorld(), c.getChannel()).getPlayerStorage().getAllCharacters()) {
                        if (!chrs.isGM() && chrs != player && chrs.getMapId() != player.getMapId()) {
                            chrs.changeMap(player.getMapId());
                        }
                    }
                    return true;
                    case "watch":
                    victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                    if (splitted.length == 3) {
                        if (victim != null) {
                            victim.clearWatcher();
                            player.dropMessage(6, "You stopped watching " + victim.getName());
                        } else {
                            player.dropMessage(6, "This player isn't in the same channel as you or logged off");
                            return true;
                        }
                    } else if (splitted.length == 2) {
                        if (splitted[1].equalsIgnoreCase("clear")) {
                            for (MapleCharacter chars : cserv.getPlayerStorage().getAllCharacters()) {
                                if (chars.getWatcher() == player) {
                                    chars.clearWatcher();
                                }
                            }
                            player.dropMessage(6, "You stopped watching everyone in your channel.");
                        } else if (splitted[1].equalsIgnoreCase("all")) {
                            for (MapleCharacter chars : cserv.getPlayerStorage().getAllCharacters()) {
                                if (chars.getWatcher() == null && chars != player) {
                                    chars.setWatcher(player);
                                }
                            }
                            player.dropMessage(6, "You started watching everyone in your channel. Use '!watch clear' to stop.");
                        } else {
                            if (victim != null) {
                                if (victim.getWatcher() == null) {
                                    victim.setWatcher(player);
                                    player.dropMessage(6, "You started watching " + victim.getName());
                                } else {
                                    if (victim.getWatcher().getName() != null) {
                                        player.dropMessage(6, victim.getWatcher().getName() + " is already watching this player. Use !watch " + victim.getName() + " clear so you can watch this player instead");
                                    } else {
                                        player.dropMessage(6, "Someone is already watching this player. Use !watch " + victim.getName() + " clear so you can watch this player instead");
                                        return true;
                                    }
                                }
                            } else {
                                player.dropMessage(6, "This player isn't in the same channel as you or logged off");
                                return true;
                            }
                        }
                    } else {
                        player.dropMessage(6, "Syntax: !watch <ign> / !watch <ign> stop / !watch all / !watch clear");
                        return true;
                    }
                    return true;
                case "clisten":
                    for (MapleCharacter chars : LoginServer.getInstance().getWorld(player.getWorld()).getPlayerStorage().getAllCharacters()) {
                            if (chars.getWatcher() == player) {
                                chars.clearWatcher();
                            }
                        }
                        player.dropMessage(6, "You stopped watching everyone in Development.");
                    return true;
                case "listen":
                case "watchserver":
                    for (MapleCharacter chars : LoginServer.getInstance().getWorld(player.getWorld()).getPlayerStorage().getAllCharacters()) {
                        if (chars.getWatcher() == null && chars != player) {
                             chars.setWatcher(player);
                        }
                     }
                    player.dropMessage(6, "You started watching everyone in Development. Use '!clisten' to stop.");
                    return true;
                case "memory":
                case "ram":
                    if (splitted[0].equalsIgnoreCase("ram")) {
                    player.dropMessage("Free RAM: " + Runtime.getRuntime().freeMemory() + "/" + Runtime.getRuntime().maxMemory());
                    player.dropMessage("Total RAM: " + Runtime.getRuntime().totalMemory());
                    } else {
                    player.dropMessage("Free memory: " + Runtime.getRuntime().freeMemory() + "/" + Runtime.getRuntime().maxMemory());
                    player.dropMessage("Total memory: " + Runtime.getRuntime().totalMemory());
                    }
                return true;
                case "emote":
                    String name = splitted[1];
                    victim = cserv.getPlayerStorage().getCharacterByName(name);
                    int emote = Integer.parseInt(splitted[2]);
                        if (emote > 7) { // for nx?
                            int emoteid = 5159992 + emote;
                        }
                        if (victim != null) {
                            victim.getClient().getSession().write(CField.facialExpression(victim, emote));
                            victim.getMap().broadcastMessage(victim, CField.facialExpression(victim, emote), true);
                            victim.getMap().broadcastMessage(victim, CField.facialExpression(victim, emote), victim.getPosition());
                        } else {
                            player.dropMessage("Player was not found");
                            return true;
                    }
                return true;
                case "pvpstate": // TODO: make this from world to map. :(
                    int state = Integer.parseInt(splitted[1]);
                    String[] states = {"Regular PvP", "Survival PvP", "Guild PvP", "Party PvP", "Racist PvP", "Occ PvP", "Job PvP", "Gender PvP"};
                        if (state >= 0 && state <= 7) {
                            World.setPvpState(state);
                            player.dropMessage(5, "You have changed the Pvp State to " + states[state] + " (State " + state + ").");
                            for (MapleCharacter pplz : player.getMap().getCharacters())
                                pplz.dropMessage(5, player.getName() + " has updated the Pvp State to " + states[state] + ".");
                        } else {
                            player.dropMessage(5, "!pvpstate <1/2/3/4/5/6/7>");
                            return true;
                        }
                    return true;
                case "cface":
                        victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                        int face = Integer.parseInt(splitted[2]);
                        c.getPlayer().getMap().broadcastMessage(c.getPlayer(), CField.facialExpression(victim, face), false);
                        return true; 
                case "fakedmgp":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim.fakeDamage()) {
                        victim.setFakeDamage(false);
                        victim.dropMessage(5, "You can't deal max damage in PvP.");
                        player.dropMessage(5, victim.getName() + " can't deal max damage in PvP.");
                    } else {
                        victim.setFakeDamage(true);
                        victim.dropMessage(5, "You can deal max damage in PvP.");
                        player.dropMessage(5, victim.getName() + " can deal max damage in PvP.");
                    }
                    return true;
                case "fakedmg":
                    if (player.fakeDamage()) {
                        player.setFakeDamage(false);
                        player.dropMessage(5, "You can't deal max damage in PvP.");
                    } else {
                        player.setFakeDamage(true);
                        player.dropMessage(5, "You can deal max damage in PvP.");
                    }
                    return true;
                case "makepet":
                    int petid = Integer.parseInt(splitted[1]);
                    if (petid >= 5000000 && petid <= 5000500) {
                        MapleInventoryManipulator.addById(c, petid, (short) 1, "", MaplePet.createPet(petid, MapleItemInformationProvider.getInstance().getName(petid), 1, 0, 100, MapleInventoryIdentifier.getInstance(), 0, (short) 0), 20000, "");
                    } else {
                        player.dropMessage("Item is not a pet..");
                    }
                    /*final MapleItemInformationProvider li = MapleItemInformationProvider.getInstance();
                    Item item_ = li.getEquipById(Integer.parseInt(splitted[1]));
                    short pos = MapleInventoryManipulator.addbyItem(c, item_, true);
                if (pos >= 0) {
                    if (item_.getPet() != null) {
                        // MapleInventoryManipulator.addbyItem(c, item_);
                        item_.getPet().setInventoryPosition(pos);
                        c.getPlayer().addPet(item_.getPet());
                        c.getPlayer().showMessage("You just got a " + MapleItemInformationProvider.getInstance().getName(Integer.parseInt(splitted[1])) + "!");
                    }
                }*/
                    return true;
                case "copy":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                        for (Item ii : victim.getInventory(MapleInventoryType.EQUIPPED).list()) {
                            MapleInventoryManipulator.addById(c, ii.getItemId(), (short) 1, "");
                        }
                        player.setFace(victim.getFace());
                        player.setHair(victim.getHair());
                        player.setGender(victim.getGender());
                        player.setSkinColor(victim.getSkinColor());
                        c.getPlayer().getClient().getSession().write(CField.getCharInfo(c.getPlayer()));
                        c.getPlayer().getMap().removePlayer(c.getPlayer());
                        c.getPlayer().getMap().addPlayer(c.getPlayer());
                return true;
                case "smega":
                    if (splitted.length == 1) {
                    player.dropMessage("Usage: !smega [name] [type] [message], where [type] is love, cloud, ctiger(cute tiger), rtiger(roaring tiger), goal, soccer or diablo.");
                }
                victim = cserv.getPlayerStorage().getCharacterByName(splitted[1]);
                String type = splitted[2];
                int channel = victim.getClient().getChannel();
                String text = StringUtil.joinStringFrom(splitted, 3);
                int itemID = 0;
                if (type.equals("love"))
                    itemID = 5390002;
                else if (type.equalsIgnoreCase("cloud"))
                    itemID = 5390001;
                else if (type.equalsIgnoreCase("diablo"))
                    itemID = 5390000;
                else if (type.equalsIgnoreCase("ctiger"))
                    itemID = 5390005;
                else if (type.equalsIgnoreCase("rtiger"))
                    itemID = 5390006;
                else if (type.equalsIgnoreCase("goal"))
                    itemID = 5390007;
                else if (type.equalsIgnoreCase("soccer"))
                    itemID = 5390008;
                else {
                    player.dropMessage("Invalid type (use love, cloud, or diablo)");
                    return true;
                }
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
                LinkedList list = new LinkedList();
                list.add(lines[0]);
                list.add(lines[1]);
                list.add(lines[2]);
                list.add(lines[3]);
                World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.getAvatarMega(victim, channel, itemID, list, false));
                return true;
                case "pinkbean":
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8820001), player.getPosition());
                    return true;
                case "iplist":
                    for (MapleCharacter chr : LoginServer.getInstance().getWorld(player.getWorld()).getPlayerStorage().getAllCharacters()) {
                        if (chr == player || chr.isAdmin()) {
                            player.dropMessage(5, chr.getName() + " is an Admin, will not show IP.");
                        } else {
                            player.dropNPC(chr.getClient().getSession().getRemoteAddress() + "     - " + chr.getName() + "\r\n");
                        }
                    }
                return true;
                case "rickroll": // big array is big (worry)
                    String[] lyrics = {"Never gonna tell a lie and hurt you", "Never gonna say goodbye", "Never gonna make you cry", "Never gonna run around and desert you", "Never gonna let you down", "Never gonna give you up", "Never gonna tell a lie and hurt you",
                    "Never gonna say goodbye", "Never gonna make you cry", "Never gonna run around and desert you", "Never gonna let you down", "Never gonna give you up", "Never gonna tell a lie and hurt you", "Never gonna say goodbye", "Never gonna make you cry", 
                    "Never gonna run around and desert you", "Never gonna let you down", "Never gonna give you up", "Gotta make you understand", "I just wanna tell you how I'm feeling", "We know the game and we're gonna play it", "Inside, we both know what's been going on", "You're too shy to say it", "Your heart's been aching, but",
                    "We've known each other for so long", "(Give you up)", "Never gonna give, never gonna give", "(Give you up)", "Never gonna give, never gonna give", "(Ooh, give you up)", "(Ooh, give you up)", "Never gonna tell a lie and hurt you", "Never gonna say goodbye", "Never gonna make you cry", "Never gonna run around and desert you", 
                    "Never gonna let you down", "Never gonna give you up", "Never gonna tell a lie and hurt you", "Never gonna say goodbye", "Never gonna make you cry", "Never gonna run around and desert you", "Never gonna let you down", "Never gonna give you up", "Don't tell me you're too blind to see", "And if you ask me how I'm feeling", 
                    "We know the game and we're gonna play it", "Inside, we both know what's been going on", "You're too shy to say it", "Your heart's been aching, but", "We've known each other for so long", "Never gonna tell a lie and hurt you", "Never gonna say goodbye", "Never gonna make you cry", "Never gonna run around and desert you", 
                    "Never gonna let you down", "Never gonna give you up", "Gotta make you understand", "I just wanna tell you how I'm feeling", "You wouldn't get this from any other guy", "A full commitment's what I'm thinking of", "You know the rules and so do I", "We're no strangers to love"}; 
                    for (String i : lyrics) { // don't even have to use brackets but whatever.
                        c.getChannelServer().broadcastPacket(CWvsContext.serverNotice(1, i));
                    }
                    return true;
                case "rapgod":
                    String[] rapgod = {"Look, I was gonna go easy on you not to hurt your feelings", "But I'm only going to get this one chance", "(Six minutes, six minutes)", "Something's wrong, I can feel it", "(Six minutes, six minutes, Slim Shady, you're on)", "Just a feeling I've got", "Like something's about to happen", "But I don't know what", "If that means, what I think it means, we're in trouble", "Big trouble. And if he is as bananas as you say", "I'm not taking any chances", "You were just what the doctor ordered", "...", "I'm beginning to feel like a Rap God, Rap God", "All my people from the front to the back nod, back nod", "Now who thinks their arms are long enough to slap box, slap box?", "They said I rap like a robot, so call me rap-bot", "...", "But for me to rap like a computer must be in my genes", "I got a laptop in my back pocket", 
                    "My pen'll go off when I half-cock it", "Got a fat knot from that rap profit", "Made a living and a killing off it", "Ever since Bill Clinton was still in office", "With Monica Lewinski feeling on his nutsack", "I'm an MC still as honest", "But as rude and as indecent as all hell", "Syllables, skill-a-holic (Kill 'em all with)", "This flippity, dippity-hippity hip-hop", "You don't really wanna get into a pissing match", "With this rappity-rap", "Packing a mack in the back of the Ac", "backpack rap, crap, yap-yap, yackety-yack", "and at the exact same time", "I attempt these lyrical acrobat stunts while I'm practicing that", "I'll still be able to break a motherfuckin' table", "Over the back of a couple of faggots and crack it in half", "Only realized it was ironic", "I was signed to Aftermath after the fact", 
                    "How could I not blow? All I do is drop 'F' bombs", "Feel my wrath of attack", "Rappers are having a rough time period", "Here's a Maxi-Pad", "It's actually disastrously bad", "For the wack while I'm masterfully constructing this masterpiece yeah", "...", "'Cause I'm beginning to feel like a Rap God, Rap God", "All my people from the front to the back nod, back nod", "Now who thinks their arms are long enough to slap box, slap box?", "Let me show you maintaining this shit ain't that hard, that hard", "...", "Everybody want the key and the secret to rap", "Immortality like I have got", "Well, to be truthful the blueprint's", "Simply rage and youthful exuberance", "Everybody loves to root for a nuisance", "Hit the earth like an asteroid", "and did nothing but shoot for the moon since (PPEEYOOM)", 
                    "MC's get taken to school with this music", "'Cause I use it as a vehicle to 'bus the rhyme'", "Now I lead a New School full of students", "Me? Me, I'm a product of Rakim", "Lakim Shabazz, 2Pac, N-W-A., Cube, hey, Doc, Ren", "Yella, Eazy, thank you, they got Slim", "Inspired enough to one day grow up", "Blow up and being in a position", "To meet Run-D.M.C. and induct them", "Into the motherfuckin' Rock n'", "Roll Hall of Fame even though I walk in the church", "And burst in a ball of flames", "Only Hall of Fame I'll be inducted in is the alcohol of fame", "On the wall of shame", "You fags think it's all a game", "'Til I walk a flock of flames", "Off a plank and", "Tell me what in the fuck are you thinking?", "Little gay looking boy", "So gay I can barely say it with a 'straight' face looking boy", 
                    "You're witnessing a mass-occur like you're watching a church gathering", "And take place looking boy", "Oy vey, that boy's gay", "That's all they say looking boy", "You get a thumbs up, pat on the back", "And a 'way to go' from your label every day looking boy", "Hey, looking boy, what d'you say looking boy?", "I get a 'hell yeah' from Dre looking boy", "I'mma work for everything I have", "Never asked nobody for shit", "Git out my face looking boy", "Basically boy you're never gonna be capable", "of keeping up with the same pace looking boy, 'cause", "...", "I'm beginning to feel like a Rap God, Rap God", "All my people from the front to the back nod, back nod", "The way I'm racing around the track, call me Nascar, Nascar", "Dale Earnhardt of the trailer park, the White Trash God", 
                    "Kneel before General Zod this planet's Krypton, no Asgard, Asgard", "...", "So you'll be Thor and I'll be Odin", "You rodent, I'm omnipotent", "Let off then I'm reloading", "Immediately with these bombs I'm totin'", "And I should not be woken", "I'm the walking dead", "But I'm just a talking head, a zombie floating", "But I got your mom deep throating", "I'm out my Ramen Noodle", "We have nothing in common, poodle", "I'm a Doberman, pinch yourself", "In the arm and pay homage, pupil", "It's me", "My honesty's brutal", "But it's honestly futile if I don't utilize", "What I do though for good", "At least once in a while so I wanna make sure", "Somewhere in this chicken scratch I scribble and doodle", "Enough rhymes to", "Maybe try to help get some people through tough times", "But I gotta keep a few punchlines", 
                    "Just in case 'cause even you unsigned", "Rappers are hungry looking at me like it's lunchtime", "I know there was a time where once I", "Was king of the underground", "But I still rap like I'm on my Pharoahe Monch grind", "So I crunch rhymes", "But sometimes when you combine", "Appeal with the skin color of mine", "You get too big and here they come trying to", "Censor you like that one line I said", "On 'I'm Back' from the Mathers LP", "One when I tried to say I'll take seven kids from Columbine", "Put 'em all in a line", "Add an AK-47, a revolver and a nine", "See if I get away with it now", "That I ain't as big as I was, but I'm", "Morphin' into an immortal coming through the portal", "You're stuck in a time warp from two thousand four though", "And I don't know what the fuck that you rhyme for", "You're pointless as Rapunzel", 
                    "With fucking cornrows", "You write normal, fuck being normal", "And I just bought a new ray gun from the future", "Just to come and shoot ya", "Like when Fabulous made Ray J mad", "'Cause Fab said he looked like a fag", "At Mayweather's pad singin' to a man", "While he play piano", "Man, oh man, that was the 24/7 special", "On the cable channel", "So Ray J went straight to radio station the very next day", "Hey, Fab, I'mma kill you", "Lyrics coming at you at supersonic speed, (JJ Fad)", "Uh, summa lumma dooma lumma you assuming I'm a human", "What I gotta do to get it through to you I'm superhuman", "Innovative and I'm made of rubber, so that anything you say is", "Ricochet in off a me and it'll glue to you", "And I'm devastating more than ever demonstrating", "How to give a motherfuckin' audience a feeling like it's levitating", 
                    "Never fading, and I know that haters are forever waiting", "For the day that they can say I fell off, they'll be celebrating", "'Cause I know the way to get 'em motivated", "I make elevating music", "You make elevator music", "Oh, he's too mainstream.", "Well, that's what they do", "When they get jealous, they confuse it", "It's not hip hop, it's pop.", "'Cause I found a hella way to fuse it", "With rock, shock rap with Doc", "Throw on 'Lose Yourself' and make 'em lose it", "I don't know how to make songs like that", "I don't know what words to use", "Let me know when it occurs to you", "While I'm ripping any one of these verses that versus you", "It's curtains, I'm inadvertently hurtin' you", "How many verses I gotta murder to", "Prove that if you were half as nice,", "your songs you could sacrifice virgins to", "Unghh, school flunky, pill junky", 
                    "But look at the accolades these skills brung me", "Full of myself, but still hungry", "I bully myself 'cause I make me do what I put my mind to", "When I'm a million leagues above you", "Ill when I speak in tongues", "But it's still tongue-and-cheek, fuck you", "I'm drunk so Satan take the fucking wheel", "I'm asleep in the front seat", "Bumping Heavy D and the Boys", "Still chunky, but funky", "But in my head there's something", "I can feel tugging and struggling", "Angels fight with devils and", "Here's what they want from me", "They're asking me to eliminate some of the women hate", "But if you take into consideration the bitter hatred I had", "Then you may be a little patient and more sympathetic to the situation", "And understand the discrimination", "But fuck it", "Life's handing you lemons", "Make lemonade then", 
                    "But if I can't batter the women", "How the fuck am I supposed to bake them a cake then?", "Don't mistake him for Satan", "It's a fatal mistake if you think I need to be overseas", "And take a vacation to trip a broad", "And make her fall on her face and", "Don't be a retard, be a king?", "Think not", "Why be a king when you can be a God?"};
                    for (String i : rapgod) {
                        World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(1, i));
                    }
                return true;
                case "disableui":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (!victim.isAdmin()) {
                     victim.startTrollLock();
                    }
                return true;
                case "enableui":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.stopTrollLock();
                    return true;
                case "setleeton":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.setLeetness(true);
                    player.dropMessage(6, "You have given " + victim.getName() + " the gift of 1337.");
                    victim.dropMessage("You have been given the gift of 1337.");
                    return true;
                case "setleetoff":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.setLeetness(false);
                    player.dropMessage(6, "You have taken away " + victim.getName() + "'s 1337.");
                    victim.dropMessage("Uh oh! It looks like those silly Trolls have taken away your 1337..");
                return true;
                case "morphmap":
                    for (MapleCharacter map : player.getMap().getCharacters()) {
                      MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                      ii.getItemEffect(Integer.parseInt("22100" + splitted[1])).applyTo(map);
                    }
                return true;
                case "insult1":
                   // for (MapleCharacter random_player : World.getAllCharacters()) {
                   //     HashMap<String, Integer> players = new HashMap<>();
                   //     players.put(random_player.getName(), random_player.getId());
                   //     for (int i : players.values()) {
                   //         MapleCharacter chosen = c.getChannelServer().getPlayerStorage().getCharacterById(i);
                   //         final String announced = chosen.getName();
                   //     }
                   // }
                    c.getChannelServer().broadcastPacket(CWvsContext.serverNotice(6, "[Development's Insult System] : " + InsultBot.getInsult()));
                    return true;
                case "hairperson":
                    int hair = Integer.parseInt(splitted[2]);
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (!victim.isEric()) {
                        victim.setHair(hair);
                        victim.getClient().getSession().write(CField.getCharInfo(victim));
                        victim.getMap().removePlayer(victim);
                        victim.getMap().addPlayer(victim);   
                    }
                    return true;
                case "eyesperson":
                    int eyes = Integer.parseInt(splitted[2]);
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (!victim.isEric()) {
                        victim.setFace(eyes);
                        victim.getClient().getSession().write(CField.getCharInfo(victim));
                        victim.getMap().removePlayer(victim);
                        victim.getMap().addPlayer(victim);   
                    }
                    return true;
                case "jailall":
                    String users = splitted[1];
                    victim = cserv.getPlayerStorage().getCharacterByName(users);
                    int mapid = 980000404;
                    if (victim != null) {
			MapleMap target = cserv.getMapFactory().getMap(mapid);
			MaplePortal targetPortal = target.getPortal(0);
			victim.changeMap(target, targetPortal);
                    }
                    return true;
                case "lolhaha":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim.getGender() == 0){
                        victim.setGender((byte)1);
                        player.dropMessage(6, "TROLOLOLOL! " + victim.getName() + " is now a " + victim.getJQWinner() + "");
                    } else {
                        victim.setGender((byte)0);
                        player.dropMessage(6, "TROLOLOLOL! " + victim.getName() + " is now a " + victim.getJQWinner() + "");
                    }
                    return true;
                case "killeveryone":
                    for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters())
                        if (mch.isAdmin() == true) {
                            player.dropMessage(6, "[Error]: " + mch.getName() + " is an Owner, he will not be killed!");
                        } else {
                            if (mch != null) {
                                mch.setHp(0);
                                mch.setMp(0);
                                mch.updateSingleStat(MapleStat.HP, 0);
                                mch.updateSingleStat(MapleStat.MP, 0);
                                mch.dropMessage(6, "BOOM HEADSHOT!");
                            }
                        }
                    return true;
                case "seducemap":
                    int level = Integer.parseInt(splitted[1]);
                    for (MapleCharacter map : player.getMap().getCharacters()) {
                        if (map.gmLevel() >= 5) {
                            player.dropMessage(6, "[Error]: " + map.getName() + " Is A GM/Admin, You can't seduce him/her!");
                        } else {
                            map.enableSeduce();
                            map.setChair(0);
                            map.getClient().getSession().write(CField.cancelChair(-1));
                            map.getMap().broadcastMessage(map, CField.showChair(map.getId(), 0), false);
                            map.giveDebuff(MapleDisease.SEDUCE,MobSkillFactory.getMobSkill(128,level));
                        }
                    }
                return true;
                case "seduceinfo":
                    player.dropMessage(6, "1 - Walks to the Left");
                    player.dropMessage(6, "2 - Walks to the Right");
                    player.dropMessage(6, "3 - Jumping");
                    player.dropMessage(6, "10 - Jumping + Right Direction");
                    player.dropMessage(6, "11 - Prone (Basically the down Arrow)");
                    return true;
                case "saveall":
                    for (World w : LoginServer.getInstance().getWorlds()) {
                        for (MapleCharacter chr : w.getPlayerStorage().getAllCharacters()) {
                            chr.dropMessage(6, "Development is saving all users, please wait..");
                            chr.saveToDB(false, false);
                            chr.dropMessage("Save Completed!");
                        }
                    }
                    return true;
                case "jobperson":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (MapleCarnivalChallenge.getJobNameById(Integer.parseInt(splitted[2])).length() == 0) { //wut?
                        player.dropMessage(5, "Invalid Job");
                        return true;
                    }
                    victim.changeJob((short)Integer.parseInt(splitted[2]));
                    return true;
                case "levelperson":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.setLevel((short)(Short.parseShort(splitted[2])-1));
                    victim.levelUp();
                    victim.levelUp();
                    if (victim.getExp() < 0) {
                        victim.gainExp(-victim.getExp(), false, false, true);
                    }
                    return true;
                case "setrebirths":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    int rebirths = 0;
                    try {
                        rebirths = Integer.parseInt(splitted[2]);
                    } catch (NumberFormatException asd) {
                    }
                    if (victim != null) {
                        victim.setReborns(rebirths);
                    } else {
                        player.dropMessage("Player was not found");
                    }
                return true;
                case "trolljail":
                       victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                       int trolljail = 90000000;
                       victim.changeMap(trolljail);
                       return true;
                case "giftdpoints":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.gainPoints(Integer.parseInt(splitted[2])); // gain not set :|
                    return true;
                case "donation": // Official Donation command.
                    String Donator = splitted[1];
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(Donator);
                    int donationAmount = Integer.parseInt(splitted[2]);
                    int points = donationAmount * 100;
                     if (victim != null) {
                        victim.gainPoints(points);
                        player.dropMessage(victim.getName() + " has been gifted " + points + " Donator Points.");
                        victim.dropMessage("You've been gifted " + points + " Donator Points.");
                     } else {
                      int acc = getAccountID(Donator);
                         try {
                                Connection con = DatabaseConnection.getConnection();
                                PreparedStatement ps = con.prepareStatement("UPDATE accounts SET points = points + ? WHERE id = ?");
                                ps.setInt(2, getAccountID(Donator));
                                ps.setInt(1, points);
                                ps.executeUpdate();
                                ps.close();
                            } catch (SQLException e) {
                            }
                         player.dropMessage("Gifted " + points + " Donator Points to " + Donator + ". (Account ID: " + acc + ")");
                     }
                    return true;
                case "setdonor":
                    String Donator1 = splitted[1];
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(Donator1);
                    if (victim != null) {
                        victim.setGmLevel((byte)1);
                    } else {
                        try {
                                Connection con = DatabaseConnection.getConnection();
                                PreparedStatement ps = con.prepareStatement("UPDATE characters SET gm = ? WHERE name = ?");
                                ps.setString(2, Donator1);
                                ps.setInt(1, 1);
                                ps.executeUpdate();
                                ps.close();
                            } catch (SQLException e) {
                            }
                    }
                    player.dropMessage(6, Donator1 + " is now a Donor.");
                    return true;
                case "setsdonor":
                    String Donator2 = splitted[1];
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(Donator2);
                    if (victim != null) {
                        victim.setGmLevel((byte)2); // super donor
                    } else {
                        try {
                                Connection con = DatabaseConnection.getConnection();
                                PreparedStatement ps = con.prepareStatement("UPDATE characters SET gm = ? WHERE name = ?");
                                ps.setString(2, Donator2);
                                ps.setInt(1, 2);
                                ps.executeUpdate();
                                ps.close();
                            } catch (SQLException e) {
                            }
                    }
                    player.dropMessage(6, Donator2 + " is now a Super Donor");
                    return true;
                case "setgmlevel":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    int gmlevel = (byte)Integer.parseInt(splitted[2]);
                    victim.setGmLevel(gmlevel);
                    player.dropMessage(5, "Done.");
                    return true;
                case "getmap":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    player.dropMessage(5, victim.getName() + " is at " + victim.getMap().getMapName() + " (Map " + victim.getMapId() + ")");
                    return true;
                case "checkplayers":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    int playerzzz = victim.getMap().getCharacters().size();
                    StringBuilder builder = new StringBuilder("Names of players on " + victim.getName() + "'s map: ").append(victim.getMap().getCharactersThreadsafe().size()).append(", ");
                    for (MapleCharacter chr2 : victim.getMap().getCharactersThreadsafe()) {
                        if (builder.length() > 150) { // wild guess :o
                            builder.setLength(builder.length() - 2);
                            c.getPlayer().dropMessage(6, builder.toString());
                            builder = new StringBuilder();
                        }
                        builder.append(MapleCharacterUtil.makeMapleReadable(chr2.getName()));
                        builder.append(", ");
                    }
                    builder.setLength(builder.length() - 2);
                    c.getPlayer().dropMessage(5, "There are " + playerzzz + " players on " + victim.getName() + "'s map.");
                    c.getPlayer().dropMessage(5, builder.toString());
                    return true;
                case "setname":
                    if (splitted.length != 3) {
                    }
            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            String newname = splitted[2];
            if (splitted.length == 3) {
                if (MapleCharacter.getIdByName(newname, 0) == -1) {
                    if (victim != null) {
                        victim.setName(newname);
                        victim.saveToDB(true, false);
                        victim.getClient().disconnect(true, true); 
                        player.dropMessage(splitted[1] + " is now named " + newname + "");
                    } else {
                        player.dropMessage("The player " + splitted[1] + " is either offline or not in this channel");
                        return true;
                    }
                } else {
                    player.dropMessage("Character name in use.");
                    return true;
                }
            } else {
                player.dropMessage("Incorrect syntax !");
                return true;
            }
                return true;
                // End of Eric's Commands
                case "playmovie":
                    player.playMovie(splitted[1]);
                    return true;
                case "movieperson":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.playMovie(splitted[2]);
                    return true;
                case "potinfo":
                    text = "";
                    for (StructItemOption sio : MapleItemInformationProvider.getInstance().getPotentialInfo(60001)) {
                        text += "Face: ";
                        text += sio.face;
                        text += " opID: ";
                        text += sio.opID;
                        text += " optionType: ";
                        text += sio.optionType;
                        text += " reqLevel: ";
                        text += sio.reqLevel;
                    }
                    c.getPlayer().showMessage(text);
                    return true;
                case "saveplayers":
                    World.saveAllChars();
                    return true;
                case "gmtextperson":
                case "gmtextp":
                  victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                  int gmtext;
            if (splitted[2].equalsIgnoreCase("normal")) {
                gmtext = 0;
            } else if (splitted[2].equalsIgnoreCase("orange")) {
                gmtext = 1;
            } else if (splitted[2].equalsIgnoreCase("pink")) {
                gmtext = 2;
            } else if (splitted[2].equalsIgnoreCase("purple")) {
                gmtext = 3;
            } else if (splitted[2].equalsIgnoreCase("green")) {
                gmtext = 4;
            } else if (splitted[2].equalsIgnoreCase("red")) {
                gmtext = 5;
            } else if (splitted[2].equalsIgnoreCase("blue")) {
                gmtext = 6;
            } else if (splitted[2].equalsIgnoreCase("whitebg")) {
                gmtext = 7;
            } else if (splitted[2].equalsIgnoreCase("lightinggreen")) {
                gmtext = 8;
            } else if (splitted[2].equalsIgnoreCase("yellow")) { // hidden but known text
                gmtext = 9;
            } else if (splitted[2].equalsIgnoreCase("mega")) { // these are all hidden from here on
                gmtext = 100;
            } else if (splitted[2].equalsIgnoreCase("avi")) {
                gmtext = 101;
            } else if (splitted[2].equalsIgnoreCase("spouse")) {
                gmtext = 102;
            } else if (splitted[2].equalsIgnoreCase("smega")) {
                gmtext = 103;
            } else {
                 player.dropMessage("Wrong syntax: use !gmtextp <ign> <normal/orange/pink/purple/green/blue/red/whitebg/lightinggreen>");
                 return true;
            }
            try {
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement("UPDATE characters SET gmtext = ? WHERE name = ?");
                ps.setString(2, victim.getName());
                ps.setInt(1, gmtext);
                ps.executeUpdate();
                ps.close();
                victim.setGMText(gmtext);
            } catch (SQLException e) {
            }
                return true;
                case "makemsi":
                    try {
                        int itemid = Integer.parseInt(splitted[1]);
                        Equip equip = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
                        if (equip == null) {
                            c.getPlayer().showMessage("Item does not exist.");
                        } else {
                            equip.makeMSI(c.getPlayer().getName());
                            MapleInventoryManipulator.addbyItem(c, (Item) equip);
                            c.getPlayer().showMessage("You just got a " + MapleItemInformationProvider.getInstance().getName(itemid) + "!");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return true;
                case "stripeveryone":
                    ChannelServer cs = c.getChannelServer();
                    for (MapleCharacter mchr : cs.getPlayerStorage().getAllCharacters()) {
                        if (mchr.isGM()) {
                            continue;
                        }
                        MapleInventory equipped = mchr.getInventory(MapleInventoryType.EQUIPPED);
                        MapleInventory equip = mchr.getInventory(MapleInventoryType.EQUIP);
                        List<Short> ids = new ArrayList<Short>();
                        for (Item item : equipped.newList()) {
                            ids.add(item.getPosition());
                        }
                        for (short id : ids) {
                            MapleInventoryManipulator.unequip(mchr.getClient(), id, equip.getNextFreeSlot());
                        }
                    }
                    return true;
                case "stripmap":
                    for (MapleCharacter victims : player.getMap().getCharacters()) {
                        if (!victims.isGM()) {
                            victims.unequipEverything();
                        } else {
                            player.dropMessage("[Error]: " + victims.getName() + " is a GM and won't be stripped.");
                        }
                    }
                    return true;
                case "msgtest":
                    // String hint = InternCommand.joinStringFrom(splitted, 1);
                    String hint = ServerConstants.WELCOME_MESSAGE;
                    c.getSession().write(CField.sendHint("" + hint + "", 0, 0));
                    c.getPlayer().dropMessage("Length: " + hint.length() + ".");
                    c.getPlayer().dropMessage("Width: " + Math.max(hint.length() * 10, 40) + ".");
                    c.getPlayer().dropMessage("Height: " + Math.max(0, 5) + ".");
                    return true;
                //case "reloadall":
                  //  for (MapleCharacter chra : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                        //lolwut
                    //}
                   // return true;
                case "lolumad":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            playerf.dropMessage(1, "Have fun and enjoy the movie!\r\n\r\nLove, " + playerf.getName());
                        }
                    }, 2000); // 2 secs
                    victim.playMovie("phantom"); // 3:59 playtime LOOL ;)
                    return true;
                case "portals":
                    String ids = "The portal IDs for this map are : ";
                    for (MaplePortal portal : c.getPlayer().getMap().getAllPortals()) {
                        ids += "| " + portal.getName() + " : " + portal.getId() + " |"; //portal.getScriptName()
                    }
                    player.dropMessage(6, ids);
                    return true;
                case "reactors":
                    String eids = "The reactor IDs for this map are : ";
                    for (MapleReactor reactor : c.getPlayer().getMap().getAllReactor()) {
                        eids += "| " + reactor.getName() + " : " + reactor.getReactorId() + " |";
                    }
                    c.getPlayer().dropMessage(6, eids);
                    return true;
                case "pnpc":
                    if (splitted.length < 1) {
                        c.getPlayer().dropMessage(6, "!pnpc <npcid>");
                        return true;
                    }
                    int npcId = Integer.parseInt(splitted[1]);
                    MapleNPC npc = MapleLifeFactory.getNPC(npcId);
                    if (npc != null && !npc.getName().equals("MISSINGNO")) {
                        final int xpos = c.getPlayer().getPosition().x;
                        final int ypos = c.getPlayer().getPosition().y;
                        final int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId();
                        npc.setPosition(c.getPlayer().getPosition());
                        npc.setCy(ypos);
                        npc.setRx0(xpos);
                        npc.setRx1(xpos);
                        npc.setFh(fh);
                        npc.setCustom(true);
                        try {
                            Connection con = DatabaseConnection.getConnection();
                            try(PreparedStatement ps = con.prepareStatement("INSERT INTO wz_customlife ( idd, f, fh, type, cy, rx0, rx1, x, y, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )")) {
                                ps.setInt(1, npcId);
                                ps.setInt(2, 0); // 1 = hide, 0 = show
                                ps.setInt(3, fh);
                                ps.setString(4, "n");
                                ps.setInt(5, ypos);
                                ps.setInt(6, xpos);
                                ps.setInt(7, xpos);
                                ps.setInt(8, xpos);
                                ps.setInt(9, ypos);
                                ps.setInt(10, c.getPlayer().getMapId());
                                ps.executeUpdate();
                            }
                        } catch (SQLException e) {
                            c.getPlayer().dropMessage(6, "Failed to save NPC to the database");
                            return true;
                        }
                        c.getPlayer().getMap().addMapObject(npc);
                        c.getPlayer().getMap().broadcastMessage(NPCPacket.spawnNPC(npc, true));
                        c.getPlayer().dropMessage(6, "Please do not reload this map or else the NPC will disappear till the next restart.");
                    } else {
                        c.getPlayer().dropMessage(6, "You have entered an invalid Npc-Id");
                        return true;
                    }
                    return true;
                case "pmob1":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "!pmob1 <mobid> <mobTime>");
                        return true;
                    }
                    int mobid = Integer.parseInt(splitted[1]);
                    int mobTime = Integer.parseInt(splitted[2]);
                    MapleMonster pmob;
                    try {
                        pmob = MapleLifeFactory.getMonster(mobid);
                    } catch (RuntimeException e) {
                        c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                        return true;
                    }
                    if (pmob != null) {
                        final int xpos = c.getPlayer().getPosition().x;
                        final int ypos = c.getPlayer().getPosition().y;
                        final int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId();
                        pmob.setPosition(c.getPlayer().getPosition());
                        pmob.setCy(ypos);
                        pmob.setRx0(xpos);
                        pmob.setRx1(xpos);
                        pmob.setFh(fh);
                        try {
                            Connection con = DatabaseConnection.getConnection();
                            try (PreparedStatement ps = con.prepareStatement("INSERT INTO wz_customlife (idd, f, fh, cy, type, rx0, rx1, x, y, mid, mobtime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                                ps.setInt(1, mobid);
                                ps.setInt(2, 0); // 1 = right , 0 = left
                                ps.setInt(3, fh);
                                ps.setInt(4, ypos);
                                ps.setInt(5, xpos);
                                ps.setInt(6, xpos);
                                ps.setString(7, "m");
                                ps.setInt(8, xpos);
                                ps.setInt(9, ypos);
                                ps.setInt(10, c.getPlayer().getMapId());
                                ps.setInt(11, mobTime);
                                ps.executeUpdate();
                            }
                        } catch (SQLException e) {
                            c.getPlayer().dropMessage(6, "Failed to save NPC to the database");
                        }
                        c.getPlayer().getMap().addMonsterSpawn(pmob, mobTime, (byte) -1, "");
                        c.getPlayer().dropMessage(6, "Please do not reload this map or else the MOB will disappear till the next restart.");
                    } else {
                        c.getPlayer().dropMessage(6, "You have entered an invalid Mob-Id");
                        return true;
                    }
                return true;
                case "pmob":
             npcId = Integer.parseInt(splitted[1]);
            int monsterId;
            mobTime = Integer.parseInt(splitted[2]);
            int xpos = player.getPosition().x;
            int ypos = player.getPosition().y;
            int fh = player.getMap().getFootholds().findBelow(player.getPosition()).getId();
            if (splitted[2] == null) {
                mobTime = 0;
            }
            MapleMonster mob = MapleLifeFactory.getMonster(npcId);
            if (mob != null && !mob.getName().equalsIgnoreCase("MISSINGNO")) {
                mob.setPosition(player.getPosition());
                mob.setCy(ypos);
                mob.setRx0(xpos + 50);
                mob.setRx1(xpos - 50);
                mob.setFh(fh);
                try {
                    Connection con = DatabaseConnection.getConnection();
                    PreparedStatement ps = con.prepareStatement("INSERT INTO wz_customlife ( idd, f, fh, type, cy, rx0, rx1, x, y, mobtime, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    ps.setInt(1, npcId);
                    ps.setInt(2, 0);
                    ps.setInt(3, fh);
                    ps.setString(4, "m");
                    ps.setInt(5, ypos);
                    ps.setInt(6, xpos + 50);
                    ps.setInt(7, xpos - 50);
                    ps.setInt(8, xpos);
                    ps.setInt(9, ypos);
                    ps.setInt(10, mobTime);
                    ps.setInt(11, player.getMapId());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    player.dropMessage("Failed to save MOB to the database");
                }
                player.getMap().addMonsterSpawn(mob, mobTime, (byte) -1, "");
            } else {
                player.dropMessage("You have entered an invalid Npc-Id");
            }
                    return true;
                case "openmrush": // TODO: update coordinates and change maps (?)
                    // int[] maps = {260000000, 680000000, 211000000, 600000000, 120000000};
                    // int[] mobPosX = {108, 2037, -1463, 2031, 1180};
                    // int[] mobPosY = {275, -56, 94, 501, 155};
                       player.spawnMrushMob1(260000000, 108, 275);
                       player.spawnMrushMob1(680000000, 2037, -56);
                       player.spawnMrushMob1(211000000, -1463, 94);
                       player.spawnMrushMob1(600000000, 2031, 501);
                       player.spawnMrushMob1(120000000, 1180, 155);
                    return true;
                case "mesoeveryone":
                        for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                            mch.gainMeso(Integer.parseInt(splitted[1]), true);
                        }
                    return true;
                case "exprate":
                    if (splitted.length > 1) {
                        final int rate = Integer.parseInt(splitted[1]);
                        if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                            for (World worlds : LoginServer.getInstance().getWorlds()) {
                                worlds.setExpRate(rate);
                            }
                        } else {
                            c.getWorldServer().setExpRate(rate);
                        }
                        c.getPlayer().dropMessage(6, "Exprate has been changed to " + rate + "x");
                    } else {
                        c.getPlayer().dropMessage(6, "Syntax: !exprate <number> [all]");
                    }
                    return true;
                case "mesorate":
                    if (splitted.length > 1) {
                        final int rate = Integer.parseInt(splitted[1]);
                        if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                                for (World worlds : LoginServer.getInstance().getWorlds()) {
                                worlds.setMesoRate(rate);
                            }
                        } else {
                            c.getWorldServer().setMesoRate(rate);
                        }
                        c.getPlayer().dropMessage(6, "Meso Rate has been changed to " + rate + "x");
                    } else {
                        c.getPlayer().dropMessage(6, "Syntax: !mesorate <number> [all]");
                    }
                    return true;
                case "dcall":
                    int range = -1;
                    if (splitted[1].equals("m")) {
                        range = 0;
                    } else if (splitted[1].equals("c")) {
                        range = 1;
                    } else if (splitted[1].equals("w")) {
                        range = 2;
                    }
                    if (range == -1) {
                        range = 1;
                    }
                    if (range == 0) {
                        c.getPlayer().getMap().disconnectAll();
                    } else if (range == 1) {
                        c.getChannelServer().getPlayerStorage().disconnectAll(true);
                    } else if (range == 2) {
                            cserv.getPlayerStorage().disconnectAll(true);
                    }
                    return true;
                case "chalkperson":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.setChalkboard("" + StringUtil.joinStringFrom(splitted, 2) + "");
                    victim.getMap().broadcastMessage(MTSCSPacket.useChalkboard(victim.getId(), StringUtil.joinStringFrom(splitted, 2)));
                    return true;
                case "chalkmap":
                    for (MapleCharacter mapchrs : player.getMap().getCharactersThreadsafe()) {
                        if (mapchrs != player) {
                            mapchrs.setChalkboard("" + StringUtil.joinStringFrom(splitted, 1) + "");
                            mapchrs.getMap().broadcastMessage(MTSCSPacket.useChalkboard(mapchrs.getId(), StringUtil.joinStringFrom(splitted, 1)));
                        }
                    }
                    return true;
                case "shutdownnow":
                    World.Broadcast.broadcastMessage(player.getWorld(), CWvsContext.serverNotice(1, "Performing an immediate Shutdown.."));
                    World.Shutdown = true;
                    EventTimer.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                            Thread t = new Thread(ShutdownServer.getInstance());
                    ShutdownServer.getInstance().run();
                    t.start();
                        }
                    }, 5 * 1000); // 5(1000) = 5seconds o-o
                    return true;
                 case "shutdown":
                    int time = 60; // seconds when not using an integer
                    int x, y, z;
                    x = Integer.parseInt(splitted[1]);
                    y = x * 60;
                    z = y * 1000;
                    time = z;
                    for (World w : LoginServer.getInstance().getWorlds()) {
                        for (MapleCharacter all : w.getPlayerStorage().getAllCharacters()) {
                            all.announce(CWvsContext.getMidMsg("Performing an immediate Shutdown in " + x + " minute(s)..", true, 1));
                      }
                    }
                    World.Shutdown = true;
                         EventTimer.getInstance().schedule(new Runnable() {
                             @Override
                             public void run() {
                                Thread w = new Thread(ShutdownServer.getInstance());
                                ShutdownServer.getInstance().run();
                                w.start();
                             }
                         }, time);
                    return true;
                case "charinfo":
                    builder = new StringBuilder();
                    final MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
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

                    builder.append(" || hasTrade: ");
                    builder.append(other.getTrade() != null);
                    builder.append(" || Latency: ");
                    builder.append(other.getClient().getLatency());
                    builder.append(" || PING: ");
                    builder.append(other.getClient().getLastPing());
                    builder.append(" || PONG: ");
                    builder.append(other.getClient().getLastPong());
                    builder.append(" || remoteAddress: ");

                    other.getClient().DebugMessage(builder);

                    c.getPlayer().dropMessage(6, builder.toString());
                    return true;
                case "startprofiling":
                    CPUSampler sampler = CPUSampler.getInstance();
                    sampler.addIncluded("client");
                    sampler.addIncluded("constants"); //or should we do Packages.constants etc.?
                    sampler.addIncluded("database");
                    sampler.addIncluded("handling");
                    sampler.addIncluded("provider");
                    sampler.addIncluded("scripting");
                    sampler.addIncluded("server");
                    sampler.addIncluded("tools");
                    sampler.start();
                    return true;
                case "stopprofiling":
                    CPUSampler sampler2 = CPUSampler.getInstance();
                    try {
                        String filename = "odinprofile.txt";
                        if (splitted.length > 1) {
                            filename = splitted[1];
                        }
                        File file = new File(filename);
                        if (file.exists()) {
                            c.getPlayer().dropMessage(6, "The entered filename already exists, choose a different one");
                            return true;
                        }
                        sampler2.stop();
                        FileWriter fw = new FileWriter(file);
                        sampler2.save(fw, 1, 10);
                        fw.close();
                    } catch (IOException e) {
                        System.err.println("Error saving profile" + e);
                    }
                    sampler2.reset();
                    return true;
                default:
                    if (c.getPlayer().getGMLevel() >= 100) {
                        return GodCommand.executeGodCommand(c, splitted);
                    } else {
                        return SuperGMCommand.executeSuperGMCommand(c, splitted);
                    }
            }
        } else {
            c.getPlayer().showMessage("LOL Did you really just type an Owner command? I CALL H@CK$.!");
            return true;
        }
    }
    
    public static String now(String dateFormat) {
          Calendar cal = Calendar.getInstance();
          SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
              return sdf.format(cal.getTime());
    }

    private static int getAccountID(String name) {
        try {
          PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT accountid FROM characters WHERE name = ?");
          ps.setString(1, name);
          ps.executeQuery();
          ResultSet rs = ps.executeQuery();
          while (rs.next()) {
                int accId = rs.getInt("accountid");
                if (accId > 0) // either way we are technically returning 0 xD
                    return accId;
            }
          rs.close();
          ps.close();
        } catch (SQLException e) {
        }
          return 0; // will only return 0 if it can't get id of name
    }
}
