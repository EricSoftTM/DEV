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
package handling.login;

import constants.ServerConstants;
import constants.WorldConstants;
import handling.MapleServerHandler;
import handling.channel.ChannelServer;
import handling.mina.MapleCodecFactory;
import handling.world.World;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import tools.Pair;
import tools.packet.LoginPacket.Server;

public class LoginServer {

    public static final int PORT = 8484;
    
    private static List<World> worlds = new ArrayList<>();
    private static List<Map<Integer, String>> channels = new LinkedList<>();
    private static LoginServer instance = null;
    
    private static IoAcceptor acceptor;
    private static Map<Integer, Integer> load = new HashMap<>();
    private static int maxCharacters, usersOn = 0;
    private static boolean finishedShutdown = true, adminOnly = false;
    private static HashMap<Integer, Pair<String, String>> loginAuth = new HashMap<>();
    private static HashSet<String> loginIPAuth = new HashSet<>();

    public static LoginServer getInstance() {
        if (instance == null) {
            instance = new LoginServer();
        }
        return instance;
    }
    
    public static void putLoginAuth(int chrid, String ip, String tempIP) {
        loginAuth.put(chrid, new Pair<>(ip, tempIP));
        loginIPAuth.add(ip);
    }

    public static Pair<String, String> getLoginAuth(int chrid) {
        return loginAuth.remove(chrid);
    }

    public static boolean containsIPAuth(String ip) {
        return loginIPAuth.contains(ip);
    }

    public static void removeIPAuth(String ip) {
        loginIPAuth.remove(ip);
    }

    public static void addIPAuth(String ip) {
        loginIPAuth.add(ip);
    }

    public static void addChannel(final int channel) {
        load.put(channel, 0);
    }

    public void removeChannel(int worldid, final int channel) {
        channels.remove(channel);

        World world = worlds.get(worldid);
        if (world != null) {
            world.removeChannel(channel);
        }
    }
    
    public ChannelServer getChannel(int world, int channel) {
        return worlds.get(world).getChannel(channel);
    }

    public List<ChannelServer> getChannelsFromWorld(int world) {
        return worlds.get(world).getChannels();
    }

    public List<ChannelServer> getAllChannels() {
        List<ChannelServer> channelz = new ArrayList<>();
        for (World world : worlds) {
            for (ChannelServer ch : world.getChannels()) {
                channelz.add(ch);
            }
        }
        return channelz;
    }
    
    public String getIP(int world, int channel) {
        return channels.get(world).get(channel);
    }

    public static void run_startup_configurations() {
        adminOnly = ServerConstants.Admin_Only;

        IoBuffer.setUseDirectBuffer(false);
        IoBuffer.setAllocator(new SimpleBufferAllocator());
        acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("codec", (IoFilter) new ProtocolCodecFilter(new MapleCodecFactory()));
        acceptor.setHandler(new MapleServerHandler());
        
        byte[] flagg = new byte[17];
        int[] expp = new int[17];
        int[] mesoo = new int[17];
        int[] dropp = new int[17];
        int userLimit = WorldConstants.UserLimit;
        maxCharacters = WorldConstants.maxCharacters; // replace this with below
        int maxCharacterss = WorldConstants.maxCharacters; // todo
        String[] eventMessagee = new String[17];
        for (Pair<Integer, Byte> flags : WorldConstants.flag) {
            flagg[flags.left] = flags.right;
        }
        for (Pair<Integer, Integer> loadexp : WorldConstants.expRates) {
            expp[loadexp.left] = loadexp.right;
        }
        for (Pair<Integer, Integer> loadmeso : WorldConstants.mesoRates) {
            mesoo[loadmeso.left] = loadmeso.right;
        }
        for (Pair<Integer, Integer> loaddrop : WorldConstants.dropRates) {
            dropp[loaddrop.left] = loaddrop.right;
        }
        for (Pair<Integer, String> eventmsg : WorldConstants.eventMessages) {
            eventMessagee[eventmsg.left] = eventmsg.right;
        }
        for (int i = 0; i < WorldConstants.Worlds; i++) {
            World world = new World(i, flagg[i], eventMessagee[i], expp[i], mesoo[i], dropp[i]);
            worlds.add(world);
            channels.add(new LinkedHashMap<Integer, String>());
            for (int z = 0; z < WorldConstants.Channels; z++) {
                int channelid = z + 1;
                ChannelServer channel = ChannelServer.newInstance(i, channelid);
                world.addChannel(channel);
                world.setUserLimit(userLimit);
                channel.init(); // initialize
                channels.get(i).put(channelid, channel.getIP());
            }
            System.out.println("    " + Server.getById(world.getWorldId()).toString() + " is online:");
            System.out.println("        Channels: " + LoginServer.getInstance().getWorld(world.getWorldId()).getChannels().size());
            System.out.println("        EXP: " + LoginServer.getInstance().getWorld(world.getWorldId()).getExpRate());
            System.out.println("        MESO: " + LoginServer.getInstance().getWorld(world.getWorldId()).getMesoRate());
            System.out.println("        DROP: " + LoginServer.getInstance().getWorld(world.getWorldId()).getDropRate());
        }
        try {
            acceptor.bind(new InetSocketAddress(PORT));
        } catch (IOException e) {
            System.err.println("Binding to port " + PORT + " failed" + e);
        }
    }
    
    public World getWorld(int id) {
        return worlds.get(id);
    }
    
    public static World getWorldStatic(int id) {
        return worlds.get(id);
    }
    
    public static List<World> getWorlds() {
        return worlds;
    }

    public static void shutdown() {
        if (finishedShutdown) {
            return;
        }
        System.out.println("Shutting down login...");
        acceptor.unbind();
        finishedShutdown = true; //nothing. lol
    }

    public static int getMaxCharacters() {
        return maxCharacters;
    }

    // TODO: remove most/all of this below
    public static Map<Integer, Integer> getLoad() {
        return load;
    }

    public static void setLoad(final Map<Integer, Integer> load_, final int usersOn_) {
        load = load_;
        usersOn = usersOn_;
    }

    public static int getUsersOn() {
        return usersOn;
    }

    public static int getNumberOfSessions() {
        return acceptor.getManagedSessions().size();
    }

    public static boolean isAdminOnly() {
        return adminOnly;
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }

    public static void setOn() {
        finishedShutdown = false;
    }
}
