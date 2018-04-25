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
package client;

import client.MapleTrait.MapleTraitType;
import client.inventory.*;
import client.inventory.MapleImp.ImpFlag;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.Occupations;
import constants.ServerConstants;
import constants.ServerConstants.PlayerGMRank;
import database.DatabaseConnection;
import database.DatabaseException;
import handling.channel.ChannelServer;
import handling.channel.handler.NPCHandler;
import handling.login.LoginInformationProvider.JobType;
import handling.login.LoginServer;
import handling.world.*;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyBuff;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import scripting.EventInstanceManager;
import scripting.EventScriptManager;
import scripting.NPCConversationManager;
import scripting.NPCScriptManager;
import server.*;
import server.MapleStatEffect.CancelEffectAction;
import server.Timer.BuffTimer;
import server.Timer.EtcTimer;
import server.Timer.EventTimer;
import server.Timer.MapTimer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.life.OverrideMonsterStats;
import server.life.PlayerNPC;
import server.maps.*;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.IMaplePlayerShop;
import tools.ConcurrentEnumMap;
import tools.FileoutputUtil;
import tools.Pair;
import tools.StringUtil;
import tools.Triple;
import tools.packet.*;
import tools.packet.CField.EffectPacket;
import tools.packet.CField.SummonPacket;
import tools.packet.CField.UIPacket;
import tools.packet.CWvsContext.BuddylistPacket;
import tools.packet.CWvsContext.BuffPacket;
import tools.packet.CWvsContext.InfoPacket;
import tools.packet.CWvsContext.InventoryPacket;

public class MapleCharacter extends AnimatedMapleMapObject implements Serializable, MapleCharacterLook {

    private static final long serialVersionUID = 845748950829L;

    public static String getNameById(int id_) {
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		try {
			ps = con.prepareStatement("SELECT name FROM characters WHERE id = ? AND world = ?");
			ps.setInt(1, id_);
			ps.setInt(2, 0);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return "nobody";
			}
			String name = rs.getString("name");
			rs.close();
			ps.close();
			return name;
		} catch (SQLException e) {
			System.out.print("ERROR" + e);
		}
		return "nobody";
	}
    
    public static int getIdByName(String name, int world) {
		Connection con = DatabaseConnection.getConnection();
		PreparedStatement ps;
		try {
			ps = con.prepareStatement("SELECT id FROM characters WHERE name = ? AND world = ?");
			ps.setString(1, name);
			ps.setInt(2, world);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				rs.close();
				ps.close();
				return -1;
			}
			int id = rs.getInt("id");
			rs.close();
			ps.close();
			return id;
		} catch (SQLException e) {
			System.out.print("ERROR" + e);
		}
		return -1;
	}
    private String name, chalktext, BlessOfFairy_Origin, BlessOfEmpress_Origin, teleportname;
    private long lastCombo, lastfametime, keydown_skill, nextConsume, pqStartTime, lastDragonBloodTime,
            lastBerserkTime, lastRecoveryTime, lastSummonTime, mapChangeTime, lastFishingTime, lastFairyTime,
            lastHPTime, lastMPTime, lastFamiliarEffectTime, lastDOTTime;
    private byte gmLevel, gender, initialSpawnPoint, skinColor, guildrank = 5, allianceRank = 5,
            world, fairyExp, subcategory;
    private short level, mulung_energy, combo, force, availableCP, fatigue, totalCP, hpApUsed, job, scrolledPosition;
    private int accountid, id, meso, hair, face, demonMarking, mapid, fame, pvpPoints, totalWins, totalLosses,
            guildid = 0, fallcounter, maplepoints, nxprepaid, nxcredit, chair, itemEffect, points, vpoints,
            rank = 1, rankMove = 0, jobRank = 1, jobRankMove = 0, marriageId, marriageItemId, dotHP, pvpKills = 1, pvpDeaths = 1,
            currentrep, totalrep, coconutteam, followid, battleshipHP, gachexp, challenge, guildContribution = 0, remainingAp, redeemhn;
    public int pvpExp;
    private Point old;
    private AtomicInteger exp = new AtomicInteger();
    private MonsterFamiliar summonedFamiliar;
    private int[] wishlist, rocks, savedLocations, regrocks, hyperrocks, remainingSp = new int[10];
    private transient AtomicInteger inst, insd;
    private transient List<LifeMovementFragment> lastres;
    private List<Integer> lastmonthfameids, lastmonthbattleids, extendedSlots;
    private List<MapleDoor> doors;
    private List<MechDoor> mechDoors;
    private int gml;
    private List<MaplePet> pets;
    private List<Item> rebuy;
    private Map<Short, String> area_info = new LinkedHashMap<>();
    private MapleImp[] imps;
    private transient Set<MapleMonster> controlled;
    private transient Set<MapleMapObject> visibleMapObjects;
    private transient ReentrantReadWriteLock visibleMapObjectsLock;
    private transient ReentrantReadWriteLock summonsLock;
    private transient ReentrantReadWriteLock controlledLock;
    public transient MapleAndroid android;
    private Map<MapleQuest, MapleQuestStatus> quests;
    private Map<Integer, String> questinfo;
    private Map<Skill, SkillEntry> skills;
    private transient Map<MapleBuffStat, MapleBuffStatValueHolder> effects;
    private int noacc;
    private transient List<MapleSummon> summons;
    private transient Map<Integer, MapleCoolDownValueHolder> coolDowns;
    private transient Map<MapleDisease, MapleDiseaseValueHolder> diseases;
    private CashShop cs;
    private transient Deque<MapleCarnivalChallenge> pendingCarnivalRequests;
    private transient MapleCarnivalParty carnivalParty;
    private BuddyList buddylist;
    private MonsterBook monsterbook;
    private MapleClient client;
    private transient MapleParty party;
    private PlayerStats stats;
    private transient MapleMap map;
    private transient MapleShop shop;
    private transient MapleDragon dragon;
    private transient MapleExtractor extractor;
    private transient RockPaperScissors rps;
    private Map<Integer, MonsterFamiliar> familiars;
    private MapleStorage storage;
    private transient MapleTrade trade;
    private MapleMount mount;
    private MapleMessenger messenger;
    private byte[] petStore;
    private transient IMaplePlayerShop playerShop;
    private boolean invincible, canTalk, followinitiator, followon, smega, hasSummon;
    private MapleGuildCharacter mgc;
    private MapleFamilyCharacter mfc;
    private transient EventInstanceManager eventInstance;
    private int cardStack;
    private int runningStack;
    private MapleInventory[] inventory;
    private SkillMacro[] skillMacros = new SkillMacro[5];
    public EnumMap<MapleTraitType, MapleTrait> traits;
    private MapleKeyLayout keylayout;
    private transient ScheduledFuture<?> mapTimeLimitTask;
    private MapleCharacterCards characterCard;
    private transient Event_PyramidSubway pyramidSubway = null;
    private transient List<Integer> pendingExpiration = null;
    private transient Map<Skill, SkillEntry> pendingSkills = null;
    private transient Map<Integer, Integer> linkMobs;
    private boolean changed_wishlist, changed_trocklocations, changed_regrocklocations, changed_hyperrocklocations, changed_skillmacros,
            changed_savedlocations, changed_questinfo, changed_skills, changed_extendedSlots;
    private int honourExp, honourLevel;
    /*Start of Custom Feature*/
    /*All custom shit declare here*/
    public boolean keyvalue_changed = false, innerskill_changed = true;
    private int reborns, apstorage;
    private boolean muted;
    Calendar unmuteTime = null;
    private int location, todo, birthday, found;
    private int reactorCount = 0; // PQ Reactor click counting for various pq's
    /*End of Custom Feature*/
    private int MSIPoints;
    private int clanId;
    private int dgm;
    private int toSteal;
    private List<InnerSkillValueHolder> innerSkills;
    // Start of Development's features
    private boolean megaHidden;
    public boolean[] warning = new boolean[25];
    public boolean[] gate = new boolean[25];
    private MapleClans clan = new MapleClans();
    private Occupations occupation = Occupations.Pioneer;
    public int occupationExp = 0; //I don't give a shit about this fuckin thing. @Eric
    public int occupationLevel = 1; //Since toon and I wanted upgradable bases.
    private int occupationId = 1;
    public int JQExp = 0;
    public int JQLevel = 1; // Rev 1.2 is Level += Job
    private boolean testingdps = false; // toggle if player is already DPS testing
    private long dps; // set the dps value 
    private boolean monsterChalkOn = false;
    private int monsterChalk = 0;
    private boolean dropToggle = true; // By default, we do want drops ENABLED. 
    private boolean leetness;
    private boolean flying = false;
    private MapleCharacter watcher = null;
    private int ticklePower = 0; // 0 = off, 1 = on
    private int charToggle = 0;
    private long askmastertime; // master time
    private long askdualtime; // wizer duel time
    public int master = 0; // master ID for apprentice
    public int apprentice = 0; // apprentice ID for master
    private boolean wantHit = true; // By default let GMs get hit
    private boolean fakeDamage = false; // By default let GMs hit normal damage
    public int pvpVictim = 0; // store victim id's for each character
    private int wantFame = 0; // 0 = wants fame, 1 = does not
    private int muteLevel = 0; // 0 = off, 1 = on, 2 = save to db
    private int gmtext;
    private boolean autoToken = false;
    private int autoAP = 0; // 0 = Off, 1 = STR, 2 = DEX, 3 = INT, 4 = LUK, 5 = AP Storage
    private boolean hidden = false; // TODO: gm hide
    private boolean elf = false; // For Non-Mercedes Elf Ears. :P
    private static String[] ariantroomleader = new String[3]; // AriantPQ
    private static int[] ariantroomslot = new int[3]; // AriantPQ
    public long dojoStartTime;
    public long dojoMapEndTime;

    public MapleCharacter(final boolean ChannelServer) {
        setStance(0);
        setPosition(new Point(0, 0));
        leetness = false;
        inventory = new MapleInventory[MapleInventoryType.values().length];
        for (MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type);
        }
        quests = new LinkedHashMap<>(); // Stupid erev quest.
        skills = new LinkedHashMap<>(); //Stupid UAs.
        stats = new PlayerStats();
        innerSkills = new LinkedList<>();
        characterCard = new MapleCharacterCards();
        for (int i = 0; i < remainingSp.length; i++) {
            remainingSp[i] = 0;
        }
        traits = new EnumMap<>(MapleTraitType.class);
        for (MapleTraitType t : MapleTraitType.values()) {
            traits.put(t, new MapleTrait(t));
        }
        if (ChannelServer) {
            changed_skills = false;
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_regrocklocations = false;
            changed_hyperrocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_extendedSlots = false;
            changed_questinfo = false;
            changed_reports = false;
            scrolledPosition = 0;
            lastCombo = 0;
            mulung_energy = 0;
            combo = 0;
            force = 0;
            keydown_skill = 0;
            nextConsume = 0;
            /*  304 */ cardStack = 0;
            /*  305 */ runningStack = 1;
            pqStartTime = 0;
            fairyExp = 0;
            mapChangeTime = 0;
            lastRecoveryTime = 0;
            lastDragonBloodTime = 0;
            lastBerserkTime = 0;
            lastFishingTime = 0;
            lastFairyTime = 0;
            lastHPTime = 0;
            lastMPTime = 0;
            lastFamiliarEffectTime = 0;
            old = new Point(0, 0);
            coconutteam = 0;
            followid = 0;
            battleshipHP = 0;
            marriageItemId = 0;
            fallcounter = 0;
            challenge = 0;
            dotHP = 0;
            lastSummonTime = 0;
            hasSummon = false;
            invincible = false;
            canTalk = true;
            followinitiator = false;
            followon = false;
            rebuy = new ArrayList<>();
            linkMobs = new HashMap<>();
            teleportname = "";
            smega = true;
            petStore = new byte[3];
            for (int i = 0; i < petStore.length; i++) {
                petStore[i] = (byte) -1;
            }
            wishlist = new int[10];
            rocks = new int[10];
            regrocks = new int[5];
            hyperrocks = new int[13];
            imps = new MapleImp[3];
            familiars = new LinkedHashMap<>();
            extendedSlots = new ArrayList<>();
            effects = new ConcurrentEnumMap<>(MapleBuffStat.class);
            coolDowns = new LinkedHashMap<>();
            diseases = new ConcurrentEnumMap<>(MapleDisease.class);
            inst = new AtomicInteger(0);// 1 = NPC/ Quest, 2 = Duey, 3 = Hired Merch store, 4 = Storage
            insd = new AtomicInteger(-1);
            keylayout = new MapleKeyLayout();
            doors = new ArrayList<>();
            mechDoors = new ArrayList<>();
            controlled = new LinkedHashSet<>();
            controlledLock = new ReentrantReadWriteLock();
            summons = new LinkedList<>();
            summonsLock = new ReentrantReadWriteLock();
            visibleMapObjects = new LinkedHashSet<>();
            visibleMapObjectsLock = new ReentrantReadWriteLock();
            pendingCarnivalRequests = new LinkedList<>();
            savedLocations = new int[SavedLocationType.values().length];
            for (int i = 0; i < SavedLocationType.values().length; i++) {
                savedLocations[i] = -1;
            }
            questinfo = new LinkedHashMap<>();
            pets = new ArrayList<>();
        }
    }

    public void addHonourExp(int amount) {
        if (getHonourLevel() == 0) {
            setHonourLevel(1);
        }
        if (getHonourExp() + amount >= getHonourLevel() * 500) {
            honourLevelUp();
            int leftamount = (getHonourExp() + amount) - ((getHonourLevel() - 1) * 500);
            leftamount = Math.min(leftamount, ((getHonourLevel()) * 500) - 1);
            setHonourExp(leftamount);
            return;
        }
        setHonourExp(getHonourExp() + amount);
        client.getSession().write(CWvsContext.updateAzwanFame(getHonourLevel(), getHonourExp(), true));
        client.getSession().write(CWvsContext.professionInfo("honorLeveling", 0, getHonourLevel(), getHonourNextExp()));
    }

    public int getHonourNextExp() {
        if (getHonourLevel() == 0) {
            return 0; //shush
        }
        return (getHonourLevel() + 1) * 500;
    }

    public void honourLevelUp() {
        setHonourLevel(getHonourLevel() + 1);
        client.getSession().write(CWvsContext.updateAzwanFame(getHonourLevel(), getHonourExp(), true));
        if (getHonourLevel() == 2) {
            InnerSkillValueHolder diella = InnerAbility.getInstance().renewSkill(0, -1);
            innerSkills.add(diella);
            changeSkillLevel(SkillFactory.getSkill(diella.getSkillId()), diella.getSkillLevel(), diella.getSkillLevel());
            client.getSession().write(CField.getCharInfo(this));
        } else if (getHonourLevel() == 30) {
            InnerSkillValueHolder is = InnerAbility.getInstance().renewSkill(Randomizer.rand(0, 2), -1);
            innerSkills.add(is);
            changeSkillLevel(SkillFactory.getSkill(is.getSkillId()), is.getSkillLevel(), is.getSkillLevel());
            client.getSession().write(CField.getCharInfo(this));
        } else if (getHonourLevel() == 70) {
            InnerSkillValueHolder beautiful = InnerAbility.getInstance().renewSkill(Randomizer.rand(1, 3), -1);
            innerSkills.add(beautiful);
            changeSkillLevel(SkillFactory.getSkill(beautiful.getSkillId()), beautiful.getSkillLevel(), beautiful.getSkillLevel());
            client.getSession().write(CField.getCharInfo(this));
        }

    }

    public void changeSkillLevel(Skill skill, byte newLevel, byte newMasterlevel) {
        changeSkillLevel(skill, newLevel, newMasterlevel);
    }
    
    public static String getAriantRoomLeaderName(int room) {
        return ariantroomleader[room];
    }

    public static int getAriantSlotsRoom(int room) {
        return ariantroomslot[room];
    }
    
    public static void removeAriantRoom(int room) {
        ariantroomleader[room] = "";
        ariantroomslot[room] = 0;
    }
    
    public static void setAriantRoomLeader(int room, String charname) {
        ariantroomleader[room] = charname;
    }

    public static void setAriantSlotRoom(int room, int slot) {
        ariantroomslot[room] = slot;
    }

    public static MapleCharacter getDefault(final MapleClient client, final JobType type) {
        MapleCharacter ret = new MapleCharacter(false);
        ret.client = client;
        ret.map = null;
        ret.gmLevel = 0;
        ret.job = (short) type.id;
        ret.meso = 0;
        ret.pvpKills = 1;
        ret.pvpDeaths = 1;
        ret.level = 1;
        ret.remainingAp = 0;
        ret.fame = 0;
        ret.accountid = client.getAccID();
        ret.buddylist = new BuddyList(232);
        ret.exp.set(0);
        ret.stats.str = 4;
        ret.stats.dex = 4;
        ret.stats.int_ = 4;
        ret.stats.luk = 4;
        ret.stats.maxhp = 50;
        ret.stats.hp = 50;
        ret.stats.maxmp = 50;
        ret.stats.mp = 50;
        ret.gachexp = 0;

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ret.client.setAccountName(rs.getString("name"));
                    ret.nxcredit = rs.getInt("nxcredit");
                    ret.maplepoints = rs.getInt("mPoints");
                    ret.nxprepaid = rs.getInt("nxprepaid");
                    ret.points = rs.getInt("points");
                    ret.vpoints = rs.getInt("vpoints");
                    ret.redeemhn = rs.getInt("redeemhn");
                }
            }
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error getting character default" + e);
        }
        return ret;
    }

    public static MapleCharacter ReconstructChr(final CharacterTransfer ct, final MapleClient client, final boolean isChannel) {
        final MapleCharacter ret = new MapleCharacter(true); // Always true, it's change channel
        ret.client = client;
        if (!isChannel) {
            ret.client.setChannel(ct.channel);
        }
        ret.id = ct.characterid;
        ret.name = ct.name;
        ret.level = ct.level;
        ret.fame = ct.fame;

        ret.CRand = new PlayerRandomStream();

        ret.stats.str = ct.str;
        ret.stats.dex = ct.dex;
        ret.stats.int_ = ct.int_;
        ret.stats.luk = ct.luk;
        ret.stats.maxhp = ct.maxhp;
        ret.stats.maxmp = ct.maxmp;
        ret.stats.hp = ct.hp;
        ret.stats.mp = ct.mp;
        ret.characterCard.setCards(ct.cardsInfo);
        ret.gml = ct.gml;
        ret.chalktext = ct.chalkboard;
        ret.gmLevel = ct.gmLevel;
        ret.exp = ct.exp;
        ret.hpApUsed = ct.hpApUsed;
        ret.remainingSp = ct.remainingSp;
        ret.remainingAp = ct.remainingAp;
        ret.meso = ct.meso;
        ret.skinColor = ct.skinColor;
        ret.gender = ct.gender;
        ret.job = ct.job;
        ret.hair = ct.hair;
        ret.face = ct.face;
        ret.demonMarking = ct.demonMarking;
        ret.accountid = ct.accountid;
        ret.totalWins = ct.totalWins;
        ret.totalLosses = ct.totalLosses;
        client.setAccID(ct.accountid);
        ret.mapid = ct.mapid;
        ret.initialSpawnPoint = ct.initialSpawnPoint;
        ret.world = ct.world;
        ret.guildid = ct.guildid;
        ret.guildrank = ct.guildrank;
        ret.guildContribution = ct.guildContribution;
        ret.allianceRank = ct.alliancerank;
        ret.points = ct.points;
        ret.vpoints = ct.vpoints;
        ret.fairyExp = ct.fairyExp;
        ret.marriageId = ct.marriageId;
        ret.currentrep = ct.currentrep;
        ret.totalrep = ct.totalrep;
        ret.gachexp = ct.gachexp;
        ret.pvpExp = ct.pvpExp;
        ret.pvpPoints = ct.pvpPoints;
        /*Start of Custom Feature*/
        ret.reborns = ct.reborns;
        ret.apstorage = ct.apstorage;
        ret.MSIPoints = ct.MSIPoints;
        ret.noacc = ct.noacc;
        ret.muted = ct.muted;
        ret.unmuteTime = ct.unmuteTime;
        ret.dgm = ct.dgm;
        ret.cardStack = ct.cardStack;
        ret.honourExp = ct.honourexp;
        ret.honourLevel = ct.honourlevel;
        ret.innerSkills = (LinkedList<InnerSkillValueHolder>) ct.innerSkills;
        ret.occupationId = ct.occupationId;
        ret.occupationExp = ct.occupationExp;
        ret.occupationLevel = ct.occupationLevel;
        ret.JQLevel = ct.JQLevel;
        ret.JQExp = ct.JQExp;
        //ret.JQId = ct.JQId;
        ret.wantFame = ct.wantFame;
        ret.gmtext = ct.gmtext;
        ret.charToggle = ct.charToggle;
        ret.dps = ct.dps;
        ret.pvpKills = ct.pvpKills; // custom
        ret.pvpDeaths = ct.pvpDeaths; // custom
        ret.autoAP = ct.autoAP;
        ret.autoToken = ct.autoToken;
        ret.elf = ct.elf;
        ret.clanId = ct.clanId;
        /*End of Custom Feature*/
        ret.makeMFC(ct.familyid, ct.seniorid, ct.junior1, ct.junior2);
        if (ret.guildid > 0) {
            ret.mgc = new MapleGuildCharacter(ret);
        }
        ret.fatigue = ct.fatigue;
        ret.buddylist = new BuddyList(ct.buddysize);
        ret.subcategory = ct.subcategory;

        if (isChannel) {
            final MapleMapFactory mapFactory = ChannelServer.getInstance(client.getWorld(), client.getChannel()).getMapFactory();
            ret.map = mapFactory.getMap(ret.mapid);
            if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                ret.map = mapFactory.getMap(100000000);
            } else {
                if (ret.map.getForcedReturnId() != 999999999 && ret.map.getForcedReturnMap() != null) {
                    ret.map = ret.map.getForcedReturnMap();
                }
            }
            MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
            if (portal == null) {
                portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                ret.initialSpawnPoint = 0;
            }
            ret.setPosition(portal.getPosition());

            final int messengerid = ct.messengerid;
            if (messengerid > 0) {
                ret.messenger = World.Messenger.getMessenger(messengerid);
            }
        } else {

            ret.messenger = null;
        }
        int partyid = ct.partyid;
        if (partyid >= 0) {
            MapleParty party = World.Party.getParty(partyid);
            if (party != null && party.getMemberById(ret.id) != null) {
                ret.party = party;
            }
        }

        MapleQuestStatus queststatus_from;
        for (final Map.Entry<Integer, Object> qs : ct.Quest.entrySet()) {
            queststatus_from = (MapleQuestStatus) qs.getValue();
            queststatus_from.setQuest(qs.getKey());
            ret.quests.put(queststatus_from.getQuest(), queststatus_from);
        }
        for (final Map.Entry<Integer, SkillEntry> qs : ct.Skills.entrySet()) {
            ret.skills.put(SkillFactory.getSkill(qs.getKey()), qs.getValue());
        }
        for (Entry<MapleTraitType, Integer> t : ct.traits.entrySet()) {
            ret.traits.get(t.getKey()).setExp(t.getValue());
        }
        ret.monsterbook = new MonsterBook(ct.mbook, ret);
        ret.inventory = (MapleInventory[]) ct.inventorys;
        ret.BlessOfFairy_Origin = ct.BlessOfFairy;
        ret.BlessOfEmpress_Origin = ct.BlessOfEmpress;
        ret.skillMacros = (SkillMacro[]) ct.skillmacro;
        ret.petStore = ct.petStore;
        ret.keylayout = new MapleKeyLayout(ct.keymap);
        ret.questinfo = ct.InfoQuest;
        ret.familiars = ct.familiars;
        ret.savedLocations = ct.savedlocation;
        ret.wishlist = ct.wishlist;
        ret.rocks = ct.rocks;
        ret.regrocks = ct.regrocks;
        ret.hyperrocks = ct.hyperrocks;
        ret.buddylist.loadFromTransfer(ct.buddies);
        // ret.lastfametime
        // ret.lastmonthfameids
        ret.keydown_skill = 0; // Keydown skill can't be brought over
        ret.lastfametime = ct.lastfametime;
        ret.lastmonthfameids = ct.famedcharacters;
        ret.extendedSlots = ct.extendedSlots;
        ret.storage = (MapleStorage) ct.storage;
        ret.cs = (CashShop) ct.cs;
        client.setAccountName(ct.accountname);
        ret.nxcredit = ct.nxcredit;
        ret.redeemhn = ct.redeemhn;
        ret.location = ct.location;
        ret.birthday = ct.birthday;
        ret.found = ct.found;
        ret.todo = ct.found;
        ret.maplepoints = ct.MaplePoints;
        ret.nxprepaid = ct.nxprepaid;
        ret.imps = ct.imps;
        ret.rebuy = ct.rebuy;
        ret.mount = new MapleMount(ret, ct.mount_itemid, PlayerStats.getSkillByJob(1004, ret.job), ct.mount_Fatigue, ct.mount_level, ct.mount_exp);
        ret.expirationTask(false, false);
        ret.stats.recalcLocalStats(true, ret);
        client.setTempIP(ct.tempIP);

        return ret;
    }

    public boolean isMuted() {
        if (Calendar.getInstance().after(unmuteTime)) {
            muted = false;
        }
        return muted;
    }

    public void setMuted(boolean mute) {
        this.muted = mute;
    }

    public Calendar getUnmuteTime() {
        return this.unmuteTime;
    }

    public void setUnmuteTime(Calendar time) {
        unmuteTime = time;
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver, Map<Integer, CardData> cads) {
        final MapleCharacter ret = new MapleCharacter(channelserver);
        ret.client = client;
        ret.id = charid;

        Connection con = DatabaseConnection.getConnection();
        PreparedStatement ps = null;
        PreparedStatement pse;
        ResultSet rs = null;

        try {
            ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("Loading the Char Failed (char not found)");
            }
            ret.name = rs.getString("name");
            ret.level = rs.getShort("level");
            ret.fame = rs.getInt("fame");

            ret.stats.str = rs.getShort("str");
            ret.stats.dex = rs.getShort("dex");
            ret.stats.int_ = rs.getShort("int");
            ret.stats.luk = rs.getShort("luk");
            ret.stats.maxhp = rs.getInt("maxhp");
            ret.stats.maxmp = rs.getInt("maxmp");
            ret.stats.hp = rs.getInt("hp");
            ret.stats.mp = rs.getInt("mp");
            ret.job = rs.getShort("job");
            ret.gmLevel = rs.getByte("gm");
            ret.exp.set(rs.getInt("exp"));
            ret.hpApUsed = rs.getShort("hpApUsed");
            final String[] sp = rs.getString("sp").split(",");
            for (int i = 0; i < ret.remainingSp.length; i++) {
                ret.remainingSp[i] = Integer.parseInt(sp[i]);
            }
            ret.remainingAp = rs.getInt("ap");
            ret.meso = rs.getInt("meso");
            ret.skinColor = rs.getByte("skincolor");
            ret.gender = rs.getByte("gender");

            ret.hair = rs.getInt("hair");
            ret.face = rs.getInt("face");
            ret.demonMarking = rs.getInt("demonMarking");
            ret.accountid = rs.getInt("accountid");
            client.setAccID(ret.accountid);
            ret.mapid = rs.getInt("map");
            ret.initialSpawnPoint = rs.getByte("spawnpoint");
            ret.world = rs.getByte("world");
            ret.guildid = rs.getInt("guildid");
            ret.guildrank = rs.getByte("guildrank");
            ret.allianceRank = rs.getByte("allianceRank");
            ret.guildContribution = rs.getInt("guildContribution");
            ret.totalWins = rs.getInt("totalWins");
            ret.totalLosses = rs.getInt("totalLosses");
            ret.currentrep = rs.getInt("currentrep");
            ret.totalrep = rs.getInt("totalrep");
            ret.makeMFC(rs.getInt("familyid"), rs.getInt("seniorid"), rs.getInt("junior1"), rs.getInt("junior2"));
            if (ret.guildid > 0) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            ret.gachexp = rs.getInt("gachexp");
            ret.buddylist = new BuddyList(rs.getInt("buddyCapacity"));
            ret.subcategory = rs.getByte("subcategory");
            ret.mount = new MapleMount(ret, 0, PlayerStats.getSkillByJob(1004, ret.job), (byte) 0, (byte) 1, 0);
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");
            ret.marriageId = rs.getInt("marriageId");
            ret.fatigue = rs.getShort("fatigue");
            ret.pvpExp = rs.getInt("pvpExp");
            ret.pvpPoints = rs.getInt("pvpPoints");
            ret.todo = rs.getInt("todo");
            ret.birthday = rs.getInt("birthday");
            ret.location = rs.getInt("location");
            ret.found = rs.getInt("found");
            /*Start of Custom Features*/
            ret.reborns = rs.getInt("reborns");
            ret.apstorage = rs.getInt("apstorage");
            ret.MSIPoints = rs.getInt("msipoints");
            ret.clanId = rs.getInt("clanid");
            ret.noacc = rs.getInt("noacc");
            ret.muted = rs.getInt("muted") == 1 ? true : false;
            Calendar c = Calendar.getInstance();
            c.setTime(new Date(rs.getLong("unmutetime")));
            ret.unmuteTime = c;
            ret.dgm = rs.getInt("dgm");
            ret.honourExp = rs.getInt("honourExp");
            ret.honourLevel = rs.getInt("honourLevel");
            ret.gml = rs.getInt("gml");
            ret.occupationId = rs.getInt("occupationId");
            ret.occupationExp = rs.getInt("occupationExp");
            ret.occupationLevel = rs.getInt("occupationLevel");
            ret.JQLevel = rs.getInt("jqlevel");
            ret.JQExp = rs.getInt("jqexp");
            //ret.JQId = rs.getInt("jqid");
            ret.wantFame = rs.getInt("fametoggle");
            ret.pvpKills = rs.getInt("pvpKills");
            ret.pvpDeaths = rs.getInt("pvpDeaths");
            ret.gmtext = rs.getInt("gmtext");
            ret.charToggle = rs.getInt("charToggle");//(rs.getInt("chartoggle") != 0);
            ret.dps = rs.getLong("dps");
            ret.autoAP = rs.getInt("autoap"); // the type
            ret.autoToken = rs.getInt("autotoken") == 1 ? true : false;
            ret.elf = rs.getInt("elf") == 1 ? true : false;
            /*End of Custom Features*/
            for (MapleTrait t : ret.traits.values()) {
                t.setExp(rs.getInt(t.getType().name()));
            }
            if (channelserver) {
                ret.CRand = new PlayerRandomStream();
                MapleMapFactory mapFactory = ChannelServer.getInstance(client.getWorld(), client.getChannel()).getMapFactory();
                ret.map = mapFactory.getMap(ret.mapid);
                if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                    ret.map = mapFactory.getMap(100000000);
                }
                MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());

                int partyid = rs.getInt("party");
                if (partyid >= 0) {
                    MapleParty party = World.Party.getParty(partyid);
                    if (party != null && party.getMemberById(ret.id) != null) {
                        ret.party = party;
                    }
                }
                final String[] pets = rs.getString("pets").split(",");
                for (int i = 0; i < ret.petStore.length; i++) {
                    ret.petStore[i] = Byte.parseByte(pets[i]);
                }
                rs.close();
                ps.close();

            }
            rs.close();
            ps.close();

            if (cads != null) {
                ret.characterCard.setCards(cads);
            } /*      */ else {
                /*  745 */ ret.characterCard.loadCards(client, channelserver);
                /*      */            }

            ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");

            while (rs.next()) {
                final int id = rs.getInt("quest");
                final MapleQuest q = MapleQuest.getInstance(id);
                final byte stat = rs.getByte("status");
                if ((stat == 1 || stat == 2) && channelserver && (q == null || q.isBlocked())) { //bigbang
                    continue;
                }
                if (stat == 1 && channelserver && !q.canStart(ret, null)) { //bigbang
                    continue;
                }
                final MapleQuestStatus status = new MapleQuestStatus(q, stat);
                final long cTime = rs.getLong("time");
                if (cTime > -1) {
                    status.setCompletionTime(cTime * 1000);
                }
                status.setForfeited(rs.getInt("forfeited"));
                status.setCustomData(rs.getString("customData"));
                ret.quests.put(q, status);
                pse.setInt(1, rs.getInt("queststatusid"));
                try (ResultSet rsMobs = pse.executeQuery()) {
                    while (rsMobs.next()) {
                        status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                    }
                }
            }
            rs.close();
            ps.close();
            pse.close();

            if (channelserver) {
                ret.monsterbook = MonsterBook.loadCards(ret.accountid, ret);


                for (Pair<Item, MapleInventoryType> mit : ItemLoader.INVENTORY.loadItems(false, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                    if (mit.getLeft().getPet() != null) {
                        ret.pets.add(mit.getLeft().getPet());
                    }
                }

                ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                if (rs.next()) {
                    ret.getClient().setAccountName(rs.getString("name"));
                    ret.nxcredit = rs.getInt("nxcredit");
                    ret.maplepoints = rs.getInt("mPoints");
                    ret.nxprepaid = rs.getInt("nxprepaid");
                    ret.points = rs.getInt("points");
                    ret.vpoints = rs.getInt("vpoints");
                    ret.redeemhn = rs.getInt("redeemhn");
                    if (rs.getTimestamp("lastlogon") != null) {
                        final Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(rs.getTimestamp("lastlogon").getTime());
                        if (cal.get(Calendar.DAY_OF_WEEK) + 1 == Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                            ret.nxcredit += 500;
                        }
                    }
                    if (rs.getInt("banned") > 0) {
                        rs.close();
                        ps.close();
                        ret.getClient().getSession().close(true);
                        throw new RuntimeException("Loading a banned character");
                    }
                    rs.close();
                    ps.close();

                    ps = con.prepareStatement("UPDATE accounts SET lastlogon = CURRENT_TIMESTAMP() WHERE id = ?");
                    ps.setInt(1, ret.accountid);
                    ps.executeUpdate();
                } else {
                    rs.close();
                }
                ps.close();

                ps = con.prepareStatement("SELECT * FROM questinfo WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                while (rs.next()) {
                    ret.questinfo.put(rs.getInt("quest"), rs.getString("customData"));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT skillid, skilllevel, masterlevel, expiration, slot, equipped FROM skills WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                Skill skil;
                while (rs.next()) {
                    final int skid = rs.getInt("skillid");
                    skil = SkillFactory.getSkill(skid);
                    int skl = rs.getInt("skilllevel");
                    int msl = rs.getInt("masterlevel");
                    if (skil != null && GameConstants.isApplicableSkill(skid)) {
                        if (skl > skil.getMaxLevel() && skid < 92000000) {
                            if (!skil.isBeginnerSkill() && skil.canBeLearnedBy(ret.job) && !skil.isSpecialSkill()) {
                                ret.remainingSp[GameConstants.getSkillBookForSkill(skid)] += (skl - skil.getMaxLevel());
                            }
                            skl = (byte) skil.getMaxLevel();
                        }
                        if (msl > skil.getMaxLevel()) {
                            msl = (byte) skil.getMaxLevel();
                        }
                        ret.skills.put(skil, new SkillEntry(skl, msl, rs.getLong("expiration"), rs.getByte("slot"), rs.getByte("equipped")));
                    } else if (skil == null) { //doesnt. exist. e.g. bb
                        if (!GameConstants.isBeginnerJob(skid / 10000) && skid / 10000 != 900 && skid / 10000 != 800 && skid / 10000 != 9000) {
                            ret.remainingSp[GameConstants.getSkillBookForSkill(skid)] += skl;
                        }
                    }
                }
                rs.close();
                ps.close();

                ret.expirationTask(false, true); //do it now

                // Bless of Fairy handling
                ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY level DESC");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                int maxlevel_ = 0, maxlevel_2 = 0;
                while (rs.next()) {
                    if (rs.getInt("id") != charid) { // Not this character
                        if (GameConstants.isKOC(rs.getShort("job"))) {
                            int maxlevel = (rs.getShort("level") / 5);

                            if (maxlevel > 24) {
                                maxlevel = 24;
                            }
                            if (maxlevel > maxlevel_2 || maxlevel_2 == 0) {
                                maxlevel_2 = maxlevel;
                                ret.BlessOfEmpress_Origin = rs.getString("name");
                            }
                        }
                        int maxlevel = (rs.getShort("level") / 10);

                        if (maxlevel > 20) {
                            maxlevel = 20;
                        }
                        if (maxlevel > maxlevel_ || maxlevel_ == 0) {
                            maxlevel_ = maxlevel;
                            ret.BlessOfFairy_Origin = rs.getString("name");
                        }

                    }
                }
                /*if (!compensate_previousSP) {
                 for (Entry<Skill, SkillEntry> skill : ret.skills.entrySet()) {
                 if (!skill.getKey().isBeginnerSkill() && !skill.getKey().isSpecialSkill()) {
                 ret.remainingSp[GameConstants.getSkillBookForSkill(skill.getKey().getId())] += skill.getValue().skillevel;
                 skill.getValue().skillevel = 0;
                 }
                 }
                 ret.setQuestAdd(MapleQuest.getInstance(170000), (byte) 0, null); //set it so never again
                 }*/
                if (ret.BlessOfFairy_Origin == null) {
                    ret.BlessOfFairy_Origin = ret.name;
                }
                ret.skills.put(SkillFactory.getSkill(GameConstants.getBOF_ForJob(ret.job)), new SkillEntry(maxlevel_, (byte) 0, -1));
                if (SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)) != null) {
                    if (ret.BlessOfEmpress_Origin == null) {
                        ret.BlessOfEmpress_Origin = ret.BlessOfFairy_Origin;
                    }
                    ret.skills.put(SkillFactory.getSkill(GameConstants.getEmpress_ForJob(ret.job)), new SkillEntry(maxlevel_2, (byte) 0, -1));
                }
                ps.close();
                rs.close();
                // END
                ps = con.prepareStatement("SELECT skill_id, skill_level, max_level, rank FROM inner_ability_skills WHERE player_id = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.innerSkills.add(new InnerSkillValueHolder(rs.getInt("skill_id"), rs.getByte("skill_level"), rs.getByte("max_level"), rs.getByte("rank")));
                }
                rs.close();
                ps.close();


                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int position;
                while (rs.next()) {
                    position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM familiars WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getLong("expiry") <= System.currentTimeMillis()) {
                        continue;
                    }
                    ret.familiars.put(rs.getInt("familiar"), new MonsterFamiliar(charid, rs.getInt("id"), rs.getInt("familiar"), rs.getLong("expiry"), rs.getString("name"), rs.getInt("fatigue"), rs.getByte("vitality")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();

                final Map<Integer, Pair<Byte, Integer>> keyb = ret.keylayout.Layout();
                while (rs.next()) {
                    keyb.put(Integer.valueOf(rs.getInt("key")), new Pair<>(rs.getByte("type"), rs.getInt("action")));
                }
                rs.close();
                ps.close();
                ret.keylayout.unchanged();

                ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.savedLocations[rs.getInt("locationtype")] = rs.getInt("map");
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastfametime = 0;
                ret.lastmonthfameids = new ArrayList<>(31);
                while (rs.next()) {
                    ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                    ret.lastmonthfameids.add(Integer.valueOf(rs.getInt("characterid_to")));
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT `itemId` FROM extendedslots WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.extendedSlots.add(Integer.valueOf(rs.getInt("itemId")));
                }
                rs.close();
                ps.close();

                ret.buddylist.loadFromDb(charid);
                ret.storage = MapleStorage.loadStorage(ret.accountid);
                ret.cs = new CashShop(ret.accountid, charid, ret.getJob());

                ps = con.prepareStatement("SELECT sn FROM wishlist WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int i = 0;
                while (rs.next()) {
                    ret.wishlist[i] = rs.getInt("sn");
                    i++;
                }
                while (i < 10) {
                    ret.wishlist[i] = 0;
                    i++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM trocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int r = 0;
                while (rs.next()) {
                    ret.rocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 10) {
                    ret.rocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM regrocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.regrocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 5) {
                    ret.regrocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT mapid FROM hyperrocklocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.hyperrocks[r] = rs.getInt("mapid");
                    r++;
                }
                while (r < 13) {
                    ret.hyperrocks[r] = 999999999;
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM imps WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.imps[r] = new MapleImp(rs.getInt("itemid"));
                    ret.imps[r].setLevel(rs.getByte("level"));
                    ret.imps[r].setState(rs.getByte("state"));
                    ret.imps[r].setCloseness(rs.getShort("closeness"));
                    ret.imps[r].setFullness(rs.getShort("fullness"));
                    r++;
                }
                rs.close();
                ps.close();

                ps = con.prepareStatement("SELECT * FROM mountdata WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException("No mount data found on SQL column");
                }
                final Item mount = ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (GameConstants.GMS ? -18 : -23));
                ret.mount = new MapleMount(ret, mount != null ? mount.getItemId() : 0, GameConstants.GMS ? 80001000 : PlayerStats.getSkillByJob(1004, ret.job), rs.getByte("Fatigue"), rs.getByte("Level"), rs.getInt("Exp"));
                ps.close();
                rs.close();

                ret.stats.recalcLocalStats(true, ret);
                ret.antiMacro = new MapleLieDetector(ret);
            } else { // Not channel server
                for (Pair<Item, MapleInventoryType> mit : ItemLoader.INVENTORY.loadItems(true, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                }
                ret.stats.recalcPVPRank(ret);
            }
        } catch (SQLException ess) {
            System.out.println("Failed to load character..");
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ess);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignore) {
            }
        }
        return ret;
    }

    public static void saveNewCharToDB(final MapleCharacter chr, final JobType type, short db) {
        Connection con = DatabaseConnection.getConnection();

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("INSERT INTO characters (level, str, dex, luk, `int`, hp, mp, maxhp, maxmp, sp, ap, skincolor, gender, job, hair, face, demonMarking, map, meso, party, buddyCapacity, pets, subcategory, accountid, name, world) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, chr.level); // Level
            final PlayerStats stat = chr.stats;
            ps.setShort(2, stat.getStr()); // Str
            ps.setShort(3, stat.getDex()); // Dex
            ps.setShort(4, stat.getInt()); // Int
            ps.setShort(5, stat.getLuk()); // Luk
            ps.setInt(6, stat.getHp()); // HP
            ps.setInt(7, stat.getMp());
            ps.setInt(8, stat.getMaxHp()); // MP
            ps.setInt(9, stat.getMaxMp());
            final StringBuilder sps = new StringBuilder();
            for (int i = 0; i < chr.remainingSp.length; i++) {
                sps.append(chr.remainingSp[i]);
                sps.append(",");
            }
            final String sp = sps.toString();
            ps.setString(10, sp.substring(0, sp.length() - 1));
            ps.setInt(11, chr.remainingAp); // Remaining AP
            ps.setByte(12, chr.skinColor);
            ps.setByte(13, chr.gender);
            ps.setShort(14, chr.job);
            ps.setInt(15, chr.hair);
            ps.setInt(16, chr.face);
            ps.setInt(17, chr.demonMarking);
            if (db < 0 || db > 2) { //todo legend
                db = 0;
            }
            ps.setInt(18, db == 2 ? 3000600 : type.map);
            ps.setInt(19, chr.meso); // Meso
            ps.setInt(20, -1); // Party
            ps.setInt(21, chr.buddylist.getCapacity()); // Buddylist
            ps.setString(22, "-1,-1,-1");
            ps.setInt(23, db); //for now
            ps.setInt(24, chr.getAccountID());
            ps.setString(25, chr.name);
            ps.setByte(26, chr.world);
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                chr.id = rs.getInt(1);
            } else {
                ps.close();
                rs.close();
                throw new DatabaseException("Inserting char failed.");
            }
            ps.close();
            rs.close();
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (final MapleQuestStatus q : chr.quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();

            ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration, slot, equipped) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);

            for (final Entry<Skill, SkillEntry> skill : chr.skills.entrySet()) {
                if (GameConstants.isApplicableSkill(skill.getKey().getId())) { //do not save additional skills
                    ps.setInt(2, skill.getKey().getId());
                    ps.setInt(3, skill.getValue().skillevel);
                    ps.setInt(4, skill.getValue().masterlevel);
                    ps.setLong(5, skill.getValue().expiration);
                    /* 1243 */ ps.setByte(6, ((SkillEntry) skill.getValue()).slot);
                    /* 1244 */ ps.setByte(7, ((SkillEntry) skill.getValue()).equipped);
                    ps.execute();
                }
            }
            ps.close();


            ps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setByte(2, (byte) 1);
            ps.setInt(3, 0);
            ps.setByte(4, (byte) 0);
            ps.execute();
            ps.close();


            final int[] array1 = {18, 65, 2, 23, 3, 4, 5, 6, 16, 17, 19, 25, 26, 27, 31, 34, 35, 37, 38, 40, 43, 44, 45, 46, 50, 56, 59, 60, 61, 62, 63, 64, 57, 48, 29, 7, 24, 33, 41, 39, 8, 20, 21, 49};
            final int[] array2 = {4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 5, 6, 6, 6, 6, 6, 6, 5, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4};
            final int[] array3 = {0, 106, 10, 1, 12, 13, 18, 24, 8, 5, 4, 19, 14, 15, 2, 17, 11, 3, 20, 16, 9, 50, 51, 6, 7, 53, 100, 101, 102, 103, 104, 105, 54, 30, 52, 21, 25, 26, 23, 27, 29, 28, 31, 22};


            ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (int i = 0; i < array1.length; i++) {
                ps.setInt(2, array1[i]);
                ps.setInt(3, array2[i]);
                ps.setInt(4, array3[i]);
                ps.execute();
            }
            ps.close();

            List<Pair<Item, MapleInventoryType>> listing = new ArrayList<>();
            for (final MapleInventory iv : chr.inventory) {
                for (final Item item : iv.list()) {
                    listing.add(new Pair<>(item, iv.getType()));
                }
            }
            ItemLoader.INVENTORY.saveItems(listing, con, chr.id);

            con.commit();
        } catch (SQLException | DatabaseException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            System.err.println("[charsave] Error saving character data");
            try {
                con.rollback();
            } catch (SQLException ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
                System.err.println("[charsave] Error Rolling Back");
            }
        } finally {
            try {
                if (pse != null) {
                    pse.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                System.err.println("[charsave] Error going back to autocommit mode");
            }
        }
    }

    public void saveToDB(boolean dc, boolean fromcs) {
        Connection con = DatabaseConnection.getConnection();

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;

        try {
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, face = ?, demonMarking = ?, map = ?, meso = ?, hpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, pets = ?, subcategory = ?, marriageId = ?, currentrep = ?, totalrep = ?, gachexp = ?, fatigue = ?, charm = ?, charisma = ?, craft = ?, insight = ?, sense = ?, will = ?, totalwins = ?, totallosses = ?, pvpExp = ?, pvpPoints = ?, reborns = ?, apstorage = ?, msipoints = ?, muted = ?, unmutetime = ?, dgm = ?, honourExp = ?, honourLevel = ?, gml = ?, noacc = ?, location = ?, birthday = ?, found = ?, todo = ?, occupationId = ?, occupationExp = ?, occupationLevel = ?, charToggle = ?, jqlevel = ?, jqexp = ?, pvpKills = ?, pvpDeaths = ?, fametoggle = ?, dps = ?, autoap = ?, autotoken = ?, elf = ?, clanid = ?, name = ? WHERE id = ?", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, level);
            ps.setInt(2, fame);
            ps.setShort(3, stats.getStr());
            ps.setShort(4, stats.getDex());
            ps.setShort(5, stats.getLuk());
            ps.setShort(6, stats.getInt());
            ps.setInt(7, Math.abs(exp.get()));
            ps.setInt(8, stats.getHp() < 1 ? 50 : stats.getHp());
            ps.setInt(9, stats.getMp());
            ps.setInt(10, stats.getMaxHp());
            ps.setInt(11, stats.getMaxMp());
            final StringBuilder sps = new StringBuilder();
            for (int i = 0; i < remainingSp.length; i++) {
                sps.append(remainingSp[i]);
                sps.append(",");
            }
            final String sp = sps.toString();
            ps.setString(12, sp.substring(0, sp.length() - 1));
            ps.setInt(13, remainingAp);
            ps.setByte(14, gmLevel);
            ps.setByte(15, skinColor);
            ps.setByte(16, gender);
            ps.setShort(17, job);
            ps.setInt(18, hair);
            ps.setInt(19, face);
            ps.setInt(20, demonMarking);
            if (!fromcs && map != null) {
                if (map.getForcedReturnId() != 999999999 && map.getForcedReturnMap() != null) {
                    ps.setInt(21, map.getForcedReturnId());
                } else {
                    ps.setInt(21, stats.getHp() < 1 ? map.getReturnMapId() : map.getId());
                }
            } else {
                ps.setInt(21, mapid);
            }
            ps.setInt(22, meso);
            ps.setShort(23, hpApUsed);
            if (map == null) {
                ps.setByte(24, (byte) 0);
            } else {
                final MaplePortal closest = map.findClosestSpawnpoint(getTruePosition());
                ps.setByte(24, (byte) (closest != null ? closest.getId() : 0));
            }
            ps.setInt(25, party == null ? -1 : party.getId());
            ps.setInt(26, buddylist.getCapacity());
            final StringBuilder petz = new StringBuilder();
            int petLength = 0;
            for (final MaplePet pet : pets) {
                if (pet.getSummoned()) {
                    pet.saveToDb();
                    petz.append(pet.getInventoryPosition());
                    petz.append(",");
                    petLength++;
                }
            }
            while (petLength < 3) {
                petz.append("-1,");
                petLength++;
            }
            final String petstring = petz.toString();
            ps.setString(27, petstring.substring(0, petstring.length() - 1));
            ps.setByte(28, subcategory);
            ps.setInt(29, marriageId);
            ps.setInt(30, currentrep);
            ps.setInt(31, totalrep);
            ps.setInt(32, gachexp);
            ps.setShort(33, fatigue);
            ps.setInt(34, traits.get(MapleTraitType.charm).getTotalExp());
            ps.setInt(35, traits.get(MapleTraitType.charisma).getTotalExp());
            ps.setInt(36, traits.get(MapleTraitType.craft).getTotalExp());
            ps.setInt(37, traits.get(MapleTraitType.insight).getTotalExp());
            ps.setInt(38, traits.get(MapleTraitType.sense).getTotalExp());
            ps.setInt(39, traits.get(MapleTraitType.will).getTotalExp());
            ps.setInt(40, totalWins);
            ps.setInt(41, totalLosses);
            ps.setInt(42, pvpExp);
            ps.setInt(43, pvpPoints);
            /*Start of Custom Features*/
            ps.setInt(44, reborns); // delete
            ps.setInt(45, apstorage); // delete
            ps.setInt(46, MSIPoints);
            ps.setInt(47, muted ? 1 : 0);
            ps.setLong(48, unmuteTime == null ? 0 : unmuteTime.getTimeInMillis());
            ps.setInt(49, dgm);
            ps.setInt(50, getHonourExp());
            ps.setInt(51, getHonourLevel());
            ps.setInt(52, gml);
            ps.setInt(53, noacc);
            ps.setInt(54, location);
            ps.setInt(55, birthday);
            ps.setInt(56, found);
            ps.setInt(57, todo);
            ps.setInt(58, occupationId); // delete
            ps.setInt(59, occupationExp); // delete
            ps.setInt(60, occupationLevel); // delete
            ps.setInt(61, charToggle);
            ps.setInt(62, JQLevel); // delete
            ps.setInt(63, JQExp); // delete
            //ps.setInt(56, JQId);
            ps.setInt(64, pvpKills);
            ps.setInt(65, pvpDeaths);
            ps.setInt(66, wantFame);
            ps.setLong(67, dps);
            ps.setInt(68, autoAP); // delete
            ps.setInt(69, autoToken ? 1 : 0); // delete
            ps.setInt(70, elf ? 1 : 0); // delete
            ps.setInt(71, clanId);
            ps.setString(72, name);
            ps.setInt(73, id);
            if (ps.executeUpdate() < 1) {
                ps.close();
                throw new DatabaseException("Character not in database (" + id + ")");
            }
            ps.close();
            if (changed_skillmacros) {
                deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
                for (int i = 0; i < 5; i++) {
                    final SkillMacro macro = skillMacros[i];
                    if (macro != null) {
                        ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                        ps.setInt(1, id);
                        ps.setInt(2, macro.getSkill1());
                        ps.setInt(3, macro.getSkill2());
                        ps.setInt(4, macro.getSkill3());
                        ps.setString(5, macro.getName());
                        ps.setInt(6, macro.getShout());
                        ps.setInt(7, i);
                        ps.execute();
                        ps.close();
                    }
                }
            }

            if (innerskill_changed) {
                if (innerSkills != null) {
                    deleteWhereCharacterId(con, "DELETE FROM inner_ability_skills WHERE player_id = ?");
                    ps = con.prepareStatement("INSERT INTO inner_ability_skills (player_id, skill_id, skill_level, max_level, rank) VALUES (?, ?, ?, ?, ?)");
                    ps.setInt(1, id);

                    for (int i = 0; i < innerSkills.size(); ++i) {
                        ps.setInt(2, innerSkills.get(i).getSkillId());
                        ps.setInt(3, innerSkills.get(i).getSkillLevel());
                        ps.setInt(4, innerSkills.get(i).getMaxLevel());
                        ps.setInt(5, innerSkills.get(i).getRank());
                        ps.executeUpdate();
                    }
                    ps.close();
                }
            }

            saveInventory(con);

            if (changed_questinfo) {
                deleteWhereCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO questinfo (`characterid`, `quest`, `customData`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (final Entry<Integer, String> q : questinfo.entrySet()) {
                    ps.setInt(2, q.getKey());
                    ps.setString(3, q.getValue());
                    ps.execute();
                }
                ps.close();
            }

            deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, id);
            for (final MapleQuestStatus q : quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();

            if (changed_skills) {
                deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration, slot, equipped) VALUES (?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, id);

                for (final Entry<Skill, SkillEntry> skill : skills.entrySet()) {
                    if (GameConstants.isApplicableSkill(skill.getKey().getId())) { //do not save additional skills
                        ps.setInt(2, skill.getKey().getId());
                        ps.setInt(3, skill.getValue().skillevel);
                        ps.setInt(4, skill.getValue().masterlevel);
                        ps.setLong(5, skill.getValue().expiration);
                        /* 1580 */ ps.setByte(6, ((SkillEntry) skill.getValue()).slot);
                        /* 1581 */ ps.setByte(7, ((SkillEntry) skill.getValue()).equipped);
                        ps.execute();
                    }
                }
                ps.close();
            }

            List<MapleCoolDownValueHolder> cd = getCooldowns();
            if (dc && cd.size() > 0) {
                ps = con.prepareStatement("INSERT INTO skills_cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
                ps.setInt(1, getId());
                for (final MapleCoolDownValueHolder cooling : cd) {
                    ps.setInt(2, cooling.skillId);
                    ps.setLong(3, cooling.startTime);
                    ps.setLong(4, cooling.length);
                    ps.execute();
                }
                ps.close();
            }


            if (changed_savedlocations) {
                deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (final SavedLocationType savedLocationType : SavedLocationType.values()) {
                    if (savedLocations[savedLocationType.getValue()] != -1) {
                        ps.setInt(2, savedLocationType.getValue());
                        ps.setInt(3, savedLocations[savedLocationType.getValue()]);
                        ps.execute();
                    }
                }
                ps.close();
            }

            if (changed_reports) {
                deleteWhereCharacterId(con, "DELETE FROM reports WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO reports VALUES(DEFAULT, ?, ?, ?)");
                for (Entry<ReportType, Integer> achid : reports.entrySet()) {
                    ps.setInt(1, id);
                    ps.setByte(2, achid.getKey().i);
                    ps.setInt(3, achid.getValue());
                    ps.execute();
                }
                ps.close();
            }
            
            if (buddylist.changed()) {
                deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, ?)");
                ps.setInt(1, id);
                for (BuddylistEntry entry : buddylist.getBuddies()) {
                    ps.setInt(2, entry.getCharacterId());
                    ps.setInt(3, entry.isVisible() ? 0 : 1);
                    ps.execute();
                }
                ps.close();
                buddylist.setChanged(false);
            }

            ps = con.prepareStatement("UPDATE accounts SET `nxcredit` = ?, `mPoints` = ?, `nxprepaid` = ?,  `points` = ?, `vpoints` = ?, `redeemhn` = ? WHERE id = ?");
            ps.setInt(1, nxcredit);
            ps.setInt(2, maplepoints);
            ps.setInt(3, nxprepaid);
            ps.setInt(4, points);
            ps.setInt(5, vpoints);
            ps.setInt(6, redeemhn);
            ps.setInt(7, client.getAccID());
            ps.executeUpdate();
            ps.close();

            if (storage != null) {
                storage.saveToDB();
            }
            if (cs != null) {
                cs.save();
            }
            PlayerNPC.updateByCharId(this);
            keylayout.saveKeys(id);
            mount.saveMount(id);
            monsterbook.saveCards(accountid);
            deleteWhereCharacterId(con, "DELETE FROM familiars WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO familiars (characterid, expiry, name, fatigue, vitality, familiar) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            for (MonsterFamiliar f : familiars.values()) {
                ps.setLong(2, f.getExpiry());
                ps.setString(3, f.getName());
                ps.setInt(4, f.getFatigue());
                ps.setByte(5, f.getVitality());
                ps.setInt(6, f.getFamiliar());
                ps.executeUpdate();
            }
            ps.close();
            deleteWhereCharacterId(con, "DELETE FROM imps WHERE characterid = ?");
            ps = con.prepareStatement("INSERT INTO imps (characterid, itemid, closeness, fullness, state, level) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            for (int i = 0; i < imps.length; i++) {
                if (imps[i] != null) {
                    ps.setInt(2, imps[i].getItemId());
                    ps.setShort(3, imps[i].getCloseness());
                    ps.setShort(4, imps[i].getFullness());
                    ps.setByte(5, imps[i].getState());
                    ps.setByte(6, imps[i].getLevel());
                    ps.executeUpdate();
                }
            }
            ps.close();
            if (changed_wishlist) {
                deleteWhereCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?");
                for (int i = 0; i < getWishlistSize(); i++) {
                    ps = con.prepareStatement("INSERT INTO wishlist(characterid, sn) VALUES(?, ?) ");
                    ps.setInt(1, getId());
                    ps.setInt(2, wishlist[i]);
                    ps.execute();
                    ps.close();
                }
            }
            if (changed_trocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?");
                for (int i = 0; i < rocks.length; i++) {
                    if (rocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, rocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }

            if (changed_regrocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM regrocklocations WHERE characterid = ?");
                for (int i = 0; i < regrocks.length; i++) {
                    if (regrocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO regrocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, regrocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            if (changed_hyperrocklocations) {
                deleteWhereCharacterId(con, "DELETE FROM hyperrocklocations WHERE characterid = ?");
                for (int i = 0; i < hyperrocks.length; i++) {
                    if (hyperrocks[i] != 999999999) {
                        ps = con.prepareStatement("INSERT INTO hyperrocklocations(characterid, mapid) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, hyperrocks[i]);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            if (changed_extendedSlots) {
                deleteWhereCharacterId(con, "DELETE FROM extendedslots WHERE characterid = ?");
                for (int i : extendedSlots) {
                    if (getInventory(MapleInventoryType.ETC).findById(i) != null) { //just in case
                        ps = con.prepareStatement("INSERT INTO extendedslots(characterid, itemId) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, i);
                        ps.execute();
                        ps.close();
                    }
                }
            }
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_regrocklocations = false;
            changed_hyperrocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_questinfo = false;
            changed_extendedSlots = false;
            changed_skills = false;
            changed_reports = false;
            con.commit();
        } catch (SQLException | DatabaseException e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            System.err.println(MapleClient.getLogMessage(this, "[charsave] Error saving character data") + e);
            try {
                con.rollback();
            } catch (SQLException ex) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
                System.err.println(MapleClient.getLogMessage(this, "[charsave] Error Rolling Back") + e);
            }
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (pse != null) {
                    pse.close();
                }
                if (rs != null) {
                    rs.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
                System.err.println(MapleClient.getLogMessage(this, "[charsave] Error going back to autocommit mode") + e);
            }
        }
    }
    
    public int wantFame() {
        return wantFame;
    }
    
    public void fameToggle(int onoff) {
        wantFame = onoff;
    }

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        deleteWhereCharacterId(con, sql, id);
    }

    public static void deleteWhereCharacterId(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public static void deleteWhereCharacterId_NoLock(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.execute();
        }
    }
    
    public long getLastAskMasterTime() {
        return askmastertime;
    }

    public long setLastAskMasterTime() {
        return askmastertime = System.currentTimeMillis() / 60000;
    }
    
    public void setMaster(int mstr) {
        this.master = mstr;
    }
    
    public boolean hasMaster() {
        if (master > 0) {
            return true;
        } else {
            return false;
      }
    }
    
    public void setApprentice(int app) {
        this.apprentice = app;
    }
    
    public boolean hasApprentice() {
        if (apprentice > 0) {
            return true;
        } else {
            return false;
      }
    }

    public int getMaster() {
        return this.master;
    }
    
    public int getApprentice() {
        return this.apprentice;
    }
    
    public MapleCharacter getApp() {
        return client.getChannelServer().getPlayerStorage().getCharacterById(this.apprentice);
    }
    
    public MapleCharacter getMster() {
        return client.getChannelServer().getPlayerStorage().getCharacterById(this.master);
    }
    
    public static boolean fuckToon() {
        // This is if Toon wants to fuck with Eric.
        // This will check from 'trolled' column if Toon's Account ID is in there.
        // Every time he logs in or tries to unban himself he'll never know it's stored here!
        // It will continue to block/'ip ban' his account and he can't connect anymore.
        // This will only work for Toon, i'm just using the un-used 'trolled' column LOOOL xD
        try {
            int accountId = 0;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM trolled");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                accountId = rs.getInt("accountid");
            }
            return accountId == 2318; // Toon
        } catch (SQLException e) {
        }
            return false;
    }
    
    
    
    public void setHpMp(int hpmp) {
        getStat().setHp(hpmp, this);
        updateSingleStat(MapleStat.HP, hpmp);
        getStat().setMp(hpmp, this);
        updateSingleStat(MapleStat.MP, hpmp);
    }
    
    public int pvpVictim() {
        return pvpVictim;
    }
    
    public void setPvpVictim(int vic) {
        pvpVictim = vic;
    }
    
    public long getLastAskDualTime() {
        return askdualtime;
    }

    public long setLastAskDualTime() {
        return askdualtime = System.currentTimeMillis() / 60000;
    }

    public byte[] makeHPBarPacket(MapleCharacter player) {
        // 01 = Red || 02 = Blue || 03 = Yellow || 04 = Green || 05 = Black[BG?] || 06 = Light Green || 07 = Purple
        // 9999999 || 9300442 - Ice Knight || 9300460 - ??? || 9300471 - Skeleton || 9410070 - Pink Bean 
        // 9410166 - Anime girl?
            byte tagcolor = 01;
            byte tagbgcolor = 05;
            long hp = player.getStat().getHp();
            long maxhp = player.getStat().getMaxHp();
            return MobPacket.showBossHPPlayer(9300442, hp, maxhp, tagcolor, tagbgcolor);
        }
    
    public void handlePvpDmg(int damage) {
        if ((getStat().dodgeChance > 0) && (Randomizer.nextInt(100) < getStat().dodgeChance)) {
            getClient().getSession().write(CField.EffectPacket.showForeignEffect(20));
            return;
        }    
        if (getBuffedValue(MapleBuffStat.MAGIC_GUARD) != null) {
            int hploss = 0; int mploss = 0;
            mploss = (int)(damage * (getBuffedValue(MapleBuffStat.MAGIC_GUARD).doubleValue() / 100.0D));
            hploss = damage - mploss;
            if (getBuffedValue(MapleBuffStat.INFINITY) != null) {
                mploss = 0;
            } else if (mploss > stats.getMp()) {
                mploss = stats.getMp();
                hploss = damage - mploss;
            }
            addMPHP(-hploss, -mploss);
        } else if (getStat().mesoGuardMeso > 0.0D) {
            int mesoloss = (int)(damage * (getStat().mesoGuardMeso / 100.0D));
            if (getMeso() < mesoloss) {
                gainMeso(-getMeso(), false);
                cancelBuffStats(new MapleBuffStat[] {MapleBuffStat.MESOGUARD});
            } else {
                gainMeso(-mesoloss, false);
            }
            addMPHP(-damage, 0);
        } else {    
            addMPHP(-damage, 0);
        }
        if (!GameConstants.GMS) {
            handleBattleshipHP(-damage);
        }
        
        byte[] dmgPacket = CField.damagePlayer(getId(), -2, damage, -1, (byte) -1, -1, -1, false, -1, (byte) -1, null, (byte) -1, -1, 0);
        getMap().broadcastMessage(dmgPacket);
        getMap().broadcastMessage(MobPacket.showMonsterHP(getObjectId(), getStat().getHPPercent()));
    }
    
    public boolean wantHit() {
        return wantHit;
    }
    
    public void toggleHit(boolean hittable) {
        wantHit = hittable;
    }
    
    public boolean fakeDamage() {
        return fakeDamage;
    }
    
    public void setFakeDamage(boolean fakedmg) {
        fakeDamage = fakedmg;
    }

    public void saveInventory(final Connection con) throws SQLException {
        List<Pair<Item, MapleInventoryType>> listing = new ArrayList<>();
        for (final MapleInventory iv : inventory) {
            for (final Item item : iv.list()) {
                listing.add(new Pair<>(item, iv.getType()));
            }
        }
        if (con != null) {
            ItemLoader.INVENTORY.saveItems(listing, con, id);
        } else {
            ItemLoader.INVENTORY.saveItems(listing, id);
        }
    }

    public final PlayerStats getStat() {
        return stats;
    }

    public final void QuestInfoPacket(final tools.data.MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(questinfo.size()); // // Party Quest data (quest needs to be added in the quests list)

        for (final Entry<Integer, String> q : questinfo.entrySet()) {
            mplew.writeShort(q.getKey());
            mplew.writeMapleAsciiString(q.getValue() == null ? "" : q.getValue());
        }
    }

    public final void updateInfoQuest(final int questid, final String data) {
        questinfo.put(questid, data);
        changed_questinfo = true;
        client.getSession().write(InfoPacket.updateInfoQuest(questid, data));
    }

    public final String getInfoQuest(final int questid) {
        if (questinfo.containsKey(questid)) {
            return questinfo.get(questid);
        }
        return "";
    }

    public final int getNumQuest() {
        int i = 0;
        for (final MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !(q.isCustom())) {
                i++;
            }
        }
        return i;
    }

    public final byte getQuestStatus(final int quest) {
        final MapleQuest qq = MapleQuest.getInstance(quest);
        if (getQuestNoAdd(qq) == null) {
            return 0;
        }
        return getQuestNoAdd(qq).getStatus();
    }

    public final MapleQuestStatus getQuest(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            return new MapleQuestStatus(quest, (byte) 0);
        }
        return quests.get(quest);
    }

    public final void setQuestAdd(final MapleQuest quest, final byte status, final String customData) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus stat = new MapleQuestStatus(quest, status);
            stat.setCustomData(customData);
            quests.put(quest, stat);
        }
    }

    public final MapleQuestStatus getQuestNAdd(final MapleQuest quest) {
        if (!quests.containsKey(quest)) {
            final MapleQuestStatus status = new MapleQuestStatus(quest, (byte) 0);
            quests.put(quest, status);
            return status;
        }
        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestNoAdd(final MapleQuest quest) {
        return quests.get(quest);
    }

    public final MapleQuestStatus getQuestRemove(final MapleQuest quest) {
        return quests.remove(quest);
    }

    public final void updateQuest(final MapleQuestStatus quest) {
        updateQuest(quest, false);
    }

    public final void updateQuest(final MapleQuestStatus quest, final boolean update) {
        quests.put(quest.getQuest(), quest);
        if (!(quest.isCustom())) {
            client.getSession().write(InfoPacket.updateQuest(quest));
            if (quest.getStatus() == 1 && !update) {
                client.getSession().write(CField.updateQuestInfo(this, quest.getQuest().getId(), quest.getNpc(), (byte) 10));
            }
        }
    }

    public final Map<Integer, String> getInfoQuest_Map() {
        return questinfo;
    }

    public final Map<MapleQuest, MapleQuestStatus> getQuest_Map() {
        return quests;
    }

    public Integer getBuffedValue(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : Integer.valueOf(mbsvh.value);
    }

    public final Integer getBuffedSkill_X(final MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getX();
    }

    public final Integer getBuffedSkill_Y(final MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getY();
    }

    public boolean isBuffFrom(MapleBuffStat stat, Skill skill) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        if (mbsvh == null || mbsvh.effect == null) {
            return false;
        }
        return mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skill.getId();
    }

    public int getBuffSource(MapleBuffStat stat) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        return mbsvh == null ? -1 : mbsvh.effect.getSourceId();
    }

    public int getTrueBuffSource(MapleBuffStat stat) {
        final MapleBuffStatValueHolder mbsvh = effects.get(stat);
        return mbsvh == null ? -1 : (mbsvh.effect.isSkill() ? mbsvh.effect.getSourceId() : -mbsvh.effect.getSourceId());
    }

    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[GameConstants.getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public void setBuffedValue(MapleBuffStat effect, int value) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.value = value;
    }

    public void setSchedule(MapleBuffStat effect, ScheduledFuture<?> sched) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        if (mbsvh == null) {
            return;
        }
        mbsvh.schedule.cancel(false);
        mbsvh.schedule = sched;
    }

    public Long getBuffedStarttime(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : Long.valueOf(mbsvh.startTime);
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat effect) {
        final MapleBuffStatValueHolder mbsvh = effects.get(effect);
        return mbsvh == null ? null : mbsvh.effect;
    }

    public void doDragonBlood() {
        final MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.DRAGONBLOOD);
        if (bloodEffect == null) {
            lastDragonBloodTime = 0;
            return;
        }
        prepareDragonBlood();
        if (stats.getHp() - bloodEffect.getX() <= 1) {
            cancelBuffStats(MapleBuffStat.DRAGONBLOOD);
        } else {
            //     addHP(-bloodEffect.getX());
            client.getSession().write(EffectPacket.showOwnBuffEffect(bloodEffect.getSourceId(), 7, getLevel(), bloodEffect.getLevel()));
            map.broadcastMessage(MapleCharacter.this, EffectPacket.showBuffeffect(getId(), bloodEffect.getSourceId(), 7, getLevel(), bloodEffect.getLevel()), false);
        }
    }

    public final boolean canBlood(long now) {
        return lastDragonBloodTime > 0 && lastDragonBloodTime + 4000 < now;
    }

    private void prepareDragonBlood() {
        lastDragonBloodTime = System.currentTimeMillis();
    }

    public void doRecovery() {
        MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.RECOVERY);
        if (bloodEffect == null) {
            bloodEffect = getStatForBuff(MapleBuffStat.MECH_CHANGE);
            if (bloodEffect == null) {
                lastRecoveryTime = 0;
            } else if (bloodEffect.getSourceId() == 35121005) {
                prepareRecovery();
                if (stats.getMp() < bloodEffect.getU()) {
                    cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                    cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
                } else {
                    addMP(-bloodEffect.getU());
                }
            }
        } else {
            prepareRecovery();
            if (stats.getHp() >= stats.getCurrentMaxHp()) {
                cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
            } else {
                healHP(bloodEffect.getX());
            }
        }
    }

    public final boolean canRecover(long now) {
        return lastRecoveryTime > 0 && lastRecoveryTime + 5000 < now;
    }

    private void prepareRecovery() {
        lastRecoveryTime = System.currentTimeMillis();
    }
    
    public void startMapEffect(String msg, int itemId) {
        startMapEffect(msg, itemId, 30000);
    }

    public void startMapEffect(String msg, int itemId, int duration) {
        final MapleMapEffect mapEffect = new MapleMapEffect(msg, itemId);
        getClient().getSession().write(mapEffect.makeStartData());
        MapTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                getClient().getSession().write(mapEffect.makeDestroyData());
            }
        }, duration);
    }

    public void startMapTimeLimitTask(int time, final MapleMap to) {
        if (time <= 0) { //jail
            time = 1;
        }
        client.getSession().write(CField.getClock(time));
        final MapleMap ourMap = getMap();
        time *= 1000;
        mapTimeLimitTask = MapTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                if (ourMap.getId() == GameConstants.JAIL) {
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME)).setCustomData(String.valueOf(System.currentTimeMillis()));
                    getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData("0"); //release them!
                    changeMap(getMap().getCrap2(), to.getPortal(0));
                } else {
                    changeMap(to, to.getPortal(0));
                }

            }
        }, time, time);
    }

    public boolean canDOT(long now) {
        return lastDOTTime > 0 && lastDOTTime + 8000 < now;
    }

    public boolean hasDOT() {
        return dotHP > 0;
    }

    public void doDOT() {
        addHP(-(dotHP * 4));
        dotHP = 0;
        lastDOTTime = 0;
    }

    public void setDOT(int d, int source, int sourceLevel) {
        this.dotHP = d;
        addHP(-(dotHP * 4));
        map.broadcastMessage(CField.getPVPMist(id, source, sourceLevel, d));
        lastDOTTime = System.currentTimeMillis();
    }

    public void startFishingTask() {
        cancelFishingTask();
        lastFishingTime = System.currentTimeMillis();
    }

    public boolean canFish(long now) {
        return lastFishingTime > 0 && lastFishingTime + GameConstants.getFishingTime(stats.canFishVIP, isGM()) < now;
    }

    public void doFish(long now) {
        lastFishingTime = now;
        final boolean expMulti = haveItem(2300001, 1, false, true);
        if (client == null || client.getPlayer() == null || !client.isReceiving() || (!expMulti && !haveItem(GameConstants.GMS ? 2270008 : 2300000, 1, false, true)) || !GameConstants.isFishingMap(getMapId()) || !stats.canFish || chair <= 0) {
            cancelFishingTask();
            return;
        }
        MapleInventoryManipulator.removeById(client, MapleInventoryType.USE, expMulti ? 2300001 : (GameConstants.GMS ? 2270008 : 2300000), 1, false, false);
        boolean passed = false;
        while (!passed) {
            int randval = RandomRewards.getFishingReward();
            switch (randval) {
                case 0: // Meso
                    final int money = Randomizer.rand(expMulti ? 15 : 10, expMulti ? 75000 : 50000);
                    gainMeso(money, true);
                    passed = true;
                    break;
                case 1: // EXP
                    final int experi = Math.min(Randomizer.nextInt(Math.abs(getNeededExp() / 200) + 1), 500000);
                    gainExp(expMulti ? (experi * 3 / 2) : experi, true, false, true);
                    passed = true;
                    break;
                default:
                    if (MapleItemInformationProvider.getInstance().itemExists(randval)) {
                        MapleInventoryManipulator.addById(client, randval, (short) 1, "Fishing" + " on " + FileoutputUtil.CurrentReadable_Date());
                        passed = true;
                    }
                    break;
            }
        }
    }
    
    public int getReactorClicks() {
        return reactorCount;
    }
    
    public int getCount() {
        return reactorCount;
    }
    
    public void addCount(int amount) {
        reactorCount += amount;
    }
    
    public void setCount(int count) {
        reactorCount = count;
    }
    
    public boolean inAzwan() {
        return mapid / 1000000 == 262;
    }

    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
            mapTimeLimitTask = null;
        }
    }

    public int getNeededExp() {
        return GameConstants.getExpNeededForLevel(level);
    }

    public void cancelFishingTask() {
        lastFishingTime = 0;
    }
    
    private byte numAntiKSMonsters = 0;

    public byte getNumAntiKSMonsters() {
          return numAntiKSMonsters;
    }
  
    public void incAntiKSNum() {
          numAntiKSMonsters++;
    }

    public void decAntiKSNum() {
       if (numAntiKSMonsters > 0) {
           numAntiKSMonsters--;
       }
    }
    
    public void toggleTestingDPS(){
        this.testingdps = !testingdps;
    }
    
    public static int rand(int l, int u) {
        return Randomizer.nextInt(u - l + 1) + l;
    }
    
    public void updateAP() {
        // idk how i fucked this up when removing Auto Assign, but meh.
        updateSingleStat(MapleStat.STR, client.getPlayer().getStat().getStr());
        updateSingleStat(MapleStat.DEX, client.getPlayer().getStat().getDex());
        updateSingleStat(MapleStat.INT, client.getPlayer().getStat().getInt());
        updateSingleStat(MapleStat.LUK, client.getPlayer().getStat().getLuk());
    }
    
    // Cheat Tracker
    private long[] lastTime = new long[6];
    public synchronized boolean Spam(int limit, int type) {
        if (type < 0 || lastTime.length < type) {
            type = 1; // default xD
        }
        if (System.currentTimeMillis() < limit + lastTime[type]) {
            return true;
        }
        lastTime[type] = System.currentTimeMillis();
        return false;
    }
    // End of Cheat Tracker
    
    public boolean isTestingDPS() {
        return testingdps;
    }
    
    public int getPvpKills() {
        return pvpKills;
    }
    
    public int getPvpDeaths() {
        return pvpDeaths;
    }
    
    public void setPvpKills(int kills) {
        pvpKills = kills;
    }
    
    public void setPvpDeaths(int deaths) {
        pvpDeaths = deaths;
    }
    
    public void gainPvpKills(int kills) {
        pvpKills += kills;
    }
    
    public void gainPvpDeaths(int amt) {
        pvpDeaths += amt;
    }
    
    public void gainPvpKill() {
        pvpKills += 1;
    }
    
    public void gainPvpDeath() {
        pvpDeaths += 1;
    }
    
    public boolean canHoldMeso(int mesos) {
        // True = Can Hold || False = Can't Hold
        if (meso + mesos >= Integer.MAX_VALUE) { // max mesos
            return false;
        } else if (meso + mesos >= Integer.MAX_VALUE - 10) { // max mesos minus 10, just incase they go over we want to check if we'll exceed.
            return false;
        } else {
            return true;
        }
    }
    
    public void gainMeso(int mesos) {
        meso += mesos;
        updateSingleStat(MapleStat.MESO, meso, false); // (Y)
    }
    
    public void setMeso(int mesos) {
        meso = mesos;
    }
    
    public boolean getCustomFace() {
        int[] faces = {21992, 21999};
         for (int i : faces) {
             if (this.getFace() == i) {
                 return true;
             }
         }
            return false;
    }

    public boolean getCustomHair() {
        int[] hairs = {39997};
         for (int i : hairs) {
             if (this.getHair() == i) {
                 return true;
             }
         }
            return false;
    }
    
    public boolean isPresident() {
        if (World.president.equalsIgnoreCase(getName())) {
            return true;
        } else {
            return false;
           }
    }

    public void setDPS(long newdps) {
        dps = newdps;
    }

    public long getDPS() {
        return dps;
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, int from) {
        registerEffect(effect, starttime, schedule, effect.getStatups(), false, effect.getDuration(), from);
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, Map<MapleBuffStat, Integer> statups, boolean silent, final int localDuration, final int cid) {
        if (effect.isHide()) {
            if (this.isHidden()) 
                map.broadcastMessage(this, CField.spawnPlayerMapobject(this),false);
            else
                map.broadcastNONGMMessage(this, CField.removePlayerFromMap(getId()), false);
        }
        if (effect.isDragonBlood()) {
            prepareDragonBlood();
        } else if (effect.isRecovery()) {
            prepareRecovery();
        } else if (effect.isBerserk()) {
            checkBerserk();
        } else if (effect.isMonsterRiding_()) {
            getMount().startSchedule();
        }
        for (Entry<MapleBuffStat, Integer> statup : statups.entrySet()) {
            int value = statup.getValue().intValue();
            if (statup.getKey() == MapleBuffStat.MONSTER_RIDING) {
                if (effect.getSourceId() == 5221006 && battleshipHP <= 0) {
                    battleshipHP = maxBattleshipHP(effect.getSourceId()); //copy this as well
                }
                removeFamiliar();
            }
            effects.put(statup.getKey(), new MapleBuffStatValueHolder(effect, starttime, schedule, value, localDuration, cid));
        }
        if (!silent) {
            stats.recalcLocalStats(this);
        }
        //System.out.println("Effect registered. Effect: " + effect.getSourceId());
    }

    public List<MapleBuffStat> getBuffStats(final MapleStatEffect effect, final long startTime) {
        final List<MapleBuffStat> bstats = new ArrayList<>();
        final Map<MapleBuffStat, MapleBuffStatValueHolder> allBuffs = new EnumMap<>(effects);
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> stateffect : allBuffs.entrySet()) {
            final MapleBuffStatValueHolder mbsvh = stateffect.getValue();
            if (mbsvh.effect.sameSource(effect) && (startTime == -1 || startTime == mbsvh.startTime || stateffect.getKey().canStack())) {
                bstats.add(stateffect.getKey());
            }
        }
        return bstats;
    }

    private void deregisterBuffStats(List<MapleBuffStat> stats) {
        List<MapleBuffStatValueHolder> effectsToCancel = new ArrayList<>(stats.size());
        for (MapleBuffStat stat : stats) {
            final MapleBuffStatValueHolder mbsvh = effects.remove(stat);
            if (mbsvh != null) {
                boolean addMbsvh = true;
                for (MapleBuffStatValueHolder contained : effectsToCancel) {
                    if (mbsvh.startTime == contained.startTime && contained.effect == mbsvh.effect) {
                        addMbsvh = false;
                    }
                }
                if (addMbsvh) {
                    effectsToCancel.add(mbsvh);
                }
                if (stat == MapleBuffStat.SUMMON || stat == MapleBuffStat.PUPPET || stat == MapleBuffStat.REAPER || stat == MapleBuffStat.BEHOLDER || stat == MapleBuffStat.DAMAGE_BUFF || stat == MapleBuffStat.RAINING_MINES || stat == MapleBuffStat.ANGEL_ATK) {
                    final int summonId = mbsvh.effect.getSourceId();
                    final List<MapleSummon> toRemove = new ArrayList<>();
                    visibleMapObjectsLock.writeLock().lock(); //We need to lock this later on anyway so do it now to prevent deadlocks.
                    summonsLock.writeLock().lock();
                    try {
                        for (MapleSummon summon : summons) {
                            if (summon.getSkill() == summonId || (stat == MapleBuffStat.RAINING_MINES && summonId == 33101008) || (summonId == 35121009 && summon.getSkill() == 35121011) || ((summonId == 86 || summonId == 88 || summonId == 91) && summon.getSkill() == summonId + 999) || ((summonId == 1085 || summonId == 1087 || summonId == 1090 || summonId == 1179) && summon.getSkill() == summonId - 999)) { //removes bots n tots
                                map.broadcastMessage(SummonPacket.removeSummon(summon, true));
                                map.removeMapObject(summon);
                                visibleMapObjects.remove(summon);
                                toRemove.add(summon);
                            }
                        }
                        for (MapleSummon s : toRemove) {
                            summons.remove(s);
                        }
                    } finally {
                        summonsLock.writeLock().unlock();
                        visibleMapObjectsLock.writeLock().unlock(); //lolwut
                    }
                    if (summonId == 3111005 || summonId == 3211005) {
                        cancelEffectFromBuffStat(MapleBuffStat.SPIRIT_LINK);
                    }
                } else if (stat == MapleBuffStat.DRAGONBLOOD) {
                    lastDragonBloodTime = 0;
                } else if (stat == MapleBuffStat.RECOVERY || mbsvh.effect.getSourceId() == 35121005) {
                    lastRecoveryTime = 0;
                } else if (stat == MapleBuffStat.HOMING_BEACON || stat == MapleBuffStat.ARCANE_AIM) {
                    linkMobs.clear();
                }
            }
            for (MapleBuffStatValueHolder cancelEffectCancelTasks : effectsToCancel) {
                if (getBuffStats(cancelEffectCancelTasks.effect, cancelEffectCancelTasks.startTime).isEmpty()) {
                    if (cancelEffectCancelTasks.schedule != null) {
                        cancelEffectCancelTasks.schedule.cancel(false);
                    }
                }

            }
        }
    }
    
    public boolean inJQ() {
        boolean injq = false;
        for (int i = 0; i < jqmaps.length; i++) {
            if (getMapId() == jqmaps[i]) {
                injq = true;
            }
        }
        return injq;
    }

    public int[] jqmaps = {
        //Official AutoJQ's:
        280020000, // zakum
        280020001, // zakum part 2
        220000006, //ludi
        100000202, //henesys
        922020000, //that one dark invisible field jq
        682000200, //chimney jq
        690000067, // Forest of patience
        690000066, // Forest of patience
        690000068,
        690000069,
        690000070,
        105040310, //the one with the big red balls o-e
        105040311, // - Hidden Street - The Deep Forest of Patience
        105040312, // - Hidden Street - The Deep Forest of Patience
        105040313, // - Hidden Street - The Deep Forest of Patience
        105040314, // - Hidden Street - The Deep Forest of Patience
        105040315, // - Hidden Street - The Deep Forest of Patience
        910530000,
        910530001,
        105040316, // - Hid
        101000100, //main ellinia jq part 1
        101000101, //main ellinia jq part 2
        101000102, //main ellinia jq part 3
        101000103, //main ellinia jq part 4
        101000104, //main ellinia jq part 5
        //GM Jq's (!startevent)
        109040000, //GM Stage pt 0 - entering jq
        109040001, //GM Stage pt 1 - big stage
        109040002, //GM Stage pt 2 - going to the right
        109040003, //GM Stage pt 3 - all the major obstacales
        109040004, //GM Stage pt 4 - Final stage
        910130000,
        910130001,
        910130100,
        910130101,
        910130102, 
    };
    
    public boolean inTutorial() {
        boolean intutorial = false;
        for (int i = 0; i < tutmaps.length; i++) {
            if (getMapId() == tutmaps[i]) {
                intutorial = true;
            }
        }
        return intutorial;
    }

    public int[] tutmaps = {
        90000000,
        90000001,
        90000002,
        90000003,
        90000004,
        90000009,
        1337, // universe
    };

    public void createCleanInstance() {
        // Instancing -- like v83.
        // Handles a character in their own virtual world, no hearing or doing anything from anyone else.
        int instanceid = EventScriptManager.getNewInstanceMapId();
        map = getClient().getChannelServer().getMapFactory().CreateInstanceMap(getMapId(), false, false, false, instanceid);
        mapid = getMapId();
        // fakeRelog(); // if we were to use a NPC method.. We'd 
        System.out.println("Created new instance ID \\" + instanceid + "\\ on map " + mapid + ".");
    }
    
    public void createInstance() {
        // Instancing -- like v83.
        // This instancing will handle the usage of NPC's, Respawning, and Reactors.
        // Do note, to fully enter the instance you will need to be added to the map, or, re-added.
        // We need to be able to get player's to join instances if we were to ever make party-based maps and such. :)
        int instanceid = EventScriptManager.getNewInstanceMapId();
        map = getClient().getChannelServer().getMapFactory().CreateInstanceMap(getMapId(), false, true, false, instanceid);
        mapid = getMapId();
        // fakeRelog(); // if we were to use a NPC method.. We'd 
        System.out.println("Created new instance ID \\" + instanceid + "\\ on map " + mapid + ".");
    }
    
    /**
     * @param effect
     * @param overwrite when overwrite is set no data is sent and all the
     * Buffstats in the StatEffect are deregistered
     * @param startTime
     */
    public void cancelEffect(final MapleStatEffect effect, final boolean overwrite, final long startTime) {
        if (effect == null) {
            return;
        }
        cancelEffect(effect, overwrite, startTime, effect.getStatups());
    }

    public void cancelEffect(final MapleStatEffect effect, final boolean overwrite, final long startTime, Map<MapleBuffStat, Integer> statups) {
        if (effect == null) {
            return;
        }
        List<MapleBuffStat> buffstats;
        if (!overwrite) {
            buffstats = getBuffStats(effect, startTime);
        } else {
            buffstats = new ArrayList<>(statups.keySet());
        }
        if (buffstats.size() <= 0) {
            return;
        }
        if (effect.isInfinity() && getBuffedValue(MapleBuffStat.INFINITY) != null) { //before
            int duration = Math.max(effect.getDuration(), effect.alchemistModifyVal(this, effect.getDuration(), false));
            final long start = getBuffedStarttime(MapleBuffStat.INFINITY);
            duration += (int) ((start - System.currentTimeMillis()));
            if (duration > 0) {
                final int neworbcount = getBuffedValue(MapleBuffStat.INFINITY) + effect.getDamage();
                final Map<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
                stat.put(MapleBuffStat.INFINITY, neworbcount);
                setBuffedValue(MapleBuffStat.INFINITY, neworbcount);
                client.getSession().write(BuffPacket.giveBuff(effect.getSourceId(), duration, stat, effect));
                addHP((int) (effect.getHpR() * this.stats.getCurrentMaxHp()));
                addMP((int) (effect.getMpR() * this.stats.getCurrentMaxMp(this.getJob())));
                setSchedule(MapleBuffStat.INFINITY, BuffTimer.getInstance().schedule(new CancelEffectAction(this, effect, start, stat), effect.alchemistModifyVal(this, 4000, false)));
                return;
            }
        }
        deregisterBuffStats(buffstats);
        if (effect.isMagicDoor()) {
            // remove for all on maps
            if (!getDoors().isEmpty()) {
                removeDoor();
                silentPartyUpdate();
            }
        } else if (effect.isMechDoor()) {
            if (!getMechDoors().isEmpty()) {
                removeMechDoor();
            }
        } else if (effect.isMonsterRiding_()) {
            getMount().cancelSchedule();
        } else if (effect.isMonsterRiding()) {
            cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        } else if (effect.isAranCombo()) {
            combo = 0;
        }
        // check if we are still logged in o.o
        cancelPlayerBuffs(buffstats, overwrite);
        if (!overwrite) {
            if (effect.isHide() && client.getChannelServer().getPlayerStorage().getCharacterById(this.getId()) != null) { //Wow this is so fking hacky...
                map.broadcastMessage(this, CField.spawnPlayerMapobject(this), false);

                for (final MaplePet pet : pets) {
                    if (pet.getSummoned()) {
                        map.broadcastMessage(this, PetPacket.showPet(this, pet, false, false), false);
                    }
                }
            }
        }
        if (effect.getSourceId() == 35121013 && !overwrite) { //when siege 2 deactivates, missile re-activates
            SkillFactory.getSkill(35121005).getEffect(getTotalSkillLevel(35121005)).applyTo(this);
        }
        //System.out.println("Effect deregistered. Effect: " + effect.getSourceId());
    }

    public void cancelBuffStats(MapleBuffStat... stat) {
        List<MapleBuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList);
        cancelPlayerBuffs(buffStatList, false);
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        if (effects.get(stat) != null) {
            cancelEffect(effects.get(stat).effect, false, -1);
        }
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat, int from) {
        if (effects.get(stat) != null && effects.get(stat).cid == from) {
            cancelEffect(effects.get(stat).effect, false, -1);
        }
    }

    private void cancelPlayerBuffs(List<MapleBuffStat> buffstats, boolean overwrite) {
        boolean write = client != null && client.getChannelServer() != null && client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
        if (buffstats.contains(MapleBuffStat.HOMING_BEACON)) {
            client.getSession().write(BuffPacket.cancelHoming());
        } else {
            if (overwrite) {
                List<MapleBuffStat> z = new ArrayList<>();
                for (MapleBuffStat s : buffstats) {
                    if (s.canStack()) {
                        z.add(s);
                    }
                }
                if (z.size() > 0) {
                    buffstats = z;
                } else {
                    return; //don't write anything
                }
            } else if (write) {
                stats.recalcLocalStats(this);
            }
            client.getSession().write(BuffPacket.cancelBuff(buffstats));
            map.broadcastMessage(this, BuffPacket.cancelForeignBuff(getId(), buffstats), false);
        }
    }

    public void dispel() {
        if (!isHidden()) {
            final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());
            for (MapleBuffStatValueHolder mbsvh : allBuffs) {
                if (mbsvh.effect.isSkill() && mbsvh.schedule != null && !mbsvh.effect.isMorph() && !mbsvh.effect.isGmBuff() && !mbsvh.effect.isMonsterRiding() && !mbsvh.effect.isMechChange() && !mbsvh.effect.isEnergyCharge() && !mbsvh.effect.isAranCombo()) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            }
        }
    }

    public void dispelSkill(int skillid) {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public void dispelSummons() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSummonMovementType() != null) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    public void dispelBuff(int skillid) {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSourceId() == skillid) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public void cancelAllBuffs_() {
        effects.clear();
    }

    public void cancelAllBuffs() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
        }
    }

    public void cancelMorphs() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            switch (mbsvh.effect.getSourceId()) {
                case 5111005:
                case 5121003:
                case 15111002:
                case 13111005:
                    return; // Since we can't have more than 1, save up on loops
                default:
                    if (mbsvh.effect.isMorph()) {
                        cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                        continue;
                    }
            }
        }
    }

    public int getMorphState() {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMorph()) {
                return mbsvh.effect.getSourceId();
            }
        }
        return -1;
    }

    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        if (buffs == null) {
            return;
        }
        for (PlayerBuffValueHolder mbsvh : buffs) {
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime, mbsvh.localDuration, mbsvh.statup, mbsvh.cid);
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        final List<PlayerBuffValueHolder> ret = new ArrayList<>();
        final Map<Pair<Integer, Byte>, Integer> alreadyDone = new HashMap<>();
        final LinkedList<Entry<MapleBuffStat, MapleBuffStatValueHolder>> allBuffs = new LinkedList<>(effects.entrySet());
        for (Entry<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : allBuffs) {
            final Pair<Integer, Byte> key = new Pair<>(mbsvh.getValue().effect.getSourceId(), mbsvh.getValue().effect.getLevel());
            if (alreadyDone.containsKey(key)) {
                ret.get(alreadyDone.get(key)).statup.put(mbsvh.getKey(), mbsvh.getValue().value);
            } else {
                alreadyDone.put(key, ret.size());
                final EnumMap<MapleBuffStat, Integer> list = new EnumMap<>(MapleBuffStat.class);
                list.put(mbsvh.getKey(), mbsvh.getValue().value);
                ret.add(new PlayerBuffValueHolder(mbsvh.getValue().startTime, mbsvh.getValue().effect, list, mbsvh.getValue().localDuration, mbsvh.getValue().cid));
            }
        }
        return ret;
    }

    public void cancelMagicDoor() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMagicDoor()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    public int getSkillLevel(int skillid) {
        return getSkillLevel(SkillFactory.getSkill(skillid));
    }

    public int getTotalSkillLevel(int skillid) {
        return getTotalSkillLevel(SkillFactory.getSkill(skillid));
    }

    public final void handleEnergyCharge(final int skillid, final int targets) {
        final Skill echskill = SkillFactory.getSkill(skillid);
        final int skilllevel = getTotalSkillLevel(echskill);
        if (skilllevel > 0) {
            final MapleStatEffect echeff = echskill.getEffect(skilllevel);
            if (targets > 0) {
                if (getBuffedValue(MapleBuffStat.ENERGY_CHARGE) == null) {
                //    echeff.applyEnergyBuff(this, true); // Infinity time
                } else {
                    Integer energyLevel = getBuffedValue(MapleBuffStat.ENERGY_CHARGE);
                    //TODO: bar going down
                    if (energyLevel < 10000) {
                        energyLevel += (echeff.getX() * targets);

                        client.getSession().write(EffectPacket.showOwnBuffEffect(skillid, 2, getLevel(), skilllevel));
                        map.broadcastMessage(this, EffectPacket.showBuffeffect(id, skillid, 2, getLevel(), skilllevel), false);

                        if (energyLevel >= 10000) {
                            energyLevel = 10000;
                        }
                        client.getSession().write(BuffPacket.giveEnergyChargeTest(energyLevel, echeff.getDuration() / 1000));
                        setBuffedValue(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(energyLevel));
                    } else if (energyLevel == 10000) {
                      //  echeff.applyEnergyBuff(this, false); // One with time
                        setBuffedValue(MapleBuffStat.ENERGY_CHARGE, Integer.valueOf(10001));
                    }
                }
            }
        }
    }

    public final void handleBattleshipHP(int damage) {
        if (damage < 0) {
            final MapleStatEffect effect = getStatForBuff(MapleBuffStat.MONSTER_RIDING);
            if (effect != null && effect.getSourceId() == 5221006) {
                battleshipHP += damage;
                client.getSession().write(CField.skillCooldown(5221999, battleshipHP / 10));
                if (battleshipHP <= 0) {
                    battleshipHP = 0;
                    client.getSession().write(CField.skillCooldown(5221006, effect.getCooldown(this)));
                    addCooldown(5221006, System.currentTimeMillis(), effect.getCooldown(this) * 1000);
                    cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                }
            }
        }
    }

    public final void handleOrbgain() {
        int orbcount = getBuffedValue(MapleBuffStat.COMBO);
        Skill comboh;
        Skill advcombo;

        switch (getJob()) {
            case 1110:
            case 1111:
            case 1112:
                comboh = SkillFactory.getSkill(11111001);
                advcombo = SkillFactory.getSkill(11110005);
                break;
            default:
                comboh = SkillFactory.getSkill(1111002);
                advcombo = SkillFactory.getSkill(1120003);
                break;
        }

        MapleStatEffect ceffect;
        int advComboSkillLevel = getTotalSkillLevel(advcombo);
        if (advComboSkillLevel > 0) {
            ceffect = advcombo.getEffect(advComboSkillLevel);
        } else if (getSkillLevel(comboh) > 0) {
            ceffect = comboh.getEffect(getTotalSkillLevel(comboh));
        } else {
            return;
        }

        if (orbcount < ceffect.getX() + 1) {
            int neworbcount = orbcount + 1;
            if (advComboSkillLevel > 0 && ceffect.makeChanceResult()) {
                if (neworbcount < ceffect.getX() + 1) {
                    neworbcount++;
                }
            }
            EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
            stat.put(MapleBuffStat.COMBO, neworbcount);
            setBuffedValue(MapleBuffStat.COMBO, neworbcount);
            int duration = ceffect.getDuration();
            duration += (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));

            client.getSession().write(BuffPacket.giveBuff(comboh.getId(), duration, stat, ceffect));
            map.broadcastMessage(this, BuffPacket.giveForeignBuff(getId(), stat, ceffect), false);
        }
    }

    public void handleOrbconsume(int howmany) {
        Skill comboh;

        switch (getJob()) {
            case 1110:
            case 1111:
            case 1112:
                comboh = SkillFactory.getSkill(11111001);
                break;
            default:
                comboh = SkillFactory.getSkill(1111002);
                break;
        }
        if (getSkillLevel(comboh) <= 0) {
            return;
        }
        MapleStatEffect ceffect = getStatForBuff(MapleBuffStat.COMBO);
        if (ceffect == null) {
            return;
        }
        EnumMap<MapleBuffStat, Integer> stat = new EnumMap<>(MapleBuffStat.class);
        stat.put(MapleBuffStat.COMBO, Math.max(1, getBuffedValue(MapleBuffStat.COMBO) - howmany));
        setBuffedValue(MapleBuffStat.COMBO, Math.max(1, getBuffedValue(MapleBuffStat.COMBO) - howmany));
        int duration = ceffect.getDuration();
        duration += (int) ((getBuffedStarttime(MapleBuffStat.COMBO) - System.currentTimeMillis()));

        client.getSession().write(BuffPacket.giveBuff(comboh.getId(), duration, stat, ceffect));
        map.broadcastMessage(this, BuffPacket.giveForeignBuff(getId(), stat, ceffect), false);
    }

    public void silentEnforceMaxHpMp() {
        stats.setMp(stats.getMp(), this);
        stats.setHp(stats.getHp(), true, this);
    }

    public void enforceMaxHpMp() {
        Map<MapleStat, Integer> statups = new EnumMap<>(MapleStat.class);
        if (stats.getMp() > stats.getCurrentMaxMp(this.getJob())) {
            stats.setMp(stats.getMp(), this);
            statups.put(MapleStat.MP, Integer.valueOf(stats.getMp()));
        }
        if (stats.getHp() > stats.getCurrentMaxHp()) {
            stats.setHp(stats.getHp(), this);
            statups.put(MapleStat.HP, Integer.valueOf(stats.getHp()));
        }
        if (statups.size() > 0) {
            client.getSession().write(CWvsContext.updatePlayerStats(statups, this));
        }
    }

    public MapleMap getMap() {
        return map;
    }

    public MonsterBook getMonsterBook() {
        return monsterbook;
    }

    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }
    
    public boolean getEventMap() {
        if (getMapId() == 690000067 || getMapId() == 690000066) {
            return true; // Forest of Patience
        } else if (getMapId() == 280020001 || getMapId() == 280020000) {
            return true; // Zakum
        } else if (getMapId() == 109040004 || getMapId() == 109040000 || getMapId() == 109040001 || getMapId() == 109040002 || getMapId() == 109040003) {
            return true; // Fitness
        } else if (getMapId() == 910130102 || getMapId() == 910130100 || getMapId() == 910130101) {
            return true; // Forest of Endurance
        } else if (getMapId() == 910530001 || getMapId() == 910530000) {
            return true; // Forest of Tenacity
        } else if (getMapId() == 100000202 || getMapId() == 220000006 || getMapId() == 682000200 || getMapId() == 922020000) {
            return true;
        } else if (getMapId() == World.getEventMap()) {
            return true;    
        } else {
            return false; 
        }
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public byte getInitialSpawnpoint() {
        return initialSpawnPoint;
    }
    
    public void spawnMonster(int mobid, long HP, int MP, int amount) {
        OverrideMonsterStats newStats = new OverrideMonsterStats();
        if (HP >= 0) {
            newStats.setOHp(HP);
        }
        if (MP >= 0) {
            newStats.setOMp(MP);
        }
        //newStats.setBoss(boss == 1);
        //newStats.setUndead(undead == 1);
        for (int i = 0; i < amount; i++) {
            MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
            npcmob.setOverrideStats(newStats);
            npcmob.setHp(npcmob.getMobMaxHp());
            npcmob.setMp(npcmob.getMobMaxMp());
            getMap().spawnMonsterOnGroundBelow(npcmob, getPosition());
        }
    }
    
    public void spawnCustomMonster(int mobid, long HP, int MP, int amount, int x, int y) {
        OverrideMonsterStats newStats = new OverrideMonsterStats();
        if (HP >= 0) {
            newStats.setOHp(HP);
        }
        if (MP >= 0) {
            newStats.setOMp(MP);
        }
        //newStats.setBoss(boss == 1);
        //newStats.setUndead(undead == 1);
        for (int i = 0; i < amount; i++) {
            MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
            npcmob.setOverrideStats(newStats);
            npcmob.setHp(npcmob.getMobMaxHp());
            npcmob.setMp(npcmob.getMobMaxMp());
            getMap().spawnMonsterOnGroundBelow(npcmob, new Point(x, y));
        }
    }
    
    public void spawnCustomMonster(int mobid, long HP, int MP, int amount, int x, int y, int level) {
        OverrideMonsterStats newStats = new OverrideMonsterStats();
        if (HP >= 0) {
            newStats.setOHp(HP);
        }
        if (MP >= 0) {
            newStats.setOMp(MP);
        }
        for (int i = 0; i < amount; i++) {
            MapleMonster npcmob = MapleLifeFactory.getMonster(mobid);
            if (level > npcmob.getStats().getLevel()) {
                npcmob.changeLevel(level, true);
            }
            npcmob.setOverrideStats(newStats);
            npcmob.setHp(npcmob.getMobMaxHp());
            npcmob.setMp(npcmob.getMobMaxMp());
            getMap().spawnMonsterOnGroundBelow(npcmob, new Point(x, y));
        }
    }
    
    public final boolean ban(String reason, boolean IPMac, boolean autoban, boolean hellban) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }
        client.getSession().write(CWvsContext.GMPoliceMessage(true));
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
            ps.setInt(1, autoban ? 2 : 1);
            ps.setString(2, reason);
            ps.setInt(3, accountid);
            ps.execute();
            ps.close();

            if (IPMac) {
                client.banMacs();
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSessionIPAddress());
                ps.execute();
                ps.close();

                if (hellban) {
                    PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, accountid);
                    ResultSet rsa = psa.executeQuery();
                    if (rsa.next()) {
                        PreparedStatement pss = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE email = ? OR SessionIP = ?");
                        pss.setInt(1, autoban ? 2 : 1);
                        pss.setString(2, reason);
                        pss.setString(3, rsa.getString("email"));
                        pss.setString(4, client.getSessionIPAddress());
                        pss.execute();
                        pss.close();
                    }
                    rsa.close();
                    psa.close();
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
            return false;
        }
        client.getSession().close();
        return true;
    }
    
    public static boolean ban(String id, String reason, boolean accountId, int gmlevel, boolean hellban) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (id.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, id);
                ps.execute();
                ps.close();
                return true;
            }
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            boolean ret = false;
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int z = rs.getInt(1);
                PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ? AND gm < ?");
                psb.setString(1, reason);
                psb.setInt(2, z);
                psb.setInt(3, gmlevel);
                psb.execute();
                psb.close();

                if (gmlevel > 100) { //admin ban
                    PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, z);
                    ResultSet rsa = psa.executeQuery();
                    if (rsa.next()) {
                        String sessionIP = rsa.getString("sessionIP");
                        if (sessionIP != null && sessionIP.matches("/[0-9]{1,3}\\..*")) {
                            PreparedStatement psz = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                            psz.setString(1, sessionIP);
                            psz.execute();
                            psz.close();
                        }
                        if (rsa.getString("macs") != null) {
                            String[] macData = rsa.getString("macs").split(", ");
                            if (macData.length > 0) {
                                MapleClient.banMacs(macData);
                            }
                        }
                        if (hellban) {
                            PreparedStatement pss = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE email = ?" + (sessionIP == null ? "" : " OR SessionIP = ?"));
                            pss.setString(1, reason);
                            pss.setString(2, rsa.getString("email"));
                            if (sessionIP != null) {
                                pss.setString(3, sessionIP);
                            }
                            pss.execute();
                            pss.close();
                        }
                    }
                    rsa.close();
                    psa.close();
                }
                ret = true;
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
        }
        return false;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public final String getBlessOfFairyOrigin() {
        return this.BlessOfFairy_Origin;
    }

    public final String getBlessOfEmpressOrigin() {
        return this.BlessOfEmpress_Origin;
    }

    public final short getLevel() {
        return level;
    }

    public final int getFame() {
        return fame;
    }

    public final int getFallCounter() {
        return fallcounter;
    }

    public final MapleClient getClient() {
        return client;
    }

    public final void setClient(final MapleClient client) {
        this.client = client;
    }

    public int getExp() {
        return exp.get();
    }

    public int getRemainingAp() {
        return remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp[GameConstants.getSkillBook(job)]; //default
    }

    public int getRemainingSp(final int skillbook) {
        return remainingSp[skillbook];
    }

    public int[] getRemainingSps() {
        return remainingSp;
    }

    public int getRemainingSpSize() {
        int ret = 0;
        for (int i = 0; i < remainingSp.length; i++) {
            if (remainingSp[i] > 0) {
                ret++;
            }
        }
        return ret;
    }
    
    public String getGenderString(boolean lc) {
       if (getGender() == 0) {
           if (!lc) // lowercase
         return "His";
           else
         return "his";
      } else {
           if (!lc)
         return "Her";
           else 
         return "her";
     }
 }
    
    public String getPvPGender() {
        if (getGender() == 0) {
            return "him";
        } else if (getGender() == 1) {
            return "her";
        } else { // (byte) 3 
            return "them";
        }
    }
    
     public String getGenderString() {
         return getGenderString(true);
 }
    
    public String getJQWinner(boolean lc) {
       if (getGender() == 0) {
           if (!lc)
         return "He";
           else 
         return "he";
      } else {
           if (!lc)
         return "She";
           else
         return "she";
    }
 }
    
    public String getJQWinner() {
       return getJQWinner(true);
 }
    
    public final boolean canHold(final int itemid) {
        return getInventory(GameConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public void unequipEverything() {
        MapleInventory equipped = this.getInventory(MapleInventoryType.EQUIPPED);
        List<Byte> position = new ArrayList<Byte>();
        for (Item item : equipped.list()) {
            position.add((byte)item.getPosition());
            if (!canHold(item.getItemId())) {
                client.getSession().write(CWvsContext.enableActions());
                return;
            }
        }
        for (byte pos : position) {
             MapleInventoryManipulator.unequip(client, pos, getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
        }
    }
    
    public void unequipStarter() {
        MapleInventory equipped = this.getInventory(MapleInventoryType.EQUIPPED);
        List<Byte> position = new ArrayList<Byte>();
        for (Item item : equipped.list()) {
            if (item.getItemId() == 1004042 || item.getItemId() == 1004043) {
                position.add((byte)item.getPosition());
            }
        }
        for (byte pos : position) {
             MapleInventoryManipulator.unequip(client, pos, getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
        }
    }
    
    public void unequipShield() { // When rebirthing using mihile walao :(
        MapleInventory equipped = this.getInventory(MapleInventoryType.EQUIPPED);
        List<Byte> position = new ArrayList<Byte>();
        for (Item item : equipped.list()) {
            if (item.getItemId() == 1098000 || item.getItemId() == 1098001 || item.getItemId() == 1098002 || item.getItemId() == 1098003) {
                position.add((byte)item.getPosition());
            }
        }
        for (byte pos : position) {
             MapleInventoryManipulator.unequip(client, pos, getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
        }
    }

    public short getHpApUsed() {
        return hpApUsed;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHpApUsed(short hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    @Override
    public byte getSkinColor() {
        return skinColor;
    }

    public void setSkinColor(byte skinColor) {
        this.skinColor = skinColor;
    }

    @Override
    public short getJob() {
        return job;
    }

    @Override
    public byte getGender() {
        return gender;
    }

    @Override
    public int getHair() {
        return hair;
    }

    @Override
    public int getFace() {
        return face;
    }

    @Override
    public int getDemonMarking() {
        return demonMarking;
    }

    public void setDemonMarking(int mark) {
        this.demonMarking = mark;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExp(int amount) {
        this.exp.set(amount);
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    public void setFace(int face) {
        this.face = face;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public void setFallCounter(int fallcounter) {
        this.fallcounter = fallcounter;
    }

    public Point getOldPosition() {
        return old;
    }

    public void setOldPosition(Point x) {
        this.old = x;
    }

    public void setRemainingAp(int remainingAp) {
        updateAP();
        this.remainingAp = remainingAp;
        updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp[GameConstants.getSkillBook(job)] = remainingSp; //default
    }

    public void setRemainingSp(int remainingSp, final int skillbook) {
        this.remainingSp[skillbook] = remainingSp;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public void setInvincible(boolean invinc) {
        invincible = invinc;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public BuddyList getBuddylist() {
        return buddylist;
    }

    public void addFame(int famechange) {
        this.fame += famechange;
        getTrait(MapleTraitType.charm).addLocalExp(famechange);
    }

    public void updateFame() {
        updateSingleStat(MapleStat.FAME, this.fame);
    }

    public void changeMapBanish(final int mapid, final String portal, final String msg) {
        dropMessage(5, msg);
        final MapleMap mapppi = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(mapppi, mapppi.getPortal(portal));
    }

    public void changeMap2(int map, int portal) {
        MapleMap warpMap = getClient().getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, warpMap.getPortal(portal));
    }
    
    public void changeMap(int map, int portal) {
        MapleMap warpMap = client.getChannelServer().getMapFactory().getMap(map);
        changeMap(warpMap, warpMap.getPortal(portal));
    }

    public void changeMap(final MapleMap to, final Point pos) {
        changeMapInternal(to, pos, CField.getWarpToMap(to, 0x80, this), null);
    }

    public void changeMap(final MapleMap to) {
        changeMapInternal(to, to.getPortal(0).getPosition(), CField.getWarpToMap(to, 0, this), to.getPortal(0));
    }
    
    public void changeMap(final int to) {
        MapleMap map = ChannelServer.getInstance(getClient().getWorld(), getClient().getChannel()).getMapFactory().getMap(to);
        changeMapInternal(map, map.getPortal(0).getPosition(), CField.getWarpToMap(map, 0, this), map.getPortal(0));
    }

    public void changeMap(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), CField.getWarpToMap(to, pto.getId(), this), null);
    }

    public void changeMapPortal(final MapleMap to, final MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), CField.getWarpToMap(to, pto.getId(), this), pto);
    }

    private void changeMapInternal(final MapleMap to, final Point pos, byte[] warpPacket, final MaplePortal pto) {
        if (to == null) {
            return;
        }
        if (getAntiMacro().inProgress()) { 
            dropMessage(5, "You cannot use it in the middle of the Lie Detector Test."); 
            return;             
        }
        if (getTrade() != null) {
            MapleTrade.cancelTrade(getTrade(), client, this);
        }
        final int nowmapid = map.getId();
        if (eventInstance != null) {
            eventInstance.changedMap(this, to.getId());
        }
        List<MapleMonster> monsters = map.getAllMonster();
           for (MapleMapObject mmo : monsters) {
                 MapleMonster m = (MapleMonster) mmo;
              if (m.getBelongsTo() == getId()) {
                  decAntiKSNum();
                  m.expireAntiKS();
              }
           }
        final boolean pyramid = pyramidSubway != null;
        if (map.getId() == nowmapid) {
            client.getSession().write(warpPacket);
            final boolean shouldChange = client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
            final boolean shouldState = map.getId() == to.getId();
            if (shouldChange && shouldState) {
                to.setCheckStates(false);
            }
            map.removePlayer(this);
            if (shouldChange) {
                map = to;
                setPosition(pos);
                to.addPlayer(this);
                stats.relocHeal(this);
                if (shouldState) {
                    to.setCheckStates(true);
                }
            }
            client.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(client);
            client.getSession().write(CWvsContext.enableActions());
        }
        if (pyramid && pyramidSubway != null) { //checks if they had pyramid before AND after changing
            pyramidSubway.onChangeMap(this, to.getId());
        }
    }
    
    public final void warpParty(final int mapId) {
        if (getParty() == null || getParty().getMembers().size() == 1) {
            changeMap(mapId, 0);
            return;
        }
        final MapleMap target = getMap(mapId);
        final int cMap = getMapId();
        for (final MaplePartyCharacter chr : getParty().getMembers()) {
            final MapleCharacter curChar = client.getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }
    
    private MapleMap getWarpMap(final int map) {
        return ChannelServer.getInstance(client.getWorld(), client.getChannel()).getMapFactory().getMap(map);
    }

    public final MapleMap getMap(final int map) {
        return getWarpMap(map);
    }

    public void cancelChallenge() {
        if (challenge != 0 && client.getChannelServer() != null) {
            final MapleCharacter chr = client.getChannelServer().getPlayerStorage().getCharacterById(challenge);
            if (chr != null) {
                chr.dropMessage(6, getName() + " has denied your request.");
                chr.setChallenge(0);
            }
            dropMessage(6, "Denied the challenge.");
            challenge = 0;
        }
    }

    public void leaveMap(MapleMap map) {
        controlledLock.writeLock().lock();
        visibleMapObjectsLock.writeLock().lock();
        try {
            for (MapleMonster mons : controlled) {
                if (mons != null) {
                    mons.setController(null);
                    mons.setControllerHasAggro(false);
                    map.updateMonsterController(mons);
                }
            }
            controlled.clear();
            visibleMapObjects.clear();
        } finally {
            controlledLock.writeLock().unlock();
            visibleMapObjectsLock.writeLock().unlock();
        }
        if (chair != 0) {
            chair = 0;
        }
        clearLinkMid();
        cancelFishingTask();
        cancelChallenge();
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        cancelMapTimeLimitTask();
        antiMacro.reset(); // reset lie detector
        if (getTrade() != null) {
            MapleTrade.cancelTrade(getTrade(), client, this);
        }
    }

    public void changeJob(int newJob) {
        if (getBuffedValue(MapleBuffStat.SHADOWPARTNER) != null || getBuffedValue(MapleBuffStat.WATER_SHIELD) != null) {
            dropMessage(5, "Please disable Shadow Partner/Mirror Image/Water Shield(the dragon thing) before rebirthing or changing job.");
        } else {
            try {
                this.job = (short) newJob;
                updateSingleStat(MapleStat.JOB, newJob);
                /* 3023 */ if (GameConstants.isPhantom(this.job)) {
                    /* 3024 */ this.client.getSession().write(CField.updateCardStack(0));
                    /*      */ }
                /*     if (!GameConstants.isBeginnerJob(newJob)) {
                 if (GameConstants.isEvan(newJob) || GameConstants.isResist(newJob) || GameConstants.isMercedes(newJob)) {
                 int changeSp = (newJob == 2200 || newJob == 2210 || newJob == 2211 || newJob == 2213 ? 3 : 5);
                 if (GameConstants.isResist(job) && newJob != 3100 && newJob != 3200 && newJob != 3300 && newJob != 3500) {
                 // changeSp = 3;
                 }
                 remainingSp[GameConstants.getSkillBook(newJob)] += changeSp;
                 client.getSession().write(InfoPacket.getSPMsg((byte) changeSp, (short) newJob));
                 } else {
                 remainingSp[GameConstants.getSkillBook(newJob)]++;
                 if (newJob % 10 >= 2) {
                 remainingSp[GameConstants.getSkillBook(newJob)] += 2;
                 }
                 }
                 if (newJob % 10 >= 1 && level >= 70) { //3rd job or higher. lucky for evans who get 80, 100, 120, 160 ap...
                 remainingAp += 5;
                 //   updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
                 //MAKER STILL EXISTS
                 final Skill skil = SkillFactory.getSkill(stats.getSkillByJob(1007, getJob()));
                 if (skil != null && getSkillLevel(skil) <= 0) {
                 dropMessage(-1, "You have gained the Maker skill.");
                 changeSingleSkillLevel(skil, skil.getMaxLevel(), (byte) skil.getMaxLevel());
                 }
                 }
                 if (!isGM()) {
                 //     resetStatsByJob(true);
                 if (!GameConstants.isEvan(newJob)) {
                 if (getLevel() > (newJob == 200 ? 8 : 10) && newJob % 100 == 0 && (newJob % 1000) / 100 > 0) { //first job
                 remainingSp[GameConstants.getSkillBook(newJob)] += 3 * (getLevel() - (newJob == 200 ? 8 : 10));
                 }
                 } else if (newJob == 2200) {
                 MapleQuest.getInstance(22100).forceStart(this, 0, null);
                 MapleQuest.getInstance(22100).forceComplete(this, 0);
                 //  expandInventory((byte) 1, 4);
                 //  expandInventory((byte) 2, 4);
                 //  expandInventory((byte) 3, 4);
                 //  expandInventory((byte) 4, 4);
                 client.getSession().write(NPCPacket.getEvanTutorial("UI/tutorial/evan/14/0"));
                 dropMessage(5, "The baby Dragon hatched and appears to have something to tell you. Click the baby Dragon to start a conversation.");
                 }
                 }
                 updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
                 }
                 * 
                 */

                int maxhp = stats.getMaxHp(), maxmp = stats.getMaxMp();

                switch (job) {
                    case 100: // Warrior
                    case 1100: // Soul Master
                    case 2100: // Aran
                    case 3200:
                        maxhp += Randomizer.rand(200, 250);
                        break;
                    case 3100:
                        maxhp += Randomizer.rand(200, 250);
                        break;
                    case 3110:
                        maxhp += Randomizer.rand(300, 350);
                        break;
                    case 200: // Magician
                    case 2200: //evan
                    case 2210: //evan
                        maxmp += Randomizer.rand(100, 150);
                        break;
                    case 300: // Bowman
                    case 400: // Thief
                    case 500: // Pirate
                    case 2300:
                    case 3300:
                    case 3500:
                        maxhp += Randomizer.rand(100, 150);
                        maxmp += Randomizer.rand(25, 50);
                        break;
                    case 110: // Fighter
                    case 120: // Page
                    case 130: // Spearman
                    case 1110: // Soul Master
                    case 2110: // Aran
                    case 3210:
                        maxhp += Randomizer.rand(300, 350);
                        break;
                    case 210: // FP
                    case 220: // IL
                    case 230: // Cleric
                        maxmp += Randomizer.rand(400, 450);
                        break;
                    case 310: // Bowman
                    case 320: // Crossbowman
                    case 410: // Assasin
                    case 420: // Bandit
                    case 430: // Semi Dualer
                    case 510:
                    case 520:
                    case 530:
                    case 2310:
                    case 1310: // Wind Breaker
                    case 1410: // Night Walker
                    case 3310:
                    case 3510:
                        maxhp += Randomizer.rand(200, 250);
                        maxhp += Randomizer.rand(150, 200);
                        break;
                    case 900: // GM
                    case 800: // Manager
                        maxhp += 99999;
                        maxmp += 99999;
                        break;
                }
                if (maxhp >= 99999) {
                    maxhp = 99999;
                }
                if (maxmp >= 99999) {
                    maxmp = 99999;
                }
                if (GameConstants.isDemon(job)) {
                    maxmp = GameConstants.getMPByJob(job);
                }
                stats.setInfo(maxhp, maxmp, maxhp, maxmp);
                Map<MapleStat, Integer> statup = new EnumMap<>(MapleStat.class);
                statup.put(MapleStat.MAXHP, Integer.valueOf(maxhp));
                statup.put(MapleStat.MAXMP, Integer.valueOf(maxmp));
                statup.put(MapleStat.HP, Integer.valueOf(maxhp));
                statup.put(MapleStat.MP, Integer.valueOf(maxmp));
                characterCard.recalcLocalStats(this);
                stats.recalcLocalStats(this);
                client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
                map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 11), false);
                silentPartyUpdate();
                guildUpdate();
                familyUpdate();
                if (dragon != null) {
                    map.broadcastMessage(CField.removeDragon(this.id));
                    dragon = null;
                }
                //baseSkills();
                if (newJob >= 2200 && newJob <= 2218) { //make new
                    if (getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                        cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
                    }
                    makeDragon();
                }
            } catch (Exception e) {
                FileoutputUtil.outputFileError(FileoutputUtil.ScriptEx_Log, e); //all jobs throw errors :(
            }
        }
    }

    public void baseSkills() {
        Map<Skill, SkillEntry> list = new HashMap<>();
        if (GameConstants.getJobNumber(job) >= 3) { //third job.
            List<Integer> skillssz = SkillFactory.getSkillsByJob(job);
            if (skillssz != null) {
                for (int i : skillssz) {
                    final Skill skil = SkillFactory.getSkill(i);
                    if (skil != null && !skil.isInvisible() && skil.isFourthJob() && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0 && skil.getMasterLevel() > 0) {
                        list.put(skil, new SkillEntry((byte) 0, skil.getMasterLevel(), SkillFactory.getDefaultSExpiry(skil))); //usually 10 master
                    } else if (skil != null && skil.getName() != null && skil.getName().contains("Maple Warrior") && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0) {
                        list.put(skil, new SkillEntry((byte) 0, 10, SkillFactory.getDefaultSExpiry(skil))); //hackish
                    }
                }

            }
        }
        Skill skil;
        if (job >= 2211 && job <= 2218) { // evan fix magic guard
            skil = SkillFactory.getSkill(22111001);
            if (skil != null) {
                if (getSkillLevel(skil) <= 0) { // no total
                    list.put(skil, new SkillEntry((byte) 0, (byte) 20, -1));
                }
            }
        }
        if (job >= 430 && job <= 434) { 
            final int[] ss0 = {4331002, 4330009, 4341004, 4341006, 4341007, 4341011, 4340013}; 
            for (int i : ss0) { 
                skil = SkillFactory.getSkill(i); 
                if (skil != null) { 
                    if (getSkillLevel(skil) <= 0) { // no total 
                        list.put(skil, new SkillEntry((byte) 0, (byte) 10, -1)); 
                    } 
                } 
            } 
            skil = SkillFactory.getSkill(4311003); 
            if (skil != null) { 
                if (getSkillLevel(skil) <= 0) { // no total 
                    list.put(skil, new SkillEntry((byte) -1, (byte) 5, -1)); 
                } 
            } 
            skil = SkillFactory.getSkill(4321006); 
            if (skil != null) { 
                if (getSkillLevel(skil) <= 0) { // no total 
                    list.put(skil, new SkillEntry((byte) -1, (byte) 5, -1)); 
                } 
            } 
        }  
        if (GameConstants.isMercedes(job)) {
            final int[] ss = {20021000, 20021001, 20020002, 20020022, 20020109, 20021110, 20020111, 20020112};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null) {
                    if (getSkillLevel(skil) <= 0) { // no total
                        list.put(skil, new SkillEntry((byte) 1, (byte) 1, -1));
                    }
                }
            }
            skil = SkillFactory.getSkill(20021181);
            if (skil != null) {
                if (getSkillLevel(skil) <= 0) { // no total
                    list.put(skil, new SkillEntry((byte) -1, (byte) 0, -1));
                }
            }
            skil = SkillFactory.getSkill(20021166);
            if (skil != null) {
                if (getSkillLevel(skil) <= 0) { // no total
                    list.put(skil, new SkillEntry((byte) -1, (byte) 0, -1));
                }
            }
        }
        if (GameConstants.isDemon(job)) {
            final int[] ss1 = {30011000, 30011001, 30010002, 30010185, 30010112, 30010111, 30010110, 30010022, 30011109};
            for (int i : ss1) {
                skil = SkillFactory.getSkill(i);
                if (skil != null) {
                    if (getSkillLevel(skil) <= 0) { // no total
                        list.put(skil, new SkillEntry((byte) 1, (byte) 1, -1));
                    }
                }
            }
            final int[] ss2 = {30011170, 30011169, 30011168, 30011167, 30010166, 30010184, 30010183, 30010186};
            for (int i : ss2) {
                skil = SkillFactory.getSkill(i);
                if (skil != null) {
                    if (getSkillLevel(skil) <= 0) { // no total
                        list.put(skil, new SkillEntry((byte) -1, (byte) -1, -1));
                    }
                }
            }
        }
        if (!list.isEmpty()) {
            changeSkillsLevel(list);
        }
        //redemption for completed quests. holy fk. ex
	    /*List<MapleQuestStatus> cq = getCompletedQuests();
         for (MapleQuestStatus q : cq) {
         for (MapleQuestAction qs : q.getQuest().getCompleteActs()) {
         if (qs.getType() == MapleQuestActionType.skill) {
         for (Pair<Integer, Pair<Integer, Integer>> skill : qs.getSkills()) {
         final Skill skil = SkillFactory.getSkill(skill.left);
         if (skil != null && getSkillLevel(skil) <= skill.right.left && getMasterLevel(skil) <= skill.right.right) {
         changeSkillLevel(skil, (byte) (int)skill.right.left, (byte) (int)skill.right.right);
         }
         }
         } else if (qs.getType() == MapleQuestActionType.item) { //skillbooks
         for (MapleQuestAction.QuestItem item : qs.getItems()) {
         if (item.itemid / 10000 == 228 && !haveItem(item.itemid,1)) { //skillbook
         //check if we have the skill
         final Map<String, Integer> skilldata = MapleItemInformationProvider.getInstance().getSkillStats(item.itemid);
         if (skilldata != null) {
         byte i = 0;
         Skill finalSkill = null;
         Integer skillID = 0;
         while (finalSkill == null) {
         skillID = skilldata.get("skillid" + i);
         i++;
         if (skillID == null) {
         break;
         }
         final Skill CurrSkill = SkillFactory.getSkill(skillID);
         if (CurrSkill != null && CurrSkill.canBeLearnedBy(job) && getSkillLevel(CurrSkill) <= 0 && getMasterLevel(CurrSkill) <= 0) {
         finalSkill = CurrSkill;
         }
         }
         if (finalSkill != null) {
         //may as well give the skill
         changeSkillLevel(finalSkill, (byte) 0, (byte)10);
         //MapleInventoryManipulator.addById(client, item.itemid, item.count);
         }
         }
         }
         }
         }
         }
         }*/
    }

    public void makeDragon() {
        dragon = new MapleDragon(this);
        map.broadcastMessage(CField.spawnDragon(dragon));
    }

    public MapleDragon getDragon() {
        return dragon;
    }

    public void gainAp(int ap) {
        updateAP();
        this.remainingAp += ap;
        updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
    }

    public void gainSP(int sp) {
        this.remainingSp[GameConstants.getSkillBook(job)] += sp; //default
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
        client.getSession().write(InfoPacket.getSPMsg((byte) sp, (short) job));
    }

    public void gainSP(int sp, final int skillbook) {
        this.remainingSp[skillbook] += sp; //default
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
        client.getSession().write(InfoPacket.getSPMsg((byte) sp, (short) 0));
    }

    public void resetSP(int sp) {
        for (int i = 0; i < remainingSp.length; i++) {
            this.remainingSp[i] = sp;
        }
        updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
    }

    public List<Integer> getProfessions() {
        List<Integer> prof = new ArrayList<>();
        for (int i = 9200; i <= 9204; i++) {
            if (getProfessionLevel(id * 10000) > 0) {
                prof.add(i);
            }
        }
        return prof;
    }

    public byte getProfessionLevel(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (byte) ((ret >>> 24) & 0xFF); //the last byte
    }

    public short getProfessionExp(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (short) (ret & 0xFFFF); //the first two byte
    }

    public boolean addProfessionExp(int id, int expGain) {
        int ret = getProfessionLevel(id);
        if (ret <= 0 || ret >= 10) {
            return false;
        }
        int newExp = getProfessionExp(id) + expGain;
        if (newExp >= GameConstants.getProfessionEXP(ret)) {
            //gain level
            changeProfessionLevelExp(id, ret + 1, newExp - GameConstants.getProfessionEXP(ret));
            int traitGain = (int) Math.pow(2, ret + 1);
            switch (id) {
                case 92000000:
                    traits.get(MapleTraitType.sense).addExp(traitGain, this);
                    break;
                case 92010000:
                    traits.get(MapleTraitType.will).addExp(traitGain, this);
                    break;
                case 92020000:
                case 92030000:
                case 92040000:
                    traits.get(MapleTraitType.craft).addExp(traitGain, this);
                    break;
            }
            return true;
        } else {
            changeProfessionLevelExp(id, ret, newExp);
            return false;
        }
    }

    public void changeProfessionLevelExp(int id, int level, int exp) {
        changeSingleSkillLevel(SkillFactory.getSkill(id), ((level & 0xFF) << 24) + (exp & 0xFFFF), (byte) 10);
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, int newMasterlevel) { //1 month
        if (skill == null) {
            return;
        }
        changeSingleSkillLevel(skill, newLevel, newMasterlevel, SkillFactory.getDefaultSExpiry(skill));
    }

    public void changeSingleSkillLevel(final Skill skill, int newLevel, int newMasterlevel, long expiration) {
        final Map<Skill, SkillEntry> list = new HashMap<>();
        boolean hasRecovery = false, recalculate = false;
        if (changeSkillData(skill, newLevel, newMasterlevel, expiration, (byte) -1, (byte) -1)) { // no loop, only 1
            list.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration));
            if (GameConstants.isRecoveryIncSkill(skill.getId())) {
                hasRecovery = true;
            }
            if (skill.getId() < 80000000) {
                recalculate = true;
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.getSession().write(CWvsContext.updateSkills(list));
        reUpdateStat(hasRecovery, recalculate);
    }

    private void reUpdateStat(boolean hasRecovery, boolean recalculate) {
        changed_skills = true;
        if (hasRecovery) {
            stats.relocHeal(this);
        }
        if (recalculate) {
            stats.recalcLocalStats(this);
        }
    }

    /*      */ public boolean changeSkillData(Skill skill, int newLevel, int newMasterlevel, long expiration, byte slot, byte equipped) /*      */ {
        /* 3467 */ if ((skill == null) || ((!GameConstants.isApplicableSkill(skill.getId())) && (!GameConstants.isApplicableSkill_(skill.getId())))) {
            /* 3468 */ return false;
            /*      */        }
        /* 3470 */ if ((newLevel == 0) && (newMasterlevel == 0)) {
            /* 3471 */ if (this.skills.containsKey(skill)) {
                this.skills.remove(skill);
            } /*      */ else {
                return false;
            }
            /*      */        } /*      */ else {
            /* 3477 */ this.skills.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration, slot, equipped));
            /*      */        }
        /* 3479 */ return true;
        /*      */    }

    public void changeSkillLevel_Skip(final Map<Skill, SkillEntry> skill, final boolean write) { // only used for temporary skills (not saved into db)
        if (skill.isEmpty()) {
            return;
        }
        final Map<Skill, SkillEntry> newL = new HashMap<>();
        for (final Entry<Skill, SkillEntry> z : skill.entrySet()) {
            if (z.getKey() == null) {
                continue;
            }
            newL.put(z.getKey(), z.getValue());
            if (z.getValue().skillevel == 0 && z.getValue().masterlevel == 0) {
                if (skills.containsKey(z.getKey())) {
                    skills.remove(z.getKey());
                } else {
                    continue;
                }
            } else {
                skills.put(z.getKey(), z.getValue());
            }
        }
        if (write && !newL.isEmpty()) {
            client.getSession().write(CWvsContext.updateSkills(newL));
        }
    }

    public void playerDead() {
        final MapleStatEffect statss = getStatForBuff(MapleBuffStat.SOUL_STONE);
        if (statss != null) {
            dropMessage(5, "You have been revived by Soul Stone.");
            getStat().setHp(((getStat().getMaxHp() / 100) * statss.getX()), this);
            setStance(0);
            changeMap(getMap(), getMap().getPortal(0));
            return;
        }
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        cancelEffectFromBuffStat(MapleBuffStat.SHADOWPARTNER);
        cancelEffectFromBuffStat(MapleBuffStat.MORPH);
        cancelEffectFromBuffStat(MapleBuffStat.SOARING);
        cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
        cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
        cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
        cancelEffectFromBuffStat(MapleBuffStat.HP_BOOST_PERCENT);
        cancelEffectFromBuffStat(MapleBuffStat.MP_BOOST_PERCENT);
        cancelEffectFromBuffStat(MapleBuffStat.HP_BOOST);
        cancelEffectFromBuffStat(MapleBuffStat.MP_BOOST);
        cancelEffectFromBuffStat(MapleBuffStat.ENHANCED_MAXHP);
        cancelEffectFromBuffStat(MapleBuffStat.ENHANCED_MAXMP);
        cancelEffectFromBuffStat(MapleBuffStat.MAXHP);
        cancelEffectFromBuffStat(MapleBuffStat.MAXMP);
        dispelSummons();
        checkFollow();
        dotHP = 0;
        lastDOTTime = 0;
        if (!GameConstants.isBeginnerJob(job) && !inPVP()) {
            int charms = getItemQuantity(5130000, false);
            if (charms > 0) {
                MapleInventoryManipulator.removeById(client, MapleInventoryType.CASH, 5130000, 1, true, false);

                charms--;
                if (charms > 0xFF) {
                    charms = 0xFF;
                }
                client.getSession().write(EffectPacket.useCharm((byte) charms, (byte) 0, true));
            } else {
                float diepercentage;
                int expforlevel = getNeededExp();
                if (map.isTown() || FieldLimitType.RegularExpLoss.check(map.getFieldLimit())) {
                    diepercentage = 0.01f;
                } else {
                    diepercentage = (float) (0.1f - ((traits.get(MapleTraitType.charisma).getLevel() / 20) / 100f));
                }
                int v10 = (int) (exp.get() - (long) ((double) expforlevel * diepercentage));
                if (v10 < 0) {
                    v10 = 0;
                }
                this.exp.set(v10);
            }
            this.updateSingleStat(MapleStat.EXP, this.exp.get());
        }
        if (!stats.checkEquipDurabilitys(this, -100)) { //i guess this is how it works ?
            dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
        } //lol
        if (pyramidSubway != null) {
            stats.setHp((short) 50, this);
            pyramidSubway.fail(this);
        }
    }

    public void updatePartyMemberHP() {
        if (party != null && client.getChannelServer() != null) {
            final int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    final MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.getClient().getSession().write(CField.updatePartyMemberHP(getId(), stats.getHp(), stats.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void receivePartyMemberHP() {
        if (party == null) {
            return;
        }
        int channel = client.getChannel();
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                if (other != null) {
                    client.getSession().write(CField.updatePartyMemberHP(other.getId(), other.getStat().getHp(), other.getStat().getCurrentMaxHp()));
                }
            }
        }
    }

    public void healHP(int delta) {
        addHP(delta);
        client.getSession().write(EffectPacket.showOwnHpHealed(delta));
        getMap().broadcastMessage(this, EffectPacket.showHpHealed(getId(), delta), false);
    }

    public void healMP(int delta) {
        addMP(delta);
        client.getSession().write(EffectPacket.showOwnHpHealed(delta));
        getMap().broadcastMessage(this, EffectPacket.showHpHealed(getId(), delta), false);
    }

    /**
     * Convenience function which adds the supplied parameter to the current hp
     * then directly does a updateSingleStat.
     *
     * @see MapleCharacter#setHp(int)
     * @param delta
     */
    public void addHP(int delta) {
        if (stats.setHp(stats.getHp() + delta, this)) {
            updateSingleStat(MapleStat.HP, stats.getHp());
        }
    }

    /**
     * Convenience function which adds the supplied parameter to the current mp
     * then directly does a updateSingleStat.
     *
     * @see MapleCharacter#setMp(int)
     * @param delta
     */
    public void addMP(int delta) {
        addMP(delta, false);
    }

    public void addMP(int delta, boolean ignore) {
        if ((delta < 0 && GameConstants.isDemon(getJob())) || !GameConstants.isDemon(getJob()) || ignore) {
            if (stats.setMp(stats.getMp() + delta, this)) {
                updateSingleStat(MapleStat.MP, stats.getMp());
            }
        }
    }

    public void addMPHP(int hpDiff, int mpDiff) {
        Map<MapleStat, Integer> statups = new EnumMap<>(MapleStat.class);

        if (stats.setHp(stats.getHp() + hpDiff, this)) {
            statups.put(MapleStat.HP, Integer.valueOf(stats.getHp()));
        }
        if ((mpDiff < 0 && GameConstants.isDemon(getJob())) || !GameConstants.isDemon(getJob())) {
            if (stats.setMp(stats.getMp() + mpDiff, this)) {
                statups.put(MapleStat.MP, Integer.valueOf(stats.getMp()));
            }
        }
        if (statups.size() > 0) {
            client.getSession().write(CWvsContext.updatePlayerStats(statups, this));
        }
    }

    public void updateSingleStat(MapleStat stat, int newval) {
        updateSingleStat(stat, newval, false);
    }

    /**
     * Updates a single stat of this MapleCharacter for the client. This method
     * only creates and sends an update packet, it does not update the stat
     * stored in this MapleCharacter instance.
     *
     * @param stat
     * @param newval
     * @param itemReaction
     */
    public void updateSingleStat(MapleStat stat, int newval, boolean itemReaction) {
        Map<MapleStat, Integer> statup = new EnumMap<>(MapleStat.class);
        statup.put(stat, newval);
        client.getSession().write(CWvsContext.updatePlayerStats(statup, itemReaction, this));
    }

    public void gainExp(int total, final boolean show, final boolean inChat, final boolean white) {
        int[] codex_levels = {15, 30, 45, 60, 75, 90, 105};
        for (int i : codex_levels) {
            if (getLevel() == i) {
                setLevel((short)(getLevel() + 3));
            }
        }
        /*if (getLevel() == 15) {
         setLevel((short) 18);  
         } else if (getLevel() == 30) {
         setLevel((short) 33);  
         } else if (getLevel() == 45) {
         setLevel((short) 48);
         } else if (getLevel() == 60) {
         setLevel((short) 63);
         } else if (getLevel() == 75) {
         setLevel((short) 78);
         } else if (getLevel() == 90) {
         setLevel((short) 93);
         } else if (getLevel() == 105) {
         setLevel((short) 108);
         }
         * 
         */
        boolean leveled = false;
        if (level < 200) {
            if ((long) this.exp.get() + (long) total > (long) Integer.MAX_VALUE) {
                int gainFirst = GameConstants.getExpNeededForLevel(level) - this.exp.get();
                total -= gainFirst + 1;
                this.gainExp(gainFirst + 1, false, inChat, white);
            }
            if (show && total > 0) {
                client.getSession().write(InfoPacket.GainEXP_Others(total, inChat, white));
            }
            exp.addAndGet(total);
            updateSingleStat(MapleStat.EXP, this.exp.get());
            if (gmLevel > -1) {
                while (exp.get() >= GameConstants.getExpNeededForLevel(level)) {
                    levelUp();
                    leveled = true;
                }
                if (total > 0) {
                    familyRep(getExp(), GameConstants.getExpNeededForLevel(level), leveled);
                }
            }

        }
    }

    public void familyRep(int prevexp, int needed, boolean leveled) {
        if (mfc != null) {
            int onepercent = needed / 100;
            if (onepercent <= 0) {
                return;
            }
            int percentrep = (getExp() / onepercent - prevexp / onepercent);
            if (leveled) {
                percentrep = 100 - percentrep + (level / 2);
            }
            if (percentrep > 0) {
                int sensen = World.Family.setRep(mfc.getFamilyId(), mfc.getSeniorId(), percentrep * 10, level, name);
                if (sensen > 0) {
                    World.Family.setRep(mfc.getFamilyId(), sensen, percentrep * 5, level, name); //and we stop here
                }
            }
        }
    }

    public void gainExpMonster(final int gain, final boolean show, final boolean white, final byte pty, int Class_Bonus_EXP, int Equipment_Bonus_EXP, int Premium_Bonus_EXP, boolean partyBonusMob, final int partyBonusRate) {
        int total = gain + Class_Bonus_EXP + Equipment_Bonus_EXP + Premium_Bonus_EXP;
        int partyinc = 0;
        int prevexp = getExp();
        if (pty > 1) {
            final double rate = (partyBonusRate > 0 ? (partyBonusRate / 100.0) : (map == null || !partyBonusMob || map.getPartyBonusRate() <= 0 ? 0.05 : (map.getPartyBonusRate() / 100.0)));
            partyinc = (int) (((float) (gain * rate)) * (pty + (rate > 0.05 ? -1 : 1)));
            total += partyinc;
        }

        if (gain > 0 && total < gain) { //just in case
            total = Integer.MAX_VALUE;
        }
        // if (total > 0) {
        //   stats.checkEquipLevels(this, total); //gms like
        // }
        int needed = getNeededExp();
        if ((level >= 200 || (GameConstants.isKOC(job) && level >= 200))) {// && !isIntern()) {
            setExp(0);
            //if (exp + total > needed) {
            //    setExp(needed);
            //} else {
            //    exp += total;
            //}
        } else {
            boolean leveled = false;
            if ((long) this.exp.get() + (long) total > (long) Integer.MAX_VALUE) {
                int gainFirst = GameConstants.getExpNeededForLevel(level) - this.exp.get();
                total -= gainFirst + 1;
                this.gainExp(gainFirst + 1, false, true, white);
            }
         //   if (show && gain > 0) {
           //     client.getSession().write(InfoPacket.GainEXP_Monster(gain, white, partyinc, Class_Bonus_EXP, Equipment_Bonus_EXP, Premium_Bonus_EXP));
           // }
            exp.addAndGet(total);
            updateSingleStat(MapleStat.EXP, this.exp.get());
            if (gmLevel > -1) {
                while (exp.get() >= GameConstants.getExpNeededForLevel(level)) {
                    levelUp();
                    leveled = true;
                }
            }
            if (total > 0) {
                familyRep(prevexp, needed, leveled);
            }
        }
        if (gain != 0) {
            if (exp.get() < 0) { // After adding, and negative
                if (gain > 0) {
                    setExp(getNeededExp());
                } else if (gain < 0) {
                    setExp(0);
                }
            }
            updateSingleStat(MapleStat.EXP, getExp());
            if (show) { // still show the expgain even if it's not there
                client.getSession().write(InfoPacket.GainEXP_Monster(gain, white, partyinc, Class_Bonus_EXP, Equipment_Bonus_EXP, Premium_Bonus_EXP));
            }
        }
    }

    public boolean containsAreaInfo(int area, String info) {
        Short area_ = Short.valueOf((short) area);
        if (area_info.containsKey(area_)) {
            return area_info.get(area_).contains(info);
        }
        return false;
    }

    public void updateAreaInfo(int area, String info) {
        area_info.put(Short.valueOf((short) area), info);
        announce(CWvsContext.updateAreaInfo(area, info));
    }

    public String getAreaInfo(int area) {
        return area_info.get(Short.valueOf((short) area));
    }

    public Map<Short, String> getAreaInfos() {
        return area_info;
    }

    public void announce(byte[] packet) {
        client.announce(packet);
    }

    public void changeElf() {//questEx 7784
        if (getElf()) // for non-mercedes :P
            setElf(false);
        else
            setElf(true);
        updateAreaInfo(7784, containsAreaInfo(7784, "sw=") ? containsAreaInfo(7784, "sw=1") ? "sw=0" : "sw=1" : "sw=1");
        if (containsAreaInfo(7784, GameConstants.isMercedes(getJob()) ? "sw=0" : "sw=1")) {
            announce(CWvsContext.showWeirdEffect("Effect/BasicEff.img/JobChangedElf", 5155000));
            getMap().broadcastMessage(this, CWvsContext.showWeirdEffect(getId(), "Effect/BasicEff.img/JobChangedElf", 5155000), false);
        } else {
            announce(CWvsContext.showWeirdEffect("Effect/BasicEff.img/JobChanged", 5155000));
            getMap().broadcastMessage(this, CWvsContext.showWeirdEffect(getId(), "Effect/BasicEff.img/JobChanged", 5155000), false);
        }
        equipChanged();
    }
    
    public boolean getElf() {
        return elf; // this one is easier for me "get" > "has" LOL
    }
    
    public void setElf(boolean enabled) {
        elf = enabled;
    }

    @Override
    public boolean isElf(MapleCharacter chr) {
        if (getElf()) {
            return true;
        } else if (getElf() == false) {
            return false;
        }
        if (containsAreaInfo(7784, "sw=")) {
            return containsAreaInfo(7784, GameConstants.isMercedes(getJob()) ? "sw=0" : "sw=1");
        }
        return GameConstants.isMercedes(getJob());
    }
    
    public void changeElf2() {//questEx 7784 
        if (getElf()) // for non-mercedes :P
            setElf(false);
        else
            setElf(true);
        updateAreaInfo(7784, containsAreaInfo(7784, "sw=") ? containsAreaInfo(7784, "sw=1") ? "sw=0" : "sw=1" : "sw=1"); 
        if (containsAreaInfo(7784, GameConstants.isMercedes(getJob()) ? "sw=0" : "sw=1")) { 
            announce(CWvsContext.showWeirdEffect("Effect/BasicEff.img/JobChangedElf", 5155000)); 
            getMap().broadcastMessage(this, CWvsContext.showWeirdEffect(getId(), "Effect/BasicEff.img/JobChangedElf", 5155000), false); 
        } else { 
            announce(CWvsContext.showWeirdEffect("Effect/BasicEff.img/JobChanged", 5155000)); 
            getMap().broadcastMessage(this, CWvsContext.showWeirdEffect(getId(), "Effect/BasicEff.img/JobChanged", 5155000), false); 
        } 
        equipChanged(); 
    } 

    public boolean isElf2() { 
        if (getElf()) {
            return true;
        }
        if (containsAreaInfo(7784, "sw=")) { 
            return containsAreaInfo(7784, GameConstants.isMercedes(getJob()) ? "sw=0" : "sw=1"); 
        } 
        return GameConstants.isMercedes(getJob()); 
    }  

    public void forceReAddItem_NoUpdate(Item item, MapleInventoryType type) {
        getInventory(type).removeSlot(item.getPosition());
        getInventory(type).addFromDB(item);
    }

    public void forceReAddItem(Item item, MapleInventoryType type) { //used for stuff like durability, item exp/level, probably owner?
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(InventoryPacket.updateSpecialItemUse(item, type == MapleInventoryType.EQUIPPED ? (byte) 1 : type.getType(), this));
        }
    }

    public void forceReAddItem_Flag(Item item, MapleInventoryType type) { //used for flags
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(InventoryPacket.updateSpecialItemUse_(item, type == MapleInventoryType.EQUIPPED ? (byte) 1 : type.getType(), this));
        }
    }

    public void forceReAddItem_Book(Item item, MapleInventoryType type) { //used for mbook
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.getSession().write(CWvsContext.upgradeBook(item, this));
        }
    }

    public void silentPartyUpdate() {
        if (party != null) {
            World.Party.updateParty(party.getId(), PartyOperation.SILENT_UPDATE, new MaplePartyCharacter(this));
        }
    }
    
    private transient MapleLieDetector antiMacro; 
    public final MapleLieDetector getAntiMacro() { 
        return antiMacro; 
    }

    public boolean isSuperGM() {
        if (this.getAccountID() == ServerConstants.ERIC_ACC_ID) {
            return true;
        } else {
            return gmLevel >= 3;
        }
    }

    public boolean isIntern() {
        if (this.getAccountID() == ServerConstants.ERIC_ACC_ID) {
            return true;
        } else {
            return gmLevel >= 3;
        }
    }

    public boolean isGM() {
        if (this.getAccountID() == ServerConstants.ERIC_ACC_ID) {
            return true;
        } else {
            return gmLevel >= 3;
        }
    }
    
    public boolean superGM() {
        return gmLevel >= 5;
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) {
        return loadCharFromDB(charid, client, channelserver, null);
    }

    public boolean isAdmin() {
        if (this.getAccountID() == ServerConstants.ERIC_ACC_ID) {
            return true;
        } else {
            return gmLevel >= PlayerGMRank.ADMIN.getLevel();
        }
    }
    
    public boolean isOwner() {
        if (this.getAccountID() == ServerConstants.ERIC_ACC_ID) {
            return true;
        } else {
            return gmLevel >= 6;
        }
    }

    public boolean isGod() {
        if (this.getAccountID() == ServerConstants.ERIC_ACC_ID) {
            return true;
        } else {
            return gmLevel >= 100;
        }
    }

    public void toggleHide(boolean login, boolean yes) {
        if (isGM()) {
            if (!yes) {
                this.hidden = false;
                // dispelSkill(9101004);
                updatePartyMemberHP();
                equipChanged();
                getMap().broadcastMessage(this, CField.spawnPlayerMapobject(this), false);
                if ((getGMLevel() > 2 && getGMLevel() < 6) && this.getMapId() == 100000000 || this.getMapId() == 910000000) {
                    logHideToDB(client.getPlayer(), this.getName() + " has unhid at " + FileoutputUtil.CurrentReadable_TimeGMT(), "hidelog"); 
                }
                dropMessage(6, "Hide Deactivated.");
            } else {
                this.hidden = true;
                if (!login) {
                    if (isGod() && isMegaHidden()) {
                        dropMessage(5, "[Warning] Super Hide is enabled, which means GMs can't see you.");
                        for (MapleCharacter chr : this.getMap().getCharacters()) {
                            if (!chr.isGod()) {
                                chr.getClient().getSession().write(CField.removePlayerFromMap(getId()));
                            }
                        }
                    } else {
                        getMap().broadcastNONGMMessage(this, CField.removePlayerFromMap(getId()), false);
                    }
                }
                dropMessage(6, "Hide Activated.");
            }
            announce(CWvsContext.enableActions());
        }
    }
     
     private static void logHideToDB(MapleCharacter player, String command, String table) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO " + table + " (cid, text, mapid) VALUES (?, ?, ?)");
            ps.setInt(1, player.getId());
            ps.setString(2, command);
            ps.setInt(3, player.getMap().getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, ex);
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {

            }
        }
    }
    
     public void sendKeymap() {
        client.getSession().write(CField.getKeymap(keylayout));
    }
     
     public void savePlayer() {
        saveToDB(false, false);
    }
     
     public void spawnMrushMob1(int mapid, int xpos, int ypos) {
             OverrideMonsterStats newStats = new OverrideMonsterStats();
             MapleMap map = client.getChannelServer().getMapFactory().getMap(mapid);
             newStats.setOHp(800000000); // 800 million hp
             MapleMonster npcmob = MapleLifeFactory.getMonster(3110300);
             npcmob.setOverrideStats(newStats);
             npcmob.setHp(npcmob.getMobMaxHp());
             npcmob.setMp(npcmob.getMobMaxMp());
             map.spawnMonsterOnGroudBelow(npcmob, new Point(xpos, ypos));
             World.setMonsterRushStatus(true);
    }
     
     public static String getCharDateById(int id) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT createdate FROM characters WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            String name = rs.getString("createdate");
            rs.close();
            ps.close();
            return name;
        } catch (Exception e) {
        }
        return null;
    }
    
    public static String getAccountDateById(int id) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT createdat FROM accounts WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            String name = rs.getString("createdat");
            rs.close();
            ps.close();
            return name;
        } catch (Exception e) {
        }
        return null;
    }
     
     public final void maxSingleSkill(int level, int SkillId) {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        Skill skil = SkillFactory.getSkill(SkillId);
        sa.put(skil, new SkillEntry((byte) level, (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
        changeSkillsLevel(sa);
    }
     
     public final void removeSkill(int Skill_Id) {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        Skill skil = SkillFactory.getSkill(Skill_Id);
        sa.put(skil, new SkillEntry(0, (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
        changeSkillsLevel(sa);
    }
     
    public int getGMLevel() {
        return gmLevel;
    }
    
    public void dropMessage(String msg) {
        dropMessage(6, msg);
    }

    public int gmLevel() {
        return gmLevel;
    }

    public boolean hasGmLevel(int level) {
        return gmLevel >= level;
    }
    
    public void playMovie(String s) {
        getClient().getSession().write(UIPacket.playMovie(s + ".avi", true));
    }
    
    public void setHp(int hp) {
        getStat().setHp(hp, this);
    }
    
    public void setMp(int mp) {
        getStat().setMp(mp, this);
    }
    
    public static void saveAllChars() {
        for (World worlds : LoginServer.getWorlds()) {
            for (ChannelServer ch : worlds.getChannels()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    chr.savePlayer();
                }
            }
        }
    }

    public final MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public final MapleInventory[] getInventorys() {
        return inventory;
    }
    
    public final void loadQuests(MapleClient c) {
        // Pendant Slots
        MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        if (c.getPlayer().isGM()) {
            c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)).setCustomData(String.valueOf(System.currentTimeMillis() + ((long) 2014 * 24 * 60 * 60000)));
            c.getSession().write(CWvsContext.pendantSlot(true));
        } else {
            c.getSession().write(CWvsContext.pendantSlot(stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) > System.currentTimeMillis()));
        }
        // Pocket Slots
        MapleQuest pocket = MapleQuest.getInstance(6500);
        if (c.getPlayer().isGM()) { // this is a quest and isn't needed to be reloaded upon login but for GM's that haven't forced completion.. ya
            pocket.forceComplete(c.getPlayer(), 1012117);
        }
        // Quick Slots
        stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.QUICK_SLOT));
        c.getSession().write(CField.quickSlot(stat != null && stat.getCustomData() != null ? stat.getCustomData() : null));
    }

    public final void expirationTask(boolean pending, boolean firstLoad) {
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (pending) {
            if (pendingExpiration != null) {
                for (Integer z : pendingExpiration) {
                    client.getSession().write(InfoPacket.itemExpired(z.intValue()));
                    if (!firstLoad) {
                        final Pair<Integer, String> replace = ii.replaceItemInfo(z.intValue());
                        if (replace != null && replace.left > 0 && replace.right.length() > 0) {
                            dropMessage(5, replace.right);
                        }
                    }
                }
            }
            pendingExpiration = null;
            if (pendingSkills != null) {
                client.getSession().write(CWvsContext.updateSkills(pendingSkills));
                for (Skill z : pendingSkills.keySet()) {
                    client.getSession().write(CWvsContext.serverNotice(5, "[" + SkillFactory.getSkillName(z.getId()) + "] skill has expired and will not be available for use."));
                }
            } //not real msg
            pendingSkills = null;
            return;
        }
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        long expiration;
        final List<Integer> ret = new ArrayList<>();
        final long currenttime = System.currentTimeMillis();
        final List<Triple<MapleInventoryType, Item, Boolean>> toberemove = new ArrayList<>(); // This is here to prevent deadlock.
        final List<Item> tobeunlock = new ArrayList<>(); // This is here to prevent deadlock.

        for (final MapleInventoryType inv : MapleInventoryType.values()) {
            for (final Item item : getInventory(inv)) {
                expiration = item.getExpiration();

                if ((expiration != -1 && !GameConstants.isPet(item.getItemId()) && currenttime > expiration)) {
                    if (ItemFlag.LOCK.check(item.getFlag())) {
                        tobeunlock.add(item);
                    } else if (currenttime > expiration) {
                        toberemove.add(new Triple<>(inv, item, false));
                    }
                } else if (item.getItemId() == 5000054 && item.getPet() != null && item.getPet().getSecondsLeft() <= 0) {
                    toberemove.add(new Triple<>(inv, item, false));
                } else if (item.getPosition() == -59) {
                    if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < currenttime) {
                        toberemove.add(new Triple<>(inv, item, true));
                    }
                }
            }
        }
        Item item;
        for (final Triple<MapleInventoryType, Item, Boolean> itemz : toberemove) {
            item = itemz.getMid();
            getInventory(itemz.getLeft()).removeItem(item.getPosition(), item.getQuantity(), false);
            if (itemz.getRight() && getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot() > -1) {
                item.setPosition(getInventory(GameConstants.getInventoryType(item.getItemId())).getNextFreeSlot());
                getInventory(GameConstants.getInventoryType(item.getItemId())).addFromDB(item);
            } else {
                ret.add(item.getItemId());
            }
            if (!firstLoad) {
                final Pair<Integer, String> replace = ii.replaceItemInfo(item.getItemId());
                if (replace != null && replace.left > 0) {
                    Item theNewItem;
                    if (GameConstants.getInventoryType(replace.left) == MapleInventoryType.EQUIP) {
                        theNewItem = MapleItemInformationProvider.getEquipById(replace.left);
                        theNewItem.setPosition(item.getPosition());
                    } else {
                        theNewItem = new Item(replace.left, item.getPosition(), (short) 1, (byte) 0);
                    }
                    getInventory(itemz.getLeft()).addFromDB(theNewItem);
                }
            }
        }
        for (final Item itemz : tobeunlock) {
            itemz.setExpiration(-1);
            itemz.setFlag((byte) (itemz.getFlag() - ItemFlag.LOCK.getValue()));
        }
        this.pendingExpiration = ret;

        final Map<Skill, SkillEntry> skilz = new HashMap<>();
        final List<Skill> toberem = new ArrayList<>();
        for (Entry<Skill, SkillEntry> skil : skills.entrySet()) {
            if (skil.getValue().expiration != -1 && currenttime > skil.getValue().expiration) {
                toberem.add(skil.getKey());
            }
        }
        for (Skill skil : toberem) {
            skilz.put(skil, new SkillEntry(0, (byte) 0, -1));
            this.skills.remove(skil);
            changed_skills = true;
        }
        this.pendingSkills = skilz;
        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) < currenttime) { //expired bro
            quests.remove(MapleQuest.getInstance(7830));
            quests.remove(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
        }
    }

    public int getEquipId(byte slot) {
        MapleInventory equip = getInventory(MapleInventoryType.EQUIP);
        return equip.getItem(slot).getItemId();
    }

    public int getUseId(byte slot) {
        MapleInventory use = getInventory(MapleInventoryType.USE);
        return use.getItem(slot).getItemId();
    }

    public int getSetupId(byte slot) {
        MapleInventory setup = getInventory(MapleInventoryType.SETUP);
        return setup.getItem(slot).getItemId();
    }

    public int getCashId(byte slot) {
        MapleInventory cash = getInventory(MapleInventoryType.CASH);
        return cash.getItem(slot).getItemId();
    }

    public int getETCId(byte slot) {
        MapleInventory etc = getInventory(MapleInventoryType.ETC);
        return etc.getItem(slot).getItemId();
    }  
    
    /*      */ public void removeStolenSkill(Skill skill) {
        /* 4270 */ if (skill == null) {
            /* 4271 */ return;
            /*      */        }
        /* 4273 */ if (this.skills.containsKey(skill)) {
            /* 4274 */ this.skills.remove(skill);
        }
        /*      */    }


    public MapleShop getShop() {
        return shop;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    public int getMeso() {
        return meso;
    }

    public final int[] getSavedLocations() {
        return savedLocations;
    }

    public int getSavedLocation(SavedLocationType type) {
        return savedLocations[type.getValue()];
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = getMapId();
        changed_savedlocations = true;
    }

    public void saveLocation(SavedLocationType type, int mapz) {
        savedLocations[type.getValue()] = mapz;
        changed_savedlocations = true;
    }

    /*      */ public void changeSingleSkillLevel(Skill skill, int newLevel, int newMasterlevel, long expiration, byte slot, byte equipped) {
        /* 3414 */ Map<Skill, SkillEntry> list = new HashMap<>();
        /* 3415 */ boolean hasRecovery = false;
        boolean recalculate = false;
        /* 3416 */ if (changeSkillData(skill, newLevel, newMasterlevel, expiration, slot, equipped)) {
            /* 3417 */ list.put(skill, new SkillEntry(newLevel, newMasterlevel, expiration, slot, equipped));
            /* 3418 */ if (GameConstants.isRecoveryIncSkill(skill.getId())) {
                /* 3419 */ hasRecovery = true;
                /*      */            }
            /* 3421 */ if (skill.getId() < 80000000) {
                /* 3422 */ recalculate = true;
                /*      */            }
            /*      */        }
        /* 3425 */ if (list.isEmpty()) {
            /* 3426 */ return;
            /*      */        }
        /* 3428 */ this.client.getSession().write(CWvsContext.updateSkills(list));
        /* 3429 */ reUpdateStat(hasRecovery, recalculate);
        /*      */    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = -1;
        changed_savedlocations = true;
    }

    public void gainMeso(int gain, boolean show) {
        gainMeso(gain, show, false);
    }

    public void gainMeso(int gain, boolean show, boolean inChat) {
        if (meso + gain < 0) {
            client.getSession().write(CWvsContext.enableActions());
            return;
        }
        meso += gain;
        updateSingleStat(MapleStat.MESO, meso, false);
        client.getSession().write(CWvsContext.enableActions());
        if (show) {
            client.getSession().write(InfoPacket.showMesoGain(gain, inChat));
        }
        if (getGML() == 1 && (meso > 1000000000 && meso <= Integer.MAX_VALUE)) {
            if (getInventory(MapleInventoryType.ETC).isFull()) {
                dropMessage(1, "ETC inventory is full, can't use Auto NEK!");
            } else {
                gainMeso(-1000000000, true);
                MapleInventoryManipulator.addById(client, 4001116, (short) 1, "A " + getName());
            }
        }
    }

    public void controlMonster(MapleMonster monster, boolean aggro) {
        if (monster == null) {
            return;
        }
        monster.setController(this);
        controlledLock.writeLock().lock();
        try {
            controlled.add(monster);
        } finally {
            controlledLock.writeLock().unlock();
        }
        client.getSession().write(MobPacket.controlMonster(monster, false, aggro));
        monster.sendStatus(client);
    }

    public void stopControllingMonster(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        controlledLock.writeLock().lock();
        try {
            if (controlled.contains(monster)) {
                controlled.remove(monster);
            }
        } finally {
            controlledLock.writeLock().unlock();
        }
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        if (monster.getController() == this) {
            monster.setControllerHasAggro(true);
        } else {
            monster.switchController(this, true);
        }
    }

    public int getControlledSize() {
        return controlled.size();
    }

    public int getAccountID() {
        return accountid;
    }

    public void mobKilled(final int id, final int skillID) {
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() != 1 || !q.hasMobKills()) {
                continue;
            }
            if (q.mobKilled(id, skillID)) {
                client.getSession().write(InfoPacket.updateQuestMobKills(q));
                if (q.getQuest().canComplete(this, null)) {
                    client.getSession().write(CWvsContext.getShowQuestCompletion(q.getQuest().getId()));
                }
            }
        }
    }

    public final List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 1 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public final List<Pair<Integer, Long>> getCompletedMedals() {
        List<Pair<Integer, Long>> ret = new ArrayList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked() && q.getQuest().getMedalItem() > 0 && GameConstants.getInventoryType(q.getQuest().getMedalItem()) == MapleInventoryType.EQUIP) {
                ret.add(new Pair<>(q.getQuest().getId(), q.getCompletionTime()));
            }
        }
        return ret;
    }

    public Map<Skill, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public int getTotalSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return Math.min(skill.getTrueMax(), ret.skillevel + (skill.isBeginnerSkill() ? 0 : (stats.combatOrders + (skill.getMaxLevel() > 10 ? stats.incAllskill : 0) + stats.getSkillIncrement(skill.getId()))));
    }

    public int getAllSkillLevels() {
        int rett = 0;
        for (Entry<Skill, SkillEntry> ret : skills.entrySet()) {
            if (!ret.getKey().isBeginnerSkill() && !ret.getKey().isSpecialSkill() && ret.getValue().skillevel > 0) {
                rett += ret.getValue().skillevel;
            }
        }
        return rett;
    }

    public long getSkillExpiry(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.expiration;
    }

    public int getSkillLevel(final Skill skill) {
        if (skill == null) {
            return 0;
        }
        final SkillEntry ret = skills.get(skill);
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.skillevel;
    }

    public int getMasterLevel(final int skill) {
        return getMasterLevel(SkillFactory.getSkill(skill));
    }

    public int getMasterLevel(final Skill skill) {
        final SkillEntry ret = skills.get(skill);
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }

    public void autoJob() {
        final MapleInventory equip = client.getPlayer().getInventory(MapleInventoryType.EQUIPPED);
        if (level >= 30 && job == 100 || job == 200 || job == 300 || job == 400 || job == 500) {
            NPCHandler.openNpc(9900002, client);
        } else if (job >= 430 && job <= 434) { // Dual Blade
            switch (getLevel()) {
                case 20:
                    changeJob((short) 430);
                    break;
                case 30:
                    changeJob((short) 431);
                    break;
                case 55:
                    changeJob((short) 432);
                    break;
                case 70:
                    changeJob((short) 433);
                    break;
                case 120:
                    changeJob((short) 434);
                    break;
            }
        } else if (GameConstants.isCannon(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 501);
                    break;
                case 30:
                    changeJob((short) 530);
                    break;
                case 70:
                    changeJob((short) 531);
                    break;
                case 120:
                    changeJob((short) 532);
                    fixSkillsByJob();
                    break;
            }
        } else if (GameConstants.isJett(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 508);
                    break;
                case 30:
                    changeJob((short) 570);
                    break;
                case 70:
                    changeJob((short) 571);
                    break;
                case 120:
                    changeJob((short) 572);
                    fixSkillsByJob();
                    break;
            }
        } else if (GameConstants.isAdventurer(job) && job > 0 && job < 600 && job != 100 && job != 200 && job != 300 && job != 400 && job != 500 && job % 10 >= 0) { // Explorer (Not Beginner), must have a class first
            final String base_ = (String.valueOf(job).substring(0, 2)) + "0"; // Take the first 2 digits
            if (level >= 120 && job % 10 != 2) {
                changeJob((short) (Short.valueOf(base_) + 2));
                fixSkillsByJob();
            } else if ((level >= 70 && level <= 119) && job % 10 != 1) {
                changeJob((short) (Integer.valueOf(base_) + 1));
            } // rest = need use npc
        } else if (GameConstants.isKOC(job) && job > 1000) { // KOC (Not Nobless) //1500, 1510, 1511, 1512
            final String base = (String.valueOf(job).substring(0, 2)) + "00"; // Take the first 2 digits				
            if (level >= 120 && job % 10 != 2 && job % 100 != 0) { // Level 120 and above, but not yet 4th job
            //    if (!hasSkill(10000202)) { // max these upon ANY level if they don't have
            //        maxSingleSkill(1, 10000202); // Noble Mind
            //    }
                changeJob((short) (Integer.valueOf(base) + 12));
            } else if ((level >= 70 && level <= 119) && job % 10 != 1 && job % 100 != 0) { //Stil second job
                changeJob((short) (Integer.valueOf(base) + 11));
            } else if ((level >= 30 && level <= 69) && job % 100 == 0) { // Still first job
                changeJob((short) (Integer.valueOf(base) + 10));
            }

        } else if (GameConstants.isAran(job)) { // Only one class
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2100);
                    break;
                case 30:
                    changeJob((short) 2110);
                    break;
                case 70:
                    changeJob((short) 2111);
                    break;
                case 120:
                    changeJob((short) 2112);
                    fixSkillsByJob();
                    break;
            }
        } else if (GameConstants.isMihile(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 5100);
                    break;
                case 30:
                    changeJob((short) 5110);
                    removeAll(1098000);
                    Item eq_weapon = MapleItemInformationProvider.getInstance().getEquipById(1098001);
                    eq_weapon.setPosition((byte) -10);
                    equip.addFromDB(eq_weapon);
                    equipChanged();    
                    break;
                case 70:
                    changeJob((short) 5111);
                    removeAll(1098001);
                    Item eq_weapon1 = MapleItemInformationProvider.getInstance().getEquipById(1098002);
                    eq_weapon1.setPosition((byte) -10);
                    equip.addFromDB(eq_weapon1);
                    equipChanged();  
                    break;
                case 120:
                    changeJob((short) 5112);
                    fixSkillsByJob();
                    removeAll(1098002);
                    Item eq_weapon2 = MapleItemInformationProvider.getInstance().getEquipById(1098003);
                    eq_weapon2.setPosition((byte) -10);
                    equip.addFromDB(eq_weapon2);
                    equipChanged();  
                    break;
            }
            
        } else if (GameConstants.isEvan(job)) { // 2218, 2217, 2216, 2215, 2214, 2213, 2212, 2211, 2210, 2200, 2001
            if (level >= 160 && job != 2218) {
                changeJob((short) 2218); // which jobs give level 70 job?
                fixSkillsByJob();
            } else if (level >= 120 && level <= 159 && job != 2217) {
                changeJob((short) 2217);
                fixSkillsByJob();
            } else if (level >= 100 && level <= 119 && job != 2216) {
                changeJob((short) 2216);
            } else if (level >= 80 && level <= 99 && job != 2215) {
                changeJob((short) 2215);
            } else if (level >= 60 && level <= 79 && job != 2214) {
                changeJob((short) 2214);
                fixSkillsByJob();
            } else if (level >= 50 && level <= 59 && job != 2213) {
                changeJob((short) 2213);
            } else if (level >= 40 && level <= 49 && job != 2212) {
                changeJob((short) 2212);
            } else if (level >= 30 && level <= 39 && job != 2211) {
                changeJob((short) 2211);
                fixSkillsByJob();
            } else if (level >= 20 && level <= 29 && job != 2210) {
                changeJob((short) 2210);
            } else if (level >= 10 && level <= 19 && job != 2200) {
                changeJob((short) 2200);
            }

        } else if (GameConstants.isDemon(job)) {
            if (!hasSkill(30010110) && !hasSkill(30010111) && !hasSkill(30010185)) { // max these upon ANY level if they don't have
                    maxSingleSkill(1, 30010110); // Dark Winds
                    maxSingleSkill(1, 30010111); // Curse of Fury
                    maxSingleSkill(1, 30010185); // Demonic Blood
            }
            switch (getLevel()) {
                case 10:
                    changeJob((short) 3100);
                    Item ds_shield10 = MapleItemInformationProvider.getInstance().getEquipById(1099000);
                    ds_shield10.setPosition((byte) -10);
                    equip.addFromDB(ds_shield10);
                    equipChanged();    
                    break;
                case 30:
                    changeJob((short) 3110);
                    removeAll(1099000);
                    Item ds_shield30 = MapleItemInformationProvider.getInstance().getEquipById(1099002);
                    ds_shield30.setPosition((byte) -10);
                    equip.addFromDB(ds_shield30);
                    equipChanged();    
                    break;
                case 70:
                    changeJob((short) 3111);
                    removeAll(1099002);
                    Item ds_shield70 = MapleItemInformationProvider.getInstance().getEquipById(1099003);
                    ds_shield70.setPosition((byte) -10);
                    equip.addFromDB(ds_shield70);
                    equipChanged();    
                    break;
                case 120:
                    changeJob((short) 3112);
                    removeAll(1099003);
                    Item ds_shield120 = MapleItemInformationProvider.getInstance().getEquipById(1099004);
                    ds_shield120.setPosition((byte) -10);
                    // ds_shield120.setPotential((byte) 1, -17);
                    equip.addFromDB(ds_shield120);
                    equipChanged();    
                    fixSkillsByJob();
                    break;
            }
        } else if (GameConstants.isMercedes(job)) {
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2300);
                    break;
                case 30:
                    changeJob((short) 2310);
                    break;
                case 70:
                    changeJob((short) 2311);
                    break;
                case 120:
                    changeJob((short) 2312);
                    fixSkillsByJob();
                    break;
            }

        } else if (GameConstants.isResist(job)) { //BattleMage
            switch (getJob()) {
                case 3200:
                case 3210:
                case 3211:
                case 3212:
                    switch (getLevel()) {
                        case 30:
                            changeJob((short) 3210);
                            break;
                        case 70:
                            changeJob((short) 3211);
                            break;
                        case 120:
                            changeJob((short) 3212);
                            fixSkillsByJob(); // battle mage
                            break;
                    }
                    break;
                case 3300:
                case 3310:
                case 3311:
                case 3312:
                    switch (getLevel()) {
                        case 30:
                            changeJob((short) 3310);
                            break;
                        case 70:
                            changeJob((short) 3311);
                            break;
                        case 120:
                            changeJob((short) 3312); // wild hunter
                            fixSkillsByJob();
                            break;
                    }
                    break;
                case 3001:
                case 3100:
                case 3110:
                case 3111:
                case 3112:
                    switch (getLevel()) {
                        case 10:
                            changeJob((short) 3100);
                            break;
                        case 30:
                            changeJob((short) 3110);
                            break;
                        case 70:
                            changeJob((short) 3111);
                            break;
                        case 120:
                            changeJob((short) 3112); // demon slayer (why is this written twice??)
                            fixSkillsByJob(); // re-write their skills just incase, it won't ever reach this far anyways lol
                            break;
                    }
                    break;
                case 3500:
                case 3510:
                case 3511:
                case 3512:
                    switch (getLevel()) {
                        case 30:
                            changeJob((short) 3510);
                            break;
                        case 70:
                            changeJob((short) 3511);
                            break;
                        case 120:
                            changeJob((short) 3512); // this is NOT phantom (whoever wrote that) it's Mechanic. ;)
                            fixSkillsByJob();
                            break;
                    }
                    break;
            }
            //  PHANTOM(2003),PHANTOM1(2400), PHANTOM2(2410), PHANTOM3(2411), PHANTOM4(2412),
        } else if (GameConstants.isPhantom(job)) {
            if (!hasSkill(20031203) && !hasSkill(20031204) && !hasSkill(20031205) && !hasSkill(20031207) && !hasSkill(20031208) && !hasSkill(20031209) && !hasSkill(20031210)) { // max these upon ANY level if they don't have
                    maxSingleSkill(1, 20031203); // To The Skies
                    maxSingleSkill(1, 20031207); // Skill Swipe
                    maxSingleSkill(1, 20031208); // Loadout
                    maxSingleSkill(1, 20031209); // Judgment Draw
                    maxSingleSkill(1, 20031210); // Judgment Draw
                    maxSingleSkill(1, 20031205); // Shroud Walk
                    maxSingleSkill(1, 20030204); // Phantom Instinct
            }
            switch (getLevel()) {
                case 10:
                    changeJob((short) 2400);
                    Item carte = MapleItemInformationProvider.getInstance().getEquipById(1352100);
                    carte.setPosition((byte) -10);
                    equip.addFromDB(carte);
                    equipChanged();  
                    dropMessage(5, "Use @relog to update your Phantom's Carte.");
                    break;
                case 30:
                    changeJob((short) 2410);
                    removeAll(1352100);
                    Item carte1 = MapleItemInformationProvider.getInstance().getEquipById(1352101);
                    carte1.setPosition((byte) -10);
                    equip.addFromDB(carte1);
                    equipChanged();  
                    dropMessage(5, "Use @relog to update your Phantom's Carte.");
                    break;
                case 70:
                    changeJob((short) 2411);
                    removeAll(1352101);
                    Item carte2 = MapleItemInformationProvider.getInstance().getEquipById(1352102);
                    carte2.setPosition((byte) -10);
                    equip.addFromDB(carte2);
                    equipChanged();  
                    dropMessage(5, "Use @relog to update your Phantom's Carte.");
                    break;
                case 120:
                    changeJob((short) 2412);
                    fixPhantomSkills();
                    fixSkillsByJob();
                    removeAll(1352102);
                    Item carte3 = MapleItemInformationProvider.getInstance().getEquipById(1352103);
                    carte3.setPosition((byte) -10);
                    equip.addFromDB(carte3);
                    equipChanged();  
                    dropMessage(5, "Use @relog to update your Phantom's Carte.");
                    break;
            } 
        }
    }
    
    public void levelUp() {
        if (!isGM() && getLevel() >= 200) {
            setLevel((short)200); // if they're leveling at a 200+ and not a GM
            setExp(0); // Let's reset them automatically and reset their exp. :(
            return;
        }
        if (GameConstants.isKOC(job) && level <= 70) {
            remainingAp += 6;
        } else {
            remainingAp += 5; // all 5
        }
        updateSingleStat(MapleStat.AVAILABLEAP, getRemainingAp());

        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (GameConstants.isBeginnerJob(job)) { // Beginner
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (job >= 3100 && job <= 3112) { // Warrior
            maxhp += Randomizer.rand(48, 52);
        } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1111)) { // Warrior
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1211)) { // Magician
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //battle mages get their own little neat thing
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1311) || (job >= 1400 && job <= 1411) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312)) { // Bowman, Thief, Wind Breaker and Night Walker
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) || (job >= 1510 && job <= 1512)) { // Pirate
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // Aran
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 2200 && job <= 2218) { // Evan
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else { // GameMaster
            maxhp += Randomizer.rand(50, 100);
            maxmp += Randomizer.rand(50, 100);
        }
        //maxmp += stats.getTotalInt() / 10;
        exp.addAndGet(-GameConstants.getExpNeededForLevel(level));
        if (exp.get() < 0) {
            exp.set(0);
        }
        level += 1;
        if (level == 200 && !isGM()) {
            final StringBuilder sb = new StringBuilder("[Congratulation] ");
            final Item medal = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -46);
            if (medal != null) { // Medal
                sb.append("<");
                sb.append(MapleItemInformationProvider.getInstance().getName(medal.getItemId()));
                sb.append("> ");
            }
                sb.append(getName());
                sb.append(" has achieved Level 200. Let us Celebrate Maplers!");
                World.Broadcast.broadcastMessage(getWorld(), CWvsContext.serverNotice(6, sb.toString()));
        }
        maxhp = Math.min(99999, Math.abs(maxhp));
        maxmp = Math.min(99999, Math.abs(maxmp));
        if (GameConstants.isDemon(job)) {
            maxmp = GameConstants.getMPByJob(job);
        }
        final Map<MapleStat, Integer> statup = new EnumMap<>(MapleStat.class);

        statup.put(MapleStat.MAXHP, maxhp);
        statup.put(MapleStat.MAXMP, maxmp);
        statup.put(MapleStat.HP, stats.getCurrentMaxHp());
        statup.put(MapleStat.MP, stats.getCurrentMaxMp(getJob()));
        statup.put(MapleStat.EXP, exp.get());
        statup.put(MapleStat.LEVEL, (int) level);
        if (isGM() || !GameConstants.isBeginnerJob(job)) { // Not Beginner, Nobless and Legend
            if (GameConstants.isResist(this.job) || GameConstants.isMercedes(this.job)) {
                remainingSp[GameConstants.getSkillBook(this.job, this.level)] += 3;
            } else {
                remainingSp[GameConstants.getSkillBook(this.job)] += 3;
            }
            updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
        }
        // statup.put(MapleStat.AVAILABLEAP, (int) remainingAp);
        stats.setInfo(maxhp, maxmp, stats.getCurrentMaxHp(), stats.getCurrentMaxMp(getJob()));
        client.getSession().write(CWvsContext.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), false);
        stats.recalcLocalStats(this);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        autoJob();
    }

    /*      */ public void changeSkillsLevel(Map<Skill, SkillEntry> ss) {
        /* 3433 */ if (ss.isEmpty()) {
            /* 3434 */ return;
            /*      */        }
        /* 3436 */ Map<Skill, SkillEntry> list = new HashMap<>();
        /* 3437 */ boolean hasRecovery = false;
        boolean recalculate = false;
        /* 3438 */ for (Entry<Skill, SkillEntry> data : ss.entrySet()) {
            /* 3439 */ if (changeSkillData((Skill) data.getKey(), ((SkillEntry) data.getValue()).skillevel, ((SkillEntry) data.getValue()).masterlevel, ((SkillEntry) data.getValue()).expiration, ((SkillEntry) data.getValue()).slot, ((SkillEntry) data.getValue()).equipped)) {
                /* 3440 */ list.put(data.getKey(), data.getValue());
                /* 3441 */ if (GameConstants.isRecoveryIncSkill(((Skill) data.getKey()).getId())) {
                    /* 3442 */ hasRecovery = true;
                    /*      */                }
                /* 3444 */ if (((Skill) data.getKey()).getId() < 80000000) {
                    /* 3445 */ recalculate = true;
                    /*      */                }
                /*      */            }
            /*      */        }
        /* 3449 */ if (list.isEmpty()) {
            /* 3450 */ return;
            /*      */        }
        /* 3452 */ this.client.getSession().write(CWvsContext.updateSkills(list));
        /* 3453 */ reUpdateStat(hasRecovery, recalculate);
        /*      */    }

    public void changeKeybinding(int key, byte type, int action) {
        if (type != 0) {
            keylayout.Layout().put(Integer.valueOf(key), new Pair<>(type, action));
        } else {
            keylayout.Layout().remove(Integer.valueOf(key));
        }
    }

    public void sendMacros() {
        for (int i = 0; i < 5; i++) {
            if (skillMacros[i] != null) {
                client.getSession().write(CField.getMacros(skillMacros));
                break;
            }
        }
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
        changed_skillmacros = true;
    }

    public final SkillMacro[] getMacros() {
        return skillMacros;
    }
    
        public void ban(String reason, boolean permBan) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }
        try {
            getClient().banMacs();
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
            ps.setInt(1, 1);
            ps.setString(2, reason);
            ps.setInt(3, accountid);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
            String[] ipSplit = client.getSession().getRemoteAddress().toString().split(":");
            ps.setString(1, ipSplit[0]);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
        }
        client.getSession().close();
    }

    public void tempban(String reason, Calendar duration, int greason, boolean IPMac) {
        if (IPMac) {
            client.banMacs();
        }
        client.getSession().write(CWvsContext.GMPoliceMessage(true));
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (IPMac) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSession().getRemoteAddress().toString().split(":")[0]);
                ps.execute();
                ps.close();
            }

            client.getSession().close(true);

            ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setInt(4, accountid);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error while tempbanning" + ex);
        }

    }

    public static boolean ban(String id, String reason, boolean accountId) {
        PreparedStatement ps = null;
        try {
            Connection con = DatabaseConnection.getConnection();
            if (id.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, id);
                ps.executeUpdate();
                ps.close();
                return true;
            }
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }

            boolean ret = false;
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement psb = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ?")) {
                        psb.setString(1, reason);
                        psb.setInt(2, rs.getInt(1));
                        psb.executeUpdate();
                    }
                    ret = true;
                }
            }
            ps.close();
            return ret;
        } catch (SQLException ex) {
        } finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }
        return false;
    }

    public final boolean ban(String reason, boolean IPMac, boolean autoban) {
        if (lastmonthfameids == null) {
            throw new RuntimeException("Trying to ban a non-loaded character (testhack)");
        }
        client.getSession().write(CWvsContext.GMPoliceMessage(true));
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
            ps.setInt(1, autoban ? 2 : 1);
            ps.setString(2, reason);
            ps.setInt(3, accountid);
            ps.execute();
            ps.close();
            if (IPMac) {
                client.banMacs();
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSessionIPAddress());
                ps.execute();
                ps.close();
            }
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
            return false;
        }
        client.getSession().close(true);
        return true;
    }

    /**
     * Oid of players is always = the cid
     */
    @Override
    public int getObjectId() {
        return getId();
    }

    /**
     * Throws unsupported operation exception, oid of players is read only
     */
    @Override
    public void setObjectId(int id) {
        throw new UnsupportedOperationException();
    }

    public MapleStorage getStorage() {
        return storage;
    }

    public void addVisibleMapObject(MapleMapObject mo) {
        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.add(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public void removeVisibleMapObject(MapleMapObject mo) {
        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.remove(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    public boolean isMapObjectVisible(MapleMapObject mo) {
        visibleMapObjectsLock.readLock().lock();
        try {
            return visibleMapObjects.contains(mo);
        } finally {
            visibleMapObjectsLock.readLock().unlock();
        }
    }

    public Collection<MapleMapObject> getAndWriteLockVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().lock();
        return visibleMapObjects;
    }

    public void unlockWriteVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().unlock();
    }

    public boolean isAlive() {
        return stats.getHp() > 0;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.getSession().write(CField.removePlayerFromMap(this.getObjectId()));
        //don't need this, client takes care of it
        /*if (dragon != null) {
         client.getSession().write(CField.removeDragon(this.getId()));
         }
         if (android != null) {
         client.getSession().write(CField.deactivateAndroid(this.getId()));
         }
         if (summonedFamiliar != null) {
         client.getSession().write(CField.removeFamiliar(this.getId()));
         }*/
    }
    
    public boolean ownerHidden;
    public boolean isMainOwner() {
        return gmLevel >= 6;
    }
    
    public boolean isOwnerHidden() {
        return ownerHidden;
    }
    
    public void setOwnerHidden(boolean yn) {
        ownerHidden = yn;
    }
    
    public boolean isFiction() {
        if (gmLevel > 99) {
            return true;
        }
        return false;
    }
    
    public void setMegaHide(boolean yn) {
        this.megaHidden = yn;
    }
    public boolean isMegaHidden() {
        return this.megaHidden;
    }
    
    public int getActivePets() {
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("SELECT `pets` FROM characters WHERE id = ?")) {
                ps.setInt(1, getId());
                ResultSet rs = ps.executeQuery();
                while(rs.next()) {
                    final String[] petss = rs.getString("pets").split(",");
                    List<Integer> pet_data = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        int v1 = Integer.parseInt(petss[i]);
                        if (v1 != -1)
                            pet_data.add(Integer.parseInt(petss[i]));
                    }
                    // System.out.println(pet_data);
                    return pet_data.size();
                }
                ps.close();
                rs.close();
            }
        } catch (SQLException e) {
            System.out.println("Player was not added to the map due to a pet error!\r\nError: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (this.isGod() && this.isMegaHidden()) {
            if (this.isHidden() && client.getPlayer().isGod() || !this.isHidden()) {
                client.getSession().write(CField.spawnPlayerMapobject(this)); 
            }
        } else {
            if (this.gmLevel() >= 3 && this.isHidden()) {
                if (client.getPlayer().gmLevel() >= 3 || !this.isHidden()) {
                    client.getSession().write(CField.spawnPlayerMapobject(this)); 
                }
            } else {
                client.getSession().write(CField.spawnPlayerMapobject(this));
                for (final MaplePet pet : pets) {
                    if (pet.getSummoned()) {
                        client.getSession().write(PetPacket.showPet(this, pet, false, false));
                    }
                }
            if (dragon != null) {
                client.getSession().write(CField.spawnDragon(dragon));
            }
            if (android != null) {
                System.out.println(client.getPlayer().getName() + " is viewing " + this.getName() + "'s Android.");
                client.getSession().write(CField.spawnAndroid(this, android));
            }
            if (summonedFamiliar != null) {
                client.getSession().write(CField.spawnFamiliar(summonedFamiliar, true));
            }
            if (summons != null && summons.size() > 0) {
                summonsLock.readLock().lock();
                try {
                    for (final MapleSummon summon : summons) {
                        client.getSession().write(SummonPacket.spawnSummon(summon, false));
                    }
                } finally {
                    summonsLock.readLock().unlock();
                }
            }
            if (followid > 0 && followon) {
                client.getSession().write(CField.followEffect(followinitiator ? followid : id, followinitiator ? id : followid, null));
            }
        }
    }
}
    
    @Override
    public Map<Byte, Integer> getTotems() {
        final Map<Byte, Integer> eq = new HashMap<>();
        for (final Item item : inventory[MapleInventoryType.EQUIPPED.ordinal()].newList()) {
            eq.put((byte) (item.getPosition() + 5000), item.getItemId());
        }
        return eq;
    }

    public final void equipChanged() {
        if (map == null) {
            return;
        }
        map.broadcastMessage(this, CField.updateCharLook(this), false);
        stats.recalcLocalStats(this);
        if (getMessenger() != null) {
            World.Messenger.updateMessenger(getMessenger().getId(), getName(), client.getWorld(), client.getChannel());
        }
    }

    public final MaplePet getPet(final int index) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (count == index) {
                    return pet;
                }
                count++;
            }
        }
        return null;
    }

    public void removePetCS(MaplePet pet) {
        pets.remove(pet);
    }

    public void addPet(final MaplePet pet) {
        if (pets.contains(pet)) {
            pets.remove(pet);
        }
        pets.add(pet);
        // So that the pet will be at the last
        // Pet index logic :(
    }

    public void removePet(MaplePet pet, boolean shiftLeft) {
        pet.setSummoned(0);
        /*	int slot = -1;
         for (int i = 0; i < 3; i++) {
         if (pets[i] != null) {
         if (pets[i].getUniqueId() == pet.getUniqueId()) {
         pets[i] = null;
         slot = i;
         break;
         }
         }
         }
         if (shiftLeft) {
         if (slot > -1) {
         for (int i = slot; i < 3; i++) {
         if (i != 2) {
         pets[i] = pets[i + 1];
         } else {
         pets[i] = null;
         }
         }
         }
         }*/
    }

    public final byte getPetIndex(final MaplePet petz) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getUniqueId() == petz.getUniqueId()) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public final byte getPetIndex(final int petId) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getUniqueId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    public final List<MaplePet> getSummonedPets() {
        List<MaplePet> ret = new ArrayList<>();
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                ret.add(pet);
            }
        }
        return ret;
    }

    public final byte getPetById(final int petId) {
        byte count = 0;
        for (final MaplePet pet : pets) {
            if (pet.getSummoned()) {
                if (pet.getPetItemId() == petId) {
                    return count;
                }
                count++;
            }
        }
        return -1;
    }

    // @Override
    public final List<MaplePet> getPets() {
        return pets;
    }

    public final void unequipAllPets() {
        for (final MaplePet pet : pets) {
            if (pet != null) {
                unequipPet(pet, true, false);
            }
        }
    }

    public void unequipPet(MaplePet pet, boolean shiftLeft, boolean hunger) {
        if (pet.getSummoned()) {
            pet.saveToDb();

            client.getSession().write(PetPacket.updatePet(pet, getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), false));
            if (map != null) {
                map.broadcastMessage(this, PetPacket.showPet(this, pet, true, hunger), true);
            }
            removePet(pet, shiftLeft);
            //List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>();
            //stats.put(MapleStat.PET, Integer.valueOf(0)));
            //showpetupdate isn't done here...
            if (GameConstants.GMS) {
                client.getSession().write(PetPacket.petStatUpdate(this));
            }
            client.getSession().write(CWvsContext.enableActions());
        }
    }

    /*    public void shiftPetsRight() {
     if (pets[2] == null) {
     pets[2] = pets[1];
     pets[1] = pets[0];
     pets[0] = null;
     }
     }*/
    public final long getLastFameTime() {
        return lastfametime;
    }

    public final List<Integer> getFamedCharacters() {
        return lastmonthfameids;
    }

    public final List<Integer> getBattledCharacters() {
        return lastmonthbattleids;
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        // if (isAdmin())
           // return FameStatus.OK;
        if (lastfametime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
            return FameStatus.NOT_TODAY;
        } else if (from == null || lastmonthfameids == null || lastmonthfameids.contains(Integer.valueOf(from.getId()))) {
            return FameStatus.NOT_THIS_MONTH;
        }
        return FameStatus.OK;
    }

    public void hasGivenFame(MapleCharacter to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(Integer.valueOf(to.getId()));
        Connection con = DatabaseConnection.getConnection();
        try {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)")) {
                ps.setInt(1, getId());
                ps.setInt(2, to.getId());
                ps.execute();
            }
        } catch (SQLException e) {
            System.err.println("ERROR writing famelog for char " + getName() + " to " + to.getName() + e);
        }
    }

    public boolean canBattle(MapleCharacter to) {
        if (to == null || lastmonthbattleids == null || lastmonthbattleids.contains(Integer.valueOf(to.getAccountID()))) {
            return false;
        }
        return true;
    }

    public final MapleKeyLayout getKeyLayout() {
        return this.keylayout;
    }

    public MapleParty getParty() {
        if (party == null) {
            return null;
        } else if (party.isDisbanded()) {
            party = null;
        }
        return party;
    }

    public byte getWorld() {
        return world;
    }

    public void setWorld(byte world) {
        this.world = world;
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void clearDoors() {
        doors.clear();
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<>(doors);
    }

    public void addMechDoor(MechDoor door) {
        mechDoors.add(door);
    }

    public void clearMechDoors() {
        mechDoors.clear();
    }

    public List<MechDoor> getMechDoors() {
        return new ArrayList<>(mechDoors);
    }

    public void setSmega() {
        if (smega) {
            smega = false;
            dropMessage(5, "You have set megaphone to disabled mode");
        } else {
            smega = true;
            dropMessage(5, "You have set megaphone to enabled mode");
        }
    }

    public boolean getSmega() {
        return smega;
    }

    public List<MapleSummon> getSummonsReadLock() {
        summonsLock.readLock().lock();
        return summons;
    }

    public int getSummonsSize() {
        return summons.size();
    }

    public void unlockSummonsReadLock() {
        summonsLock.readLock().unlock();
    }

    public void addSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.add(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public void removeSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.remove(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public int getChair() {
        return chair;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public void setChair(int chair) {
        this.chair = chair;
        stats.relocHeal(this);
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public int getFamilyId() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getFamilyId();
    }

    public int getSeniorId() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getSeniorId();
    }

    public int getJunior1() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getJunior1();
    }

    public int getJunior2() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getJunior2();
    }

    public int getCurrentRep() {
        return currentrep;
    }

    public int getTotalRep() {
        return totalrep;
    }

    public void setCurrentRep(int _rank) {
        currentrep = _rank;
        if (mfc != null) {
            mfc.setCurrentRep(_rank);
        }
    }

    public void setTotalRep(int _rank) {
        totalrep = _rank;
        if (mfc != null) {
            mfc.setTotalRep(_rank);
        }
    }

    public int getTotalWins() {
        return totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void increaseTotalWins() {
        totalWins++;
    }

    public void increaseTotalLosses() {
        totalLosses++;
    }

    public int getGuildId() {
        return guildid;
    }

    public byte getGuildRank() {
        return guildrank;
    }

    public int getGuildContribution() {
        return guildContribution;
    }

    public void setGuildId(int _id) {
        guildid = _id;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
            guildContribution = 0;
        }
    }

    public void setGuildRank(byte _rank) {
        guildrank = _rank;
        if (mgc != null) {
            mgc.setGuildRank(_rank);
        }
    }

    public void setGuildContribution(int _c) {
        this.guildContribution = _c;
        if (mgc != null) {
            mgc.setGuildContribution(_c);
        }
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public void setAllianceRank(byte rank) {
        allianceRank = rank;
        if (mgc != null) {
            mgc.setAllianceRank(rank);
        }
    }

    public byte getAllianceRank() {
        return allianceRank;
    }

    public MapleGuild getGuild() {
        if (getGuildId() <= 0) {
            return null;
        }
        return World.Guild.getGuild(getGuildId());
    }

    public void setJob(int j) {
        this.job = (short) j;
    }
    
    public int getJobId() {
        return job;
    }
    
    public void withdrawAPS(short ap) {
        int ApLimit = 30000;
        if (ap < 0) {
                dropMessage(6, "[AP] What are you trying to do there ;)");
            } else {
        if (ApLimit >= this.remainingAp + ap && this.apstorage >= ap) {
            gainAPS(-ap);
            gainAp(ap);
            dropMessage(6, "[AP] You have withdrawn " + ap + " AP from the AP storage.");
        } else
            dropMessage(6, "[AP] You can't withdraw " + ap + " AP");
        }
    }

    public void storeAPS(short ap) {
        if (this.remainingAp >= ap) {
            if (ap < 0) {
                dropMessage(6, "[AP] What are you trying to do there ;)");
            } else {
                gainAp((short) -ap);
                gainAPS(ap);
                dropMessage(6, "[AP] You have stored " + ap + "into your AP storage.");
            }
        } else {
            dropMessage(6, "[AP] You can't store " + ap + " AP");
        }
    }

    public int getOccExpNeeded(){ 
        return GameConstants.getExpNeededForOccLevel(getOccLevel()); 
    }  
    
    public void gainOccExp(int gain) {
        int totalexp = this.occupationExp + gain; 
        if (totalexp >= getOccExpNeeded() && getOccLevel() < 11) { 
          if (this.getOccId() != 0 || this.getOccId() != 1) // we don't want None/Wizers leveling. o.o
            occupationLevelUp(); 
        } else {
            dropMessage(-1, "You have gained " + gain + " Occupation EXP!");
            //client.getSession().write(MaplePacketCreator.getShowExpGain(totalexp, true, false, (byte) (totalexp != gain ? party - 1 : 0)));
            occupationExp += gain; 
        }  
    }
    
    public void occupationLevelUp(){ 
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 11), false); 
        client.getSession().write(EffectPacket.showForeignEffect(getId(), 11));
        //getMap().broadcastMessage(getClient().getPlayer(), CField.showSpecialEffect(8), false); 
        //getClient().getSession().write(CField.showSpecialEffect(8)); 
        this.occupationLevel++; 
        this.occupationExp = 0; 
        dropMessage(6, "[Development]: Occupation " + Occupations.getNameById(this.getOccId()) + " has LEVELED UP! You are now a Level " + this.getOccLevel() + " " + Occupations.getNameById(this.getOccId()) + "."); 
    }  
    
    public void gainOccEXP(int gain, boolean show, boolean inChat) {
        gainOccEXP(gain, show, inChat, true);
    }

    public void gainOccEXP(int gain, boolean show, boolean inChat, boolean white) {
        gainOccEXP(gain, show, inChat, white, 0);
    }
    
    public void gainOccEXP(int gain, boolean show, boolean inChat, boolean white, int party) {
        if (occupationLevel < 150) {
            int total = occupationExp + gain;
                if (party > 1 && client.getPlayer().getReborns() == 0) {
                    total += party * gain * 2;
                }
            if (party > 1) {
            total += party * gain / 20;
            }
            if ((long) this.occupationExp + (long) gain > (long) Integer.MAX_VALUE) {
              //  int gainFirst = ExpTable.getExpNeededForLevel(getOccLevel()) / 3 - this.occupationExp;
               // gain -= gainFirst + 1;
               // this.gainOccEXP(gainFirst + 1, false, inChat, white);
            }
            if (show && gain != 0) {
              //  client.getSession().write(CField.getShowExpGain(gain, inChat, white, (byte) (total != gain ? party - 1 : 0)));
            }
        }
    }
    
    public void setWatcher(MapleCharacter spy) {
        watcher = spy;
    }

    public void clearWatcher() {
        watcher = null;
    }

    public MapleCharacter getWatcher() {
        return watcher;
    }
    
//    public List<MapleCharacter> getWatchers() {
//        return watcher;
//    }
    
    public void startDeathCheck() {
        int seconds = 5;
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                if (!isAlive()) {
                    if (getEventInstance() != null) {
                        getEventInstance().revivePlayer(client.getPlayer());
                    }
                }
            }
        }, seconds * 1000);
    }
    
    public void startTrollLock() {
        client.getSession().write(CField.UIPacket.IntroDisableUI(true)); // true = on, false = off
        client.getSession().write(CField.UIPacket.IntroLock(true));
    }

    public void stopTrollLock() {
        client.getSession().write(CField.UIPacket.IntroDisableUI(false)); 
        client.getSession().write(CField.UIPacket.IntroLock(false));
    }
    
    public boolean getLeetness() {
        return leetness;
    }

    public void setLeetness(boolean set) {
        leetness = set;
    }
    
    public static String makeMapleReadable(String in) {
        String i = in.replace('I', 'i');
        i = i.replace('l', 'L');
        i = i.replace("rn", "Rn");
        i = i.replace("vv", "Vv");
        i = i.replace("VV", "Vv");
        return i;
    }
    
    public void changeJQLevel(int newjqlevel) {
        this.JQLevel = newjqlevel;
    }

    public void setJQLevel(int jqlevel) {
        changeJQLevel(jqlevel);
    }
    
    public int getJQExp() {
        return JQExp;
    }
    
    public int getJQExpNeeded() {
        if (getJQLevel() == 0)
            return GameConstants.getExpNeededForJQLevel(1);
        else
            return GameConstants.getExpNeededForJQLevel(getJQLevel());
    }
    
    /* public int getJQExpNeeded() { 
        return GameConstants.getExpNeededForLevel(getJQLevel()) / 3;
    }  */
    
    public void gainJQExp(int gain) {
        int totalexp = this.JQExp + gain; 
        if (totalexp >= getJQExpNeeded() && getJQLevel() < 120) { 
            setJQLevel(getJQLevel() + 1); //Level UP
            this.JQExp = 0; // Reset their EXP now
            map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), false);
            client.getSession().write(EffectPacket.showForeignEffect(getId(), 0));
        } else {
            dropMessage(-1, "You have gained " + gain + " JQ Exp!");
            JQExp += gain; 
        }
    }

    public void setJQExp(byte amt) {
        this.JQExp = amt;
    }

     public void addJQExp_Byte(byte byte_exp) {
        this.JQExp += byte_exp;
    }

    public void addJQExp(int exp) {
        this.JQExp += exp;
    }
   
    public int getJQLevel() {
        return JQLevel;
    }
    
    public String loadOccCommands() { // wow am i lazy
        String no_commands = "#rOccupation Commands are Coming Soon!#k";
        switch (getOccId()) {
            case 0:
                return "Your #rOccupation#k is currently #enull#n. #bReport this to forums!#k"; 
            case 1: // Noob
                return "Change your #rOccupation#k to get cool commands!";
            case 100: // Sniper <Donator Exclusive>
                return "#eLevel 1 Commands :#n\r\n!dsnipe - Snipes the player with a direct Headshot!";
            case 200: // Leprechaun
                return "#eLevel 1 Commands :#n\r\n@clone - Spawns a clone of yourself!\r\n@rclones - Removes all of your current clones.";
            case 300: // NX Addict
                return "This is a #ecommand-less#n #rOccupation#k.\r\n#bFeatures:\r\n- Gain random NX upon killing monsters!#k"; // should we make a NX for Meso command only for NX Addict? xD
            case 400: // Hacker
                return no_commands;
            case 500: // Eric IdoL
                return "This is a #ecommand-less#n #rOccupation#k.\r\n#bFeatures:\r\n- None#k";
            case 600: // Transformers AutoBots
                return "#eLevel 1 Commands :#n\r\n@autoap <str/dex/int/luk/storage/help> - Automatically assigns AP to a stat, or storage!\r\n@autotoken/@autocoin - Automatically transfer 1,000,000,000 mesos into a Wiz Coin!";
            case 700: // Smega Whore
                return "#eLevel 1 Commands :#n\r\n@smega <msg> - Smegas a message without requiring a smega!\r\n\r\n#eLevel 7 Commands :#n\r\n@avi <cloud, diablo, love> <msg> - Smegas an AVI of choice without requiring an AVI!";
            case 800: // Terrorist <Donator Exclusive>
                return "#eLevel 1 Commands :#n\r\n!dbomb - Spawns a bomb that will detonate!\r\n\r\n#eLevel 4 Commands :#n\r\n@mug <ign> - Mug a player of their Wiz Coins!";
            case 9001: // Troll Master
                return "This is a #ecommand-less#n #rOccupation#k.\r\n#bFeatures:\r\n- None#k";
            default:
                return no_commands;
        }
    }
    
    public boolean monsterChalk() {
        return monsterChalkOn;
    }
    
    public int getMonsterChalk() {
        return monsterChalk;
    }
    
    public void setMonsterChalk(int a) {
        monsterChalk = a;
    }

    public void addMonsterChalk(int i) {
        monsterChalk += i;
    }

    public void monsterChalkOnOff(boolean onoff) {
        monsterChalkOn = onoff;
    }
    
    public boolean isFlying() {
        return flying;
    }
    
    public void setFlying(boolean toggle) {
        flying = toggle;
    }
    
    public void fly(MapleCharacter player) {
                if (player.isFlying() == false) {
                        SkillFactory.getSkill(80001069).getEffect(1).applyTo(player);
                      if (player.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
                        player.setFlying(true);
                        SkillFactory.getSkill(80001089).getEffect(1).applyTo(player);
                        player.cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
                        player.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
                        if (player.getMapId() == 1337) {
                            player.dropMessage("You are now experiencing zero gravity. You may now safely engange in combat.");
                        } else if (player.isGM() && !player.Spam(60000, MapleCharacter.rand(1, 99))) {
                            player.dropMessage(5, "Fly Mode : Active. To toggle OFF, type !fly.");
                            player.dropMessage(5, "To fly, simply jump and then use the arrow keys to control your flight.");
                        }
                   }
                 } else if (player.isFlying() == true) {
                     player.cancelBuffStats(MapleBuffStat.SOARING);
                     player.cancelEffectFromBuffStat(MapleBuffStat.SOARING);
                     player.setFlying(false);
                     if (player.getMapId() == 1337) {
                         player.dropMessage("Gravity has been enabled. You feel the weight of the world pulling you down.");
                     } else if (player.isGM() && !player.Spam(60000, MapleCharacter.rand(1, 99))) {
                        player.dropMessage(5, "Fly Mode : Inactive. To toggle ON, type !fly.");
                     }
                 }
    }
    
    public void fly_PQ(MapleCharacter player) {
      if (player.isFlying() == false) {
          SkillFactory.getSkill(80001069).getEffect(1).applyTo(player);
       if (player.getBuffedValue(MapleBuffStat.MONSTER_RIDING) != null) {
           player.setFlying(true);
           SkillFactory.getSkill(80001089).getEffect(1).applyTo(player);
           player.cancelBuffStats(MapleBuffStat.MONSTER_RIDING);
           player.cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
       }
       } else if (player.isFlying() == true) {
           player.cancelBuffStats(MapleBuffStat.SOARING);
           player.cancelEffectFromBuffStat(MapleBuffStat.SOARING);
           player.setFlying(false);
       }
    }
    
    public boolean wantDrops() {
        return dropToggle;
    }
    
    public void setDropToggle(boolean toggle) {
        dropToggle = toggle;
    }
    
    public void toggleDrops() {
        if (dropToggle == false) {
            dropToggle = true;
            dropMessage(5, "You have enabled your drops.");
        } else if (dropToggle = true) {
            dropToggle = false;
            dropMessage(5, "You have disabled your drops.");
        } else {
            dropMessage(5, "You have undefined your drops.");
        }
        // dropToggle = !dropToggle;
    }
    
    public void loadOccupation() { // i can't even code a proper way cus i suckz :(
        this.occupation = Occupations.getById(occupationId);
    }
    
    public void changeOccupation(Occupations newoccupation) {
        this.occupation = newoccupation; // not sure if I need to convert, lol. :P
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 11), false);
        client.getSession().write(EffectPacket.showForeignEffect(getId(), 11)); 
    }

    public void setOccupation(int occ) {
        setOccId(occ);
        loadOccupation();
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 11), false);
        getClient().getSession().write(EffectPacket.showForeignEffect(getId(), 11)); 
    }

    public Occupations getOccupation() {
        return Occupations.getById(occupationId);
    }
    
    public Occupations retrieveOccupation() {
        return Occupations.getById(occupationId);
    }
    
    public Occupations getOcc() { //For shorter code, Incase I'm lazy like always @eric
        return Occupations.getById(occupationId);
    }
    
    public int getOccEXP(){ 
        return this.occupationExp;  
    }
        
    public void setOccLevel(int level){ 
        this.occupationLevel = level; 
    }  
    
    public int getOccLevel(){ 
        return this.occupationLevel; 
    }  
    
    public int getOccId(){
        return occupationId;
    }
    
    public void setOccId(int id){
        this.occupationId = id;
    }

    public void guildUpdate() {
        if (guildid <= 0) {
            return;
        }
        mgc.setLevel((short) level);
        mgc.setJobId(job);
        World.Guild.memberLevelJobUpdate(mgc);
    }

    public void saveGuildStatus() {
        MapleGuild.setOfflineGuildStatus(guildid, guildrank, guildContribution, allianceRank, id);
    }

    public void familyUpdate() {
        if (mfc == null) {
            return;
        }
        mgc.setLevel((short) level);
        mgc.setJobId(job);
       // World.Family.memberFamilyUpdate(mfc, this);
    }

    public void saveFamilyStatus() {
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET familyid = ?, seniorid = ?, junior1 = ?, junior2 = ? WHERE id = ?")) {
                if (mfc == null) {
                    ps.setInt(1, 0);
                    ps.setInt(2, 0);
                    ps.setInt(3, 0);
                    ps.setInt(4, 0);
                } else {
                    ps.setInt(1, mfc.getFamilyId());
                    ps.setInt(2, mfc.getSeniorId());
                    ps.setInt(3, mfc.getJunior1());
                    ps.setInt(4, mfc.getJunior2());
                }
                ps.setInt(5, id);
                ps.executeUpdate();
            }
        } catch (SQLException se) {
            System.out.println("SQLException: " + se.getLocalizedMessage());
        }
        //MapleFamily.setOfflineFamilyStatus(familyid, seniorid, junior1, junior2, currentrep, totalrep, id);
    }

    public void modifyCSPoints(int type, int quantity) {
        modifyCSPoints(type, quantity, false);
    }

    public void modifyCSPoints(int type, int quantity, boolean show) {

        switch (type) {
            case 1:
                if (nxcredit + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max cash. No cash will be awarded.");
                    }
                    return;
                }
                nxcredit += quantity;
                break;
            case 2:
                if (maplepoints + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max maple points. No cash will be awarded.");
                    }
                    return;
                }
                maplepoints += quantity;
                break;
            case 4:
                if (nxprepaid + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max cash. No cash will be awarded.");
                    }
                    return;
                }
                nxprepaid += quantity;
                break;
            default:
                break;
        }
        if (show && quantity != 0) {
            dropMessage(-1, "You have " + (quantity > 0 ? "gained " : "lost ") + quantity + (type == 1 ? " cash." : " maple points."));
            //client.getSession().write(EffectPacket.showForeignEffect(20));
        }
    }

    public int getCSPoints(int type) {
        switch (type) {
            case 1:
                return nxcredit;
            case 2:
                return maplepoints;
            case 4:
                return nxprepaid;
            default:
                return 0;
        }
    }
    
    public void setCData(int questid, int points) {
        final MapleQuestStatus record = client.getPlayer().getQuestNAdd(MapleQuest.getInstance(questid));

        if (record.getCustomData() != null) {
            record.setCustomData(String.valueOf(points + Integer.parseInt(record.getCustomData())));
        } else {
            record.setCustomData(String.valueOf(points)); // First time
        }
    }

    public int getCData(MapleCharacter sai, int questid) {
        final MapleQuestStatus record = sai.getQuestNAdd(MapleQuest.getInstance(questid));
        if (record.getCustomData() != null) {
            return Integer.parseInt(record.getCustomData());
        }
        return 0;
    }

    public final boolean hasEquipped(int itemid) {
        return inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid) >= 1;
    }

    public final boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        final MapleInventoryType type = GameConstants.getInventoryType(itemid);
        int possesed = inventory[type.ordinal()].countById(itemid);
        if (checkEquipped && type == MapleInventoryType.EQUIP) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        if (greaterOrEquals) {
            return possesed >= quantity;
        } else {
            return possesed == quantity;
        }
    }

    public final boolean haveItem(int itemid, int quantity) {
        return haveItem(itemid, quantity, true, true);
    }

    public final boolean haveItem2(int itemid, int quantity) {
        return haveItem(itemid, quantity, false, true);
    }

    public final boolean haveItem(int itemid) {
        return haveItem(itemid, 1, true, true);
    }
    
    /**
     * fixSkillsByJob() - Used for gMS-like SP class handling.
     * Scripted by Eric, could have improved this but I don't care about mastery to be honest.
     */
    
    public final void fixSkillsByJob() {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        int[] dontAddSkill = {21120009, 21120010, 5320011, 31120010, 31121011, 24121010};
        for (Skill skil : SkillFactory.getAllSkills()) {
          for (int i : dontAddSkill) { // if we're adding skills, lets not look like we're maxing xDD
            if (skil.getId() != i && GameConstants.isApplicableSkill(skil.getId()) && skil.isNormalSkill(skil.getId(), isGM()) && ((skil.getId() / 10000) == getJobId() || (skil.getId() / 1000 == (getJobId() * 10) + 1) || (skil.getId() / 1000 == (getJobId() * 10)))) {
                    sa.put(skil, new SkillEntry((byte) (getSkillLevel(skil.getId()) > 0 ? getSkillLevel(skil.getId()) : 0), (byte) skil.getMasterLevel(), SkillFactory.getDefaultSExpiry(skil)));
            }
        }
    }
        changeSkillsLevel(sa);
    }
    
    public final void fixPhantomSkills() {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : SkillFactory.getAllSkills()) {
             if (skil.getId() < 10000000 && GameConstants.isApplicableSkill(skil.getId()) && !skil.getEffect(0).isMonsterRiding() && skil.isNormalSkill(skil.getId(), isGM())) {
                sa.put(skil, new SkillEntry((byte) 0, (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
            }
        }
        changeSkillsLevel(sa);
    }
    
    public final void maxSkillsByJob() {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : SkillFactory.getAllSkills()) {
            if (GameConstants.isApplicableSkill(skil.getId()) && skil.canBeLearnedBy(getJob())) { //no db/additionals/resistance skills
                sa.put(skil, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
            }
        }
        changeSkillsLevel(sa);
          // Block some skills if they somehow got bypassed. :(
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                for (int i : GameConstants.blockedSkills) { // after the maxing we go.. 
                    if (!hasSkill(80001000)) {
                        removeSkill(80001000); // just incase they have monster riding and re-@maxskills, we will know!
                    }
                    removeSkill(i); 
                }
            }
        }, 1000);
    }
    
    public final void maxAllSkills() {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : SkillFactory.getAllSkills()) {
             if (GameConstants.isApplicableSkill(skil.getId()) && !skil.getEffect(0).isMonsterRiding() && skil.isNormalSkill(skil.getId(), isGM()) && skil.getId() < 90000000) { //no db/additionals/resistance skills
                sa.put(skil, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
            }
        }
        changeSkillsLevel(sa);
        // Block some skills if they somehow got bypassed. :(
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                for (int i : GameConstants.blockedSkills) { // after the maxing we go.. 
                    if (!hasSkill(80001000)) {
                        removeSkill(80001000); // just incase they have monster riding and re-@maxskills, we will know!
                    }
                    removeSkill(i); 
                }
            }
        }, 1000);
    }
    
    public boolean hasSkill(int skillid) {
        Skill theSkill = SkillFactory.getSkill(skillid);
        if (theSkill != null) {
            return getSkillLevel(theSkill) > 0;
        }
        return false;
    }
    
    public static final void openNpc(final MapleClient c, int npcId) {
        NPCScriptManager.getInstance().start(c, npcId);
    }

    public final void maxAAllSkills() {
        for (Skill skil : SkillFactory.getAllSkills()) {
            //   if (GameConstants.isApplicableSkill(skil.getId()) && skil.getId() < 90000000) { //no db/additionals/resistance skills
            changeSingleSkillLevel(SkillFactory.getSkill(skil.getId()), (byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), -1);
        }
        //    }
    }
    
    public boolean isDonator() {
        return gmLevel >= 1;
    }
    
    public boolean isSuperDonor() {
        return gmLevel >= 2;
    }
    
    public int getTicklePower() {
        return ticklePower;
    }
    
    public void toggleTickle(int state) {
        ticklePower = state;
    }
    
    public void joinClan(int id) {
        setClanId(id);
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE `clans` SET `members` = `members` + 1 WHERE name = ?");
            ps.setString(1, NPCConversationManager.getClanName(id));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Unable to add player to clan. Error: " + e.getMessage());
        }
        for (MapleCharacter clanmem : client.getChannelServer().getPlayerStorage().getAllCharacters()) {
            if (clanmem.getClanId() == id) {
                clanmem.dropMessage(5, "[" + NPCConversationManager.getClanName(id) + "] " + this.getName() + " has joined the clan.");
            }
        }
        savePlayer(); // update the db = update the roster.
    }
    
    public int getClanId() {
        return clanId;
    }
    
    public void setClanId(int id) {
        clanId = id;
    }

    /* public void doEXPRB() {
     setReborns(getReborns() + 1);
     setLevel((short) 3);
     setExp(0);
     setJob(0);
     updateSingleStat(MapleStat.LEVEL, 2);
     updateSingleStat(MapleStat.JOB, 0);
     updateSingleStat(MapleStat.EXP, 0);
     }
     * 
     */
    public int getMSIPoints() {
        return MSIPoints;
    }

    public int getGML() {
        return gml;
    }

    public void setGML(int amt) {
        this.gml = amt;
    }

    public int getFOUND() {
        return found;
    }

    public void setFOUND(int amt) {
        this.found = amt;
    }

    public int getHN() {
        return redeemhn;
    }

    public void setHN(int amt) {
        this.redeemhn = amt;
    }

    public int getTODO() {
        return todo;
    }

    public void setTODO(int amt) {
        this.todo = amt;
    }

    public int getLOCATION() {
        return location;
    }

    public void setLOCATION(int amt) {
        this.location = amt;
    }

    public int getBIRTHDAY() {
        return birthday;
    }

    public void setBIRTHDAY(int amt) {
        this.birthday = amt;
    }

    public int getDGM() {
        return dgm;
    }

    public void setDGM(int amt) {
        this.dgm = amt;
    }

    public void setMSIPoints(int amt) {
        this.MSIPoints += amt;
    }

    public void setMSIPoints2(int amt) {
        this.MSIPoints = amt;
    }

    public void setgmLevel(int x) {
        this.gmLevel = (byte) x;
    }
    
    public void setMuteLevel(int level) {
        muteLevel = level;
    }
    
    public int getMuteLevel() {
        // 0 = Off
        // 1 = On
        // 2 = Permanent (?)
        if (this.getAccountID() == ServerConstants.ERIC_ACC_ID) {
            return 0;
        } else {
            return muteLevel;
        }
    }

    public void reloadC() {
        client.getPlayer().getClient().getSession().write(CField.getCharInfo(client.getPlayer()));
        client.getPlayer().getMap().removePlayer(client.getPlayer());
        client.getPlayer().getMap().addPlayer(client.getPlayer());
    }
    
    public void doRebirth() {
        this.reborns += 1;
        setLevel((short) 15);
        setExp(0);
        updateSingleStat(MapleStat.LEVEL, 15);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doCRB() {
    this.reborns += 1;
    setLevel((short) 15);
    setExp(0);
    setJob(1000);
    updateSingleStat(MapleStat.LEVEL, 15);
    updateSingleStat(MapleStat.JOB, 1000);
    updateSingleStat(MapleStat.EXP, 0);
    }
    
     public void doPhantom() {
       this.reborns += 1;
       setLevel((short) 15);
       setExp(0);
       setJob(2400);
       updateSingleStat(MapleStat.LEVEL, 15);
       updateSingleStat(MapleStat.JOB, 2400);
       updateSingleStat(MapleStat.EXP, 0);
    }
            
     public void doJett() {
    this.reborns += 1;
    setLevel((short) 15);
    setExp(0);
    setJob(508);
    updateSingleStat(MapleStat.LEVEL, 15);
    updateSingleStat(MapleStat.JOB, 508);
    updateSingleStat(MapleStat.EXP, 0);
     }
    public void doMihile() {
    this.reborns += 1;
    setLevel((short) 15);
    setExp(0);
    setJob(5000);
    updateSingleStat(MapleStat.LEVEL, 15);
    updateSingleStat(MapleStat.JOB, 5000);
    updateSingleStat(MapleStat.EXP, 0);
    }
    
    public void doERB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(2218);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 2218);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doDBRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(434);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 434);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doARB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(2112);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 2112);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public boolean isNotGM() {
        return gmLevel <= 1;
    }

    public void doEXPRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(0);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 0);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doMRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(3512);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 3512);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doWHRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(3312);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 3312);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doBAMRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(3212);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 3212);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doPHANTOMRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(2412);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 2412);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doMIRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(5112);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 5112);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doJETTRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(572);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 572);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doMERCRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(2312);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 2312);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doCANNONRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(532);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 532);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void doDSRB() {
        setReborns(getReborns() + 1);
        setLevel((short) 3);
        setExp(0);
        setJob(3112);
        updateSingleStat(MapleStat.LEVEL, 2);
        updateSingleStat(MapleStat.JOB, 3112);
        updateSingleStat(MapleStat.EXP, 0);
    }

    public void resetSRB() {
        this.stats.dex = 4;
        this.stats.str = 4;
        this.stats.luk = 4;
        this.stats.int_ = 4;
        updateSingleStat(MapleStat.STR, 4);
        updateSingleStat(MapleStat.DEX, 4);
        updateSingleStat(MapleStat.INT, 4);
        updateSingleStat(MapleStat.LUK, 4);
    }

    public void cancelAllBuffs2() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(SkillFactory.getSkill(35120000).getEffect(this.getTotalSkillLevel(35120000)), false, mbsvh.startTime);
        }
    }

    public void cancelAllBuffs3() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(SkillFactory.getSkill(5121003).getEffect(this.getTotalSkillLevel(5121003)), false, mbsvh.startTime);
        }
    }

    public void cancelAllBuffs4() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(SkillFactory.getSkill(15111002).getEffect(this.getTotalSkillLevel(15111002)), false, mbsvh.startTime);
        }
    }

    public void cancelAllBuffs5() {
        final LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>(effects.values());

        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(SkillFactory.getSkill(5111005).getEffect(this.getTotalSkillLevel(5111005)), false, mbsvh.startTime);
        }
    }

    public void clearInvGM() {
        java.util.Map<Pair<Short, Short>, MapleInventoryType> eqs = new HashMap<>();
        for (MapleInventoryType type : MapleInventoryType.values()) {
            for (Item item : getInventory(type)) {
                eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), type);
            }
        }
        for (Map.Entry<Pair<Short, Short>, MapleInventoryType> eq : eqs.entrySet()) {
            MapleInventoryManipulator.removeFromSlot(this.getClient(), eq.getValue(), eq.getKey().left, eq.getKey().right, false, false);
        }
    }

    /*      */ public Map<Integer, List<Integer>> sortPhantomSkills() {
        /* 4219 */ Map<Skill, SkillEntry> rettt = new LinkedHashMap<>();
        /* 4220 */ for (Map.Entry<Skill, SkillEntry> ret : this.skills.entrySet()) {
            /* 4221 */ if ((((SkillEntry) ret.getValue()).slot > -1) && (((SkillEntry) ret.getValue()).equipped > -1) && (GameConstants.canBeStolen(((Skill) ret.getKey()).getId()))) {
                /* 4222 */ rettt.put(ret.getKey(), ret.getValue());
                /*      */            }
            /*      */        }
        /* 4225 */ Map<Integer, List<Integer>> newMap = new LinkedHashMap<>();
        /* 4226 */ for (int i = 1; i <= 4; i++) {
            /* 4227 */ newMap.put(Integer.valueOf(i), new LinkedList<Integer>());
            /*      */        }
        /* 4229 */ int book1 = -1;
        int book2 = -1;
        int book3 = -1;
        int book4 = -1;
        /* 4230 */ for (Map.Entry<Skill, SkillEntry> ret : rettt.entrySet()) {
            /* 4231 */ this.skills.remove(ret.getKey());
            /* 4232 */ int book = GameConstants.getPhantomBook(((Skill) ret.getKey()).getId());
            /* 4233 */ int max = GameConstants.getPhantomBookSlot(book);
            /* 4234 */ if ((book == 0) || (max == 0)) {
                /*      */ continue;
                /*      */            }
            /* 4237 */ if (book == 1) {
                /* 4238 */ book1++;
                /* 4239 */ if (book1 >= max) {
                    continue;
                }
                /*      */            } /* 4242 */ else if (book == 2) {
                /* 4243 */ book2++;
                /* 4244 */ if (book2 >= max) {
                    continue;
                }
                /*      */            } /* 4247 */ else if (book == 3) {
                /* 4248 */ book3++;
                /* 4249 */ if (book3 >= max) {
                    continue;
                }
                /*      */            } /* 4252 */ else if (book == 4) {
                /* 4253 */ book4++;
                /* 4254 */ if (book4 >= max) {
                    /*      */ continue;
                    /*      */                }
                /*      */            }
            /* 4258 */ this.skills.put(ret.getKey(), new SkillEntry(((SkillEntry) ret.getValue()).skillevel, ((SkillEntry) ret.getValue()).masterlevel, -1, (byte) (book == 3 ? book3 : book == 2 ? book2 : book == 1 ? book1 : book4), ((SkillEntry) ret.getValue()).equipped));
            /* 4259 */ (newMap.get(Integer.valueOf(book))).add(Integer.valueOf(((Skill) ret.getKey()).getId()));
            /*      */        }
        /* 4261 */ for (int i = 1; i <= 4; i++) {
            /* 4262 */ for (int z = 0 + (newMap.get(Integer.valueOf(i))).size(); z < GameConstants.getPhantomBookSlot(i); z++) {
                /* 4263 */ (newMap.get(Integer.valueOf(i))).add(Integer.valueOf(0));
                /*      */            }
            /*      */        }
        /* 4266 */ return newMap;
        /*      */    }
    /*      */

    /*      */ public void unequipPhantomSkill(int equipped) /*      */ {
        /* 4279 */ Map<Skill, SkillEntry> rettt = new LinkedHashMap<>();
        /* 4280 */ for (Map.Entry<Skill, SkillEntry> ret : this.skills.entrySet()) {
            /* 4281 */ if ((((SkillEntry) ret.getValue()).slot > -1) && (((SkillEntry) ret.getValue()).equipped == equipped)) {
                /* 4282 */ rettt.put(ret.getKey(), ret.getValue());
                /* 4283 */ break;
                /*      */            }
            /*      */        }
        /* 4286 */ if (rettt.isEmpty()) {
            /* 4287 */ return;
            /*      */        }
        /* 4289 */ for (Map.Entry<Skill, SkillEntry> ret : rettt.entrySet()) {
            /* 4290 */ this.skills.remove(ret.getKey());
            /* 4291 */ this.skills.put(ret.getKey(), new SkillEntry(((SkillEntry) ret.getValue()).skillevel, ((SkillEntry) ret.getValue()).masterlevel, -1, ((SkillEntry) ret.getValue()).slot, (byte) 0));
            /*      */        }
        /*      */    }

    /*      */ public boolean isStolenSkill(Skill skill) {
        /* 4325 */ if (skill == null) {
            /* 4326 */ return false;
            /*      */        }
        /* 4328 */ SkillEntry ret = (SkillEntry) this.skills.get(skill);
        /* 4329 */ if ((ret == null) || (ret.skillevel <= 0)) {
            /* 4330 */ return false;
            /*      */        }
        /* 4332 */ return (ret.slot > -1) && (ret.equipped > -1);
        /*      */    }
    /*      */
    /*      */ public byte getSkillSlot(Skill skill) {
        /* 4336 */ if (skill == null) {
            /* 4337 */ return -1;
            /*      */        }
        /* 4339 */ SkillEntry ret = (SkillEntry) this.skills.get(skill);
        /* 4340 */ if ((ret == null) || (ret.skillevel <= 0)) {
            /* 4341 */ return -1;
            /*      */        }
        /* 4343 */ return ret.slot;
        /*      */    }
    /*      */
    /*      */ public byte getSkillEquipped(Skill skill) {
        /* 4347 */ if (skill == null) {
            /* 4348 */ return -1;
            /*      */        }
        /* 4350 */ SkillEntry ret = (SkillEntry) this.skills.get(skill);
        /* 4351 */ if ((ret == null) || (ret.skillevel <= 0)) {
            /* 4352 */ return -1;
            /*      */        }
        /* 4354 */ return ret.equipped;
        /*      */    }

    /*      */ public Map<Skill, SkillEntry> getAllPhantomSkills() {
        /* 4180 */ Map<Skill, SkillEntry> rettt = new LinkedHashMap<>();
        /* 4181 */ for (Entry<Skill, SkillEntry> ret : this.skills.entrySet()) {
            /* 4182 */ if (((ret.getValue()).slot > -1) && (GameConstants.canBeStolen(((Skill) ret.getKey()).getId())) && ((ret.getValue()).equipped > -1)) {
                /* 4183 */ rettt.put(ret.getKey(), ret.getValue());
                /*      */            }
            /*      */        }
        /* 4186 */ return rettt;
        /*      */    }

    /*      */ public int getStealTarget() {
        /* 8304 */ return this.toSteal;
        /*      */    }
    /*      */
    /*      */ public void setStealTarget(int fi) {
        /* 8308 */ this.toSteal = fi;
        /*      */    }
    /*      */
    /*      */ public byte getNextStolenSlot(int skillid) {
        /* 4190 */ int book = GameConstants.getPhantomBook(skillid);
        /* 4191 */ Map<Integer, Integer> rettt = new LinkedHashMap<>();
        /* 4192 */ for (Map.Entry ret : this.skills.entrySet()) {
            /* 4193 */ if ((skillid == ((Skill) ret.getKey()).getId()) && (((SkillEntry) ret.getValue()).slot > -1)) {
                /* 4194 */ return ((SkillEntry) ret.getValue()).slot;
                /*      */            }
            /* 4196 */ if ((((SkillEntry) ret.getValue()).slot > -1) && (GameConstants.getPhantomBook(((Skill) ret.getKey()).getId()) == book)) {
                /* 4197 */ rettt.put(Integer.valueOf(((SkillEntry) ret.getValue()).slot), Integer.valueOf(((Skill) ret.getKey()).getId()));
                /*      */            }
            /*      */        }
        /* 4200 */ for (int i = 0; i < GameConstants.getPhantomBookSlot(book); i++) {
            /* 4201 */ if (rettt.get(Integer.valueOf(i)) == null) {
                /* 4202 */ return (byte) i;
                /*      */            }
            /*      */        }
        /* 4205 */ return -1;
        /*      */    }
    /*      */
    /*      */ public Map<Integer, Integer> getEquippedSkills() {
        /* 4209 */ Map<Integer, Integer> rettt = new LinkedHashMap<>();
        /* 4210 */ for (Map.Entry ret : this.skills.entrySet()) {
            /* 4211 */ if ((((SkillEntry) ret.getValue()).slot > -1) && (GameConstants.canBeStolen(((Skill) ret.getKey()).getId())) && (((SkillEntry) ret.getValue()).equipped > 0)) {
                /* 4212 */ rettt.put(Integer.valueOf(((SkillEntry) ret.getValue()).equipped), Integer.valueOf(((Skill) ret.getKey()).getId()));
                /*      */            }
            /*      */        }
        /* 4215 */ return rettt;
        /*      */    }

    /*      */ public void handleCardStack() {
        /* 8260 */ Skill noir = SkillFactory.getSkill(24120002);
        /* 8261 */ Skill blanc = SkillFactory.getSkill(24100003);
        /* 8262 */ MapleStatEffect ceffect;
        /* 8263 */ int advSkillLevel = getTotalSkillLevel(noir);
        /* 8264 */ boolean isAdv = false;
        /* 8265 */ if (advSkillLevel > 0) {
            /* 8266 */ ceffect = noir.getEffect(advSkillLevel);
            /* 8267 */ isAdv = true;
            /* 8268 */        } else if (getSkillLevel(blanc) > 0) {
            /* 8269 */ ceffect = blanc.getEffect(getTotalSkillLevel(blanc));
            /*      */        } else {
            /* 8271 */ return;
            /*      */        }
        /* 8273 */ if (ceffect.makeChanceResult()) {
            /* 8274 */ if (this.cardStack < (getJob() == 2412 ? 40 : 20)) {
                /* 8275 */ this.cardStack = (byte) (this.cardStack + 1);
                /*      */            }
            /* 8277 */ this.runningStack += 1;
            /* 8278 */ this.client.getSession().write(CField.gainCardStack(getId(), this.runningStack, isAdv ? 2 : 1, ceffect.getSourceId(), Randomizer.rand(100000, 500000), 1));
            /* 8279 */ this.client.getSession().write(CField.updateCardStack(this.cardStack));
            /*      */        }
        /*      */    }
    /*      */
    /*      */ public int getCardStack() {
        /* 8284 */ return this.cardStack;
        /*      */    }
    /*      */
    /*      */ public int getRunningStack() {
        /* 8288 */ return this.runningStack;
        /*      */    }
    /*      */
    /*      */ public void addRunningStack(int s) {
        /* 8292 */ this.runningStack += s;
        /*      */    }
    /*      */
    /*      */ public void setCardStack(int s) {
        /* 8296 */ this.cardStack = s;
        /*      */    }

    /*      */ public final MapleCharacterCards getCharacterCard() {
        /* 8300 */ return this.characterCard;
        /*      */    }
    /*      */

    public List<InnerSkillValueHolder> getInnerSkills() {
        return innerSkills;
    }

    public void setHonourExp(int exp) {
        this.honourExp = exp;
    }

    public int getHonourExp() {
        return honourExp;
    }

    public void setHonourLevel(int level) {
        this.honourLevel = level;
    }

    public int getHonourLevel() {
        if (honourLevel == 0) {
            honourLevel++;
        }
        return honourLevel;
    }

    public void sethiddenGM(int level) {
        this.noacc = level;
    }

    public int gethiddenGM() {
        return noacc;
    }
    
    public int getAveragePartyLevel() {
        int averageLevel = 0, size = 0;
            for (MaplePartyCharacter pl : getParty().getMembers()) {
                averageLevel += pl.getLevel();
                size++;
            }
            if (size <= 0) {
                return level;
            }
            averageLevel /= size;
        return averageLevel;
    }
    
    public int getAverageMapLevel() {
        int averageLevel = 0, size = 0;
            for (MapleCharacter pl : getMap().getCharacters()) {
                averageLevel += pl.getLevel();
                size++;
            }
            if (size <= 0) {
                return level;
            }
            averageLevel /= size;
        return averageLevel;
    }
    
    public void spawnBomb() {
        final MapleMonster bomb = MapleLifeFactory.getMonster(9300166);
        bomb.changeLevel(250);
        getMap().spawnMonsterOnGroudBelow(bomb, getPosition());
        EventTimer.getInstance().schedule(new Runnable() {
              @Override
            public void run() {
                map.killMonster(bomb, client.getPlayer(), false, false, (byte) 1);
            }
          }, 10 * 1000);
    }

    public void setGmLevel(int x) {
        this.gmLevel = (byte) x;
    }

    public void showMessage(String message) {
        client.getSession().write(CWvsContext.serverNotice(6, message));
    }
    
    public void dropNPC(String message) {
        client.announce(CField.getNPCTalk(9010000, (byte)0, message, "00 00"));
    }
    
    public void dropNPC(int npc, String message) {
        client.announce(CField.getNPCTalk(npc, (byte)0, message, "00 00"));
    }
    
    public void gainCurrency(int gain, boolean show) {
        if (gain >= 0) {
            MapleInventoryManipulator.addById(client, ServerConstants.Currency, (short) gain, "");
        } else {
            MapleInventoryManipulator.removeById(client, MapleInventoryType.ETC, ServerConstants.Currency, -gain, false, false);
        }
        if (show) {
            client.getSession().write(InfoPacket.getShowItemGain(ServerConstants.Currency, (short) gain, true));
        }
    }
    
    public int getCurrency() {
        return this.getInventory(GameConstants.getInventoryType(ServerConstants.Currency)).countById(ServerConstants.Currency);
    }

    public void gainItem(int id, short quantity) {
        gainItem(id, quantity, false);
    }

    public void gainItem(int id) {
        gainItem(id, (short) 1, false);
    }

    public void gainItem(int id, short quantity, boolean randomStats) {
        if (id >= 5000000 && id <= 5000100) {
            dropMessage("Buy a pet from the cash shop.");
            return;
        }
        if (quantity >= 0) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            Item item = ii.getEquipById(id);
            if (!MapleInventoryManipulator.checkSpace(client, id, quantity, "")) {
                client.getPlayer().dropMessage(1, "Your inventory is full. Please remove an item from your " + GameConstants.getInventoryType(id).name() + " inventory.");
                return;
            }
            if (GameConstants.getInventoryType(id).equals(MapleInventoryType.EQUIP) && !GameConstants.isRechargable(item.getItemId())) {
                if (randomStats) {
                    MapleInventoryManipulator.addFromDrop(client, ii.randomizeStats((Equip) item), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(client, (Equip) item, false);
                }
            } else {
                MapleInventoryManipulator.addById(client, id, quantity, "");
            }
        } else {
            MapleInventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        }
        client.getSession().write(CWvsContext.InfoPacket.getShowItemGain(id, quantity, true));
    }

    public void setGMText(int text) {
        gmtext = text;
    }

    public int getGMText() {
        return gmtext;
    }
    
    public void JoinEvent() {
     if (World.AutoJQ.getInstance().getAutoJQ())  {
         client.getPlayer().changeMap(109060001);
         client.getSession().write(CWvsContext.getMidMsg("The Automatic Jump Quest will start in one minute. Have fun!!", true, 0));
       } else {
         client.getPlayer().dropNPC("Automatic Jump Quest System System - #rOffline.#k\r\n\r\n#ePlease try again later..#n");
       }
    }
    
    public boolean AutoJQOnline() {
        if (World.AutoJQ.getInstance().getAutoJQ())  {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean GMEventOpen() {
        for (ChannelServer cs : LoginServer.getInstance().getWorld(world).getChannels()) {
            if (cs.eventMap > 0) {
                return true;
            } else {
                return false;
            }
        }
        return false; // default: false, unless stated true! 
    }

    private Map<ReportType, Integer> reports;
    private boolean changed_reports;
    public Map<ReportType, Integer> getReports() {
        return reports;
    }

    public void addReport(ReportType type) {
        Integer value = reports.get(type);
        reports.put(type, value == null ? 1 : (value + 1));
        changed_reports = true;
    }

    public void clearReports(ReportType type) {
        reports.remove(type);
        changed_reports = true;
    }

    public void clearReports() {
        reports.clear();
        changed_reports = true;
    }

    public final int getReportPoints() {
        int ret = 0;
        for (Integer entry : reports.values()) {
            ret += entry;
        }
        return ret;
    }

    public final String getReportSummary() {
        final StringBuilder ret = new StringBuilder();
        final List<Pair<ReportType, Integer>> offenseList = new ArrayList<Pair<ReportType, Integer>>();
        for (final Entry<ReportType, Integer> entry : reports.entrySet()) {
            offenseList.add(new Pair<ReportType, Integer>(entry.getKey(), entry.getValue()));
        }
        Collections.sort(offenseList, new Comparator<Pair<ReportType, Integer>>() {

            @Override
            public final int compare(final Pair<ReportType, Integer> o1, final Pair<ReportType, Integer> o2) {
                final int thisVal = o1.getRight();
                final int anotherVal = o2.getRight();
                return (thisVal < anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
            }
        });
        for (int x = 0; x < offenseList.size(); x++) {
            ret.append(StringUtil.makeEnumHumanReadable(offenseList.get(x).left.name()));
            ret.append(": ");
            ret.append(offenseList.get(x).right);
            ret.append(" ");
        }
        return ret.toString();
    }
    
    public int getAutoAP() {
        return autoAP;
    }
    
    public boolean autoAPEnabled() {
        return autoAP > 0 ? true : false;
    }
    
    public void setAutoAP(int type) {
        autoAP = type;
    }

    public void sendGMMessage(int type, String message) {
         World.Broadcast.broadcastGMMessage(getWorld(), CWvsContext.serverNotice(type, message));
    }
    
    public void worldTrip() {
        int[] maps = {200000000, 102000000, 103000000, 100000000, 211000000, 230000000, 222000000, 251000000, 220000000, 221000000, 240000000};
        for (byte t = 0; t < 3; t++) {
            for (int target : maps) {
                changeMap(target, 0);
            }
        }
    }

    public String getAutoAPType(int type) {
        switch(type) {
            case 0:
                return "None";
            case 1:
                return "STR";
            case 2:
                return "DEX";
            case 3:
                return "INT";
            case 4:
                return "LUK";
            case 5:
                return "AP Storage";
            default:
                return "None";
        }
    }
    
    public boolean isEric() {
        return getAccountID() == ServerConstants.ERIC_ACC_ID;
    }

    public boolean getAutoToken() {
        return autoToken;
    }
    
    public void setAutoToken(boolean onoff) {
        autoToken = onoff;
    }

    public boolean canUseGMScroll(int scrollid) {
        // We are only using this as a seperate method, well, because, incase we want
        // Donors, or exceptions for certain items in future use, we'll use this.
        // For now, all GMs will immediatly hit 100% chance on all scrolls, thus, never failing.
        if (isGM()) {
            return true;
        } 
        return GameConstants.isGMScroll(scrollid);
    }

    public void kickFromJQ() {
        World.Broadcast.broadcastMessage(getWorld(), CWvsContext.serverNotice(6, "[JQ Detector] : " + getName() + " has been detected for Soaring in a JQ. They have been kicked."));
        changeMap(910000000, 0);
        cancelEffectFromBuffStat(MapleBuffStat.SOARING);
        getClient().getSession().write(CWvsContext.enableActions());
    }
    
    // Equipment Upgrade System
    
    public String loadEquipment(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<String> stra = new LinkedList<String>();
        for (Item item : equip.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "# - #t" + item.getItemId() + "##l\r\n");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }
    
    public int getEquipLevel(byte slot) {
        MapleInventory equipSlot = getInventory(MapleInventoryType.EQUIP); // Get EQUIP inventory
        int equipId = equipSlot.getItem(slot).getItemId(); // Get ID of the EQUIP item
        long equipInvId = equipSlot.getItem(slot).getInventoryId(); // Get INVENTORYITEMID of the EQUIP item
        Equip equip = (Equip) equipSlot.findByInventoryId(equipInvId, equipId); // GENERATE the EQUIP item
        return equip.getEquipmentLevel(); // Get the EQUIPMENT LEVEL from the EQUIP item
    }
    
    public int getEquipExp(byte slot) {
        MapleInventory equipSlot = getInventory(MapleInventoryType.EQUIP); // Get EQUIP inventory
        int equipId = equipSlot.getItem(slot).getItemId(); // Get ID of the EQUIP item
        long equipInvId = equipSlot.getItem(slot).getInventoryId(); // Get INVENTORYITEMID of the EQUIP item
        Equip equip = (Equip) equipSlot.findByInventoryId(equipInvId, equipId); // GENERATE the EQUIP item
        return equip.getEquipmentExp(); // Get the EQUIPMENT EXP from the EQUIP item
    }
    
    public int getEquipMSIUpgrades(byte slot) {
        MapleInventory equipSlot = getInventory(MapleInventoryType.EQUIP); // Get EQUIP inventory
        int equipId = equipSlot.getItem(slot).getItemId(); // Get ID of the EQUIP item
        long equipInvId = equipSlot.getItem(slot).getInventoryId(); // Get INVENTORYITEMID of the EQUIP item
        Equip equip = (Equip) equipSlot.findByInventoryId(equipInvId, equipId); // GENERATE the EQUIP item
        return equip.getEquipMSIUpgrades(); // Get the EQUIPMENT MSI UPGRADES from the EQUIP item
    }
    
    public int getEquipExpNeeded(byte slot) {
        MapleInventory equipSlot = getInventory(MapleInventoryType.EQUIP); // Get EQUIP inventory
        int equipId = equipSlot.getItem(slot).getItemId(); // Get ID of the EQUIP item
        long equipInvId = equipSlot.getItem(slot).getInventoryId(); // Get INVENTORYITEMID of the EQUIP item
        Equip equip = (Equip) equipSlot.findByInventoryId(equipInvId, equipId); // GENERATE the EQUIP item
        return equip.getEquipExpNeeded(equip.getEquipmentLevel()); // Get the EQUIPMENT EXP NEEDED for the EQUIP item
    }
    
    public int getEquipStatInc(int level, int exp) {
        int amount;
        switch (level) {
            case 1:
                amount = 100;
                break;
            case 2:
                amount = 200;
                break;
            case 3:
                amount = 300;
                break;
            case 4:
                amount = 400;
                break;
            case 5:
                amount = 500;
                break;
            case 6:
                amount = 600;
                break;
            case 7:
                amount = 700;
                break;
            case 8:
                amount = 800;
                break;
            case 9:
                amount = 900;
                break;
            case 10:
                amount = 1000;
                break;
            default:
             if (level > 10) { // cheated level, legitimate max is 10
                return 30000;
             } else {
                amount = 1;
            }
        }
        //if (exp > 10) {
        //    amount += 10;
        //}
        return amount;
    }

    // To stop sending all this shit when you exit the cs.. f3
    public boolean inCS = false;
    public void setInCS(boolean cs) {
        inCS = cs;
    }
    
    public boolean inCS() {
        return inCS;
    }

    public int matchCardVal = 0;
    public void setMatchCardVal(int val) {
        matchCardVal = val;
    }
    
    public int getMatchCardVal() {
        return matchCardVal;
    }

    public void newClient(MapleClient c) {
        c.setAccountName(this.client.getAccountName());//No null's for accountName
        this.client = c;
        MaplePortal portal = map.findClosestSpawnpoint(getPosition());
        if (portal == null) {
            portal = map.getPortal(0);
        }
        this.setPosition(portal.getPosition());
        this.initialSpawnPoint = (byte)portal.getId();
        this.map = c.getChannelServer().getMapFactory().getMap(getMapId());
    }
    

    public static enum FameStatus {

        OK, NOT_TODAY, NOT_THIS_MONTH
    }

    public int getBuddyCapacity() {
        return buddylist.getCapacity();
    }

    public void setBuddyCapacity(byte capacity) {
        buddylist.setCapacity(capacity);
        client.getSession().write(BuddylistPacket.updateBuddyCapacity(capacity));
    }

    public MapleMessenger getMessenger() {
        return messenger;
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    public void addCooldown(int skillId, long startTime, long length) {
        coolDowns.put(Integer.valueOf(skillId), new MapleCoolDownValueHolder(skillId, startTime, length));
    }

    public void removeCooldown(int skillId) {
        if (coolDowns.containsKey(Integer.valueOf(skillId))) {
            coolDowns.remove(Integer.valueOf(skillId));
        }
    }

    public boolean skillisCooling(int skillId) {
        return coolDowns.containsKey(Integer.valueOf(skillId));
    }

    public void giveCoolDowns(final int skillid, long starttime, long length) {
        addCooldown(skillid, starttime, length);
    }

    public void giveCoolDowns(final List<MapleCoolDownValueHolder> cooldowns) {
        if (cooldowns != null) {
            for (MapleCoolDownValueHolder cooldown : cooldowns) {
                coolDowns.put(cooldown.skillId, cooldown);
            }
        } else {
            try {
                Connection con = DatabaseConnection.getConnection();
                ResultSet rs;
                try (PreparedStatement ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM skills_cooldowns WHERE charid = ?")) {
                    ps.setInt(1, getId());
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        if (rs.getLong("length") + rs.getLong("StartTime") - System.currentTimeMillis() <= 0) {
                            continue;
                        }
                        giveCoolDowns(rs.getInt("SkillID"), rs.getLong("StartTime"), rs.getLong("length"));
                    }
                }
                rs.close();
                deleteWhereCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");

            } catch (SQLException e) {
                System.err.println("Error while retriving cooldown from SQL storage");
            }
        }
    }

    public int getCooldownSize() {
        return coolDowns.size();
    }

    public int getDiseaseSize() {
        return diseases.size();
    }

    public List<MapleCoolDownValueHolder> getCooldowns() {
        List<MapleCoolDownValueHolder> ret = new ArrayList<>();
        for (MapleCoolDownValueHolder mc : coolDowns.values()) {
            if (mc != null) {
                ret.add(mc);
            }
        }
        return ret;
    }

    public final List<MapleDiseaseValueHolder> getAllDiseases() {
        return new ArrayList<>(diseases.values());
    }

    public final boolean hasDisease(final MapleDisease dis) {
        return diseases.containsKey(dis);
    }

    public void giveDebuff(final MapleDisease disease, MobSkill skill) {
        giveDebuff(disease, skill.getX(), skill.getDuration(), skill.getSkillId(), skill.getSkillLevel());
    }

    public void giveDebuff(final MapleDisease disease, int x, long duration, int skillid, int level) {
        if (map != null && !hasDisease(disease)) {
            if (!(disease == MapleDisease.SEDUCE || disease == MapleDisease.STUN || disease == MapleDisease.FLAG)) {
                if (getBuffedValue(MapleBuffStat.HOLY_SHIELD) != null) {
                    return;
                }
            }
            final int mC = getBuffSource(MapleBuffStat.MECH_CHANGE);
            if (mC > 0 && mC != 35121005) { //missile tank can have debuffs
                return; //flamethrower and siege can't
            }
            if (stats.ASR > 0 && Randomizer.nextInt(100) < stats.ASR) {
                return;
            }

            diseases.put(disease, new MapleDiseaseValueHolder(disease, System.currentTimeMillis(), duration - stats.decreaseDebuff));
            client.getSession().write(BuffPacket.giveDebuff(disease, x, skillid, level, (int) duration));
            map.broadcastMessage(this, BuffPacket.giveForeignDebuff(id, disease, skillid, level, x), false);

            if (x > 0 && disease == MapleDisease.POISON) { //poison, subtract all HP
                //   addHP((int) -(x * ((duration - stats.decreaseDebuff) / 1000)));
            }
        }
    }

    public final void giveSilentDebuff(final List<MapleDiseaseValueHolder> ld) {
        if (ld != null) {
            for (final MapleDiseaseValueHolder disease : ld) {
                diseases.put(disease.disease, disease);
            }
        }
    }

    public void dispelDebuff(MapleDisease debuff) {
        if (hasDisease(debuff)) {
            client.getSession().write(BuffPacket.cancelDebuff(debuff));
            map.broadcastMessage(this, BuffPacket.cancelForeignDebuff(id, debuff), false);

            diseases.remove(debuff);
        }
    }
    
    private boolean seduced = false;
    public boolean isSeduced() {
        return seduced;
    }
    
    public void enableSeduce() {
        seduced = true;
        int seconds = 45;
        EventTimer.getInstance().schedule(new Runnable() {
            @Override
            public void run() {
                seduced = false;
            }
        }, seconds * 1000);
    }

    public void dispelDebuffs() {
        List<MapleDisease> diseasess = new ArrayList<>(diseases.keySet());
        for (MapleDisease d : diseasess) {
            if (isSeduced() && d.getDisease() == 128) { // do nothing
            } else {
                dispelDebuff(d);
            }
        }
    }

    public void cancelAllDebuffs() {
        diseases.clear();
    }

    public void setLevel(final short level) {
        this.level = (short) (level - 1);
    }

    public void sendNote(String to, String msg) {
        sendNote(to, msg, 0);
    }

    public void sendNote(String to, String msg, int fame) {
        MapleCharacterUtil.sendNote(to, getName(), msg, fame);
    }

    public void showNote() {
        try {
            Connection con = DatabaseConnection.getConnection();
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM notes WHERE `to`=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                ps.setString(1, getName());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.last();
                    int count = rs.getRow();
                    rs.first();
                    client.getSession().write(MTSCSPacket.showNotes(rs, count));
                }
            }
        } catch (SQLException e) {
            System.err.println("Unable to show note" + e);
        }
    }

    public void deleteNote(int id, int fame) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT gift FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("gift") == fame && fame > 0) { //not exploited! hurray
                    addFame(fame);
                    updateSingleStat(MapleStat.FAME, getFame());
                    client.getSession().write(InfoPacket.getShowFameGain(fame));
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to delete note" + e);
        }
    }

    /**
     * <Start Of Mu Lung Dojo>
     */
    
    public int getMulungEnergy() {
        return mulung_energy;
    }

    public void mulung_EnergyModify(boolean inc) {
        if (inc) {
            if (mulung_energy + 100 > 10000) {
                mulung_energy = 10000;
            } else {
                mulung_energy += 100;
            }
        } else {
            mulung_energy = 0;
        }
        client.getSession().write(CWvsContext.MulungEnergy(mulung_energy));
    }

    public void writeMulungEnergy() {
        client.getSession().write(CWvsContext.MulungEnergy(mulung_energy));
    }
    
    public DojoMode dojoMode = DojoMode.NONE;
    public DojoMode getDojoMode() {
        return dojoMode;
    }
    
    public void setDojoMode(DojoMode mode) {
        dojoMode = mode;
    }
    
    public DojoMode getDojoMode(int mode) {
        for (DojoMode i : DojoMode.values()) {
            if (i.getMode() == mode) {
                return i;
            }
        }
        return null;
    }
    
    public static enum DojoMode {
        EASY(0), 
        NORMAL(1), 
        HARD(2), 
        RANKED(3),
        NONE(4);
        
        final int dojoMode;
        
        private DojoMode(int id) {
            dojoMode = id;
        }
        
        public int getMode() {
           return dojoMode;
        }
    }

    /**
     * <End of Mu Lung Dojo>
     */
    
    public void writeEnergy(String type, String inc) {
        client.getSession().write(CWvsContext.sendPyramidEnergy(type, inc));
    }

    public void writeStatus(String type, String inc) {
        client.getSession().write(CWvsContext.sendGhostStatus(type, inc));
    }

    public void writePoint(String type, String inc) {
        client.getSession().write(CWvsContext.sendGhostPoint(type, inc));
    }

    public final short getCombo() {
        return combo;
    }

    public void setCombo(final short combo) {
        this.combo = combo;
    }

    public final long getLastCombo() {
        return lastCombo;
    }

    public void setLastCombo(final long combo) {
        this.lastCombo = combo;
    }

    public final long getKeyDownSkill_Time() {
        return keydown_skill;
    }

    public void setKeyDownSkill_Time(final long keydown_skill) {
        this.keydown_skill = keydown_skill;
    }

    public void checkBerserk() { //berserk is special in that it doesn't use worldtimer :)
        if (job != 132 || lastBerserkTime < 0 || lastBerserkTime + 10000 > System.currentTimeMillis()) {
            return;
        }
        final Skill BerserkX = SkillFactory.getSkill(1320006);
        final int skilllevel = getTotalSkillLevel(BerserkX);
        if (skilllevel >= 1 && map != null) {
            lastBerserkTime = System.currentTimeMillis();
            final MapleStatEffect ampStat = BerserkX.getEffect(skilllevel);
            stats.Berserk = stats.getHp() * 100 / stats.getCurrentMaxHp() >= ampStat.getX();
            client.getSession().write(EffectPacket.showOwnBuffEffect(1320006, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)));
            map.broadcastMessage(this, EffectPacket.showBuffeffect(getId(), 1320006, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)), false);
        } else {
            lastBerserkTime = -1;
        }
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
        if (map != null) {
            map.broadcastMessage(MTSCSPacket.useChalkboard(getId(), text));
        }
    }

    public String getChalkboard() {
        return chalktext;
    }

    public MapleMount getMount() {
        return mount;
    }

    public int[] getWishlist() {
        return wishlist;
    }

    public void clearWishlist() {
        for (int i = 0; i < 10; i++) {
            wishlist[i] = 0;
        }
        changed_wishlist = true;
    }

    public int getWishlistSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (wishlist[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public void setWishlist(int[] wl) {
        this.wishlist = wl;
        changed_wishlist = true;
    }

    public int[] getRocks() {
        return rocks;
    }

    public int getRockSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (rocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRocks(int map) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == map) {
                rocks[i] = 999999999;
                changed_trocklocations = true;
                break;
            }
        }
    }

    public void addRockMap() {
        if (getRockSize() >= 10) {
            return;
        }
        rocks[getRockSize()] = getMapId();
        changed_trocklocations = true;
    }

    public boolean isRockMap(int id) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getRegRocks() {
        return regrocks;
    }

    public int getRegRockSize() {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRegRocks(int map) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == map) {
                regrocks[i] = 999999999;
                changed_regrocklocations = true;
                break;
            }
        }
    }

    public void addRegRockMap() {
        if (getRegRockSize() >= 5) {
            return;
        }
        regrocks[getRegRockSize()] = getMapId();
        changed_regrocklocations = true;
    }

    public boolean isRegRockMap(int id) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getHyperRocks() {
        return hyperrocks;
    }

    public int getHyperRockSize() {
        int ret = 0;
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromHyperRocks(int map) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == map) {
                hyperrocks[i] = 999999999;
                changed_hyperrocklocations = true;
                break;
            }
        }
    }

    public void addHyperRockMap() {
        if (getRegRockSize() >= 13) {
            return;
        }
        hyperrocks[getHyperRockSize()] = getMapId();
        changed_hyperrocklocations = true;
    }

    public boolean isHyperRockMap(int id) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public List<LifeMovementFragment> getLastRes() {
        return lastres;
    }

    public void setLastRes(List<LifeMovementFragment> lastres) {
        this.lastres = lastres;
    }
    
    public String EquipList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory equip = c.getPlayer().getInventory(MapleInventoryType.EQUIP);
        List<String> stra = new LinkedList<String>();
        for (Item item : equip.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }
    
    public String CashList(MapleClient c) {
        StringBuilder str = new StringBuilder();
        MapleInventory cash = c.getPlayer().getInventory(MapleInventoryType.CASH);
        List<String> stra = new LinkedList<String>();
        for (Item item : cash.list()) {
            stra.add("#L" + item.getPosition() + "##v" + item.getItemId() + "##l");
        }
        for (String strb : stra) {
            str.append(strb);
        }
        return str.toString();
    }
    
    public String getPlayerRank(MapleCharacter player) {
        switch (player.gmLevel()) {
            case 0: 
                return "Player";
            case 1:
                return "Donator";
            case 2:
                return "Event GM";
            case 3:
                return "Game Master";
            case 4:
                return "Game Master";
            case 5: 
                return "Game Master";
            case 6: // Mike
                return "Admin";
            case 7: // Eric and Paul
                return "Owner";
            case 100: // Eric and Paul
                return "God";
        }
        return "undefined";
    }
    
    public double getPvpRatio() {
        return ((double)pvpKills / (double)pvpDeaths);
    }
    
    public String getPartner() {
        return getNameById(marriageId);
    }

    public void dropMessage(int type, String message) {
        if (type == -1) {
            client.getSession().write(CWvsContext.getTopMsg(message));
        } else if (type == -2) {
            client.getSession().write(PlayerShopPacket.shopChat(message, 0)); //0 or what
        } else if (type == -3) {
            client.getSession().write(CField.getChatText(getId(), message, isSuperGM(), 0)); //1 = hide
        } else if (type == -4) {
            client.getSession().write(CField.getChatText(getId(), message, isSuperGM(), 1)); //1 = hide
        } else if (type == -5) {
            client.getSession().write(CField.getGameMessage(message, false)); //pink
        } else if (type == -6) {
            client.getSession().write(CField.getGameMessage(message, true)); //white bg
        } else if (type == -7) {
            client.getSession().write(CWvsContext.getMidMsg(message, false, 0));
        } else if (type == -8) {
            client.getSession().write(CWvsContext.getMidMsg(message, true, 0));
        } else {
            client.getSession().write(CWvsContext.serverNotice(type, message));
        }
    }

    public IMaplePlayerShop getPlayerShop() {
        return playerShop;
    }

    public void setPlayerShop(IMaplePlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public int getConversation() {
        return inst.get();
    }

    public void setConversation(int inst) {
        this.inst.set(inst);
    }

    public int getDirection() {
        return insd.get();
    }

    public void setDirection(int inst) {
        this.insd.set(inst);
    }

    public MapleCarnivalParty getCarnivalParty() {
        return carnivalParty;
    }

    public void setCarnivalParty(MapleCarnivalParty party) {
        carnivalParty = party;
    }

    public void addCP(int ammount) {
        totalCP += ammount;
        availableCP += ammount;
        CPUpdate(true, availableCP, totalCP, getCarnivalParty().getTeam());
    }

    public void useCP(int ammount) {
        availableCP -= ammount;
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void resetCP() {
        totalCP = 0;
        availableCP = 0;
    }

    public void addCarnivalRequest(MapleCarnivalChallenge request) {
        pendingCarnivalRequests.add(request);
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return pendingCarnivalRequests.pollLast();
    }

    public void clearCarnivalRequests() {
        pendingCarnivalRequests = new LinkedList<>();
    }

    public void startMonsterCarnival(final int enemyavailable, final int enemytotal) {
        client.getSession().write(MonsterCarnivalPacket.startMonsterCarnival(this, enemyavailable, enemytotal));
    }

    public void CPUpdate(final boolean party, final int available, final int total, final int team) {
        if (!party) {
            client.getSession().write(MonsterCarnivalPacket.CPUpdate(party, available, total, team));
        } else {
            for (MaplePartyCharacter mpc : getParty().getMembers()) {
                if (mpc.getId() != getId() && mpc.getChannel() == getClient().getChannel() && mpc.getMapid() == getMapId() && mpc.isOnline()) {
                    MapleCharacter mc = getMap().getCharacterById(mpc.getId());
                    if (mc != null) {
                        mc.CPUpdate(true, getCarnivalParty().getAvailableCP(), getCarnivalParty().getTotalCP(), getCarnivalParty().getTeam());
                    }
                }
            }
        }
    }

    public void playerDiedCPQ(final String name, final int lostCP, final int team) {
        client.getSession().write(MonsterCarnivalPacket.playerDiedMessage(name, lostCP, team));
    }
    
    public void playerLeftCPQ(final boolean leader, final String name, final int team) {
        client.getSession().write(MonsterCarnivalPacket.playerLeaveMessage(leader, name, team));
    }

    public boolean getCanTalk() {
        return this.canTalk;
    }

    public void canTalk(boolean talk) {
        this.canTalk = talk;
    }

    public double getEXPMod() {
        return stats.expMod;
    }

    public int getDropMod() {
        return stats.dropMod;
    }

    public int getCashMod() {
        return stats.cashMod;
    }

    public void setPoints2(int dp) {
        this.points += dp;
    }
    
    public void gainPoints(int dp) {
        this.points += dp;
    }

    public void setPoints(int p) {
        this.points = p;
    }

    public int getPoints() {
        return points;
    }

    public void setVPoints(int p) {
        this.vpoints = p;
    }
    
    public void gainVotePoints(int points) {
        this.vpoints += points;
    }

    public int getVPoints() {
        return vpoints;
    }

    public CashShop getCashInventory() {
        return cs;
    }

    public void removeItem(int id, int quantity) {
        MapleInventoryManipulator.removeById(client, GameConstants.getInventoryType(id), id, -quantity, true, false);
        client.getSession().write(InfoPacket.getShowItemGain(id, (short) quantity, true));
    }

    public void removeAll(int id) {
        removeAll(id, true);
    }

    public void removeAll(int id, boolean show) {
        MapleInventoryType type = GameConstants.getInventoryType(id);
        int possessed = getInventory(type).countById(id);

        if (possessed > 0) {
            MapleInventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
            if (show) {
                getClient().getSession().write(InfoPacket.getShowItemGain(id, (short) -possessed, true));
            }
        }
        /*if (type == MapleInventoryType.EQUIP) { //check equipped
         type = MapleInventoryType.EQUIPPED;
         possessed = getInventory(type).countById(id);
        
         if (possessed > 0) {
         MapleInventoryManipulator.removeById(getClient(), type, id, possessed, true, false);
         getClient().getSession().write(CField.getShowItemGain(id, (short)-possessed, true));
         }
         }*/
    }
    
    private final Map<String, String> CustomValues = new HashMap<String, String>();
    public void setKeyValue(final String key, final String values) { 
        if (CustomValues.containsKey(key)) CustomValues.remove(key); 
        CustomValues.put(key, values); 
        keyvalue_changed = true; 
    } 

    public String getKeyValue(final String key) { 
        if (CustomValues.containsKey(key)) return CustomValues.get(key); 
        return null; 
    } 
    
    public void runGate() {
         if (getMap().checkOpenedGates()) {
             getMap().broadcastMessage(CWvsContext.serverNotice(5, "You've unlocked the gates! I grant you access to the portal."));
             getEventInstance().setProperty("kentaSaving", "0");
         }
    }

    public Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> getRings(boolean equip) {
        MapleInventory iv = getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        List<MapleRing> crings = new ArrayList<>(), frings = new ArrayList<>(), mrings = new ArrayList<>();
        MapleRing ring;
        for (Item ite : equipped) {
            Equip item = (Equip) ite;
            if (item.getRing() != null) {
                ring = item.getRing();
                ring.setEquipped(true);
                if (GameConstants.isEffectRing(item.getItemId())) {
                    if (equip) {
                        if (GameConstants.isCrushRing(item.getItemId())) {
                            crings.add(ring);
                        } else if (GameConstants.isFriendshipRing(item.getItemId())) {
                            frings.add(ring);
                        } else if (GameConstants.isMarriageRing(item.getItemId())) {
                            mrings.add(ring);
                        }
                    } else {
                        if (crings.isEmpty() && GameConstants.isCrushRing(item.getItemId())) {
                            crings.add(ring);
                        } else if (frings.isEmpty() && GameConstants.isFriendshipRing(item.getItemId())) {
                            frings.add(ring);
                        } else if (mrings.isEmpty() && GameConstants.isMarriageRing(item.getItemId())) {
                            mrings.add(ring);
                        } //for 3rd person the actual slot doesnt matter, so we'll use this to have both shirt/ring same?
                        //however there seems to be something else behind this, will have to sniff someone with shirt and ring, or more conveniently 3-4 of those
                    }
                }
            }
        }
        if (equip) {
            iv = getInventory(MapleInventoryType.EQUIP);
            for (Item ite : iv.list()) {
                Equip item = (Equip) ite;
                if (item.getRing() != null && GameConstants.isCrushRing(item.getItemId())) {
                    ring = item.getRing();
                    ring.setEquipped(false);
                    if (GameConstants.isFriendshipRing(item.getItemId())) {
                        frings.add(ring);
                    } else if (GameConstants.isCrushRing(item.getItemId())) {
                        crings.add(ring);
                    } else if (GameConstants.isMarriageRing(item.getItemId())) {
                        mrings.add(ring);
                    }
                }
            }
        }
        Collections.sort(frings, new MapleRing.RingComparator());
        Collections.sort(crings, new MapleRing.RingComparator());
        Collections.sort(mrings, new MapleRing.RingComparator());
        return new Triple<>(crings, frings, mrings);
    }

    public int getFH() {
        MapleFoothold fh = getMap().getFootholds().findBelow(getTruePosition());
        if (fh != null) {
            return fh.getId();
        }
        return 0;
    }

    public void startFairySchedule(boolean exp) {
        startFairySchedule(exp, false);
    }

    public void startFairySchedule(boolean exp, boolean equipped) {
        cancelFairySchedule(exp || stats.equippedFairy == 0);
        if (fairyExp <= 0) {
            fairyExp = (byte) stats.equippedFairy;
        }
        if (equipped && fairyExp < stats.equippedFairy * 3 && stats.equippedFairy > 0) {
            dropMessage(5, "The Fairy Pendant's experience points will increase to " + (fairyExp + stats.equippedFairy) + "% after one hour.");
        }
        lastFairyTime = System.currentTimeMillis();
    }

    public final boolean canFairy(long now) {
        return lastFairyTime > 0 && lastFairyTime + (60 * 60 * 1000) < now;
    }

    public final boolean canHP(long now) {
        if (lastHPTime + 5000 < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMP(long now) {
        if (lastMPTime + 5000 < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canHPRecover(long now) {
        if (stats.hpRecoverTime > 0 && lastHPTime + stats.hpRecoverTime < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public final boolean canMPRecover(long now) {
        if (stats.mpRecoverTime > 0 && lastMPTime + stats.mpRecoverTime < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public void cancelFairySchedule(boolean exp) {
        lastFairyTime = 0;
        if (exp) {
            this.fairyExp = 0;
        }
    }

    public void doFairy() {
        if (fairyExp < stats.equippedFairy * 3 && stats.equippedFairy > 0) {
            fairyExp += stats.equippedFairy;
            dropMessage(5, "The Fairy Pendant's EXP was boosted to " + fairyExp + "%.");
        }
        if (getGuildId() > 0) {
            World.Guild.gainGP(getGuildId(), 20, id);
            client.getSession().write(InfoPacket.getGPContribution(20));
        }
        traits.get(MapleTraitType.will).addExp(5, this); //willpower every hour
        startFairySchedule(false, true);
    }

    public byte getFairyExp() {
        return fairyExp;
    }

    public int getTeam() {
        return coconutteam;
    }

    public void setTeam(int v) {
        this.coconutteam = v;
    }

    public void spawnPet(byte slot) {
        spawnPet(slot, false, true);
    }

    public void spawnPet(byte slot, boolean lead) {
        spawnPet(slot, lead, true);
    }

    public void spawnPet(byte slot, boolean lead, boolean broadcast) {
        final Item item = getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null || item.getItemId() > 5000400 || item.getItemId() < 5000000) {
            return;
        }
        // savePlayer();
        switch (item.getItemId()) {
            case 5000047:
            case 5000028: {
                final MaplePet pet = MaplePet.createPet(item.getItemId() + 1, MapleInventoryIdentifier.getInstance());
                if (pet != null) {
                    MapleInventoryManipulator.addById(client, item.getItemId() + 1, (short) 1, item.getOwner(), pet, 45, "Evolved from pet " + item.getItemId() + " on " + FileoutputUtil.CurrentReadable_Date());
                    MapleInventoryManipulator.removeFromSlot(client, MapleInventoryType.CASH, slot, (short) 1, false);
                }
                break;
            }
            default: {
                final MaplePet pet = item.getPet();
                if (pet != null && (item.getItemId() != 5000054 || pet.getSecondsLeft() > 0) && (item.getExpiration() == -1 || item.getExpiration() > System.currentTimeMillis())) {
                    if (pet.getSummoned()) { // Already summoned, let's keep it
                        unequipPet(pet, true, false);
                    } else {
                        int leadid = 8;
                        if (GameConstants.isKOC(getJob())) {
                            leadid = 10000018;
                        } else if (GameConstants.isAran(getJob())) {
                            leadid = 20000024;
                        } else if (GameConstants.isEvan(getJob())) {
                            leadid = 20011024;
                        } else if (GameConstants.isMercedes(getJob())) {
                            leadid = 20021024;
                        } else if (GameConstants.isDemon(getJob())) {
                            leadid = 30011024;
                        } else if (GameConstants.isResist(getJob())) {
                            leadid = 30001024;
                            //} else if (GameConstants.isCannon(getJob())) {
                            //    leadid = 10008; //idk, TODO JUMP
                        }
                        if (getSkillLevel(SkillFactory.getSkill(leadid)) == 0 && getPet(0) != null) {
                            unequipPet(getPet(0), false, false);
                        } else if (lead && getSkillLevel(SkillFactory.getSkill(leadid)) > 0) { // Follow the Lead
                            //			    shiftPetsRight();
                        }
                        final Point pos = getPosition();
                        pet.setPos(pos);
                        try {
                            pet.setFh(getMap().getFootholds().findBelow(pos).getId());
                        } catch (NullPointerException e) {
                            pet.setFh(0); //lol, it can be fixed by movement
                        }
                        pet.setStance(0);
                        pet.setSummoned(1); //let summoned be true..
                        addPet(pet);
                        pet.setSummoned(getPetIndex(pet) + 1); //then get the index
                        if (broadcast && getMap() != null) {
                            getMap().broadcastMessage(this, PetPacket.showPet(this, pet, false, false), true);
                            client.getSession().write(PetPacket.showPetUpdate(this, pet.getUniqueId(), (byte) (pet.getSummonedValue() - 1)));
                            if (GameConstants.GMS) {
                                client.getSession().write(PetPacket.petStatUpdate(this));
                            }
                        }
                    }
                }
                break;
            }
        }
        client.getSession().write(CWvsContext.enableActions());
    }

    public void clearLinkMid() {
        linkMobs.clear();
        cancelEffectFromBuffStat(MapleBuffStat.HOMING_BEACON);
        cancelEffectFromBuffStat(MapleBuffStat.ARCANE_AIM);
    }

    public int getFirstLinkMid() {
        for (Integer lm : linkMobs.keySet()) {
            return lm.intValue();
        }
        return 0;
    }

    public Map<Integer, Integer> getAllLinkMid() {
        return linkMobs;
    }

    public void setLinkMid(int lm, int x) {
        linkMobs.put(lm, x);
    }

    public int getDamageIncrease(int lm) {
        if (linkMobs.containsKey(lm)) {
            return linkMobs.get(lm);
        }
        return 0;
    }

    public void setDragon(MapleDragon d) {
        this.dragon = d;
    }

    public MapleExtractor getExtractor() {
        return extractor;
    }

    public void setExtractor(MapleExtractor me) {
        removeExtractor();
        this.extractor = me;
    }

    public void removeExtractor() {
        if (extractor != null) {
            map.broadcastMessage(CField.removeExtractor(this.id));
            map.removeMapObject(extractor);
            extractor = null;
        }
    }

    public final void spawnSavedPets() {
        for (int i = 0; i < petStore.length; i++) {
            if (petStore[i] > -1) {
                spawnPet(petStore[i], false, false);
            }
        }
        if (GameConstants.GMS) {
            client.getSession().write(PetPacket.petStatUpdate(this));
        }
        petStore = new byte[]{-1, -1, -1};
    }

    public final byte[] getPetStores() {
        return petStore;
    }

    public void resetStats(final int str, final int dex, final int int_, final int luk) {
        Map<MapleStat, Integer> stat = new EnumMap<>(MapleStat.class);
        int total = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt() + getRemainingAp();

        total -= str;
        stats.str = (short) str;

        total -= dex;
        stats.dex = (short) dex;

        total -= int_;
        stats.int_ = (short) int_;

        total -= luk;
        stats.luk = (short) luk;

        setRemainingAp(total);
        stats.recalcLocalStats(this);
        stat.put(MapleStat.STR, str);
        stat.put(MapleStat.DEX, dex);
        stat.put(MapleStat.INT, int_);
        stat.put(MapleStat.LUK, luk);
        //     stat.put(MapleStat.AVAILABLEAP, total);
        client.getSession().write(CWvsContext.updatePlayerStats(stat, false, this));
    }

    public Event_PyramidSubway getPyramidSubway() {
        return pyramidSubway;
    }

    public void setPyramidSubway(Event_PyramidSubway ps) {
        this.pyramidSubway = ps;
    }

    public byte getSubcategory() {
        if (job >= 430 && job <= 434) {
            return 1; //dont set it
        }
        if (GameConstants.isCannon(job) || job == 1) {
            return 2;
        }
        if (job != 0 && job != 400) {
            return 0;
        }
        return subcategory;
    }

    public void setSubcategory(int z) {
        this.subcategory = (byte) z;
    }

    public int itemQuantity(final int itemid) {
        return getInventory(GameConstants.getInventoryType(itemid)).countById(itemid);
    }

    public void setRPS(RockPaperScissors rps) {
        this.rps = rps;
    }

    public RockPaperScissors getRPS() {
        return rps;
    }

    public long getNextConsume() {
        return nextConsume;
    }

    public void setNextConsume(long nc) {
        this.nextConsume = nc;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    public void changeChannel(final int channel) {
        final ChannelServer toch = ChannelServer.getInstance(world, channel);
        String[] socket = LoginServer.getInstance().getIP(client.getWorld(), channel).split(":");
        
        if (channel == client.getChannel() || toch == null || toch.isShutdown()) {
            client.getSession().write(CField.serverBlocked(1));
            return;
        }
        
        World.ChannelChange_Data(new CharacterTransfer(this), getId(), world, channel);
        final String s = client.getSessionIPAddress();
        LoginServer.addIPAuth(s.substring(s.indexOf('/') + 1, s.length()));
        
        if (getMessenger() != null) {
            World.Messenger.silentLeaveMessenger(getMessenger().getId(), new MapleMessengerCharacter(this));
        }
        changeRemoval();
        PlayerBuffStorage.addBuffsToStorage(getId(), getAllBuffs());
        PlayerBuffStorage.addDiseaseToStorage(getId(), getAllDiseases());
        PlayerBuffStorage.addCooldownsToStorage(getId(), getCooldowns());
        getMap().removePlayer(this);
        getClient().getChannelServer().removePlayer(this);
        saveToDB(false, false);
        client.updateLoginState(MapleClient.CHANGE_CHANNEL, client.getSessionIPAddress());
        client.getSession().write(CField.getChannelChange(client, Integer.parseInt(socket[1])));
        client.setPlayer(null);
        client.setReceiving(false);
    }

    public void expandInventory(byte type, int amount) {
        final MapleInventory inv = getInventory(MapleInventoryType.getByType(type));
        inv.addSlot((byte) amount);
        client.getSession().write(InventoryPacket.getSlotUpdate(type, (byte) inv.getSlotLimit()));
    }

    public boolean allowedToTarget(MapleCharacter other) {
        return other != null && !other.isHidden() || client.getPlayer().getGMLevel() >= 4;
    }

    public int getFollowId() {
        return followid;
    }

    public void setFollowId(int fi) {
        this.followid = fi;
        if (fi == 0) {
            this.followinitiator = false;
            this.followon = false;
        }
    }

    public void setFollowInitiator(boolean fi) {
        this.followinitiator = fi;
    }

    public void setFollowOn(boolean fi) {
        this.followon = fi;
    }

    public boolean isFollowOn() {
        return followon;
    }

    public boolean isFollowInitiator() {
        return followinitiator;
    }

    public void checkFollow() {
        if (followid <= 0) {
            return;
        }
        if (followon) {
            map.broadcastMessage(CField.followEffect(id, 0, null));
            map.broadcastMessage(CField.followEffect(followid, 0, null));
        }
        MapleCharacter tt = map.getCharacterById(followid);
        client.getSession().write(CField.getFollowMessage("Follow canceled."));
        if (tt != null) {
            tt.setFollowId(0);
            tt.getClient().getSession().write(CField.getFollowMessage("Follow canceled."));
        }
        setFollowId(0);
    }

    public int getMarriageId() {
        return marriageId;
    }

    public void setMarriageId(final int mi) {
        this.marriageId = mi;
    }

    public int getMarriageItemId() {
        return marriageItemId;
    }

    public void setMarriageItemId(final int mi) {
        this.marriageItemId = mi;
    }

    public boolean isStaff() {
        return this.gmLevel >= ServerConstants.PlayerGMRank.INTERN.getLevel();
    }

    public int getCharToggle() {
        return charToggle;
    }

    public void setCharToggle(int toggle) {
        this.charToggle = toggle;
    }

    // TODO: gvup, vic, lose, draw, VR
    public boolean startPartyQuest(final int questid) {
        boolean ret = false;
        MapleQuest q = MapleQuest.getInstance(questid);
        if (q == null || !q.isPartyQuest()) {
            return false;
        }
        if (!quests.containsKey(q) || !questinfo.containsKey(questid)) {
            final MapleQuestStatus status = getQuestNAdd(q);
            status.setStatus((byte) 1);
            updateQuest(status);
            switch (questid) {
                case 1300:
                case 1301:
                case 1302: //carnival, ariants.
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0;gvup=0;vic=0;lose=0;draw=0");
                    break;
                case 1303: //ghost pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0;vic=0;lose=0");
                    break;
                case 1204: //herb town pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;have2=0;have3=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                case 1206: //ellin pq
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                default:
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
            }
            ret = true;
        } //started the quest.
        return ret;
    }

    public String getOneInfo(final int questid, final String key) {
        if (!questinfo.containsKey(questid) || key == null || MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return null;
        }
        final String[] split = questinfo.get(questid).split(";");
        for (String x : split) {
            final String[] split2 = x.split("="); //should be only 2
            if (split2.length == 2 && split2[0].equals(key)) {
                return split2[1];
            }
        }
        return null;
    }

    public void updateOneInfo(final int questid, final String key, final String value) {
        if (!questinfo.containsKey(questid) || key == null || value == null || MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        final String[] split = questinfo.get(questid).split(";");
        boolean changed = false;
        final StringBuilder newQuest = new StringBuilder();
        for (String x : split) {
            final String[] split2 = x.split("="); //should be only 2
            if (split2.length != 2) {
                continue;
            }
            if (split2[0].equals(key)) {
                newQuest.append(key).append("=").append(value);
            } else {
                newQuest.append(x);
            }
            newQuest.append(";");
            changed = true;
        }

        updateInfoQuest(questid, changed ? newQuest.toString().substring(0, newQuest.toString().length() - 1) : newQuest.toString());
    }

    public void recalcPartyQuestRank(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        if (!startPartyQuest(questid)) {
            final String oldRank = getOneInfo(questid, "rank");
            if (oldRank == null || oldRank.equals("S")) {
                return;
            }
            String newRank;
            switch (oldRank) {
                case "A":
                    newRank = "S";
                    break;
                case "B":
                    newRank = "A";
                    break;
                case "C":
                    newRank = "B";
                    break;
                case "D":
                    newRank = "C";
                    break;
                case "F":
                    newRank = "D";
                    break;
                default:
                    return;
            }
            final List<Pair<String, Pair<String, Integer>>> questInfo = MapleQuest.getInstance(questid).getInfoByRank(newRank);
            if (questInfo == null) {
                return;
            }
            for (Pair<String, Pair<String, Integer>> q : questInfo) {
                boolean found = false;
                final String val = getOneInfo(questid, q.right.left);
                if (val == null) {
                    return;
                }
                int vall;
                try {
                    vall = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return;
                }
                switch (q.left) {
                    case "less":
                        found = vall < q.right.right;
                        break;
                    case "more":
                        found = vall > q.right.right;
                        break;
                    case "equal":
                        found = vall == q.right.right;
                        break;
                }
                if (!found) {
                    return;
                }
            }
            //perfectly safe
            updateOneInfo(questid, "rank", newRank);
        }
    }

    public void tryPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            pqStartTime = System.currentTimeMillis();
            updateOneInfo(questid, "try", String.valueOf(Integer.parseInt(getOneInfo(questid, "try")) + 1));
        } catch (Exception e) {
            System.out.println("tryPartyQuest error");
        }
    }

    public void endPartyQuest(final int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            if (pqStartTime > 0) {
                final long changeTime = System.currentTimeMillis() - pqStartTime;
                final int mins = (int) (changeTime / 1000 / 60), secs = (int) (changeTime / 1000 % 60);
                final int mins2 = Integer.parseInt(getOneInfo(questid, "min"));
                if (mins2 <= 0 || mins < mins2) {
                    updateOneInfo(questid, "min", String.valueOf(mins));
                    updateOneInfo(questid, "sec", String.valueOf(secs));
                    updateOneInfo(questid, "date", FileoutputUtil.CurrentReadable_Date());
                }
                final int newCmp = Integer.parseInt(getOneInfo(questid, "cmp")) + 1;
                updateOneInfo(questid, "cmp", String.valueOf(newCmp));
                updateOneInfo(questid, "CR", String.valueOf((int) Math.ceil((newCmp * 100.0) / Integer.parseInt(getOneInfo(questid, "try")))));
                recalcPartyQuestRank(questid);
                pqStartTime = 0;
            }
        } catch (Exception e) {
            System.out.println("endPartyQuest error");
        }

    }

    public void havePartyQuest(final int itemId) {
        int questid, index = -1;
        switch (itemId) {
            case 1002798:
                questid = 1200; //henesys
                break;
            case 1072369:
                questid = 1201; //kerning
                break;
            case 1022073:
                questid = 1202; //ludi
                break;
            case 1082232:
                questid = 1203; //orbis
                break;
            case 1002571:
            case 1002572:
            case 1002573:
            case 1002574:
                questid = 1204; //herbtown
                index = itemId - 1002571;
                break;
            case 1102226:
                questid = 1303; //ghost
                break;
            case 1102227:
                questid = 1303; //ghost
                index = 0;
                break;
            case 1122010:
                questid = 1205; //magatia
                break;
            case 1032061:
            case 1032060:
                questid = 1206; //ellin
                index = itemId - 1032060;
                break;
            case 3010018:
                questid = 1300; //ariant
                break;
            case 1122007:
                questid = 1301; //carnival
                break;
            case 1122058:
                questid = 1302; //carnival2
                break;
            default:
                return;
        }
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        startPartyQuest(questid);
        updateOneInfo(questid, "have" + (index == -1 ? "" : index), "1");
    }

    public void resetStatsByJob(boolean beginnerJob) {
        int baseJob = (beginnerJob ? (job % 1000) : (((job % 1000) / 100) * 100)); //1112 -> 112 -> 1 -> 100
        boolean UA = getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER)) != null;
        if (baseJob == 100) { //first job = warrior
            resetStats(UA ? 4 : 35, 4, 4, 4);
        } else if (baseJob == 200) {
            resetStats(4, 4, UA ? 4 : 20, 4);
        } else if (baseJob == 300 || baseJob == 400) {
            resetStats(4, UA ? 4 : 25, 4, 4);
        } else if (baseJob == 500) {
            resetStats(4, UA ? 4 : 20, 4, 4);
        } else if (baseJob == 0) {
            resetStats(4, 4, 4, 4);
        }
    }

    public boolean hasSummon() {
        return hasSummon;
    }

    public void setHasSummon(boolean summ) {
        this.hasSummon = summ;
    }

    public void removeDoor() {
        final MapleDoor door = getDoors().iterator().next();
        for (final MapleCharacter chr : door.getTarget().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleCharacter chr : door.getTown().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (final MapleDoor destroyDoor : getDoors()) {
            door.getTarget().removeMapObject(destroyDoor);
            door.getTown().removeMapObject(destroyDoor);
        }
        clearDoors();
    }

    public void removeMechDoor() {
        for (final MechDoor destroyDoor : getMechDoors()) {
            for (final MapleCharacter chr : getMap().getCharactersThreadsafe()) {
                destroyDoor.sendDestroyData(chr.getClient());
            }
            getMap().removeMapObject(destroyDoor);
        }
        clearMechDoors();
    }

    public void changeRemoval() {
        changeRemoval(false);
    }

    public void changeRemoval(boolean dc) {
        removeFamiliar();
        dispelSummons();
        if (!dc) {
            cancelEffectFromBuffStat(MapleBuffStat.SOARING);
            cancelEffectFromBuffStat(MapleBuffStat.MONSTER_RIDING);
            cancelEffectFromBuffStat(MapleBuffStat.MECH_CHANGE);
            cancelEffectFromBuffStat(MapleBuffStat.RECOVERY);
        }
        if (getPyramidSubway() != null) {
            getPyramidSubway().dispose(this);
        }
        if (playerShop != null && !dc) {
            playerShop.removeVisitor(this);
            if (playerShop.isOwner(this)) {
                playerShop.setOpen(true);
            }
        }
        if (!getDoors().isEmpty()) {
            removeDoor();
        }
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        NPCScriptManager.getInstance().dispose(client);
        cancelFairySchedule(false);
    }

    public boolean canUseFamilyBuff(MapleFamilyBuff buff) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(buff.questID));
        if (stat == null) {
            return true;
        }
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Long.parseLong(stat.getCustomData()) + (24 * 3600000) < System.currentTimeMillis();
    }

    public void useFamilyBuff(MapleFamilyBuff buff) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(buff.questID));
        stat.setCustomData(String.valueOf(System.currentTimeMillis()));
    }

    public List<Integer> usedBuffs() {
        //assume count = 1
        List<Integer> used = new ArrayList<>();
        MapleFamilyBuff[] z = MapleFamilyBuff.values();
        for (int i = 0; i < z.length; i++) {
            if (!canUseFamilyBuff(z[i])) {
                used.add(i);
            }
        }
        return used;
    }

    public String getTeleportName() {
        return teleportname;
    }

    public void setTeleportName(final String tname) {
        teleportname = tname;
    }

    public int getNoJuniors() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getNoJuniors();
    }

    public MapleFamilyCharacter getMFC() {
        return mfc;
    }

    public void makeMFC(final int familyid, final int seniorid, final int junior1, final int junior2) {
        if (familyid > 0) {
            MapleFamily f = World.Family.getFamily(familyid);
            if (f == null) {
                mfc = null;
            } else {
                mfc = f.getMFC(id);
                if (mfc == null) {
                    mfc = f.addFamilyMemberInfo(this, seniorid, junior1, junior2);
                }
                if (mfc.getSeniorId() != seniorid) {
                    mfc.setSeniorId(seniorid);
                }
                if (mfc.getJunior1() != junior1) {
                    mfc.setJunior1(junior1);
                }
                if (mfc.getJunior2() != junior2) {
                    mfc.setJunior2(junior2);
                }
            }
        } else {
            mfc = null;
        }
    }

    public void setFamily(final int newf, final int news, final int newj1, final int newj2) {
        if (mfc == null || newf != mfc.getFamilyId() || news != mfc.getSeniorId() || newj1 != mfc.getJunior1() || newj2 != mfc.getJunior2()) {
            makeMFC(newf, news, newj1, newj2);
        }
    }

    public int maxBattleshipHP(int skillid) {
        return (getTotalSkillLevel(skillid) * 5000) + ((getLevel() - 120) * 3000);
    }

    public int currentBattleshipHP() {
        return battleshipHP;
    }

    public void setBattleshipHP(int v) {
        this.battleshipHP = v;
    }

    public void decreaseBattleshipHP() {
        this.battleshipHP--;
    }

    public int getGachExp() {
        return gachexp;
    }

    public void setGachExp(int ge) {
        this.gachexp = ge;
    }

    public boolean isInBlockedMap() {
        if (!isAlive() || getPyramidSubway() != null || getMap().getSquadByMap() != null || getEventInstance() != null || getMap().getEMByMap() != null) {
            return true;
        }
        if ((getMapId() >= 680000210 && getMapId() <= 680000502) || (getMapId() / 10000 == 92502 && getMapId() >= 925020100) || (getMapId() / 10000 == 92503) || getMapId() == GameConstants.JAIL) {
            return true;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return true;
            }
        }
        return false;
    }

    public boolean isInTownMap() {
        if (hasBlockedInventory() || !getMap().isTown() || FieldLimitType.VipRock.check(getMap().getFieldLimit()) || getEventInstance() != null) {
            return false;
        }
        for (int i : GameConstants.blockedMaps) {
            if (getMapId() == i) {
                return false;
            }
        }
        return true;
    }

    public boolean hasBlockedInventory() {
        return !isAlive() || getTrade() != null || getConversation() > 0 || getDirection() >= 0 || getPlayerShop() != null || map == null;
    }

    public void startPartySearch(final List<Integer> jobs, final int maxLevel, final int minLevel, final int membersNeeded) {
        for (MapleCharacter chr : map.getCharacters()) {
            if (chr.getId() != id && chr.getParty() == null && chr.getLevel() >= minLevel && chr.getLevel() <= maxLevel && (jobs.isEmpty() || jobs.contains(Integer.valueOf(chr.getJob()))) && (isGM() || !chr.isGM())) {
                if (party != null && party.getMembers().size() < 6 && party.getMembers().size() < membersNeeded) {
                    chr.setParty(party);
                    World.Party.updateParty(party.getId(), PartyOperation.JOIN, new MaplePartyCharacter(chr));
                    chr.receivePartyMemberHP();
                    chr.updatePartyMemberHP();
                } else {
                    break;
                }
            }
        }
    }

    public int getChallenge() {
        return challenge;
    }

    public void setChallenge(int c) {
        this.challenge = c;
    }

    public short getFatigue() {
        return fatigue;
    }

    public void setFatigue(int j) {
        this.fatigue = (short) Math.max(0, j);
        updateSingleStat(MapleStat.FATIGUE, this.fatigue);
    }

    public void fakeRelog() {
        client.getSession().write(CField.getCharInfo(this));
        final MapleMap mapp = getMap();
        mapp.setCheckStates(false);
        mapp.removePlayer(this);
        mapp.addPlayer(this);
        mapp.setCheckStates(true);

        client.getSession().write(CWvsContext.getFamiliarInfo(this));
    }

    public boolean canSummon() {
        return canSummon(5000);
    }

    public boolean canSummon(int g) {
        if (lastSummonTime + g < System.currentTimeMillis()) {
            lastSummonTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public int getIntNoRecord(int questID) {
        final MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(questID));
        if (stat == null || stat.getCustomData() == null) {
            return 0;
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public int getIntRecord(int questID) {
        final MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(questID));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public void updatePetAuto() {
        if (getIntNoRecord(GameConstants.HP_ITEM) > 0) {
            client.getSession().write(CField.petAutoHP(getIntRecord(GameConstants.HP_ITEM)));
        }
        if (getIntNoRecord(GameConstants.MP_ITEM) > 0) {
            client.getSession().write(CField.petAutoMP(getIntRecord(GameConstants.MP_ITEM)));
        }
    }

    public void sendEnglishQuiz(String msg) {
        //client.getSession().write(CField.englishQuizMsg(msg));
    }

    public void setChangeTime() {
        mapChangeTime = System.currentTimeMillis();
    }

    public long getChangeTime() {
        return mapChangeTime;
    }
    
    public int ariantScore = 0;
    public void addAriantScore() {
        ariantScore++;
    }
    public void resetAriantScore() {
        ariantScore = 0;
    }
    public int getAriantScore() { // TODO: code entire score system
        return ariantScore;
    }
    
    public void updateAriantScore() {
        this.getMap().broadcastMessage(CField.updateAriantScore(this.getName(), getAriantScore(), false));
    }

    public short getScrolledPosition() {
        return scrolledPosition;
    }

    public void setScrolledPosition(short s) {
        this.scrolledPosition = s;
    }

    public MapleTrait getTrait(MapleTraitType t) {
        return traits.get(t);
    }

    public void forceCompleteQuest(int id) {
        MapleQuest.getInstance(id).forceComplete(this, 9270035); //troll
    }

    public List<Integer> getExtendedSlots() {
        return extendedSlots;
    }

    public int getExtendedSlot(int index) {
        if (extendedSlots.size() <= index || index < 0) {
            return -1;
        }
        return extendedSlots.get(index);
    }

    public void changedExtended() {
        changed_extendedSlots = true;
    }

    public MapleAndroid getAndroid() {
        return android;
    }

    public void removeAndroid() {
        if (map != null) {
            map.broadcastMessage(CField.deactivateAndroid(this.id));
        }
        android = null;
    }

    public void setAndroid(MapleAndroid a) {
        this.android = a;
        if (map != null && a != null) {
            map.broadcastMessage(CField.spawnAndroid(this, a));
            map.broadcastMessage(CField.showAndroidEmotion(this.getId(), Randomizer.nextInt(17) + 1));
        }
    }

    public List<Item> getRebuy() {
        return rebuy;
    }

    public Map<Integer, MonsterFamiliar> getFamiliars() {
        return familiars;
    }

    public MonsterFamiliar getSummonedFamiliar() {
        return summonedFamiliar;
    }

    public void removeFamiliar() {
        if (summonedFamiliar != null && map != null) {
            removeVisibleFamiliar();
        }
        summonedFamiliar = null;
    }

    public void removeVisibleFamiliar() {
        getMap().removeMapObject(summonedFamiliar);
        removeVisibleMapObject(summonedFamiliar);
        getMap().broadcastMessage(CField.removeFamiliar(this.getId()));
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        cancelEffect(ii.getItemEffect(ii.getFamiliar(summonedFamiliar.getFamiliar()).passive), false, System.currentTimeMillis());
    }

    public void spawnFamiliar(MonsterFamiliar mf) {
        summonedFamiliar = mf;

        mf.setStance(0);
        mf.setPosition(getPosition());
        mf.setFh(getFH());
        addVisibleMapObject(mf);
        getMap().spawnFamiliar(mf);

        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final MapleStatEffect eff = ii.getItemEffect(ii.getFamiliar(summonedFamiliar.getFamiliar()).passive);
        if (eff != null && eff.getInterval() <= 0 && eff.makeChanceResult()) { //i think this is actually done through a recv, which is ATTACK_FAMILIAR +1
            eff.applyTo(this);
        }
        lastFamiliarEffectTime = System.currentTimeMillis();
    }

    public final boolean canFamiliarEffect(long now, MapleStatEffect eff) {
        return lastFamiliarEffectTime > 0 && lastFamiliarEffectTime + eff.getInterval() < now;
    }

    /*public void doFamiliarSchedule(long now) {
     if (familiars == null) {
     return;
     }
     for (MonsterFamiliar mf : familiars.values()) {
     if (summonedFamiliar != null && summonedFamiliar.getId() == mf.getId()) {
     mf.addFatigue(this, 5);
     final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
     final MapleStatEffect eff = ii.getItemEffect(ii.getFamiliar(summonedFamiliar.getFamiliar()).passive);
     if (eff != null && eff.getInterval() > 0 && canFamiliarEffect(now, eff) && eff.makeChanceResult()) {
     eff.applyTo(this);
     }
     } else if (mf.getFatigue() > 0) {
     mf.setFatigue(Math.max(0, mf.getFatigue() - 5));
     }
     }
     }
     * 
     */
    public void doFamiliarSchedule(long now) {
        for (MonsterFamiliar mf : familiars.values()) {
            if (summonedFamiliar != null && summonedFamiliar.getId() == mf.getId()) {
                mf.addFatigue(this, 5);
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                final MapleStatEffect eff = ii.getItemEffect(ii.getFamiliar(summonedFamiliar.getFamiliar()).passive);
                if (eff != null && eff.getInterval() > 0 && canFamiliarEffect(now, eff) && eff.makeChanceResult()) {
                    eff.applyTo(this);
                }
            } else if (mf.getFatigue() > 0) {
                mf.setFatigue(Math.max(0, mf.getFatigue() - 5));
            }
        }
    }
    
    private int lastMap,lastMap1;
    private MaplePortal lastPortal;
    private boolean returningToMap = false;
    public void setLastMap(int map, MaplePortal portal){
        if (map !=  lastMap){
            lastMap1 = lastMap;
            lastMap = map;
            lastPortal = portal;
        }
    }
    public MaplePortal getLastPortal(){
        return lastPortal;
    }
    public int getLastMap(){
        return lastMap1;
    }
    public void setReturningToMap(boolean flag){
        returningToMap = flag;
    }
    public boolean getReturningToMap(){
        return returningToMap;
    }

    public MapleImp[] getImps() {
        return imps;
    }

    public void sendImp() {
        for (int i = 0; i < imps.length; i++) {
            if (imps[i] != null) {
                client.getSession().write(CWvsContext.updateImp(imps[i], ImpFlag.SUMMONED.getValue(), i, true));
            }
        }
    }

    public int getBattlePoints() {
        return pvpPoints;
    }

    public int getTotalBattleExp() {
        return pvpExp;
    }

    public void setBattlePoints(int p) {
        if (p != pvpPoints) {
            client.getSession().write(InfoPacket.getBPMsg(p - pvpPoints));
            updateSingleStat(MapleStat.BATTLE_POINTS, p);
        }
        this.pvpPoints = p;
    }

    public void setTotalBattleExp(int p) {
        final int previous = pvpExp;
        this.pvpExp = p;
        if (p != previous) {
            stats.recalcPVPRank(this);

            updateSingleStat(MapleStat.BATTLE_EXP, stats.pvpExp);
            updateSingleStat(MapleStat.BATTLE_RANK, stats.pvpRank);
        }
    }

    public void changeTeam(int newTeam) {
        this.coconutteam = newTeam;

        if (inPVP()) {
            client.getSession().write(CField.getPVPTransform(newTeam + 1));
            map.broadcastMessage(CField.changeTeam(id, newTeam + 1));
        } else {
            client.getSession().write(CField.showEquipEffect(newTeam));
        }
    }

    public void disease(int type, int level) {
        if (MapleDisease.getBySkill(type) == null) {
            return;
        }
        chair = 0;
        client.getSession().write(CField.cancelChair(-1));
        map.broadcastMessage(this, CField.showChair(id, 0), false);
        giveDebuff(MapleDisease.getBySkill(type), MobSkillFactory.getMobSkill(type, level));
    }

    public boolean inPVP() {
        return eventInstance != null && eventInstance.getName().startsWith("PVP");
    }

    public void clearAllCooldowns() {
        for (MapleCoolDownValueHolder m : getCooldowns()) {
            final int skil = m.skillId;
            removeCooldown(skil);
            client.getSession().write(CField.skillCooldown(skil, 0));
        }
    }
    
    public final void clearAllSkills() {
        HashMap<Skill, SkillEntry> sa = new HashMap<>();
        for (Skill skil : SkillFactory.getAllSkills()) {
                sa.put(skil, new SkillEntry((byte) 0, (byte) 0, SkillFactory.getDefaultSExpiry(skil)));
        }
        changeSkillsLevel(sa);
    }

    public Pair<Double, Boolean> modifyDamageTaken(double damage, MapleMapObject attacke) {
        Pair<Double, Boolean> ret = new Pair<>(damage, false);
        if (damage <= 0) {
            return ret;
        }
        if (stats.ignoreDAMr > 0 && Randomizer.nextInt(100) < stats.ignoreDAMr_rate) {
            damage -= Math.floor((stats.ignoreDAMr * damage) / 100.0f);
        }
        if (stats.ignoreDAM > 0 && Randomizer.nextInt(100) < stats.ignoreDAM_rate) {
            damage -= stats.ignoreDAM;
        }
        final Integer div = getBuffedValue(MapleBuffStat.DIVINE_SHIELD);
        final Integer div2 = getBuffedValue(MapleBuffStat.HOLY_MAGIC_SHELL);
        if (div2 != null) {
            if (div2 <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.HOLY_MAGIC_SHELL);
            } else {
                setBuffedValue(MapleBuffStat.HOLY_MAGIC_SHELL, div2 - 1);
                damage = 0;
            }
        } else if (div != null) {
            if (div <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.DIVINE_SHIELD);
            } else {
                setBuffedValue(MapleBuffStat.DIVINE_SHIELD, div - 1);
                damage = 0;
            }
        }
        MapleStatEffect barrier = getStatForBuff(MapleBuffStat.COMBO_BARRIER);
        if (barrier != null) {
            damage = ((barrier.getX() / 1000.0) * damage);
        }
        barrier = getStatForBuff(MapleBuffStat.MAGIC_SHIELD);
        if (barrier != null) {
            damage = ((barrier.getX() / 1000.0) * damage);
        }
        barrier = getStatForBuff(MapleBuffStat.WATER_SHIELD);
        if (barrier != null) {
            damage = ((barrier.getX() / 1000.0) * damage);
        }
        List<Integer> attack = attacke instanceof MapleMonster || attacke == null ? null : (new ArrayList<Integer>());
        if (damage > 0) {
            if (getJob() == 122 && !skillisCooling(1220013)) {
                final Skill divine = SkillFactory.getSkill(1220013);
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.getSession().write(CField.skillCooldown(1220013, divineShield.getCooldown(this)));
                        addCooldown(1220013, System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                    }
                }
            } else if (getBuffedValue(MapleBuffStat.SATELLITESAFE_PROC) != null && getBuffedValue(MapleBuffStat.SATELLITESAFE_ABSORB) != null && getBuffedValue(MapleBuffStat.PUPPET) != null) {
                double buff = getBuffedValue(MapleBuffStat.SATELLITESAFE_PROC).doubleValue();
                double buffz = getBuffedValue(MapleBuffStat.SATELLITESAFE_ABSORB).doubleValue();
                if ((int) ((buff / 100.0) * getStat().getMaxHp()) <= damage) {
                    damage -= ((buffz / 100.0) * damage);
                    cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
                }
            } else if (getJob() == 433 || getJob() == 434) {
                final Skill divine = SkillFactory.getSkill(4330001);
                if (getTotalSkillLevel(divine) > 0 && getBuffedValue(MapleBuffStat.DARKSIGHT) == null && !skillisCooling(divine.getId())) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (Randomizer.nextInt(100) < divineShield.getX()) {
                        divineShield.applyTo(this);
                    }
                }
            } else if ((getJob() == 512 || getJob() == 522) && getBuffedValue(MapleBuffStat.PIRATES_REVENGE) == null) {
                final Skill divine = SkillFactory.getSkill(getJob() == 512 ? 5120011 : 5220012);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId())) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.getSession().write(CField.skillCooldown(divine.getId(), divineShield.getCooldown(this)));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                    }
                }
            } else if (getJob() == 312 && attacke != null) {
                final Skill divine = SkillFactory.getSkill(3120010);
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        if (attacke instanceof MapleMonster) {
                            final Rectangle bounds = divineShield.calculateBoundingBox(getTruePosition(), isFacingLeft());
                            final List<MapleMapObject> affected = getMap().getMapObjectsInRect(bounds, Arrays.asList(attacke.getType()));
                            int i = 0;

                            for (final MapleMapObject mo : affected) {
                                MapleMonster mons = (MapleMonster) mo;
                                if (mons.getStats().isFriendly() || mons.isFake()) {
                                    continue;
                                }
                                mons.applyStatus(this, new MonsterStatusEffect(MonsterStatus.STUN, 1, divineShield.getSourceId(), null, false), false, divineShield.getDuration(), true, divineShield);
                                final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                                mons.damage(this, theDmg, true);
                                getMap().broadcastMessage(MobPacket.damageMonster(mons.getObjectId(), theDmg));
                                i++;
                                if (i >= divineShield.getMobCount()) {
                                    break;
                                }
                            }
                        } else {
                            MapleCharacter chr = (MapleCharacter) attacke;
                            // chr.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            } else if ((getJob() == 531 || getJob() == 532) && attacke != null) {
                final Skill divine = SkillFactory.getSkill(5310009); //slea.readInt() = 5310009, then slea.readInt() = damage. (175000)
                if (getTotalSkillLevel(divine) > 0) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        if (attacke instanceof MapleMonster) {
                            final MapleMonster attacker = (MapleMonster) attacke;
                            final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            final MapleCharacter attacker = (MapleCharacter) attacke;
                            //  attacker.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            } else if (getJob() == 132 && attacke != null) {
                final Skill divine = SkillFactory.getSkill(1320011);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId()) && getBuffSource(MapleBuffStat.BEHOLDER) == 1321007) {
                    final MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        client.getSession().write(CField.skillCooldown(divine.getId(), divineShield.getCooldown(this)));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                        if (attacke instanceof MapleMonster) {
                            final MapleMonster attacker = (MapleMonster) attacke;
                            final int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            final MapleCharacter attacker = (MapleCharacter) attacke;
                            //   attacker.addHP(-divineShield.getDamage());
                            attack.add((int) divineShield.getDamage());
                        }
                    }
                }
            }
            if (attacke != null) {
                final int damr = (Randomizer.nextInt(100) < getStat().DAMreflect_rate ? getStat().DAMreflect : 0) + (getBuffedValue(MapleBuffStat.POWERGUARD) != null ? getBuffedValue(MapleBuffStat.POWERGUARD) : 0);
                final int bouncedam_ = damr + (getBuffedValue(MapleBuffStat.PERFECT_ARMOR) != null ? getBuffedValue(MapleBuffStat.PERFECT_ARMOR) : 0);
                if (bouncedam_ > 0) {
                    int bouncedamage = (int) (damage * bouncedam_ / 100);
                    int bouncer = (int) (damage * damr / 100);
                    damage -= bouncer;
                    if (attacke instanceof MapleMonster) {
                        final MapleMonster attacker = (MapleMonster) attacke;
                        bouncedamage = (int) Math.min(bouncedamage, attacker.getMobMaxHp() / 10);
                        attacker.damage(this, bouncedamage, true);
                        getMap().broadcastMessage(this, MobPacket.damageMonster(attacker.getObjectId(), bouncedamage), getTruePosition());
                        if (getBuffSource(MapleBuffStat.PERFECT_ARMOR) == 31101003) {
                            MapleStatEffect eff = this.getStatForBuff(MapleBuffStat.PERFECT_ARMOR);
                            if (eff.makeChanceResult()) {
                                attacker.applyStatus(this, new MonsterStatusEffect(MonsterStatus.STUN, 1, eff.getSourceId(), null, false), false, eff.getSubTime(), true, eff);
                            }
                        }
                    } else {
                        final MapleCharacter attacker = (MapleCharacter) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getStat().getCurrentMaxHp() / 10);
                        //  attacker.addHP(-((int) bouncedamage));
                        attack.add((int) bouncedamage);
                        if (getBuffSource(MapleBuffStat.PERFECT_ARMOR) == 31101003) {
                            MapleStatEffect eff = this.getStatForBuff(MapleBuffStat.PERFECT_ARMOR);
                            if (eff.makeChanceResult()) {
                                attacker.disease(MapleDisease.STUN.getDisease(), 1);
                            }
                        }
                    }
                    ret.right = true;
                }
                if ((getJob() == 411 || getJob() == 412 || getJob() == 421 || getJob() == 422) && getBuffedValue(MapleBuffStat.SUMMON) != null && attacke != null) {
                    final List<MapleSummon> ss = getSummonsReadLock();
                    try {
                        for (MapleSummon sum : ss) {
                            if (sum.getTruePosition().distanceSq(getTruePosition()) < 400000.0 && (sum.getSkill() == 4111007 || sum.getSkill() == 4211007)) {
                                final List<Pair<Integer, Integer>> allDamage = new ArrayList<>();
                                if (attacke instanceof MapleMonster) {
                                    final MapleMonster attacker = (MapleMonster) attacke;
                                    final int theDmg = (int) (SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX() * damage / 100.0);
                                    allDamage.add(new Pair<>(attacker.getObjectId(), theDmg));
                                    getMap().broadcastMessage(SummonPacket.summonAttack(sum.getOwnerId(), sum.getObjectId(), (byte) 0x84, allDamage, getLevel(), true));
                                    attacker.damage(this, theDmg, true);
                                    checkMonsterAggro(attacker);
                                    if (!attacker.isAlive()) {
                                        getClient().getSession().write(MobPacket.killMonster(attacker.getObjectId(), 1));
                                    }
                                } else {
                                    final MapleCharacter chr = (MapleCharacter) attacke;
                                    final int dmg = SkillFactory.getSkill(sum.getSkill()).getEffect(sum.getSkillLevel()).getX();
                                    //     chr.addHP(-dmg);
                                    attack.add(dmg);
                                }
                            }
                        }
                    } finally {
                        unlockSummonsReadLock();
                    }
                }
            }
        }
        if (attack != null && attack.size() > 0 && attacke != null) {
            getMap().broadcastMessage(CField.pvpCool(attacke.getObjectId(), attack));
        }
        ret.left = damage;
        return ret;
    }

    public void onAttack(long maxhp, int maxmp, int skillid, int oid, int totDamage) {
        if (stats.hpRecoverProp > 0) {
            if (Randomizer.nextInt(100) <= stats.hpRecoverProp) {//i think its out of 100, anyway
                if (stats.hpRecover > 0) {
                    healHP(stats.hpRecover);
                }
                if (stats.hpRecoverPercent > 0) {
                    addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) stats.hpRecoverPercent / 100.0)), stats.getMaxHp() / 2))));
                }
            }
        }
        if (stats.mpRecoverProp > 0 && !GameConstants.isDemon(getJob())) {
            if (Randomizer.nextInt(100) <= stats.mpRecoverProp) {//i think its out of 100, anyway
                healMP(stats.mpRecover);
            }
        }
        if (getBuffedValue(MapleBuffStat.COMBO_DRAIN) != null) {
            addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) getStatForBuff(MapleBuffStat.COMBO_DRAIN).getX() / 100.0)), stats.getMaxHp() / 2))));
        }
        if (getBuffSource(MapleBuffStat.COMBO_DRAIN) == 23101003) {
            addMP(((int) Math.min(maxmp, Math.min(((int) ((double) totDamage * (double) getStatForBuff(MapleBuffStat.COMBO_DRAIN).getX() / 100.0)), stats.getMaxMp() / 2))));
        }
        if (getBuffedValue(MapleBuffStat.REAPER) != null && getBuffedValue(MapleBuffStat.SUMMON) == null && getSummonsSize() < 4 && canSummon()) {
            final MapleStatEffect eff = getStatForBuff(MapleBuffStat.REAPER);
            if (eff.makeChanceResult()) {
                eff.applyTo(this, this, false, null, eff.getDuration());
            }
        }
        if (getJob() == 212 || getJob() == 222 || getJob() == 232) {
            int[] skillshi = {2120010, 2220010, 2320011};
            for (int i : skillshi) {
                final Skill skill = SkillFactory.getSkill(i);
                if (getTotalSkillLevel(skill) > 0) {
                    final MapleStatEffect venomEffect = skill.getEffect(getTotalSkillLevel(skill));
                    if (venomEffect.makeChanceResult() && getAllLinkMid().size() < venomEffect.getY()) {
                        setLinkMid(oid, venomEffect.getX());
                        venomEffect.applyTo(this);
                    }
                    break;
                }
            }
        }
        // effects
        if (skillid > 0) {
            final Skill skil = SkillFactory.getSkill(skillid);
            final MapleStatEffect effect = skil.getEffect(getTotalSkillLevel(skil));
            switch (skillid) {
                case 15111001:
                case 3111008:
                case 1078:
                case 31111003:
                case 11078:
                case 14101006:
                case 33111006: //swipe
                case 4101005: //drain
                case 5111004: { // Energy Drain
                    addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) effect.getX() / 100.0)), stats.getMaxHp() / 2))));
                    break;
                }
                case 5211006:
                case 22151002: //killer wing
                case 5220011: {//homing
                    setLinkMid(oid, effect.getX());
                    break;
                }
                case 33101007: { //jaguar
                    clearLinkMid();
                    break;
                }
            }
        }
    }

    public void handleForceGain(int oid, int skillid) {
        handleForceGain(oid, skillid, 0);
    }

    public void handleForceGain(int oid, int skillid, int extraForce) {
        if (!GameConstants.isForceIncrease(skillid) && extraForce <= 0) {
            return;
        }
        int forceGain = 1;
        if (getLevel() >= 30 && getLevel() < 70) {
            forceGain = 2;
        } else if (getLevel() >= 70 && getLevel() < 120) {
            forceGain = 3;
        } else if (getLevel() >= 120) {
            forceGain = 4;
        }
        force++; // counter
        addMP(extraForce > 0 ? extraForce : forceGain, true);
        getClient().getSession().write(CField.gainForce(oid, force, forceGain));

        if (stats.mpRecoverProp > 0 && extraForce <= 0) {
            if (Randomizer.nextInt(100) <= stats.mpRecoverProp) {//i think its out of 100, anyway
                force++; // counter
                addMP(stats.mpRecover, true);
                getClient().getSession().write(CField.gainForce(oid, force, stats.mpRecover));
            }
        }
    }

    public void afterAttack(int mobCount, int attackCount, int skillid) {
        switch (getJob()) {
            case 511:
            case 512: {
                handleEnergyCharge(5110001, mobCount * attackCount);
                break;
            }
            case 1510:
            case 1511:
            case 1512: {
                handleEnergyCharge(15100004, mobCount * attackCount);
                break;
            }
            case 111:
            case 112:
            case 1111:
            case 1112:
                if (skillid != 1111008 & getBuffedValue(MapleBuffStat.COMBO) != null) { // shout should not give orbs
                    handleOrbgain();
                }
                break;
        }
        if (getBuffedValue(MapleBuffStat.OWL_SPIRIT) != null) {
            if (currentBattleshipHP() > 0) {
                decreaseBattleshipHP();
            }
            if (currentBattleshipHP() <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.OWL_SPIRIT);
            }
        }
        if (!isIntern()) {
            cancelEffectFromBuffStat(MapleBuffStat.WIND_WALK);
            cancelEffectFromBuffStat(MapleBuffStat.INFILTRATE);
            final MapleStatEffect ds = getStatForBuff(MapleBuffStat.DARKSIGHT);
            if (ds != null) {
                if (ds.getSourceId() != 4330001 || !ds.makeChanceResult()) {
                    cancelEffectFromBuffStat(MapleBuffStat.DARKSIGHT);
                }
            }
        }
    }

    public void applyIceGage(int x) {
        updateSingleStat(MapleStat.ICE_GAGE, x);
    }

    public Rectangle getBounds() {
        return new Rectangle(getTruePosition().x - 25, getTruePosition().y - 75, 50, 75);
    }

    @Override
    public final Map<Byte, Integer> getEquips() {
        final Map<Byte, Integer> eq = new HashMap<>();
        for (final Item item : inventory[MapleInventoryType.EQUIPPED.ordinal()].newList()) {
            eq.put((byte) item.getPosition(), item.getItemId());
        }
        return eq;
    }
    private transient PlayerRandomStream CRand;

    public final PlayerRandomStream CRand() {
        return CRand;
    }

    /*Start of Custom Feature*/
    public int getReborns() {
        return reborns;
    }

    public void setReborns(int reborns) {
        this.reborns = reborns;
    }

    public int getAPS() {
        return apstorage;
    }
    
    private String[] commandArgs;
    
    public void setCommandArgs(String[] args) {
        commandArgs = args;
    }
    
    public String[] getCommandArgs(){
        return commandArgs;
    }

    public void setAPS(int aps) {
        apstorage = aps;
    }

    public void gainAPS(int aps) {
        apstorage += aps;
    }
}
