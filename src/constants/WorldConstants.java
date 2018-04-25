/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package constants;

import java.util.LinkedList;
import java.util.List;
import tools.Pair;
import tools.packet.LoginPacket.Server;

/**
 *
 * @author Eric
 * 
 * Global World Properties.
 */
public class WorldConstants {
    // Global Constants : handle world count, channel count, max user per world, max char per account, and event scripts
    public static int Worlds = 4; // max: 23 (skips 24-32 and continues 33-39, making the real max: 40)
    public static int Channels = 5; // channel count per world (will differ between worlds in future)
    public static int UserLimit = 1500; // maximum users per world (will be the same unless i say so)
    public static int maxCharacters = 15; // max characters per world (will differ between worlds in future)
    // Scripts TODO: Amoria,CWKPQ,BossBalrog_EASY,BossBalrog_NORMAL,ZakumPQ,ProtectTylus,GuildQuest,Ravana_EASY,Ravna_MED,Ravana_HARD (untested or not working)
    public static String Events = "" // event scripts, programmed per world but i'll keep them the same
            + "elevator,AriantPQ1,Aswan,automsg,autoSave,MonsterPark,Trains,Boats,Flight,PVP,Visitor,cpq2,cpq,Rex,AirPlane,CygnusBattle,ScarTarBattle,VonLeonBattle,Ghost,"
            + "Prison,HillaBattle,AswanOffSeason,ArkariumBattle,OrbisPQ,HenesysPQ,Juliet,Dragonica,Pirate,BossQuestEASY,BossQuestMED,BossQuestHARD,BossQuestHELL,Ellin,"
            + "HorntailBattle,LudiPQ,KerningPQ,ZakumBattle,MV,MVBattle,DollHouse,Amoria,CWKPQ,BossBalrog_EASY,BossBalrog_NORMAL,PinkBeanBattle,ZakumPQ,ProtectTylus,ChaosHorntail,"
            + "ChaosZakum,Ravana_EASY,Ravana_HARD,Ravana_MED,GuildQuest";
    
    public static int GLOBAL_EXP_RATE = 5;
    public static int GLOBAL_MESO_RATE = 3;
    public static int GLOBAL_DROP_RATE = 2; // Default: 2
    public static int GLOBAL_CASH_RATE = 1; // Default: 3
    public static int GLOBAL_TRAIT_RATE = 3; // Default: 3
    public static boolean GLOBAL_RATES = true; // When true, all worlds use the above rates
    
    public static List<Pair<Integer, Byte>> flag = new LinkedList<>();
    public enum Flags { None((byte)0), Event((byte)1), New((byte)2), Hot((byte)3);
        final byte id;
        private Flags(byte flagId) {
            id = flagId;
        }

        public byte getId() {
            return id;
        }
    }
    public static List<Pair<Integer, Integer>> expRates = new LinkedList<>();
    public static List<Pair<Integer, Integer>> mesoRates = new LinkedList<>();
    public static List<Pair<Integer, Integer>> dropRates = new LinkedList<>();
    public static List<Pair<Integer, String>> eventMessages = new LinkedList<>();
    
    public static void init() {
        // Flags
        flag.add(new Pair<>(Server.Scania.getId(), Flags.None.getId())); // Default World
        flag.add(new Pair<>(Server.Bera.getId(), Flags.Hot.getId()));
        flag.add(new Pair<>(Server.Broa.getId(), Flags.Event.getId()));
        flag.add(new Pair<>(Server.Windia.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Khaini.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Bellocan.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Mardia.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Kradia.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Yellonde.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Demethos.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Galicia.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.El_Nido.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Zenith.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Arcania.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Chaos.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Nova.getId(), Flags.New.getId()));
        flag.add(new Pair<>(Server.Renegades.getId(), Flags.New.getId()));
        
        // Exp rates
        expRates.add(new Pair<>(Server.Scania.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Bera.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Broa.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Windia.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Khaini.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Bellocan.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Mardia.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Kradia.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Yellonde.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Demethos.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Galicia.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.El_Nido.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Zenith.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Arcania.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Chaos.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Nova.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        expRates.add(new Pair<>(Server.Renegades.getId(), (GLOBAL_RATES ? GLOBAL_EXP_RATE : 5)));
        
        // Meso rates
        mesoRates.add(new Pair<>(Server.Scania.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Bera.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Broa.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Windia.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Khaini.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Bellocan.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Mardia.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Kradia.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Yellonde.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Demethos.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Galicia.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.El_Nido.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Zenith.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Arcania.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Chaos.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Nova.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        mesoRates.add(new Pair<>(Server.Renegades.getId(), (GLOBAL_RATES ? GLOBAL_MESO_RATE : 3)));
        
        // Drop rates
        dropRates.add(new Pair<>(Server.Scania.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Bera.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Broa.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Windia.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Khaini.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Bellocan.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Mardia.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Kradia.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Yellonde.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Demethos.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Galicia.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.El_Nido.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Zenith.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Arcania.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Chaos.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Nova.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        dropRates.add(new Pair<>(Server.Renegades.getId(), (GLOBAL_RATES ? GLOBAL_DROP_RATE : 2)));
        
        // Event messages
        eventMessages.add(new Pair<>(Server.Scania.getId(), ("Welcome to #bDevelopment!#k\r\n#rTip:#k\r\n" + ServerConstants.getTip())));
        eventMessages.add(new Pair<>(Server.Bera.getId(), "Bera!"));
        eventMessages.add(new Pair<>(Server.Broa.getId(), "Broa!"));
        eventMessages.add(new Pair<>(Server.Windia.getId(), "Windia!"));
        eventMessages.add(new Pair<>(Server.Khaini.getId(), "Khaini!"));
        eventMessages.add(new Pair<>(Server.Bellocan.getId(), "Bellocan!"));
        eventMessages.add(new Pair<>(Server.Mardia.getId(), "Mardia!"));
        eventMessages.add(new Pair<>(Server.Kradia.getId(), "Kradia!"));
        eventMessages.add(new Pair<>(Server.Yellonde.getId(), "Yellonde!"));
        eventMessages.add(new Pair<>(Server.Demethos.getId(), "Demethos!"));
        eventMessages.add(new Pair<>(Server.Galicia.getId(), "Galicia!"));
        eventMessages.add(new Pair<>(Server.El_Nido.getId(), "El Nido!"));
        eventMessages.add(new Pair<>(Server.Zenith.getId(), "Zenith!"));
        eventMessages.add(new Pair<>(Server.Arcania.getId(), "Arcania!"));
        eventMessages.add(new Pair<>(Server.Chaos.getId(), "Chaos!"));
        eventMessages.add(new Pair<>(Server.Nova.getId(), "Nova!"));
        eventMessages.add(new Pair<>(Server.Renegades.getId(), "Renegades!"));
    }
}
