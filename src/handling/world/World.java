package handling.world;

import client.*;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatusEffect;
import constants.WorldConstants;
import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import handling.login.LoginServer;
import handling.world.exped.ExpeditionType;
import handling.world.exped.MapleExpedition;
import handling.world.exped.PartySearch;
import handling.world.exped.PartySearchType;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleBBSThread;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import handling.world.guild.MapleGuildCharacter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import scripting.EventScriptManager;
import server.ServerProperties;
import server.Timer.EventTimer;
import server.Timer.WorldTimer;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapItem;
import tools.packet.CField;
import tools.packet.CWvsContext;
import tools.packet.CWvsContext.AlliancePacket;
import tools.packet.CWvsContext.BuddylistPacket;
import tools.packet.CWvsContext.ExpeditionPacket;
import tools.packet.CWvsContext.GuildPacket;
import tools.packet.CWvsContext.PartyPacket;
import tools.packet.LoginPacket.Server;

public class World {
    private int id, flag, expRate, mesoRate, dropRate, cashRate = 3, traitRate = 3, flags = 0, userLimit;
    private String eventMessage;
    private List<ChannelServer> channels = new ArrayList<>();
    private static PlayerStorage players = new PlayerStorage();
    
    private static final Map<Integer, Integer> magicWheelCache = new HashMap();
    // AutoJQ and Event Maps
    public static int eventMap = 0;
    public static boolean eventOn = false;
    public static int AutoJQ_Channel = -1; // for checks in commands
    // Monster Rush
    public static boolean MonsterRush = false;
    public static boolean Monster_Rush_Enabled = false;
    // Shutdown handling
    public static boolean Shutdown = false;
    // President System
    public static String president = "None"; // World domination of president!@
    private static long lastPresident;
    // PvP System -- TODO: Map based not world O_o
    private static int pvpState = 0;
    
    public World(int world, int flag, String eventMessage, int exprate, int mesorate, int droprate) {
        this.id = world;
        this.flag = flag;
        this.eventMessage = eventMessage;
        this.expRate = exprate;
        this.mesoRate = mesorate;
        this.dropRate = droprate;
    }
    
    public List<ChannelServer> getChannels() {
        return channels;
    }

    public ChannelServer getChannel(int channel) {
        return channels.get(channel - 1);
    }

    public void addChannel(ChannelServer channel) {
        channels.add(channel);
    }

    public void removeChannel(int channel) {
        channels.remove(channel);
    }
    
    public PlayerStorage getPlayerStorage() {
        return players;
    }

    public void removePlayer(MapleCharacter chr) {
        channels.get(chr.getClient().getChannel() - 1).removePlayer(chr);
        players.deregisterPlayer(chr);
    }
    
    public void setFlag(byte b) {
        this.flag = b;
    }

    public int getFlag() {
        return flag;
    }

    public String getEventMessage() {
        return eventMessage;
    }
    
    public void setEventMessage(String message) {
        this.eventMessage = message;
    }

    public int getExpRate() {
        return expRate;
    }

    public void setExpRate(int exp) {
        this.expRate = exp;
    }

    public int getDropRate() {
        return dropRate;
    }

    public void setDropRate(int drop) {
        this.dropRate = drop;
    }

    public int getMesoRate() {
        return mesoRate;
    }

    public void setMesoRate(int meso) {
        this.mesoRate = meso;
    }
    
    public int getCashRate() {
        return cashRate;
    }
    
    public void setCashRate(int cash) {
        this.cashRate = cash;
    }
    
    public int getTraitRate() {
        return traitRate;
    }
    
    public void setTraitRate(int trait) {
        this.traitRate = trait;
    }
    
    public int getTempFlag() {
        return flags;
    }
    
    // This will be disabled as according to original
    // source info it's for enabling/disabling of PQs. 
    // We will instead be using all PQs, even old ones.
    public void setTempFlag(int flag) {
        this.flags = flag;
    }
    
    public int getWorldId() {
        return id;
    }
    
    public String getWorldName() {
        return Server.getById(id).toString();
    }
    
    public int getUserLimit() {
        return userLimit;
    }

    public void setUserLimit(final int newLimit) {
        userLimit = newLimit;
    }
    
    //Touch everything...
    public static void init() {
        World.Find.findChannel(0);
        World.Find.findWorld(0);
        World.Alliance.lock.toString();
        World.Messenger.getMessenger(0);
        World.Party.getParty(0);
    }
    
    public static boolean getShutdown() {
        return Shutdown;
    }
    
    public static void Shutdown(boolean isgoing) {
        Shutdown = isgoing;
    }
    
    public static boolean getMonsterRushStatus() {
        return Monster_Rush_Enabled;
    }
    
    public static void setMonsterRushStatus(boolean status) {
        Monster_Rush_Enabled = status;
    }

    public static String getStatus() {
        StringBuilder ret = new StringBuilder();
        int totalUsers = 0;
        for (World worlds : LoginServer.getWorlds()) {
            for (ChannelServer cs : worlds.getChannels()) {
                ret.append("World ");
                ret.append(worlds.getWorldId());
                ret.append(": ");
                ret.append("Channel ");
                ret.append(cs.getChannel());
                ret.append(": ");
                int channelUsers = cs.getConnectedClients();
                totalUsers += channelUsers;
                ret.append(channelUsers);
                ret.append(" users\n");
            }
        }
        ret.append("Total users online: ");
        ret.append(totalUsers);
        ret.append("\n");
        return ret.toString();
    }

    public static Map<Integer, Integer> getConnected() {
        Map<Integer, Integer> ret = new HashMap<>();
        int total = 0;
        for (World worlds : LoginServer.getWorlds()) {
            for (ChannelServer cs : worlds.getChannels()) {
                int curConnected = cs.getConnectedClients();
                ret.put(cs.getChannel(), curConnected);
                total += curConnected;
            }
        }
        ret.put(0, total);
        return ret;
    }
    
    public static void saveAllChars() {
        MapleCharacter.saveAllChars();
    }

    public static boolean isConnected(String charName) {
        return Find.findChannel(charName) > 0;
    }

    public static void toggleMegaphoneMuteState(int world) {
        for (ChannelServer cs : LoginServer.getInstance().getWorld(world).getChannels()) {
            cs.toggleMegaphoneMuteState();
        }
    }

    public static void ChannelChange_Data(CharacterTransfer Data, int characterid, int world, int toChannel) {
        getStorage(world, toChannel).registerPendingPlayer(Data, characterid);
    }

    public static boolean isCharacterListConnected(List<String> charName) {
        for (World worlds : LoginServer.getWorlds()) {
            for (ChannelServer cs : worlds.getChannels()) {
                for (String c : charName) {
                    if (cs.getPlayerStorage().getCharacterByName(c) != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasMerchant(int accountID, int characterID) {
        for (World worlds : LoginServer.getWorlds()) {
            PlayerStorage strg = worlds.getPlayerStorage();
            MapleCharacter chr = strg.getCharacterById(characterID);
            int world = chr.getClient().getWorld();
            for (ChannelServer cs : LoginServer.getInstance().getWorld(world).getChannels()) {
                if (cs.containsMerchant(accountID, characterID)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static PlayerStorage getStorage(int world, int channel) {
        if (channel == -20) {
            return CashShopServer.getPlayerStorageMTS();
        } else if (channel == 30) {
            return CashShopServer.getPlayerStorage();
        }
        return ChannelServer.getInstance(world, channel).getPlayerStorage();
    }

    public static int getPendingCharacterSize() {
        int ret = CashShopServer.getPlayerStorage().pendingCharacterSize() + CashShopServer.getPlayerStorageMTS().pendingCharacterSize();
        for (World worlds : LoginServer.getWorlds()) {
            for (ChannelServer cserv : worlds.getChannels()) {
                ret += cserv.getPlayerStorage().pendingCharacterSize();
            }
        }
        return ret;
    }

    public static boolean isChannelAvailable(final int wl, final int ch) {
        if (ChannelServer.getInstance(wl, ch) == null || ChannelServer.getInstance(wl, ch).getPlayerStorage() == null) {
            System.out.println("null");
            return false;
        }
        return ChannelServer.getInstance(wl, ch).getPlayerStorage().getConnectedClients() < (ch == 1 ? 600 : 400);
    }

/*      */   public static void addToWheelCache(int cid, int itemId)
/*      */   {
/* 1762 */     magicWheelCache.put(Integer.valueOf(cid), Integer.valueOf(itemId));
/*      */   }
/*      */ 
/*      */   public static int removeFromWheelCache(int cid) {
/* 1766 */     return ((Integer)magicWheelCache.remove(Integer.valueOf(cid))).intValue();
/*      */   }
/*      */ 
/*      */   public static boolean hasWheelCache(int cid) {
/* 1770 */     return magicWheelCache.containsKey(Integer.valueOf(cid));
/*      */   }

        public static boolean isCSConnected(List<Integer> charIds) {
            for (int c : charIds) {
                if (CashShopServer.getPlayerStorage().getPendingCharacter(c) != null) {
                    return true;
                }
            }
            return false;
        }
        
        public static boolean isCSConnected(Integer charId) {
            if (CashShopServer.getPlayerStorage().getPendingCharacter(charId) != null) {
                return true;
            }
            return false;
        }
        
        public static void setEventMap(int map) {
        eventMap = map;
    }
    
    public static int getEventMap() {
        return eventMap;
    }
    
    public static void setEventOn(boolean onoff) {
        eventOn = onoff;
    }
    
    public static boolean getEventOn() {
        return eventOn;
    }

    public static boolean MonsterRushOn() {
        return MonsterRush;
    }

    public static int getPvpState() {
        return pvpState;
    }
    
    public static void setPvpState(int state) {
        pvpState = state;
    }

    public static void setJQChannel(int channel) {
        AutoJQ_Channel = channel;
    }
    
    public static int getJQChannel() {
        return AutoJQ_Channel;
    }

    public static long getLastPresident() {
        return lastPresident;
    }

    public static long setLastPresident() {
        return lastPresident = System.currentTimeMillis() / 60000;
    }
    
    public void setMonsterRushOn(boolean onoff) {
        MonsterRush = onoff;
    }
    
    public static class AutoJQ {
    private static AutoJQ instance = null;
    private boolean autojq = false;
    private static boolean autojqOn = false;
    private static int autojqWaitingMap = 109060001;
    
     public boolean getAutoJQ() {
        return autojq;
     }
     
     public static int getWaitingMap() {
        return autojqWaitingMap;
    }
    
     public synchronized static AutoJQ getInstance() {
        if (instance == null) {
            instance = new AutoJQ();
        }
        return instance;
     }
     
     public static boolean getAutoJQStatus() {
         return autojqOn;
     }
    
    public void openAutoJQ() {
        autojq = true;
        
        EventTimer.getInstance().schedule(new Runnable(){
            @Override
            public void run() {
                for (MapleCharacter chr : getAllCharacters()) {
                  if (chr.getMapId() == 109060001) {
                   if (getEventMap() == 0) {
                    chr.getClient().getSession().write(CWvsContext.clearMidMsg());
                    chr.changeMap(100000000); 
                    setEventOn(false);
                    autojqOn = true;
                 } else {
                    chr.getClient().getSession().write(CWvsContext.clearMidMsg());
                    chr.changeMap(getEventMap()); 
                    chr.dropMessage(-1, "The Automatic Jump Quest has started!");
                    setEventOn(false);
                    autojqOn = true;
                    }
                }
            }
            autojq = false;
            EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                for (MapleCharacter chr : getAllCharacters()) {
                if (chr.getMapId() == getEventMap()) { 
                    chr.changeMap(910000000);
                    autojqOn = false;
                }
              } 
            } 
            }, Long.MAX_VALUE); 
            }
        }, 60000); 
    }
    }

    public static class Party {

        private static Map<Integer, MapleParty> parties = new HashMap<>();
        private static Map<Integer, MapleExpedition> expeds = new HashMap<>();
        private static Map<PartySearchType, List<PartySearch>> searches = new EnumMap<>(PartySearchType.class);
        private static final AtomicInteger runningPartyId = new AtomicInteger(1), runningExpedId = new AtomicInteger(1);

        static {
            try {
                try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE characters SET party = -1, fatigue = 0")) {
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
            }
            for (PartySearchType pst : PartySearchType.values()) {
                searches.put(pst, new ArrayList<PartySearch>()); //according to client, max 10, even though theres page numbers ?!
            }
        }

        public static void partyChat(int partyid, String chattext, String namefrom) {
            partyChat(partyid, chattext, namefrom, 1);
        }

        public static void expedChat(int expedId, String chattext, String namefrom) {
            MapleExpedition party = getExped(expedId);
            if (party == null) {
                return;
            }
            for (int i : party.getParties()) {
                partyChat(i, chattext, namefrom, 4);
            }
        }

        public static void expedPacket(int expedId, byte[] packet, MaplePartyCharacter exception) {
            MapleExpedition party = getExped(expedId);
            if (party == null) {
                return;
            }
            for (int i : party.getParties()) {
                partyPacket(i, packet, exception);
            }
        }

        public static void partyPacket(int partyid, byte[] packet, MaplePartyCharacter exception) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return;
            }

            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                int wl = Find.findWorld(partychar.getName());
                if (ch > 0 && (exception == null || partychar.getId() != exception.getId())) {
                    MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null) { //Extra check just in case
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        }

        public static void partyChat(int partyid, String chattext, String namefrom, int mode) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return;
            }

            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                int wl = Find.findWorld(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null && !chr.getName().equalsIgnoreCase(namefrom)) { //Extra check just in case
                        chr.getClient().getSession().write(CField.multiChat(namefrom, chattext, mode));
                    }
                }
            }
        }

        public static void partyMessage(int partyid, String chattext) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return;
            }

            for (MaplePartyCharacter partychar : party.getMembers()) {
                int ch = Find.findChannel(partychar.getName());
                int wl = Find.findWorld(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(partychar.getName());
                    if (chr != null) { //Extra check just in case
                        chr.dropMessage(5, chattext);
                    }
                }
            }
        }

        public static void expedMessage(int expedId, String chattext) {
            MapleExpedition party = getExped(expedId);
            if (party == null) {
                return;
            }
            for (int i : party.getParties()) {
                partyMessage(i, chattext);
            }
        }

        public static void updateParty(int partyid, PartyOperation operation, MaplePartyCharacter target) {
            MapleParty party = getParty(partyid);
            if (party == null) {
                return; //Don't update, just return. And definitely don't throw a damn exception.
                //throw new IllegalArgumentException("no party with the specified partyid exists");
            }
            final int oldExped = party.getExpeditionId();
            int oldInd = -1;
            if (oldExped > 0) {
                MapleExpedition exped = getExped(oldExped);
                if (exped != null) {
                    oldInd = exped.getIndex(partyid);
                }
            }
            switch (operation) {
                case JOIN:
                    party.addMember(target);
                    if (party.getMembers().size() >= 6) {
                        PartySearch toRemove = getSearchByParty(partyid);
                        if (toRemove != null) {
                            removeSearch(toRemove, "The Party Listing was removed because the party is full.");
                        } else if (party.getExpeditionId() > 0) {
                            MapleExpedition exped = getExped(party.getExpeditionId());
                            if (exped != null && exped.getAllMembers() >= exped.getType().maxMembers) {
                                toRemove = getSearchByExped(exped.getId());
                                if (toRemove != null) {
                                    removeSearch(toRemove, "The Party Listing was removed because the party is full.");
                                }
                            }
                        }
                    }
                    break;
                case EXPEL:
                case LEAVE:
                    party.removeMember(target);
                    break;
                case DISBAND:
                    disbandParty(partyid);
                    break;
                case SILENT_UPDATE:
                case LOG_ONOFF:
                    party.updateMember(target);
                    break;
                case CHANGE_LEADER:
                case CHANGE_LEADER_DC:
                    party.setLeader(target);
                    break;
                default:
                    throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
            }
            if (operation == PartyOperation.LEAVE || operation == PartyOperation.EXPEL) {
                int chz = Find.findChannel(target.getName());
                int wlz = Find.findWorld(target.getName());
                if (chz > 0) {
                    MapleCharacter chr = getStorage(wlz, chz).getCharacterByName(target.getName());
                    if (chr != null) {
                        chr.setParty(null);
                        if (oldExped > 0) {
                            chr.getClient().getSession().write(ExpeditionPacket.expeditionMessage(80));
                        }
                        chr.getClient().getSession().write(PartyPacket.updateParty(chr.getClient().getChannel(), party, operation, target));
                    }
                }
                if (target.getId() == party.getLeader().getId() && party.getMembers().size() > 0) { //pass on lead
                    MaplePartyCharacter lchr = null;
                    for (MaplePartyCharacter pchr : party.getMembers()) {
                        if (pchr != null && (lchr == null || lchr.getLevel() < pchr.getLevel())) {
                            lchr = pchr;
                        }
                    }
                    if (lchr != null) {
                        updateParty(partyid, PartyOperation.CHANGE_LEADER_DC, lchr);
                    }
                }
            }
            if (party.getMembers().size() <= 0) { //no members left, plz disband
                disbandParty(partyid);
            }
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar == null) {
                    continue;
                }
                int ch = Find.findChannel(partychar.getName());
                int wl = Find.findWorld(partychar.getName());
                if (ch > 0) {
                    MapleCharacter chr = getStorage(wl, ch).getCharacterByName(partychar.getName());
                    if (chr != null) {
                        if (operation == PartyOperation.DISBAND) {
                            chr.setParty(null);
                            if (oldExped > 0) {
                                chr.getClient().getSession().write(ExpeditionPacket.expeditionMessage(83));
                            }
                        } else {
                            chr.setParty(party);
                        }
                        chr.getClient().getSession().write(PartyPacket.updateParty(chr.getClient().getChannel(), party, operation, target));
                    }
                }
            }
            if (oldExped > 0) {
                expedPacket(oldExped, ExpeditionPacket.expeditionUpdate(oldInd, party), operation == PartyOperation.LOG_ONOFF || operation == PartyOperation.SILENT_UPDATE ? target : null);
            }
        }

        public static MapleParty createParty(MaplePartyCharacter chrfor) {
            MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor);
            parties.put(party.getId(), party);
            return party;
        }

        public static MapleParty createParty(MaplePartyCharacter chrfor, int expedId) {
            ExpeditionType ex = ExpeditionType.getById(expedId);
            MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor, ex != null ? runningExpedId.getAndIncrement() : -1);
            parties.put(party.getId(), party);
            if (ex != null) {
                final MapleExpedition exp = new MapleExpedition(ex, chrfor.getId(), party.getExpeditionId());
                exp.getParties().add(party.getId());
                expeds.put(party.getExpeditionId(), exp);
            }
            return party;
        }

        public static MapleParty createPartyAndAdd(MaplePartyCharacter chrfor, int expedId) {
            MapleExpedition ex = getExped(expedId);
            if (ex == null) {
                return null;
            }
            MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor, expedId);
            parties.put(party.getId(), party);
            ex.getParties().add(party.getId());
            return party;
        }

        public static MapleParty getParty(int partyid) {
            return parties.get(partyid);
        }

        public static MapleExpedition getExped(int partyid) {
            return expeds.get(partyid);
        }

        public static MapleExpedition disbandExped(int partyid) {
            PartySearch toRemove = getSearchByExped(partyid);
            if (toRemove != null) {
                removeSearch(toRemove, "The Party Listing was removed because the party disbanded.");
            }
            final MapleExpedition ret = expeds.remove(partyid);
            if (ret != null) {
                for (int p : ret.getParties()) {
                    MapleParty pp = getParty(p);
                    if (pp != null) {
                        updateParty(p, PartyOperation.DISBAND, pp.getLeader());
                    }
                }
            }
            return ret;
        }

        public static MapleParty disbandParty(int partyid) {
            PartySearch toRemove = getSearchByParty(partyid);
            if (toRemove != null) {
                removeSearch(toRemove, "The Party Listing was removed because the party disbanded.");
            }
            final MapleParty ret = parties.remove(partyid);
            if (ret == null) {
                return null;
            }
            if (ret.getExpeditionId() > 0) {
                MapleExpedition me = getExped(ret.getExpeditionId());
                if (me != null) {
                    final int ind = me.getIndex(partyid);
                    if (ind >= 0) {
                        me.getParties().remove(ind);
                        expedPacket(me.getId(), ExpeditionPacket.expeditionUpdate(ind, null), null);
                    }
                }
            }
            ret.disband();
            return ret;
        }

        public static List<PartySearch> searchParty(PartySearchType pst) {
            return searches.get(pst);
        }

        public static void removeSearch(PartySearch ps, String text) {
            List<PartySearch> ss = searches.get(ps.getType());
            if (ss.contains(ps)) {
                ss.remove(ps);
                ps.cancelRemoval();
                if (ps.getType().exped) {
                    expedMessage(ps.getId(), text);
                } else {
                    partyMessage(ps.getId(), text);
                }
            }
        }

        public static void addSearch(PartySearch ps) {
            searches.get(ps.getType()).add(ps);
        }

        public static PartySearch getSearch(MapleParty party) {
            for (List<PartySearch> ps : searches.values()) {
                for (PartySearch p : ps) {
                    if ((p.getId() == party.getId() && !p.getType().exped) || (p.getId() == party.getExpeditionId() && p.getType().exped)) {
                        return p;
                    }
                }
            }
            return null;
        }

        public static PartySearch getSearchByParty(int partyId) {
            for (List<PartySearch> ps : searches.values()) {
                for (PartySearch p : ps) {
                    if (p.getId() == partyId && !p.getType().exped) {
                        return p;
                    }
                }
            }
            return null;
        }

        public static PartySearch getSearchByExped(int partyId) {
            for (List<PartySearch> ps : searches.values()) {
                for (PartySearch p : ps) {
                    if (p.getId() == partyId && p.getType().exped) {
                        return p;
                    }
                }
            }
            return null;
        }

        public static boolean partyListed(MapleParty party) {
            return getSearchByParty(party.getId()) != null;
        }
    }

    public static class Buddy {

        public static void buddyChat(int[] recipientCharacterIds, int cidFrom, String nameFrom, String chattext) {
            for (int characterId : recipientCharacterIds) {
                int ch = Find.findChannel(characterId);
                int wl = Find.findWorld(characterId);
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(characterId);
                    if (chr != null && chr.getBuddylist().containsVisible(cidFrom)) {
                        chr.getClient().getSession().write(CField.multiChat(nameFrom, chattext, 0));
                    }
                }
            }
        }

        private static void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
            for (int buddy : buddies) {
                int ch = Find.findChannel(buddy);
                int wl = Find.findWorld(buddy);
                if (ch > 0) {
                    MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(buddy);
                    if (chr != null) {
                        BuddylistEntry ble = chr.getBuddylist().get(characterId);
                        if (ble != null && ble.isVisible()) {
                            int mcChannel;
                            if (offline) {
                                ble.setChannel(-1);
                                mcChannel = -1;
                            } else {
                                ble.setChannel(channel);
                                mcChannel = channel - 1;
                            }
                            chr.getClient().getSession().write(BuddylistPacket.updateBuddyChannel(ble.getCharacterId(), mcChannel));
                        }
                    }
                }
            }
        }

        public static void buddyChanged(int cid, int cidFrom, String name, int channel, BuddyOperation operation, String group) {
            int ch = Find.findChannel(cid);
            int wl = Find.findWorld(cid);
            if (ch > 0) {
                final MapleCharacter addChar = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(cid);
                if (addChar != null) {
                    final BuddyList buddylist = addChar.getBuddylist();
                    switch (operation) {
                        case ADDED:
                            if (buddylist.contains(cidFrom)) {
                                buddylist.put(new BuddylistEntry(name, cidFrom, group, channel, true));
                                addChar.getClient().getSession().write(BuddylistPacket.updateBuddyChannel(cidFrom, channel - 1));
                            }
                            break;
                        case DELETED:
                            if (buddylist.contains(cidFrom)) {
                                buddylist.put(new BuddylistEntry(name, cidFrom, group, -1, buddylist.get(cidFrom).isVisible()));
                                addChar.getClient().getSession().write(BuddylistPacket.updateBuddyChannel(cidFrom, -1));
                            }
                            break;
                    }
                }
            }
        }

        public static BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int cidFrom, String nameFrom, int levelFrom, int jobFrom) {
            int ch = Find.findChannel(cidFrom);
            int wl = Find.findWorld(cidFrom);
            if (ch > 0) {
                final MapleCharacter addChar = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(addName);
                if (addChar != null) {
                    final BuddyList buddylist = addChar.getBuddylist();
                    if (buddylist.isFull()) {
                        return BuddyAddResult.BUDDYLIST_FULL;
                    }
                    if (!buddylist.contains(cidFrom)) {
                        buddylist.addBuddyRequest(addChar.getClient(), cidFrom, nameFrom, channelFrom, levelFrom, jobFrom);
                    } else {
                        if (buddylist.containsVisible(cidFrom)) {
                            return BuddyAddResult.ALREADY_ON_LIST;
                        }
                    }
                }
            }
            return BuddyAddResult.OK;
        }

        public static void loggedOn(String name, int characterId, int channel, int[] buddies) {
            updateBuddies(characterId, channel, buddies, false);
        }

        public static void loggedOff(String name, int characterId, int channel, int[] buddies) {
            updateBuddies(characterId, channel, buddies, true);
        }
    }

    public static class Messenger {

        private static Map<Integer, MapleMessenger> messengers = new HashMap<>();
        private static final AtomicInteger runningMessengerId = new AtomicInteger();

        static {
            runningMessengerId.set(1);
        }

        public static MapleMessenger createMessenger(MapleMessengerCharacter chrfor) {
            int messengerid = runningMessengerId.getAndIncrement();
            MapleMessenger messenger = new MapleMessenger(messengerid, chrfor);
            messengers.put(messenger.getId(), messenger);
            return messenger;
        }

        public static void declineChat(String target, String namefrom) {
            int ch = Find.findChannel(target);
            int wl = Find.findWorld(target);
            if (ch > 0) {
                ChannelServer cs = ChannelServer.getInstance(wl, ch);
                MapleCharacter chr = cs.getPlayerStorage().getCharacterByName(target);
                if (chr != null) {
                    MapleMessenger messenger = chr.getMessenger();
                    if (messenger != null) {
                        chr.getClient().getSession().write(CField.messengerNote(namefrom, 5, 0));
                    }
                }
            }
        }

        public static MapleMessenger getMessenger(int messengerid) {
            return messengers.get(messengerid);
        }

        public static void leaveMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            int position = messenger.getPositionByName(target.getName());
            messenger.removeMember(target);

            for (MapleMessengerCharacter mmc : messenger.getMembers()) {
                if (mmc != null) {
                    int ch = Find.findChannel(mmc.getId());
                    int wl = Find.findWorld(mmc.getId());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(mmc.getName());
                        if (chr != null) {
                            chr.getClient().getSession().write(CField.removeMessengerPlayer(position));
                        }
                    }
                }
            }
        }

        public static void silentLeaveMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.silentRemoveMember(target);
        }

        public static void silentJoinMessenger(int messengerid, MapleMessengerCharacter target) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.silentAddMember(target);
        }

        public static void updateMessenger(int messengerid, String namefrom, int fromworld, int fromchannel) {
            MapleMessenger messenger = getMessenger(messengerid);
            int position = messenger.getPositionByName(namefrom);

            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null && !messengerchar.getName().equals(namefrom)) {
                    int ch = Find.findChannel(messengerchar.getName());
                    int wl = Find.findWorld(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            MapleCharacter from = ChannelServer.getInstance(fromworld, fromchannel).getPlayerStorage().getCharacterByName(namefrom);
                            chr.getClient().getSession().write(CField.updateMessengerPlayer(namefrom, from, position, fromchannel - 1));
                        }
                    }
                }
            }
        }

        public static void joinMessenger(int messengerid, MapleMessengerCharacter target, String from, int fromworld, int fromchannel) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }
            messenger.addMember(target);
            int position = messenger.getPositionByName(target.getName());
            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null) {
                    int mposition = messenger.getPositionByName(messengerchar.getName());
                    int ch = Find.findChannel(messengerchar.getName());
                    int wl = Find.findWorld(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            if (!messengerchar.getName().equals(from)) {
                                MapleCharacter fromCh = ChannelServer.getInstance(fromworld, fromchannel).getPlayerStorage().getCharacterByName(from);
                                if (fromCh != null) {
                                    chr.getClient().getSession().write(CField.addMessengerPlayer(from, fromCh, position, fromchannel - 1));
                                    fromCh.getClient().getSession().write(CField.addMessengerPlayer(chr.getName(), chr, mposition, messengerchar.getChannel() - 1));
                                }
                            } else {
                                chr.getClient().getSession().write(CField.joinMessenger(mposition));
                            }
                        }
                    }
                }
            }
        }

        public static void messengerChat(int messengerid, String charname, String text, String namefrom) {
            MapleMessenger messenger = getMessenger(messengerid);
            if (messenger == null) {
                throw new IllegalArgumentException("No messenger with the specified messengerid exists");
            }

            for (MapleMessengerCharacter messengerchar : messenger.getMembers()) {
                if (messengerchar != null && !messengerchar.getName().equals(namefrom)) {
                    int ch = Find.findChannel(messengerchar.getName());
                    int wl = Find.findWorld(messengerchar.getName());
                    if (ch > 0) {
                        MapleCharacter chr = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(messengerchar.getName());
                        if (chr != null) {
                            chr.getClient().getSession().write(CField.messengerChat(charname, text));
                        }
                    }
                }
            }
        }

        public static void messengerInvite(String sender, int messengerid, String target, int fromworld, int fromchannel, boolean gm) {

            if (isConnected(target)) {

                int ch = Find.findChannel(target);
                int wl = Find.findWorld(target);
                if (ch > 0) {
                    MapleCharacter from = ChannelServer.getInstance(fromworld, fromchannel).getPlayerStorage().getCharacterByName(sender);
                    MapleCharacter targeter = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterByName(target);
                    if (targeter != null && targeter.getMessenger() == null) {
                        if (!targeter.isIntern() || gm) {
                            targeter.getClient().getSession().write(CField.messengerInvite(sender, messengerid));
                            from.getClient().getSession().write(CField.messengerNote(target, 4, 1));
                        } else {
                            from.getClient().getSession().write(CField.messengerNote(target, 4, 0));
                        }
                    } else {
                        from.getClient().getSession().write(CField.messengerChat(sender, " : " + target + " is already using Maple Messenger"));
                    }
                }
            }

        }
    }

    public static class Guild {

        private static final Map<Integer, MapleGuild> guilds = new LinkedHashMap<>();
        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public static void addLoadedGuild(MapleGuild f) {
            if (f.isProper()) {
                guilds.put(f.getId(), f);
            }
        }

        public static int createGuild(int leaderId, String name) {
            return MapleGuild.createGuild(leaderId, name);
        }

        public static MapleGuild getGuild(int id) {
            MapleGuild ret = null;
            lock.readLock().lock();
            try {
                ret = guilds.get(id);
            } finally {
                lock.readLock().unlock();
            }
            if (ret == null) {
                lock.writeLock().lock();
                try {
                    ret = new MapleGuild(id);
                    if (ret == null || ret.getId() <= 0 || !ret.isProper()) { //failed to load
                        return null;
                    }
                    guilds.put(id, ret);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            return ret; //Guild doesn't exist?
        }

        public static MapleGuild getGuildByName(String guildName) {
            lock.readLock().lock();
            try {
                for (MapleGuild g : guilds.values()) {
                    if (g.getName().equalsIgnoreCase(guildName)) {
                        return g;
                    }
                }
                return null;
            } finally {
                lock.readLock().unlock();
            }
        }

        public static MapleGuild getGuild(MapleCharacter mc) {
            return getGuild(mc.getGuildId());
        }

        public static void setGuildMemberOnline(MapleGuildCharacter mc, boolean bOnline, int channel) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.setOnline(mc.getId(), bOnline, channel);
            }
        }

        public static void guildPacket(int gid, byte[] message) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.broadcast(message);
            }
        }

        public static int addGuildMember(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                return g.addGuildMember(mc);
            }
            return 0;
        }

        public static void leaveGuild(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.leaveGuild(mc);
            }
        }

        public static void guildChat(int gid, String name, int cid, String msg) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.guildChat(name, cid, msg);
            }
        }

        public static void changeRank(int gid, int cid, int newRank) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.changeRank(cid, newRank);
            }
        }

        public static void expelMember(MapleGuildCharacter initiator, String name, int cid) {
            MapleGuild g = getGuild(initiator.getGuildId());
            if (g != null) {
                g.expelMember(initiator, name, cid);
            }
        }

        public static void setGuildNotice(int gid, String notice) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.setGuildNotice(notice);
            }
        }

        public static void setGuildLeader(int gid, int cid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.changeGuildLeader(cid);
            }
        }

        public static int getSkillLevel(int gid, int sid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getSkillLevel(sid);
            }
            return 0;
        }

        public static boolean purchaseSkill(int gid, int sid, String name, int cid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.purchaseSkill(sid, name, cid);
            }
            return false;
        }

        public static boolean activateSkill(int gid, int sid, String name) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.activateSkill(sid, name);
            }
            return false;
        }

        public static void memberLevelJobUpdate(MapleGuildCharacter mc) {
            MapleGuild g = getGuild(mc.getGuildId());
            if (g != null) {
                g.memberLevelJobUpdate(mc);
            }
        }

        public static void changeRankTitle(int gid, String[] ranks) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.changeRankTitle(ranks);
            }
        }

        public static void setGuildEmblem(int gid, short bg, byte bgcolor, short logo, byte logocolor) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.setGuildEmblem(bg, bgcolor, logo, logocolor);
            }
        }

        public static void disbandGuild(int gid) {
            MapleGuild g = getGuild(gid);
            lock.writeLock().lock();
            try {
                if (g != null) {
                    g.disbandGuild();
                    guilds.remove(gid);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void deleteGuildCharacter(int guildid, int charid) {

            //ensure it's loaded on world server
            //setGuildMemberOnline(mc, false, -1);
            MapleGuild g = getGuild(guildid);
            if (g != null) {
                MapleGuildCharacter mc = g.getMGC(charid);
                if (mc != null) {
                    if (mc.getGuildRank() > 1) //not leader
                    {
                        g.leaveGuild(mc);
                    } else {
                        g.disbandGuild();
                    }
                }
            }
        }

        public static boolean increaseGuildCapacity(int gid, boolean b) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.increaseCapacity(b);
            }
            return false;
        }

        public static void gainGP(int gid, int amount) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.gainGP(amount);
            }
        }

        public static void gainGP(int gid, int amount, int cid) {
            MapleGuild g = getGuild(gid);
            if (g != null) {
                g.gainGP(amount, false, cid);
            }
        }

        public static int getGP(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getGP();
            }
            return 0;
        }

        public static int getInvitedId(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getInvitedId();
            }
            return 0;
        }

        public static void setInvitedId(final int gid, final int inviteid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                g.setInvitedId(inviteid);
            }
        }

        public static int getGuildLeader(final int guildName) {
            final MapleGuild mga = getGuild(guildName);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static int getGuildLeader(final String guildName) {
            final MapleGuild mga = getGuildByName(guildName);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static void save() {
            System.out.println("Saving guilds...");
            lock.writeLock().lock();
            try {
                for (MapleGuild a : guilds.values()) {
                    a.writeToDB(false);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static List<MapleBBSThread> getBBS(final int gid) {
            final MapleGuild g = getGuild(gid);
            if (g != null) {
                return g.getBBS();
            }
            return null;
        }

        public static int addBBSThread(final int guildid, final String title, final String text, final int icon, final boolean bNotice, final int posterID) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                return g.addBBSThread(title, text, icon, bNotice, posterID);
            }
            return -1;
        }

        public static void editBBSThread(final int guildid, final int localthreadid, final String title, final String text, final int icon, final int posterID, final int guildRank) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.editBBSThread(localthreadid, title, text, icon, posterID, guildRank);
            }
        }

        public static void deleteBBSThread(final int guildid, final int localthreadid, final int posterID, final int guildRank) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.deleteBBSThread(localthreadid, posterID, guildRank);
            }
        }

        public static void addBBSReply(final int guildid, final int localthreadid, final String text, final int posterID) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.addBBSReply(localthreadid, text, posterID);
            }
        }

        public static void deleteBBSReply(final int guildid, final int localthreadid, final int replyid, final int posterID, final int guildRank) {
            final MapleGuild g = getGuild(guildid);
            if (g != null) {
                g.deleteBBSReply(localthreadid, replyid, posterID, guildRank);
            }
        }

        public static void changeEmblem(int gid, int affectedPlayers, MapleGuild mgs) {
            Broadcast.sendGuildPacket(affectedPlayers, GuildPacket.guildEmblemChange(gid, (short) mgs.getLogoBG(), (byte) mgs.getLogoBGColor(), (short) mgs.getLogo(), (byte) mgs.getLogoColor()), -1, gid);
            setGuildAndRank(affectedPlayers, -1, -1, -1, -1);	//respawn player
        }

        public static void setGuildAndRank(int cid, int guildid, int rank, int contribution, int alliancerank) {
            int ch = Find.findChannel(cid);
            int wl = Find.findWorld(cid);
            if (ch == -1) {
                // System.out.println("ERROR: cannot find player in given channel");
                return;
            }
            MapleCharacter mc = getStorage(wl, ch).getCharacterById(cid);
            if (mc == null) {
                return;
            }
            boolean bDifferentGuild;
            if (guildid == -1 && rank == -1) { //just need a respawn
                bDifferentGuild = true;
            } else {
                bDifferentGuild = guildid != mc.getGuildId();
                mc.setGuildId(guildid);
                mc.setGuildRank((byte) rank);
                mc.setGuildContribution(contribution);
                mc.setAllianceRank((byte) alliancerank);
                mc.saveGuildStatus();
            }
            if (bDifferentGuild && ch > 0) {
                mc.getMap().broadcastMessage(mc, CField.loadGuildName(mc), false);
				mc.getMap().broadcastMessage(mc, CField.loadGuildIcon(mc), false);
            }
        }
    }

    public static class Broadcast {

        public static void broadcastSmega(int world, byte[] message) {
            for (MapleCharacter chr : players.getAllCharacters()) {
                if ((world == -1) || (chr.getWorld() == world)) {
                    chr.getClient().getChannelServer().broadcastSmega(message);
                }
            }
        }

        public static void broadcastGMMessage(int world, byte[] message) {
            for (MapleCharacter chr : players.getAllCharacters()) {
                if ((world == -1) || (chr.getWorld() == world)) {
                    chr.getClient().getChannelServer().broadcastGMPacket(message);
                }
            }
        }

        public static void broadcastMessage(int world, byte[] message) {
            for (MapleCharacter chr : players.getAllCharacters()) {
                if ((world == -1) || (chr.getWorld() == world)) {
                    chr.announce(message);
                }
            }
        }

        public static void sendPacket(List<Integer> targetIds, byte[] packet, int exception) {
            MapleCharacter c;
            for (int i : targetIds) {
                if (i == exception) {
                    continue;
                }
                int ch = Find.findChannel(i);
                int wl = Find.findWorld(i);
                if (ch < 0) {
                    continue;
                }
                c = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(i);
                if (c != null) {
                    c.getClient().getSession().write(packet);
                }
            }
        }

        public static void sendPacket(int targetId, byte[] packet) {
            int ch = Find.findChannel(targetId);
            int wl = Find.findWorld(targetId);
            if (ch < 0) {
                return;
            }
            final MapleCharacter c = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(targetId);
            if (c != null) {
                c.getClient().getSession().write(packet);
            }
        }

        public static void sendGuildPacket(int targetIds, byte[] packet, int exception, int guildid) {
            if (targetIds == exception) {
                return;
            }
            int ch = Find.findChannel(targetIds);
            int wl = Find.findWorld(targetIds);
            if (ch < 0) {
                return;
            }
            final MapleCharacter c = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(targetIds);
            if (c != null && c.getGuildId() == guildid) {
                c.getClient().getSession().write(packet);
            }
        }

        public static void sendFamilyPacket(int targetIds, byte[] packet, int exception, int guildid) {
            if (targetIds == exception) {
                return;
            }
            int ch = Find.findChannel(targetIds);
            int wl = Find.findWorld(targetIds);
            if (ch < 0) {
                return;
            }
            final MapleCharacter c = ChannelServer.getInstance(wl, ch).getPlayerStorage().getCharacterById(targetIds);
            if (c != null && c.getFamilyId() == guildid) {
                c.getClient().getSession().write(packet);
            }
        }
    }

    public static class Find {

        private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        // Channel
        private static HashMap<Integer, Integer> idToChannel = new HashMap<>();
        private static HashMap<String, Integer> nameToChannel = new HashMap<>();
        // World
        private static HashMap<Integer, Integer> idToWorld = new HashMap<>();
        private static HashMap<String, Integer> nameToWorld = new HashMap<>();

        public static void register(int id, String name, int world, int channel) {
            lock.writeLock().lock();
            try {
                idToChannel.put(id, channel);
                nameToChannel.put(name.toLowerCase(), channel);
                
                idToWorld.put(id, world);
                nameToWorld.put(name.toLowerCase(), world);
            } finally {
                lock.writeLock().unlock();
            }
            // System.out.println("Char added: " + id + " " + name + " to world " + world + " channel " + channel);
        }

        public static void forceDeregister(int id) {
            lock.writeLock().lock();
            try {
                idToChannel.remove(id);
                idToWorld.remove(id);
            } finally {
                lock.writeLock().unlock();
            }
            //System.out.println("Char removed: " + id);
        }

        public static void forceDeregister(String id) {
            lock.writeLock().lock();
            try {
                nameToChannel.remove(id.toLowerCase());
                nameToWorld.remove(id.toLowerCase());
            } finally {
                lock.writeLock().unlock();
            }
            //System.out.println("Char removed: " + id);
        }

        public static void forceDeregister(int id, String name) {
            lock.writeLock().lock();
            try {
                idToChannel.remove(id);
                idToWorld.remove(id);
                nameToChannel.remove(name.toLowerCase());
                nameToWorld.remove(name.toLowerCase());
            } finally {
                lock.writeLock().unlock();
            }
            //System.out.println("Char removed: " + id + " " + name);
        }

        public static int findChannel(int id) {
            Integer ret;
            Integer ret_;
            lock.readLock().lock();
            try {
                ret = idToChannel.get(id);
                ret_ = idToWorld.get(id); // get the world's channel :)
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null && ret_ != null) {
                if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret_, ret) == null) { //wha
                    forceDeregister(id);
                    return -1;
                }
                return ret;
            }
            return -1;
        }
        
        public static int findWorld(int id) {
            Integer ret;
            Integer ret_;
            lock.readLock().lock();
            try {
                ret = idToChannel.get(id);
                ret_ = idToWorld.get(id); // get the world's channel :)
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null && ret_ != null) {
                if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret_, ret) == null) { //wha
                    forceDeregister(id);
                    return -1;
                }
                return ret_;
            }
            return -1;
        }

        public static int findChannel(String st) {
            Integer ret;
            Integer ret_;
            lock.readLock().lock();
            try {
                ret = nameToChannel.get(st.toLowerCase());
                ret_ = nameToWorld.get(st.toLowerCase());
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null && ret_ != null) {
                if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret_, ret) == null) { //wha
                    forceDeregister(st);
                    return -1;
                }
                return ret;
            }
            return -1;
        }
        
        public static int findWorld(String st) {
            Integer ret;
            Integer ret_;
            lock.readLock().lock();
            try {
                ret = nameToChannel.get(st.toLowerCase());
                ret_ = nameToWorld.get(st.toLowerCase());
            } finally {
                lock.readLock().unlock();
            }
            if (ret != null && ret_ != null) {
                if (ret != -10 && ret != -20 && ChannelServer.getInstance(ret_, ret) == null) { //wha
                    forceDeregister(st);
                    return -1;
                }
                return ret_;
            }
            return -1;
        }

        public static CharacterIdChannelPair[] multiBuddyFind(int charIdFrom, int[] characterIds) {
            List<CharacterIdChannelPair> foundsChars = new ArrayList<>(characterIds.length);
            for (int i : characterIds) {
                int channel = findChannel(i);
                if (channel > 0) {
                    foundsChars.add(new CharacterIdChannelPair(i, channel));
                }
            }
            Collections.sort(foundsChars);
            return foundsChars.toArray(new CharacterIdChannelPair[foundsChars.size()]);
        }
    }

    public static class Alliance {

        private static final Map<Integer, MapleGuildAlliance> alliances = new LinkedHashMap<>();
        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        static {
            Collection<MapleGuildAlliance> allGuilds = MapleGuildAlliance.loadAll();
            for (MapleGuildAlliance g : allGuilds) {
                alliances.put(g.getId(), g);
            }
        }

        public static MapleGuildAlliance getAlliance(final int allianceid) {
            MapleGuildAlliance ret = null;
            lock.readLock().lock();
            try {
                ret = alliances.get(allianceid);
            } finally {
                lock.readLock().unlock();
            }
            if (ret == null) {
                lock.writeLock().lock();
                try {
                    ret = new MapleGuildAlliance(allianceid);
                    if (ret == null || ret.getId() <= 0) { //failed to load
                        return null;
                    }
                    alliances.put(allianceid, ret);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            return ret;
        }

        public static int getAllianceLeader(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.getLeaderId();
            }
            return 0;
        }

        public static void updateAllianceRanks(final int allianceid, final String[] ranks) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                mga.setRank(ranks);
            }
        }

        public static void updateAllianceNotice(final int allianceid, final String notice) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                mga.setNotice(notice);
            }
        }

        public static boolean canInvite(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.getCapacity() > mga.getNoGuilds();
            }
            return false;
        }

        public static boolean changeAllianceLeader(final int allianceid, final int cid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setLeaderId(cid);
            }
            return false;
        }

        public static boolean changeAllianceLeader(final int allianceid, final int cid, final boolean sameGuild) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setLeaderId(cid, sameGuild);
            }
            return false;
        }

        public static boolean changeAllianceRank(final int allianceid, final int cid, final int change) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.changeAllianceRank(cid, change);
            }
            return false;
        }

        public static boolean changeAllianceCapacity(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.setCapacity();
            }
            return false;
        }

        public static boolean disbandAlliance(final int allianceid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.disband();
            }
            return false;
        }

        public static boolean addGuildToAlliance(final int allianceid, final int gid) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.addGuild(gid);
            }
            return false;
        }

        public static boolean removeGuildFromAlliance(final int allianceid, final int gid, final boolean expelled) {
            final MapleGuildAlliance mga = getAlliance(allianceid);
            if (mga != null) {
                return mga.removeGuild(gid, expelled);
            }
            return false;
        }

        public static void sendGuild(final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                sendGuild(AlliancePacket.getAllianceUpdate(alliance), -1, allianceid);
                sendGuild(AlliancePacket.getGuildAlliance(alliance), -1, allianceid);
            }
        }

        public static void sendGuild(final byte[] packet, final int exceptionId, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    int gid = alliance.getGuildId(i);
                    if (gid > 0 && gid != exceptionId) {
                        Guild.guildPacket(gid, packet);
                    }
                }
            }
        }

        public static boolean createAlliance(final String alliancename, final int cid, final int cid2, final int gid, final int gid2) {
            final int allianceid = MapleGuildAlliance.createToDb(cid, alliancename, gid, gid2);
            if (allianceid <= 0) {
                return false;
            }
            final MapleGuild g = Guild.getGuild(gid), g_ = Guild.getGuild(gid2);
            g.setAllianceId(allianceid);
            g_.setAllianceId(allianceid);
            g.changeARank(true);
            g_.changeARank(false);

            final MapleGuildAlliance alliance = getAlliance(allianceid);

            sendGuild(AlliancePacket.createGuildAlliance(alliance), -1, allianceid);
            sendGuild(AlliancePacket.getAllianceInfo(alliance), -1, allianceid);
            sendGuild(AlliancePacket.getGuildAlliance(alliance), -1, allianceid);
            sendGuild(AlliancePacket.changeAlliance(alliance, true), -1, allianceid);
            return true;
        }

        public static void allianceChat(final int gid, final String name, final int cid, final String msg) {
            final MapleGuild g = Guild.getGuild(gid);
            if (g != null) {
                final MapleGuildAlliance ga = getAlliance(g.getAllianceId());
                if (ga != null) {
                    for (int i = 0; i < ga.getNoGuilds(); i++) {
                        final MapleGuild g_ = Guild.getGuild(ga.getGuildId(i));
                        if (g_ != null) {
                            g_.allianceChat(name, cid, msg);
                        }
                    }
                }
            }
        }

        public static void setNewAlliance(final int gid, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            final MapleGuild guild = Guild.getGuild(gid);
            if (alliance != null && guild != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    if (gid == alliance.getGuildId(i)) {
                        guild.setAllianceId(allianceid);
                        guild.broadcast(AlliancePacket.getAllianceInfo(alliance));
                        guild.broadcast(AlliancePacket.getGuildAlliance(alliance));
                        guild.broadcast(AlliancePacket.changeAlliance(alliance, true));
                        guild.changeARank();
                        guild.writeToDB(false);
                    } else {
                        final MapleGuild g_ = Guild.getGuild(alliance.getGuildId(i));
                        if (g_ != null) {
                            g_.broadcast(AlliancePacket.addGuildToAlliance(alliance, guild));
                            g_.broadcast(AlliancePacket.changeGuildInAlliance(alliance, guild, true));
                        }
                    }
                }
            }
        }

        public static void setOldAlliance(final int gid, final boolean expelled, final int allianceid) {
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            final MapleGuild g_ = Guild.getGuild(gid);
            if (alliance != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    final MapleGuild guild = Guild.getGuild(alliance.getGuildId(i));
                    if (guild == null) {
                        if (gid != alliance.getGuildId(i)) {
                            alliance.removeGuild(gid, false, true);
                        }
                        continue; //just skip
                    }
                    if (g_ == null || gid == alliance.getGuildId(i)) {
                        guild.changeARank(5);
                        guild.setAllianceId(0);
                        guild.broadcast(AlliancePacket.disbandAlliance(allianceid));
                    } else if (g_ != null) {
                        guild.broadcast(CWvsContext.serverNotice(5, "[" + g_.getName() + "] Guild has left the alliance."));
                        guild.broadcast(AlliancePacket.changeGuildInAlliance(alliance, g_, false));
                        guild.broadcast(AlliancePacket.removeGuildFromAlliance(alliance, g_, expelled));
                    }

                }
            }

            if (gid == -1) {
                lock.writeLock().lock();
                try {
                    alliances.remove(allianceid);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        public static List<byte[]> getAllianceInfo(final int allianceid, final boolean start) {
            List<byte[]> ret = new ArrayList<>();
            final MapleGuildAlliance alliance = getAlliance(allianceid);
            if (alliance != null) {
                if (start) {
                    ret.add(AlliancePacket.getAllianceInfo(alliance));
                    ret.add(AlliancePacket.getGuildAlliance(alliance));
                }
                ret.add(AlliancePacket.getAllianceUpdate(alliance));
            }
            return ret;
        }

        public static void save() {
            System.out.println("Saving alliances...");
            lock.writeLock().lock();
            try {
                for (MapleGuildAlliance a : alliances.values()) {
                    a.saveToDb();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public static class Family {

        private static final Map<Integer, MapleFamily> families = new LinkedHashMap<>();
        private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public static void addLoadedFamily(MapleFamily f) {
            if (f.isProper()) {
                families.put(f.getId(), f);
            }
        }

        public static MapleFamily getFamily(int id) {
            MapleFamily ret = null;
            lock.readLock().lock();
            try {
                ret = families.get(id);
            } finally {
                lock.readLock().unlock();
            }
            if (ret == null) {
                lock.writeLock().lock();
                try {
                    ret = new MapleFamily(id);
                    if (ret == null || ret.getId() <= 0 || !ret.isProper()) { //failed to load
                        return null;
                    }
                    families.put(id, ret);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            return ret;
        }

        public static void memberFamilyUpdate(MapleFamilyCharacter mfc, MapleCharacter mc) {
            MapleFamily f = getFamily(mfc.getFamilyId());
            if (f != null) {
                f.memberLevelJobUpdate(mc);
            }
        }

        public static void setFamilyMemberOnline(MapleFamilyCharacter mfc, boolean bOnline, int channel) {
            MapleFamily f = getFamily(mfc.getFamilyId());
            if (f != null) {
                f.setOnline(mfc.getId(), bOnline, channel);
            }
        }

        public static int setRep(int fid, int cid, int addrep, int oldLevel, String oldName) {
            MapleFamily f = getFamily(fid);
            if (f != null) {
                return f.setRep(cid, addrep, oldLevel, oldName);
            }
            return 0;
        }

        public static void save() {
            System.out.println("Saving families...");
            lock.writeLock().lock();
            try {
                for (MapleFamily a : families.values()) {
                    a.writeToDB(false);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static void setFamily(int familyid, int seniorid, int junior1, int junior2, int currentrep, int totalrep, int cid) {
            int ch = Find.findChannel(cid);
            int wl = Find.findWorld(cid);
            if (ch == -1) {
                // System.out.println("ERROR: cannot find player in given channel");
                return;
            }
            MapleCharacter mc = getStorage(wl, ch).getCharacterById(cid);
            if (mc == null) {
                return;
            }
            boolean bDifferent = mc.getFamilyId() != familyid || mc.getSeniorId() != seniorid || mc.getJunior1() != junior1 || mc.getJunior2() != junior2;
            mc.setFamily(familyid, seniorid, junior1, junior2);
            mc.setCurrentRep(currentrep);
            mc.setTotalRep(totalrep);
            if (bDifferent) {
                mc.saveFamilyStatus();
            }
        }

        public static void familyPacket(int gid, byte[] message, int cid) {
            MapleFamily f = getFamily(gid);
            if (f != null) {
                f.broadcast(message, -1, f.getMFC(cid).getPedigree());
            }
        }

        public static void disbandFamily(int gid) {
            MapleFamily g = getFamily(gid);
            if (g != null) {
                lock.writeLock().lock();
                try {
                    families.remove(gid);
                } finally {
                    lock.writeLock().unlock();
                }
                g.disbandFamily();
            }
        }
    }
    private final static int CHANNELS_PER_THREAD = 3;

    public static void registerRespawn() {
        Integer[] chs = ChannelServer.getAllInstance().toArray(new Integer[0]);
        for (int i = 0; i < chs.length; i += CHANNELS_PER_THREAD) {
            WorldTimer.getInstance().register(new Respawn(chs, i), 4500); //divisible by 9000 if possible.
        }
        //3000 good or bad? ive no idea >_>
        //buffs can also be done, but eh
    }

    public static class Respawn implements Runnable { //is putting it here a good idea?

        private int numTimes = 0;
        private final List<ChannelServer> cservs = new ArrayList<>(CHANNELS_PER_THREAD);

        public Respawn(Integer[] chs, int c) {
            StringBuilder s = new StringBuilder("[Respawn Worker] Registered for channels ");
            for (int i = 1; i <= CHANNELS_PER_THREAD && chs.length >= (c + i); i++) {
                for (int z = 0; z < (WorldConstants.Worlds * WorldConstants.Channels) + 2; z++) {
                    switch(z) {
                        case 1:// do channels start from 0?
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            cservs.add(ChannelServer.getInstance(0, c + i));
                            break;
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                            cservs.add(ChannelServer.getInstance(1, c + i));
                            break;
                    }
                }
                s.append(c + i).append(" ");
            }
            System.out.println(s.toString());
        }

        @Override
        public void run() {
            numTimes++;
            long now = System.currentTimeMillis();
            for (ChannelServer cserv : cservs) {
                if (!cserv.hasFinishedShutdown()) {
                    for (MapleMap map : cserv.getMapFactory().getAllLoadedMaps()) { //iterating through each map o_x
                        handleMap(map, numTimes, map.getCharactersSize(), now);
                    }
                }
            }
        }
    }

    public static void handleMap(final MapleMap map, final int numTimes, final int size, final long now) {
        if (map.getItemsSize() > 0) {
            for (MapleMapItem item : map.getAllItemsThreadsafe()) {
                if (item.shouldExpire(now)) {
                    item.expire(map);
                } else if (item.shouldFFA(now)) {
                    item.setDropType((byte) 2);
                }
            }
        }
        if (map.characterSize() > 0 || map.getId() == 931000500) { //jaira hack
            if (map.canSpawn(now)) {
                map.respawn(false, now);
            }
            boolean hurt = map.canHurt(now);
            for (MapleCharacter chr : map.getCharactersThreadsafe()) {
                handleCooldowns(chr, numTimes, hurt, now);
            }
            if (map.getMobsSize() > 0) {
                for (MapleMonster mons : map.getAllMonstersThreadsafe()) {
                    if (mons.isAlive() && mons.shouldKill(now)) {
                        map.killMonster(mons);
                    } else if (mons.isAlive() && mons.shouldDrop(now)) {
                        mons.doDropItem(now);
                    } else if (mons.isAlive() && mons.getStatiSize() > 0) {
                        for (MonsterStatusEffect mse : mons.getAllBuffs()) {
                            if (mse.shouldCancel(now)) {
                                mons.cancelSingleStatus(mse);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void handleCooldowns(final MapleCharacter chr, final int numTimes, final boolean hurt, final long now) { //is putting it here a good idea? expensive?
        if (chr.getCooldownSize() > 0) {
            for (MapleCoolDownValueHolder m : chr.getCooldowns()) {
                if (m.startTime + m.length < now) {
                    final int skil = m.skillId;
                    chr.removeCooldown(skil);
                    chr.getClient().getSession().write(CField.skillCooldown(skil, 0));
                }
            }
        }
        if (chr.isAlive()) {
            if (chr.getJob() == 131 || chr.getJob() == 132) {
                if (chr.canBlood(now)) {
                    chr.doDragonBlood();
                }
            }
            if (chr.canRecover(now)) {
                chr.doRecovery();
            }
            if (chr.canHPRecover(now)) {
                chr.addHP((int) chr.getStat().getHealHP());
            }
            if (chr.canMPRecover(now)) {
                chr.addMP((int) chr.getStat().getHealMP());
            }
            if (chr.canFairy(now)) {
                chr.doFairy();
            }
            if (chr.canFish(now)) {
                chr.doFish(now);
            }
            if (chr.canDOT(now)) {
            	chr.doDOT();
            }
        }

        if (chr.getDiseaseSize() > 0) {
            for (MapleDiseaseValueHolder m : chr.getAllDiseases()) {
                if (m != null && m.startTime + m.length < now) {
                    chr.dispelDebuff(m.disease);
                }
            }
        }
        if (numTimes % 7 == 0 && chr.getMount() != null && chr.getMount().canTire(now)) {
            chr.getMount().increaseFatigue();
        }
        if (numTimes % 13 == 0) { //we're parsing through the characters anyway (:
            chr.doFamiliarSchedule(now);
      /*      for (MaplePet pet : chr.getSummonedPets()) {
                if (pet.getPetItemId() == 5000054 && pet.getSecondsLeft() > 0) {
                    pet.setSecondsLeft(pet.getSecondsLeft() - 1);
                    if (pet.getSecondsLeft() <= 0) {
                        chr.unequipPet(pet, true, true);
                        return;
                    }
                }
                int newFullness = pet.getFullness() - PetDataFactory.getHunger(pet.getPetItemId());
                if (newFullness <= 5) {
                    pet.setFullness(15);
                    chr.unequipPet(pet, true, true);
                } else {
                    pet.setFullness(newFullness);
                    chr.getClient().getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition()), true));
                }
            }*/
        }
        if (hurt && chr.isAlive()) {
            if (chr.getInventory(MapleInventoryType.EQUIPPED).findById(chr.getMap().getHPDecProtect()) == null) {
                if (chr.getMapId() == 749040100 && chr.getInventory(MapleInventoryType.CASH).findById(5451000) == null) { //minidungeon
                    chr.addHP(-chr.getMap().getHPDec());
                } else if (chr.getMapId() != 749040100) {
                    chr.addHP(-(chr.getMap().getHPDec() - (chr.getBuffedValue(MapleBuffStat.HP_LOSS_GUARD) == null ? 0 : chr.getBuffedValue(MapleBuffStat.HP_LOSS_GUARD).intValue())));
                }
            }
        }
    }
    
    public static List<MapleCharacter> getAllCharacters() {
        List<MapleCharacter> chrlist = new ArrayList<>();
        for (World worlds : LoginServer.getWorlds()) {
            for (ChannelServer cs : worlds.getChannels()) {
                for (MapleCharacter chra : cs.getPlayerStorage().getAllCharacters()) {
                    chrlist.add(chra);
                }
            }
        }
        return chrlist;
    }
    
    public static List<MapleCharacter> getAllCharacters(int world) {
        List<MapleCharacter> chrlist = new ArrayList<>();
        for (ChannelServer cs : LoginServer.getInstance().getWorld(world).getChannels()) {
            for (MapleCharacter chra : cs.getPlayerStorage().getAllCharacters()) {
                chrlist.add(chra);
            }
        }
        return chrlist;
    }
}
