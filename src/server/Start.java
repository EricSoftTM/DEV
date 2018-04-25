package server;

import client.SkillFactory;
import client.inventory.MapleInventoryIdentifier;
import constants.ServerConstants;
import constants.WorldConstants;
import database.DatabaseConnection;
import handling.MapleServerHandler;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.login.LoginInformationProvider;
import handling.login.LoginServer;
import handling.world.World;
import handling.world.family.MapleFamily;
import handling.world.guild.MapleGuild;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import server.Timer.BuffTimer;
import server.Timer.EtcTimer;
import server.Timer.EventTimer;
import server.Timer.MapTimer;
import server.Timer.PingTimer;
import server.Timer.PokeTimer;
import server.Timer.WorldTimer;
import server.events.MapleOxQuizFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MobSkillFactory;
import server.life.PlayerNPC;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import tools.Pair;
import tools.packet.LoginPacket.Server;

public class Start {

    public static long startTime = System.currentTimeMillis();
    public static final Start instance = new Start();
    public static AtomicInteger CompletedLoadingThreads = new AtomicInteger(0);
    public static int itemSize = 0;

    public void run() throws InterruptedException {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0");
            ps.executeUpdate();
            ps.close();
            //ps = DatabaseConnection.getConnection().prepareStatement("UPDATE mrush SET mesos = 9223372036854775807");
            //ps.executeUpdate(); 
            //ps.close();
            ps = DatabaseConnection.getConnection().prepareStatement("UPDATE guilds SET GP = 2147483647 WHERE guildid = 1");
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        System.out.println("Starting Development v117" + "." + ServerConstants.MAPLE_PATCH +"..");
        // Worlds
        WorldConstants.init();
        World.init();
        // Timers
        WorldTimer.getInstance().start();
        PokeTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        PingTimer.getInstance().start();
        // Server Handler
        MapleServerHandler.initiate();
        // Servers
        LoginServer.run_startup_configurations();
        CashShopServer.run_startup_configurations();
        World.registerRespawn();
        // Information
        MapleItemInformationProvider.getInstance().runEtc(); 
        MapleMonsterInformationProvider.getInstance().load(); 
        MapleItemInformationProvider.getInstance().runItems(); 
        System.out.println("Development is online with " + itemSize + " items in-game.");
        LoginServer.setOn(); 
        // Every other instance cache :)
        SkillFactory.load();
        LoginInformationProvider.getInstance();
        MapleGuildRanking.getInstance().load();
        MapleGuild.loadAll(); //(this); 
        MapleFamily.loadAll(); //(this); 
        MapleLifeFactory.loadQuestCounts();
        MapleQuest.initQuests();
        RandomRewards.load();
        MapleOxQuizFactory.getInstance();
        MapleCarnivalFactory.getInstance();
        CharacterCardFactory.getInstance().initialize();
        MobSkillFactory.getInstance();
        SpeedRunner.loadSpeedRuns();
        MapleInventoryIdentifier.getInstance();
        MapleMapFactory.loadCustomLife();
        CashItemFactory.getInstance().initialize(); 
        PlayerNPC.loadAll();// touch - so we see database problems early...
        MapleMonsterInformationProvider.getInstance().addExtra();
        RankingWorker.run();
    }

    public static void main(final String args[]) throws InterruptedException {
        instance.run();
    }
}
