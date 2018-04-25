package constants;

import client.*;
import client.inventory.MapleInventoryType;
import client.inventory.MapleWeaponType;
import client.status.MonsterStatus;
import java.awt.Point;
import java.util.*;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.maps.MapleMapObjectType;
import tools.FileoutputUtil;
import tools.packet.CField;

public class GameConstants {

    public static boolean GMS = true; //true = GMS
    public static List<MapleMapObjectType> rangedMapobjectTypes = Collections.unmodifiableList(Arrays.asList(
            MapleMapObjectType.ITEM,
            MapleMapObjectType.MONSTER,
            MapleMapObjectType.DOOR,
            MapleMapObjectType.REACTOR,
            MapleMapObjectType.SUMMON,
            MapleMapObjectType.NPC,
            MapleMapObjectType.MIST,
            MapleMapObjectType.FAMILIAR,
            MapleMapObjectType.EXTRACTOR));
    public static final int[] itemBlock = {
        1102200, 1442191, 1442192, 1103008, 1103007, 1022197, 1032199, 1004008, 1004017, 1302996, 1012397, 1022999,
        1103992, 1442997, 1442999, 1303005, 1303006, 1303000, 1303001, 1303002, 1303004, 1053001, 1053099, 1052999, 1142999,
        1112139, 1102272, 1112999, 1112998, 
        2003561, 4001168, 5220013, 3993003, 2340000, 2049100, 4001129, 2040037, 2040006, 2040007, 2040303, 2040403, 2040506, 2040507, 2040603, 2040709, 2040710, 2040711, 2040806, 2040903, 2041024, 2041025, 2043003, 2043103, 2043203, 2043303, 2043703, 2043803, 2044003, 2044103, 2044203, 2044303, 2044403, 2044503, 2044603, 2044908, 2044815, 2044019, 2044703};
    // Monster Rider - [80001000], Double Jump - [3201003, 3101003, 13101004]
    public static final int[] blockedSkills = {3201003, 3101003, 13101004, 2301001,
        5001005, 5091005, 15001003, 4341003, 2221004, 2321003, 32121003, 5101004, 5111007, 5111010, 20020109};
    private static int[] exp = { // thanks to Brian for the correct formula <3
        0, 15, 34, 57, 92, 135, 372, 560, 840, 1242, 1242,
        1242, 1242, 1242, 1242, 1490, 1788, 2146, 2575, 3090, 3708, 
        4450, 5340, 6408, 7690, 9228, 11074, 13289, 15947, 19136, 19136, 
        19136, 19136, 19136, 19136, 22963, 27556, 33067, 39680, 47616, 51425, 
        55539, 59982, 64781, 69963, 75560, 81605, 88133, 95184, 102799, 111023, 
        119905, 129497, 139857, 151046, 163130, 176180, 190274, 205496, 221936, 239691, 
        258866, 279575, 301941, 326096, 352184, 380359, 410788, 443651, 479143, 479143, 
        479143, 479143, 479143, 479143, 512683, 548571, 586971, 628059, 672023, 719065, 
        769400, 823258, 880886, 942548, 1008526, 1079123, 1154662, 1235488, 1321972, 1414510, 
        1513526, 1619473, 1732836, 1854135, 1983924, 2122799, 2271395, 2430393, 2600521, 2782557, 
        2977336, 3185750, 3408753, 3647366, 3902682, 4175870, 4468181, 4780954, 5115621, 5473714, 
        5856874, 6266855, 6705535, 7174922, 7677167, 8214569, 8789589, 9404860, 10063200, 10063200, 
        10063200, 10063200, 10063200, 10063200, 10767624, 11521358, 12327853, 13190803, 14114159, 15102150, 
        16159301, 17290452, 18500784, 19795839, 21181548, 22664256, 24250754, 25948307, 27764688, 29708216, 
        31787791, 34012936, 36393842, 38941411, 41667310, 44584022, 47704904, 51044247, 54617344, 58440558, 
        62531397, 66908595, 71592197, 76603651, 81965907, 87703520, 93842766, 100411760, 107440583, 113887018, 
        120720239, 127963453, 135641260, 143779736, 152406520, 161550911, 171243966, 181518604, 192409720, 203954303, 
        216191561, 229163055, 242912838, 257487608, 272936864, 289313076, 306671861, 325072173, 344576503, 365251093, 
        387166159, 410396129, 435019897, 461121091, 488788356, 518115657, 549202596, 582154752, 617084037, 654109079, 
        693355624, 734956961, 779054379, 825797642, 875345501, 927866231, 983538205, 1042550497, 1105103527, 0};
    private static int[] closeness = {0, 1, 3, 6, 14, 31, 60, 108, 181, 287, 434, 632, 891, 1224, 1642, 2161, 2793,
        3557, 4467, 5542, 6801, 8263, 9950, 11882, 14084, 16578, 19391, 22547, 26074,
        30000};
    private static int[] setScore = {0, 10, 100, 300, 600, 1000, 2000, 4000, 7000, 10000};
        public static final int[] rankC = {70000000, 70000001, 70000002, 70000003, 70000004, 70000005, 70000006, 70000007, 70000008, 70000009, 70000010, 70000011, 70000012, 70000013};
    public static final int[] rankB = {70000014, 70000015, 70000016, 70000017, 70000018, 70000021, 70000022, 70000023, 70000024, 70000025, 70000026};
    public static final int[] rankA = {70000027, 70000028, 70000029, 70000030, 70000031, 70000032, 70000033, 70000034, 70000035, 70000036, 70000039, 70000040, 70000041, 70000042};
    public static final int[] rankS = {70000043, 70000044, 70000045, 70000047, 70000048, 70000049, 70000050, 70000051, 70000052, 70000053, 70000054, 70000055, 70000056, 70000057, 70000058, 70000059, 70000060, 70000061, 70000062};
    public static final int[] circulators = {2700000,2700100,2700200,2700300,2700400,2700500,2700600,2700700,2700800,2700900,2701000};
    private static int[] cumulativeTraitExp = {0, 20, 46, 80, 124, 181, 255, 351, 476, 639, 851, 1084,
        1340, 1622, 1932, 2273, 2648, 3061, 3515, 4014, 4563, 5128,
        5710, 6309, 6926, 7562, 8217, 8892, 9587, 10303, 11040, 11788,
        12547, 13307, 14089, 14883, 15689, 16507, 17337, 18179, 19034, 19902,
        20783, 21677, 22584, 23505, 24440, 25399, 26362, 27339, 28331, 29338,
        30360, 31397, 32450, 33519, 34604, 35705, 36823, 37958, 39110, 40279,
        41466, 32671, 43894, 45135, 46395, 47674, 48972, 50289, 51626, 52967,
        54312, 55661, 57014, 58371, 59732, 61097, 62466, 63839, 65216, 66597,
        67982, 69371, 70764, 72161, 73562, 74967, 76376, 77789, 79206, 80627,
        82052, 83481, 84914, 86351, 87792, 89237, 90686, 92139, 93596, 96000};
    private static int[] mobHpVal = {0, 15, 20, 25, 35, 50, 65, 80, 95, 110, 125, 150, 175, 200, 225, 250, 275, 300, 325, 350,
        375, 405, 435, 465, 495, 525, 580, 650, 720, 790, 900, 990, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800,
        1900, 2000, 2100, 2200, 2300, 2400, 2520, 2640, 2760, 2880, 3000, 3200, 3400, 3600, 3800, 4000, 4300, 4600, 4900, 5200,
        5500, 5900, 6300, 6700, 7100, 7500, 8000, 8500, 9000, 9500, 10000, 11000, 12000, 13000, 14000, 15000, 17000, 19000, 21000, 23000,
        25000, 27000, 29000, 31000, 33000, 35000, 37000, 39000, 41000, 43000, 45000, 47000, 49000, 51000, 53000, 55000, 57000, 59000, 61000, 63000,
        65000, 67000, 69000, 71000, 73000, 75000, 77000, 79000, 81000, 83000, 85000, 89000, 91000, 93000, 95000, 97000, 99000, 101000, 103000,
        105000, 107000, 109000, 111000, 113000, 115000, 118000, 120000, 125000, 130000, 135000, 140000, 145000, 150000, 155000, 160000, 165000, 170000, 175000, 180000,
        185000, 190000, 195000, 200000, 205000, 210000, 215000, 220000, 225000, 230000, 235000, 240000, 250000, 260000, 270000, 280000, 290000, 300000, 310000, 320000,
        330000, 340000, 350000, 360000, 370000, 380000, 390000, 400000, 410000, 420000, 430000, 440000, 450000, 460000, 470000, 480000, 490000, 500000, 510000, 520000,
        530000, 550000, 570000, 590000, 610000, 630000, 650000, 670000, 690000, 710000, 730000, 750000, 770000, 790000, 810000, 830000, 850000, 870000, 890000, 910000};
    private static int[] pvpExp = {0, 3000, 6000, 12000, 24000, 48000, 960000, 192000, 384000, 768000};
    private static int[] guildexp = {0, 20000, 160000, 540000, 1280000, 2500000, 4320000, 6860000, 10240000, 14580000};
    private static int[] mountexp = {0, 6, 25, 50, 105, 134, 196, 254, 263, 315, 367, 430, 543, 587, 679, 725, 897, 1146, 1394, 1701, 2247,
        2543, 2898, 3156, 3313, 3584, 3923, 4150, 4305, 4550};
    //public static int[] itemBlock = {4001168, 5220013, 3993003, 2340000, 2049100, 4001129, 2040037, 2040006, 2040007, 2040303, 2040403, 2040506, 2040507, 2040603, 2040709, 2040710, 2040711, 2040806, 2040903, 2041024, 2041025, 2043003, 2043103, 2043203, 2043303, 2043703, 2043803, 2044003, 2044103, 2044203, 2044303, 2044403, 2044503, 2044603, 2044908, 2044815, 2044019, 2044703};
    public static int[] cashBlock = {
        1112001, 1112002, 1112003, 1112005, 1112006, 1112007, 1112800, 1112801, 1112802, 1112810, 1112812, 1112816,
        // Booster Pack, Legendary Booster Pack, and Chair Gachapon
        5220084, 5220092, 5680021, 
        // Guardian Scroll and Shield Scrolls
        5064300, 5064301, 5064100, 5064101, 5063100, 5064002, 5064000, 
        // MAPLE ASCENSION BLOCKS
        5500005, 5500006, 5500000, 5500001, 5500002, 5050000, 5050100, 5062005, 5062002, 5062003, 5062000, 5062001, 5062100,
        5062300, 5064200, 5064201, 5451000, 5220020, 5220010, 5220000, 5220094, 5220082, 5680021, 5640000, 5570000, 
        5251000, 5251001, 5251002, 5251003, 5251004, 5251005, 5251006, 5251016, 5120049, 
        5040008, 5040000, 5040003, 5040004, 5040006, 5040007, 5041007, 5041006, 5041005, 5041004, 5041003, 5041002, 50410001, 5041000, 
        2531000, 5140006, 
        // 2x EXP & DROPS
        5360000, 5360001, 5360002, 5360003, 5360004, 5360005, 5360006, 5360007, 5360008, 
        5211008, 5211009, 5211010, 5211011, 5211012, 5211013, 5211014, 5211015, 5211016, 5211017, 5211018,
        5211037, 5211038, 5211039, 5211046, 5211045, 5211044, 5211043, 5211042, 5211041, 5211040, 5211049,
        5211000, 5211007, 5211006, 5211005, 5211004, 5710000, 5530062, 5530107, 5530106, 5530108, 
        5080001, 5080000, 5063000, 5064000, 5660000, 5660001, 5222027, 5530172, 5530173, 5530174, 5530175, 5530176, 5530177, 5251016, 5534000, 5152053, 5152058, 5150044, 5150040, 5220082, 5680021, 5150050, 5211091, 5211092, 5220087, 5220088, 5220089, 5220090, 5220085, 5220086, 5470000, 1002971, 1052202, 5060003, 5060004, 5680015, 5220082, 5530146, 5530147, 5530148, 5710000, 5500000, 5500001, 5500002, 5500002, 5500003, 5500004, 5500005, 5500006, 5050000, 5075000, 5075001, 5075002, 1122121, 5450000, 5190005, 5190007, 5600000, 5600001, 5350003, 2300002, 2300003, 5330000, 5062000, 5211073, 5211074, 5211075, 5211076, 5211077, 5211078, 5211079, 5650000, 5431000, 5431001, 5432000, 5450000, 5550000, 5550001, 5640000, 5530013, 5150039, 5150040, 5150046, 5150054, 5150052, 5150053, 5151035, 5151036, 5152053, 5152056, 5152057, 5152058, 1812006, 5650000, 5222000, 5221001, 5220014, 5220015, 5420007, 5451000,
        5210000, 5210001, 5210002, 5210003, 5210004, 5210005, 5210006, 5210007, 5210008, 5210009, 5210010, 5210011, 5211000, 5211001, 5211002, 5211003, 5211004, 5211005, 5211006, 5211007, 5211008, 5211009, 5211010, 5211011, 5211012, 5211013, 5211014, 5211015, 5211016, 5211017, 5211018,
        5211019, 5211020, 5211021, 5211022, 5211023, 5211024, 5211025, 5211026, 5211027, 5211028, 5211029, 5211030, 5211031, 5211032, 5211033, 5211034, 5211035, 5211036, 5211037, 5211038, 5211039, 5211040, 5211041, 5211042, 5211043,
        5211044, 5211045, 5211046, 5211047, 5211048, 5211049, 5211050, 5211051, 5211052, 5211053, 5211054, 5211055, 5211056, 5211057, 5211058, 5211059, 5211060, 5211061,//2x exp
        5360000, 5360001, 5360002, 5360003, 5360004, 5360005, 5360006, 5360007, 5360008, 5360009, 5360010, 5360011, 5360012, 5360013, 5360014, 5360017, 5360050, 5211050, 5360042, 5360052, 5360053, 5360050, //2x drop
        1112810, 1112811, 5530013, 4001431, 4001432, 4032605,
        5140000, 5140001, 5140002, 5140003, 5140004, 5140007, //stores
        5270000, 5270001, 5270002, 5270003, 5270004, 5270005, 5270006, //2x meso
        9102328, 9102329, 9102330, 9102331, 9102332, 9102333, 5211000, 5211048, 5211048, 5211014, 5211015, 5211016, 5211017, 5211018, //miracle cube and stuff
        9102855, 9102541, 9102542, 9102584, 9102483, 9102484, 9102911, 9102912
    };
    public static int JAIL = 30, MAX_BUFFSTAT = 8;
    public static String[] RESERVED = {"Rental", "Donor","MapleNews"};
    public static final int[] JAIL_MAPS = {30, 90000000, 90000001, 90000002, 90000003, 90000004, 90000009}; // 280090000
    public static String[] stats = {"tuc", "reqLevel", "reqJob", "reqSTR", "reqDEX", "reqINT", "reqLUK", "reqPOP", "cash", "cursed", "success", "setItemID", "equipTradeBlock", "durability", "randOption", "randStat", "masterLevel", "reqSkillLevel", "elemDefault", "incRMAS", "incRMAF", "incRMAI", "incRMAL", "canLevel", "skill", "charmEXP"};
    public static final int[] hyperTele = {10000, 20000, 30000, 40000, 50000, 1000000, 1010000, 1020000, 2000000, //Maple Island
        104000000, 104010000, 104010100, 104010200, 104020000, 103010100, 103010000, 103000000, 103050000, 103020000, 103020020, 103020100, 103020200, 103020300, 103020310, 103020320, 103020400, 103020410, 103020420, 103030000, 103030100, 103030200, 103030300, 103030400, 102000000, 102010000, 102010100, 102020000, 102020100, 102020200, 102020300, 102020400, 102020500, 102040000, 102040100, 102040200, 102040300, 102040400, 102040500, 102040600, 102030000, 102030100, 102030200, 102030300, 102030400, 101000000, 101010000, 101010100, 101020000, 101020100, 101020200, 101020300, 101030000, 101030100, 101030200, 101030300, 101030400, 101030500, 101030101, 101030201, 101040000, 101040100, 101040200, 101040300, 101040310, 101040320, 101050000, 101050400, 100000000, 100010000, 100010100, 100020000, 100020100, 100020200, 100020300, 100020400, 100020500, 100020401, 100020301, 100040000, 100040100, 100040200, 100040300, 100040400, 100020101, 106020000, 120010100, 120010000, 120000000, 120020000, 120020100, 120020200, 120020300, 120020400, 120020500, 120020600, 120020700, 120030000, 120030100, 120030200, 120030300, 120030400, 120030500, //Victoria Island
        105000000, 105010000, 105010100, 105020000, 105020100, 105020200, 105020300, 105020400, 105020500, 105030000, 105030100, 105030200, 105030300, 105030400, 105030500, 105100000, 105100100, //Sleepy Wood
        120000100, 120000101, 120000102, 120000103, 120000104, 120000201, 120000202, 120000301, //Nautilus
        103040000, 103040100, 103040101, 103040102, 103040103, 103040200, 103040201, 103040202, 103040203, 103040300, 103040301, 103040302, 103040303, 103040400, //Kerning Square
        200000000, 200010000, 200010100, 200010110, 200010120, 200010130, 200010111, 200010121, 200010131, 200010200, 200010300, 200010301, 200010302, 200020000, 200030000, 200040000, 200050000, 200060000, 200070000, 200080000, 200000100, 200000200, 200000300, 200100000, 200080100, 200080200, 200081500, 200082200, 200082300, 211000000, 211000100, 211000200, 211010000, 211020000, 211030000, 211040000, 211050000, 211040100, 211040200, 921120000, //Orbis
        211040300, 211040400, 211040500, 211040600, 211040700, 211040800, 211040900, 211041000, 211041100, 211041200, 211041300, 211041400, 211041500, 211041600, 211041700, 211041800, 211041900, 211042000, 211042100, 211042200, 211042300, 211042400, 280030000, 211060000, //Dead Mine
        211060010, 211060100, 211060200, 211060201, 211060300, 211060400, 211060401, 211060410, 211060500, 211060600, 211060601, 211060610, 211060620, 211060700, 211060800, 211060801, 211060810, 211060820, 211060830, 211060900, 211061000, 211061001, 211070000, //Lion King's Castle
        220000000, 220000100, 220000300, 220000400, 220000500, 220010000, 220010100, 220010200, 220010300, 220010400, 220010500, 220010600, 220010700, 220010800, 220010900, 220011000, 220020000, 220020100, 220020200, 220020300, 220020400, 220020500, 220020600, 220030100, 220030200, 220030300, 220030400, 220030000, 220040000, 220040100, 220040200, 220040300, 220040400, 220050000, 220050100, 220050200, 221023200, 221022300, 221022200, 221021700, 221021600, 221021100, 221020000, 221000000, 221030000, 221030100, 221030200, 221030300, 221030400, 221030500, 221030600, 221040000, 221040100, 221040200, 221040300, 221040400, 222000000, 222010000, 222010001, 222010002, 222010100, 222010101, 222010102, 222010200, 222010201, 222010300, 222010400, 222020300, 222020200, 222020100, 222020000, //Ludas Lake
        220050300, 220060000, 220060100, 220060200, 220060300, 220060400, 220070000, 220070100, 220070200, 220070300, 220070400, 220080000, 220080001, //Clock Tower Lower Floor
        300000100, 300000000, 300010000, 300010100, 300010200, 300010400, 300020000, 300020100, 300020200, 300030000, 300030100, 300010410, 300020210, 300030200, 300030300, 300030310, //Ellin Forest
        230010000, 230010100, 230010200, 230010201, 230010300, 230010400, 230020000, 230020100, 230020200, 230020201, 230020300, 230030000, 230030100, 230030101, 230030200, 230040000, 230040100, 230040200, 230040300, 230040400, 230040410, 230040420, 230000000, //Aqua Road
        250000000, 250000100, 250010000, 250010100, 250010200, 250010300, 250010301, 250010302, 250010303, 250010304, 250010400, 250010500, 250010501, 250010502, 250010503, 250010600, 250010700, 250020000, 250020100, 250020200, 250020300, 251000000, 251000100, 251010000, 251010200, 251010300, 251010400, 251010401, 251010402, 251010403, 251010500, //Mu Lung Garden
        240010100, 240010200, 240010300, 240010400, 240010500, 240010600, 240010700, 240010800, 240010900, 240011000, 240020000, 240020100, 240020101, 240020200, 240020300, 240020400, 240020401, 240020500, 240030000, 240030100, 240030101, 240030102, 240030200, 240030300, 240040000, 240040100, 240040200, 240040300, 240040400, 240040500, 240040510, 240040511, 240040520, 240040521, 240040600, 240040700, 240050000, 240010000, 240000000, //Minar Forest
        240070000, 240070010, 240070100, 240070200, 240070300, 240070400, 240070500, 240070600, //Neo City
        260010000, 260010100, 260010200, 260010300, 260010400, 260010500, 260010600, 260010700, 260020000, 260020100, 260020200, 260020300, 260020400, 260020500, 260020600, 260020610, 260020620, 260020700, 261000000, 260000000, 926010000, 261010000, 261010001, 261010002, 261010003, 261010100, 261010101, 261010102, 261010103, 261020000, 261020100, 261020200, 261020300, 261020400, 261020500, 261020600, 261020700, //Nihal Desert
        270000000, 270000100, 270010000, 270010100, 270010110, 270010111, 270010200, 270010210, 270010300, 270010310, 270010400, 270010500, 270020000, 270020100, 270020200, 270020210, 270020211, 270020300, 270020310, 270020400, 270020410, 270020500, 270030000, 270030100, 270030110, 270030200, 270030210, 270030300, 270030310, 270030400, 270030410, 270030411, 270030500, 270040000, 270050000, //Temple of Time
        271000000, 271000100, 271000200, 271000210, 271000300, 271020000, 271020100, 271010000, 271010100, 271010200, 271010300, 271010301, 271010400, 271010500, 271030000, 271030100, 271030101, 271030102, 271030200, 271030201, 271030300, 271030310, 271030320, 271030400, 271030410, 271030500, 271030510, 271030520, 271030530, 271030540, 271030600, 271040000, 271040100, //Gate of Future
        130000000, 130000100, 130000110, 130000120, 130000200, 130000210, 130010000, 130010010, 130010020, 130010100, 130010110, 130010120, 130010200, 130010210, 130010220, 130020000, 130030005, 130030006, 130030000, //Ereve
        140000000, 140010000, 140010100, 140010200, 140020000, 140020100, 140020200, 140030000, 140090000, 140020300, //Rien
        310000000, 310000010, 310020000, 310020100, 310020200, 310030000, 310030100, 310030110, 310030200, 310030300, 310030310, 310040000, 310040100, 310040110, 310040200, 310040300, 310040400, 310050000, 310050100, 310050200, 310050300, 310050400, 310050500, 310050510, 310050520, 310050600, 310050700, 310050800, 310060000, 310060100, 310060110, 310060120, 310060200, 310060210, 310060220, 310060300, 310010000//Edelstein
        };  
    private static final int[] jqexp = {0, 100, 300, 750, 1200, 1900, 2500, 3000, 4000, 5000, 10000};
    private static final int[] occexp = {0, 119905, 586971, 1321972, 3185749, 81965862, 387193381, 927931469, 1042623799, 1105181227, 1711492101, Integer.MAX_VALUE}; // everything passed 10 is 2.1b exp :|
    //Maximum damage to be dealt by OHKO skills.
    public static final int OHKODamage = 9999999;
    /*
     * DURABILITY & ITEM EXP CONSTANTS
     */
    //Durability loss per attack.
    public static final int durabilityPerAttack = -2;
    //Durability loss on character death.
    public static final int durabilityPerDeath = -10000;
    //Global multiplier for item EXP gains.
    public static final double itemEXPRate = 0.8;
    /*
     * END DURABILITY & ITEM EXP SECTION
     */

    /*
     * Specifics which job gives an additional EXP to party
     * returns the percentage of EXP to increase
     */
    public static byte Class_Bonus_EXP(final int job) {
        switch (job) {
            case 501:
            case 530:
            case 531:
            case 532:
            case 2300:
            case 2310:
            case 2311:
            case 2312:
            case 3100:
            case 3110:
            case 3111:
            case 3112:
            case 800:
            case 900:
            case 910:
                return 10;
        }
        return 0;
    }
    
    public static int getExpNeededForLevel(int level) {
        if (level < 0 || level >= exp.length) {
            return Integer.MAX_VALUE;
        }
        return exp[level];
    }
    
    public static int getExpNeededForOccLevel(final int occlevel) {
        if (occlevel < 0 || occlevel >= occexp.length) {
            return Integer.MAX_VALUE;
        }
        return occexp[occlevel];
    }
    
    public static boolean isJail(int mapid) {
        boolean jailed = false;
        for (int i = 0; i < JAIL_MAPS.length; i++) {
            if (mapid == JAIL_MAPS[i]) {
                jailed = true;
            }
        }
        return jailed;
    }
    
    public static int getExpNeededForJQLevel(final int jqlevel) {
        if (jqlevel < 0 || jqlevel >= jqexp.length) {
            return Integer.MAX_VALUE;
        }
        return jqexp[jqlevel];
    }
    
    /*      */   public static int getSkillLevel(int level) {
/* 3604 */     if ((level >= 70) && (level < 120))
/* 3605 */       return 2;
/* 3606 */     if ((level >= 120) && (level < 200))
/* 3607 */       return 3;
/* 3608 */     if (level == 200) {
/* 3609 */       return 4;
/*      */     }
/* 3611 */     return 1;
/*      */   }

    public static int getGuildExpNeededForLevel(int level) {
        if (level < 0 || level >= guildexp.length) {
            return Integer.MAX_VALUE;
        }
        return guildexp[level];
    }
    
    /*      */   public static boolean canBeStolen(int a1) {
/* 3615 */     return canBeStolen(a1, 0);
/*      */   }
/*      */ 
/*      */   public static boolean canBeStolen(int a1, int targetJob) {
/* 3619 */     if (a1 >= 90000000) {
/* 3620 */       return false;
/*      */     }
/* 3622 */     switch (a1) {
/*      */     case 1101004:
case 5211011:
/*      */     case 1121000:
/*      */     case 1121010:
/*      */     case 1121011:
/*      */     case 1201004:
/*      */     case 1211002:
/*      */     case 1211004:
/*      */     case 1211006:
/*      */     case 1221000:
/*      */     case 1221004:
/*      */     case 1221012:
/*      */     case 1301004:
/*      */     case 1321000:
/*      */     case 1321010:
/*      */     case 2111005:
/*      */     case 2121000:
/*      */     case 2121004:
/*      */     case 2121008:
/*      */     case 2221004:
/*      */     case 2221008:
/*      */     case 2311006:
/*      */     case 2311007:
/*      */     case 2321000:
/*      */     case 2321004:
/*      */     case 2321009:
/*      */     case 3101002:
/*      */     case 3101004:
/*      */     case 3121000:
/*      */     case 3121007:
/*      */     case 3121009:
/*      */     case 3201004:
/*      */     case 3221006:
/*      */     case 3221008:
/*      */     case 4111002:
/*      */     case 4111009:
/*      */     case 4121009:
/*      */     case 4201002:
/*      */     case 4221008:
/*      */     case 4341008:
/*      */     case 5001005:
/*      */     case 5111002:
/*      */     case 5121008:
/*      */     case 5211009:
/*      */     case 5221010:
/*      */     case 5921003:
case 5211015:
/*      */     case 5921010:
/* 3669 */       return false;
/*      */     }
/* 3671 */     int job = a1 / 10000;
/* 3672 */     if (((targetJob > 0) && (!isJobFamily(job, targetJob))) || (!isAdventurer(job)) || (isCannon(job)) || (isJett(job))) {
/* 3673 */       return false;
/*      */     }
/* 3675 */     int v1 = a1 / 1000 % 10;
/* 3676 */     if ((v1 <= 0) || (v1 == 9) || (job / 1000 > 0) || (a1 / 10000 / 100 == 24) || (a1 / 10000 / 1000 > 0) || (a1 / 10000 == 2003) || (a1 % 1000 <= 0) || (a1 == 2001) || (a1 == 2002) || (a1 == 3001) || (a1 == 2003) || (a1 / 10 == 43) || (a1 / 10 == 53) || (a1 == 501) || (a1 % 1000 / 100 == 9) || (!isAdventurer(a1 / 10000))) {
/* 3677 */       return false;
/*      */     }
/* 3679 */     return isApplicableSkill(a1);
/*      */   }
/*      */ 
/*      */   public static int getPhantomBookSlot(int i) {
/* 3683 */     switch (i) {
/*      */     case 1:
/*      */     case 2:
/* 3686 */       return 4;
/*      */     case 3:
/* 3688 */       return 3;
/*      */     case 4:
/* 3690 */       return 2;
/*      */     }
/* 3692 */     return 0;
/*      */   }
/*      */ 
/*      */   public static int getPhantomBookSkill(int slot) {
/* 3696 */     switch (slot) {
/*      */     case 1:
/* 3698 */       return 24001001;
/*      */     case 2:
/* 3700 */       return 24101001;
/*      */     case 3:
/* 3702 */       return 24111001;
/*      */     case 4:
/* 3704 */       return 24121001;
/*      */     }
/* 3706 */     return 0;
/*      */   }
/*      */ 
/*      */   public static int getPhantomBook(int skillid) {
/* 3710 */     int jobid = skillid / 10000;
/* 3711 */     if (jobid % 100 == 0)
/* 3712 */       return 1;
/* 3713 */     if (jobid % 10 == 2)
/* 3714 */       return 4;
/* 3715 */     if (jobid % 10 == 1)
/* 3716 */       return 3;
/* 3717 */     if (jobid % 10 == 0) {
/* 3718 */       return 2;
/*      */     }
/* 3720 */     return 0;
/*      */   }

    public static int getPVPExpNeededForLevel(int level) {
        if (level < 0 || level >= pvpExp.length) {
            return Integer.MAX_VALUE;
        }
        return pvpExp[level];
    }

    public static int getClosenessNeededForLevel(int level) {
        return closeness[level - 1];
    }

    public static int getMountExpNeededForLevel(int level) {
        return mountexp[level - 1];
    }

    public static int getTraitExpNeededForLevel(int level) {
        if (level < 0 || level >= cumulativeTraitExp.length) {
            return Integer.MAX_VALUE;
        }
        return cumulativeTraitExp[level];
    }

    public static int getSetExpNeededForLevel(int level) {
        if (level < 0 || level >= setScore.length) {
            return Integer.MAX_VALUE;
        }
        return setScore[level];
    }

    public static int getMonsterHP(int level) {
        if (level < 0 || level >= mobHpVal.length) {
            return Integer.MAX_VALUE;
        }
        return mobHpVal[level];
    }

    public static int getBookLevel(int level) {
        return (int) ((5 * level) * (level + 1));
    }

    public static int getTimelessRequiredEXP(int level) {
        return 70 + (level * 10);
    }

    public static int getReverseRequiredEXP(int level) {
        return 60 + (level * 5);
    }

    public static int getProfessionEXP(int level) {
        return ((100 * level * level) + (level * 400)) / 2;
    }

    public static boolean isHarvesting(int itemId) {
        return itemId >= 1500000 && itemId < 1520000;
    }

    public static int maxViewRangeSq() {
        return 1000000; // 1024 * 768
    }

    public static int maxViewRangeSq_Half() {
        return 500000; // 800 * 800
    }

    public static boolean isJobFamily(int baseJob, int currentJob) {
        return currentJob >= baseJob && currentJob / 100 == baseJob / 100;
    }

    public static boolean isKOC(int job) {
        return job >= 1000 && job < 2000;
    }

    public static boolean isEvan(int job) {
        return job == 2001 || (job >= 2200 && job <= 2218);
    }

    public static boolean isMercedes(int job) {
        return job == 2002 || (job >= 2300 && job <= 2312);
    }

    public static boolean isDemon(int job) {
        return job == 3001 || (job >= 3100 && job <= 3112);
    }

    public static boolean isAran(int job) {
        return job >= 2000 && job <= 2112 && job != 2001 && job != 2002;
    }

    public static boolean isResist(int job) {
        return job >= 3000 && job <= 3512;
    }

    public static boolean isAdventurer(int job) {
        return job >= 0 && job < 1000;
    }

    public static boolean isCannon(int job) {
        return job == 1 || job == 501 || (job >= 530 && job <= 532);
    }

    public static boolean isRecoveryIncSkill(int id) {
        switch (id) {
            case 1110000:
            case 2000000:
            case 1210000:
            case 11110000:
            case 4100002:
            case 4200001:
                return true;
        }
        return false;
    }

    public static boolean isLinkedAranSkill(int id) {
        return getLinkedAranSkill(id) != id;
    }

/*      */   public static int getLinkedAranSkill(int id) {
/*  263 */     switch (id) {
/*      */     case 21110007:
/*      */     case 21110008:
/*  266 */       return 21110002;
/*      */     case 21120009:
/*      */     case 21120010:
/*  269 */       return 21120002;
/*      */     case 4321001:
/*  271 */       return 4321000;
/*      */     case 33101006:
/*      */     case 33101007:
/*  274 */       return 33101005;
/*      */     case 33101008:
/*  276 */       return 33101004;
/*      */     case 35101009:
/*      */     case 35101010:
/*  279 */       return 35100008;
/*      */     case 35111009:
/*      */     case 35111010:
/*  282 */       return 35111001;
/*      */     case 35121013:
/*  284 */       return 35111004;
/*      */     case 35121011:
/*  286 */       return 35121009;
/*      */     case 32001007:
/*      */     case 32001008:
/*      */     case 32001009:
/*      */     case 32001010:
/*      */     case 32001011:
/*  292 */       return 32001001;
/*      */     case 5300007:
/*  294 */       return 5301001;
/*      */     case 5320011:
/*  296 */       return 5321004;
/*      */     case 23101007:
/*  298 */       return 23101001;
/*      */     case 23111009:
/*      */     case 23111010:
/*  301 */       return 23111008;
    /*      */     case 5211015:
/*      */     case 5211016:
/*  301 */       return 5211011;
/*      */     case 31001006:
/*      */     case 31001007:
/*      */     case 31001008:
/*  305 */       return 31000004;
/*      */     case 30010183:
/*      */     case 30010184:
/*      */     case 30010186:
/*  309 */       return 30010110;
/*      */     case 5710012:
/*  311 */       return 5711002;
/*      */     case 24111008:
/*  313 */       return 24111006;
/*      */     case 24121010:
/*  315 */       return 24121003;
/*      */     case 5001008:
/*  317 */       return 5200010;
/*      */     case 5001009:
/*  319 */       return 5101004;
/*      */     }
/*  321 */     return id;
/*      */   }


   public static boolean isForceIncrease(int skillid) {
        switch (skillid) {
            case 31000004:
            case 31001006:
            case 31001007:
            case 31001008:

            case 30010166:
            case 30011167:
            case 30011168:
            case 30011169:
            case 30011170:
                return true;
        }
        return false;
    }

    public static int getBOF_ForJob(int job) {
        return PlayerStats.getSkillByJob(12, job);
    }

    public static int getEmpress_ForJob(int job) {
        return PlayerStats.getSkillByJob(73, job);
    }

    public static boolean isElementAmp_Skill(int skill) {
        switch (skill) {
            case 2110001:
            case 2210001:
            case 12110001:
            case 22150000:
                return true;
        }
        return false;
    }

    public static int getMPEaterForJob(int job) {
        switch (job) {
            case 210:
            case 211:
            case 212:
                return 2100000;
            case 220:
            case 221:
            case 222:
                return 2200000;
            case 230:
            case 231:
            case 232:
                return 2300000;
        }
        return 2100000; // Default, in case GM
    }

    public static int getJobShortValue(int job) {
        if (job >= 1000) {
            job -= (job / 1000) * 1000;
        }
        job /= 100;
        if (job == 4) { // For some reason dagger/ claw is 8.. IDK
            job *= 2;
        } else if (job == 3) {
            job += 1;
        } else if (job == 5) {
            job += 11; // 16
        }
        return job;
    }

    public static boolean isPyramidSkill(int skill) {
        return isBeginnerJob(skill / 10000) && skill % 10000 == 1020;
    }

    public static boolean isInflationSkill(int skill) {
        return isBeginnerJob(skill / 10000) && skill % 10000 == 1092;
    }

    public static boolean isMulungSkill(int skill) {
        return isBeginnerJob(skill / 10000) && (skill % 10000 == 1009 || skill % 10000 == 1010 || skill % 10000 == 1011);
    }

    public static boolean isIceKnightSkill(int skill) {
        return isBeginnerJob(skill / 10000) && (skill % 10000 == 1098 || skill % 10000 == 99 || skill % 10000 == 100 || skill % 10000 == 103 || skill % 10000 == 104 || skill % 10000 == 1105);
    }

    public static boolean isThrowingStar(int itemId) {
        return itemId / 10000 == 207;
    }

    public static boolean isBullet(int itemId) {
        return itemId / 10000 == 233;
    }

    public static boolean isRechargable(int itemId) {
        return isThrowingStar(itemId) || isBullet(itemId);
    }

    public static boolean isOverall(int itemId) {
        return itemId / 10000 == 105;
    }

    public static boolean isPet(int itemId) {
        return itemId / 10000 == 500;
    }

    public static boolean isArrowForCrossBow(int itemId) {
        return itemId >= 2061000 && itemId < 2062000;
    }

    public static boolean isArrowForBow(int itemId) {
        return itemId >= 2060000 && itemId < 2061000;
    }

    public static boolean isMagicWeapon(int itemId) {
        int s = itemId / 10000;
        return s == 137 || s == 138;
    }

    public static boolean isWeapon(int itemId) {
        return itemId >= 1300000 && itemId < 1600000;
    }

    public static MapleInventoryType getInventoryType(int itemId) {
        byte type = (byte) (itemId / 1000000);
        if (type < 1 || type > 5) {
            return MapleInventoryType.UNDEFINED;
        }
        return MapleInventoryType.getByType(type);
    }

    public static boolean isInBag(int slot, byte type) {
        return ((slot >= 101 && slot <= 512) && type == MapleInventoryType.ETC.getType());
    }

    public static MapleWeaponType getWeaponType(int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        switch (cat) { // 39, 50, 51 ??
            case 30:
                return MapleWeaponType.SWORD1H;
            case 31:
                return MapleWeaponType.AXE1H;
            case 32:
                return MapleWeaponType.BLUNT1H;
            case 33:
                return MapleWeaponType.DAGGER;
            case 34:
                return MapleWeaponType.KATARA;
            case 35:
                return MapleWeaponType.MAGIC_ARROW; // can be magic arrow or cards
			case 36:
				return MapleWeaponType.CANE;
            case 37:
                return MapleWeaponType.WAND;
            case 38:
                return MapleWeaponType.STAFF;
            case 40:
                return MapleWeaponType.SWORD2H;
            case 41:
                return MapleWeaponType.AXE2H;
            case 42:
                return MapleWeaponType.BLUNT2H;
            case 43:
                return MapleWeaponType.SPEAR;
            case 44:
                return MapleWeaponType.POLE_ARM;
            case 45:
                return MapleWeaponType.BOW;
            case 46:
                return MapleWeaponType.CROSSBOW;
            case 47:
                return MapleWeaponType.CLAW;
            case 48:
                return MapleWeaponType.KNUCKLE;
            case 49:
                return MapleWeaponType.GUN;
            case 52:
                return MapleWeaponType.DUAL_BOW;
            case 53:
                return MapleWeaponType.CANNON;
        }
        return MapleWeaponType.NOT_A_WEAPON;
    }

    public static boolean isShield(int itemId) {
        int cat = itemId / 10000;
        cat = cat % 100;
        return cat == 9;
    }

    public static boolean isEquip(int itemId) {
        return itemId / 1000000 == 1;
    }

    public static boolean isCleanSlate(int itemId) {
        return itemId / 100 == 20490;
    }

    public static boolean isAccessoryScroll(int itemId) {
        return itemId / 100 == 20492;
    }
    
        public static boolean isInnocence(int itemId) {
        return itemId == 2049600 || itemId == 2049601 || itemId == 2049604;
    }

    public static boolean isChaosScroll(int itemId) {
        if (itemId >= 2049105 && itemId <= 2049110) {
            return false;
        }
        return itemId / 100 == 20491 || itemId == 2040126;
    }

    public static int getChaosNumber(int itemId) {
        return itemId == 2049116 ? 10 : 5;
    }

    public static boolean isEquipScroll(int scrollId) {
        return scrollId / 100 == 20493;
    }

    public static boolean isPotentialScroll(int scrollId) {
        return scrollId / 100 == 20494 || scrollId / 100 == 20497 || scrollId == 5534000;
    }

    public static boolean isSpecialScroll(int scrollId) {
        switch (scrollId) {
            case 2040727: // Spikes on show
            case 2041058: // Cape for Cold protection
            case 2530000:
            case 2530001:
            case 2531000:
            case 5063000:
            case 5064000:
            case 5064002:
            case 5064100:
            case 5064300:
            case 5064301:
                return true;
        }
        return false;
    }

    public static boolean isTwoHanded(int itemId) {
        switch (getWeaponType(itemId)) {
            case AXE2H:
            case GUN:
            case KNUCKLE:
            case BLUNT2H:
            case BOW:
            case CLAW:
            case CROSSBOW:
            case POLE_ARM:
            case SPEAR:
            case SWORD2H:
            case CANNON:
                //case DUAL_BOW: //magic arrow
                return true;
            default:
                return false;
        }
    }

    public static boolean isTownScroll(int id) {
        return id >= 2030000 && id < 2040000;
    }

    public static boolean isUpgradeScroll(int id) {
        return id >= 2040000 && id < 2050000;
    }

    public static boolean isGun(int id) {
        return id >= 1492000 && id < 1500000;
    }

    public static boolean isUse(int id) {
        return id >= 2000000 && id < 3000000;
    }

    public static boolean isSummonSack(int id) {
        return id / 10000 == 210;
    }

    public static boolean isMonsterCard(int id) {
        return id / 10000 == 238;
    }

    public static boolean isSpecialCard(int id) {
        return id / 1000 >= 2388;
    }

    public static int getCardShortId(int id) {
        return id % 10000;
    }

    public static boolean isGem(int id) {
        return id >= 4250000 && id <= 4251402;
    }

    public static boolean isOtherGem(int id) {
        switch (id) {
            case 4001174:
            case 4001175:
            case 4001176:
            case 4001177:
            case 4001178:
            case 4001179:
            case 4001180:
            case 4001181:
            case 4001182:
            case 4001183:
            case 4001184:
            case 4001185:
            case 4001186:
            case 4031980:
            case 2041058:
            case 2040727:
            case 1032062:
            case 4032334:
            case 4032312:
            case 1142156:
            case 1142157:
                return true; //mostly quest items
        }
        return false;
    }

    public static boolean isCustomQuest(int id) {
        return id > 99999;
    }

    public static int getTaxAmount(int meso) {
        if (meso >= 100000000) {
            return (int) Math.round(0.06 * meso);
        } else if (meso >= 25000000) {
            return (int) Math.round(0.05 * meso);
        } else if (meso >= 10000000) {
            return (int) Math.round(0.04 * meso);
        } else if (meso >= 5000000) {
            return (int) Math.round(0.03 * meso);
        } else if (meso >= 1000000) {
            return (int) Math.round(0.018 * meso);
        } else if (meso >= 100000) {
            return (int) Math.round(0.008 * meso);
        }
        return 0;
    }

    public static int EntrustedStoreTax(int meso) {
        if (meso >= 100000000) {
            return (int) Math.round(0.03 * meso);
        } else if (meso >= 25000000) {
            return (int) Math.round(0.025 * meso);
        } else if (meso >= 10000000) {
            return (int) Math.round(0.02 * meso);
        } else if (meso >= 5000000) {
            return (int) Math.round(0.015 * meso);
        } else if (meso >= 1000000) {
            return (int) Math.round(0.009 * meso);
        } else if (meso >= 100000) {
            return (int) Math.round(0.004 * meso);
        }
        return 0;
    }

    public static int getAttackDelay(int id, Skill skill) {
        switch (id) { // Assume it's faster(2)
            case 3121004: // Storm of Arrow
            case 23121000:
            case 33121009:
            case 13111002: // Storm of Arrow
            case 5221004: // Rapidfire
            case 5201006: // Recoil shot/ Back stab shot
            case 35121005:
            case 35111004:
            case 35121013:
                return 99; //reason being you can spam with assaulter
            case 14111005:
            case 4121007:
            case 5221007:
                return 99; //skip duh chek
            case 0: // Normal Attack, TODO delay for each weapon type
                return 570;
        }
        if (skill != null && skill.getSkillType() == 3) {
            return 0; //attack
        }
        if (skill != null && skill.getDelay() > 0 && !isNoDelaySkill(id)) {
            return skill.getDelay();
        }
        // TODO delay for attack, weapon type, swing,stab etc
        return 330; // Default usually
    }

    public static byte gachaponRareItem(int id) {
        switch (id) {
            case 2340000: // White Scroll
            case 2049100: // Chaos Scroll
            case 2049000: // Reverse Scroll
            case 2049001: // Reverse Scroll
            case 2049002: // Reverse Scroll
            case 2040006: // Miracle
            case 2040007: // Miracle
            case 2040303: // Miracle
            case 2040403: // Miracle
            case 2040506: // Miracle
            case 2040507: // Miracle
            case 2040603: // Miracle
            case 2040709: // Miracle
            case 2040710: // Miracle
            case 2040711: // Miracle
            case 2040806: // Miracle
            case 2040903: // Miracle
            case 2041024: // Miracle
            case 2041025: // Miracle
            case 2043003: // Miracle
            case 2043103: // Miracle
            case 2043203: // Miracle
            case 2043303: // Miracle
            case 2043703: // Miracle
            case 2043803: // Miracle
            case 2044003: // Miracle
            case 2044103: // Miracle
            case 2044203: // Miracle
            case 2044303: // Miracle
            case 2044403: // Miracle
            case 2044503: // Miracle
            case 2044603: // Miracle
            case 2044908: // Miracle
            case 2044815: // Miracle
            case 2044019: // Miracle
            case 2044703: // Miracle
                return 2;
            //1 = wedding msg o.o
        }
        return 0;
    }
    public static int[] goldrewards = {
        2049400, 1,
        2049401, 2,
        2049301, 2,
        2340000, 1, // white scroll
        2070007, 2,
        2070016, 1,
        2330007, 1,
        2070018, 1, // balance fury
        1402037, 1, // Rigbol Sword
        2290096, 1, // Maple Warrior 20
        2290049, 1, // Genesis 30
        2290041, 1, // Meteo 30
        2290047, 1, // Blizzard 30
        2290095, 1, // Smoke 30
        2290017, 1, // Enrage 30
        2290075, 1, // Snipe 30
        2290085, 1, // Triple Throw 30
        2290116, 1, // Areal Strike
        1302059, 3, // Dragon Carabella
        2049100, 1, // Chaos Scroll
        1092049, 1, // Dragon Kanjar
        1102041, 1, // Pink Cape
        1432018, 3, // Sky Ski
        1022047, 3, // Owl Mask
        3010051, 1, // Chair
        3010020, 1, // Portable meal table
        2040914, 1, // Shield for Weapon Atk

        1432011, 3, // Fair Frozen
        1442020, 3, // HellSlayer
        1382035, 3, // Blue Marine
        1372010, 3, // Dimon Wand
        1332027, 3, // Varkit
        1302056, 3, // Sparta
        1402005, 3, // Bezerker
        1472053, 3, // Red Craven
        1462018, 3, // Casa Crow
        1452017, 3, // Metus
        1422013, 3, // Lemonite
        1322029, 3, // Ruin Hammer
        1412010, 3, // Colonian Axe

        1472051, 1, // Green Dragon Sleeve
        1482013, 1, // Emperor's Claw
        1492013, 1, // Dragon fire Revlover

        1382049, 1,
        1382050, 1, // Blue Dragon Staff
        1382051, 1,
        1382052, 1,
        1382045, 1, // Fire Staff, Level 105
        1382047, 1, // Ice Staff, Level 105
        1382048, 1, // Thunder Staff
        1382046, 1, // Poison Staff

        1372035, 1,
        1372036, 1,
        1372037, 1,
        1372038, 1,
        1372039, 1,
        1372040, 1,
        1372041, 1,
        1372042, 1,
        1332032, 8, // Christmas Tree
        1482025, 7, // Flowery Tube

        4001011, 8, // Lupin Eraser
        4001010, 8, // Mushmom Eraser
        4001009, 8, // Stump Eraser

        2047000, 1,
        2047001, 1,
        2047002, 1,
        2047100, 1,
        2047101, 1,
        2047102, 1,
        2047200, 1,
        2047201, 1,
        2047202, 1,
        2047203, 1,
        2047204, 1,
        2047205, 1,
        2047206, 1,
        2047207, 1,
        2047208, 1,
        2047300, 1,
        2047301, 1,
        2047302, 1,
        2047303, 1,
        2047304, 1,
        2047305, 1,
        2047306, 1,
        2047307, 1,
        2047308, 1,
        2047309, 1,
        2046004, 1,
        2046005, 1,
        2046104, 1,
        2046105, 1,
        2046208, 1,
        2046209, 1,
        2046210, 1,
        2046211, 1,
        2046212, 1,
        //list
        1132014, 3,
        1132015, 2,
        1132016, 1,
        1002801, 2,
        1102205, 2,
        1332079, 2,
        1332080, 2,
        1402048, 2,
        1402049, 2,
        1402050, 2,
        1402051, 2,
        1462052, 2,
        1462054, 2,
        1462055, 2,
        1472074, 2,
        1472075, 2,
        //pro raven
        1332077, 1,
        1382082, 1,
        1432063, 1,
        1452087, 1,
        1462053, 1,
        1472072, 1,
        1482048, 1,
        1492047, 1,
        2030008, 5, // Bottle, return scroll
        1442018, 3, // Frozen Tuna
        2040900, 4, // Shield for DEF
        2049100, 10,
        2000005, 10, // Power Elixir
        2000004, 10, // Elixir
        4280000, 8,
        2430144, 10,
        2290285, 10,
        2028061, 10,
        2028062, 10,
        2530000, 5,
        2531000, 5}; // Gold Box
    public static int[] silverrewards = {
        2049401, 2,
        2049301, 2,
        3010041, 1, // skull throne
        1002452, 6, // Starry Bandana
        1002455, 6, // Starry Bandana
        2290084, 1, // Triple Throw 20
        2290048, 1, // Genesis 20
        2290040, 1, // Meteo 20
        2290046, 1, // Blizzard 20
        2290074, 1, // Sniping 20
        2290064, 1, // Concentration 20
        2290094, 1, // Smoke 20
        2290022, 1, // Berserk 20
        2290056, 1, // Bow Expert 30
        2290066, 1, // xBow Expert 30
        2290020, 1, // Sanc 20
        1102082, 1, // Black Raggdey Cape
        1302049, 1, // Glowing Whip
        2340000, 1, // White Scroll
        1102041, 1, // Pink Cape
        1452019, 2, // White Nisrock
        4001116, 3, // Hexagon Pend
        4001012, 3, // Wraith Eraser
        1022060, 2, // Foxy Racoon Eye
        2430144, 5,
        2290285, 5,
        2028062, 5,
        2028061, 5,
        2530000, 1,
        2531000, 1,
        2041100, 1,
        2041101, 1,
        2041102, 1,
        2041103, 1,
        2041104, 1,
        2041105, 1,
        2041106, 1,
        2041107, 1,
        2041108, 1,
        2041109, 1,
        2041110, 1,
        2041111, 1,
        2041112, 1,
        2041113, 1,
        2041114, 1,
        2041115, 1,
        2041116, 1,
        2041117, 1,
        2041118, 1,
        2041119, 1,
        2041300, 1,
        2041301, 1,
        2041302, 1,
        2041303, 1,
        2041304, 1,
        2041305, 1,
        2041306, 1,
        2041307, 1,
        2041308, 1,
        2041309, 1,
        2041310, 1,
        2041311, 1,
        2041312, 1,
        2041313, 1,
        2041314, 1,
        2041315, 1,
        2041316, 1,
        2041317, 1,
        2041318, 1,
        2041319, 1,
        2049200, 1,
        2049201, 1,
        2049202, 1,
        2049203, 1,
        2049204, 1,
        2049205, 1,
        2049206, 1,
        2049207, 1,
        2049208, 1,
        2049209, 1,
        2049210, 1,
        2049211, 1,
        1432011, 3, // Fair Frozen
        1442020, 3, // HellSlayer
        1382035, 3, // Blue Marine
        1372010, 3, // Dimon Wand
        1332027, 3, // Varkit
        1302056, 3, // Sparta
        1402005, 3, // Bezerker
        1472053, 3, // Red Craven
        1462018, 3, // Casa Crow
        1452017, 3, // Metus
        1422013, 3, // Lemonite
        1322029, 3, // Ruin Hammer
        1412010, 3, // Colonian Axe

        1002587, 3, // Black Wisconsin
        1402044, 1, // Pumpkin lantern
        2101013, 4, // Summoning Showa boss
        1442046, 1, // Super Snowboard
        1422031, 1, // Blue Seal Cushion
        1332054, 3, // Lonzege Dagger
        1012056, 3, // Dog Nose
        1022047, 3, // Owl Mask
        3012002, 1, // Bathtub
        1442012, 3, // Sky snowboard
        1442018, 3, // Frozen Tuna
        1432010, 3, // Omega Spear
        1432036, 1, // Fishing Pole
        2000005, 10, // Power Elixir
        2049100, 10,
        2000004, 10, // Elixir
        4280001, 8}; // Silver Box
    public static int[] peanuts = {2430091, 200, 2430092, 200, 2430093, 200, 2430101, 200, 2430102, 200, 2430136, 200, 2430149, 200,//mounts 
        2340000, 1, //rares
        1152000, 5, 1152001, 5, 1152004, 5, 1152005, 5, 1152006, 5, 1152007, 5, 1152008, 5, //toenail only comes when db is out.
        1152064, 5, 1152065, 5, 1152066, 5, 1152067, 5, 1152070, 5, 1152071, 5, 1152072, 5, 1152073, 5,
        3010019, 2, //chairs
        1001060, 10, 1002391, 10, 1102004, 10, 1050039, 10, 1102040, 10, 1102041, 10, 1102042, 10, 1102043, 10, //equips
        1082145, 5, 1082146, 5, 1082147, 5, 1082148, 5, 1082149, 5, 1082150, 5, //wg
        2043704, 10, 2040904, 10, 2040409, 10, 2040307, 10, 2041030, 10, 2040015, 10, 2040109, 10, 2041035, 10, 2041036, 10, 2040009, 10, 2040511, 10, 2040408, 10, 2043804, 10, 2044105, 10, 2044903, 10, 2044804, 10, 2043009, 10, 2043305, 10, 2040610, 10, 2040716, 10, 2041037, 10, 2043005, 10, 2041032, 10, 2040305, 10, //scrolls
        2040211, 5, 2040212, 5, 1022097, 10, //dragon glasses
        2049000, 10, 2049001, 10, 2049002, 10, 2049003, 10, //clean slate
        1012058, 5, 1012059, 5, 1012060, 5, 1012061, 5,//pinocchio nose msea only.
        1332100, 10, 1382058, 10, 1402073, 10, 1432066, 10, 1442090, 10, 1452058, 10, 1462076, 10, 1472069, 10, 1482051, 10, 1492024, 10, 1342009, 10, //durability weapons level 105
        2049400, 1, 2049401, 2, 2049301, 2,
        2049100, 10,
        2430144, 10,
        2290285, 10,
        2028062, 10,
        2028061, 10,
        2530000, 5,
        2531000, 5,
        1032080, 5,
        1032081, 4,
        1032082, 3,
        1032083, 2,
        1032084, 1,
        1112435, 5,
        1112436, 4,
        1112437, 3,
        1112438, 2,
        1112439, 1,
        1122081, 5,
        1122082, 4,
        1122083, 3,
        1122084, 2,
        1122085, 1,
        1132036, 5,
        1132037, 4,
        1132038, 3,
        1132039, 2,
        1132040, 1,
        //source
        1092070, 5,
        1092071, 4,
        1092072, 3,
        1092073, 2,
        1092074, 1,
        1092075, 5,
        1092076, 4,
        1092077, 3,
        1092078, 2,
        1092079, 1,
        1092080, 5,
        1092081, 4,
        1092082, 3,
        1092083, 2,
        1092084, 1,
        1092087, 1,
        1092088, 1,
        1092089, 1,
        1302143, 5,
        1302144, 4,
        1302145, 3,
        1302146, 2,
        1302147, 1,
        1312058, 5,
        1312059, 4,
        1312060, 3,
        1312061, 2,
        1312062, 1,
        1322086, 5,
        1322087, 4,
        1322088, 3,
        1322089, 2,
        1322090, 1,
        1332116, 5,
        1332117, 4,
        1332118, 3,
        1332119, 2,
        1332120, 1,
        1332121, 5,
        1332122, 4,
        1332123, 3,
        1332124, 2,
        1332125, 1,
        1342029, 5,
        1342030, 4,
        1342031, 3,
        1342032, 2,
        1342033, 1,
        1372074, 5,
        1372075, 4,
        1372076, 3,
        1372077, 2,
        1372078, 1,
        1382095, 5,
        1382096, 4,
        1382097, 3,
        1382098, 2,
        1392099, 1,
        1402086, 5,
        1402087, 4,
        1402088, 3,
        1402089, 2,
        1402090, 1,
        1412058, 5,
        1412059, 4,
        1412060, 3,
        1412061, 2,
        1412062, 1,
        1422059, 5,
        1422060, 4,
        1422061, 3,
        1422062, 2,
        1422063, 1,
        1432077, 5,
        1432078, 4,
        1432079, 3,
        1432080, 2,
        1432081, 1,
        1442107, 5,
        1442108, 4,
        1442109, 3,
        1442110, 2,
        1442111, 1,
        1452102, 5,
        1452103, 4,
        1452104, 3,
        1452105, 2,
        1452106, 1,
        1462087, 5,
        1462088, 4,
        1462089, 3,
        1462090, 2,
        1462091, 1,
        1472113, 5,
        1472114, 4,
        1472115, 3,
        1472116, 2,
        1472117, 1,
        1482075, 5,
        1482076, 4,
        1482077, 3,
        1482078, 2,
        1482079, 1,
        1492075, 5,
        1492076, 4,
        1492077, 3,
        1492078, 2,
        1492079, 1,
        1132012, 2,
        1132013, 1,
        1942002, 2,
        1952002, 2,
        1962002, 2,
        1972002, 2,
        1612004, 2,
        1622004, 2,
        1632004, 2,
        1642004, 2,
        1652004, 2,
        2047000, 1,
        2047001, 1,
        2047002, 1,
        2047100, 1,
        2047101, 1,
        2047102, 1,
        2047200, 1,
        2047201, 1,
        2047202, 1,
        2047203, 1,
        2047204, 1,
        2047205, 1,
        2047206, 1,
        2047207, 1,
        2047208, 1,
        2047300, 1,
        2047301, 1,
        2047302, 1,
        2047303, 1,
        2047304, 1,
        2047305, 1,
        2047306, 1,
        2047307, 1,
        2047308, 1,
        2047309, 1,
        2046004, 1,
        2046005, 1,
        2046104, 1,
        2046105, 1,
        2046208, 1,
        2046209, 1,
        2046210, 1,
        2046211, 1,
        2046212, 1,
        2049200, 1,
        2049201, 1,
        2049202, 1,
        2049203, 1,
        2049204, 1,
        2049205, 1,
        2049206, 1,
        2049207, 1,
        2049208, 1,
        2049209, 1,
        2049210, 1,
        2049211, 1,
        //ele wand
        1372035, 1,
        1372036, 1,
        1372037, 1,
        1372038, 1,
        //ele staff
        1382045, 1,
        1382046, 1,
        1382047, 1,
        1382048, 1,
        1382049, 1,
        1382050, 1, // Blue Dragon Staff
        1382051, 1,
        1382052, 1,
        1372039, 1,
        1372040, 1,
        1372041, 1,
        1372042, 1,
        2070016, 1,
        2070007, 2,
        2330007, 1,
        2070018, 1,
        2330008, 1,
        2070023, 1,
        2070024, 1,
        2028062, 5,
        2028061, 5};
    public static int[] eventCommonReward = {
        0, 10,
        1, 10,
        4, 5,
        5060004, 25,
        4170024, 25,
        4280000, 5,
        4280001, 6,
        5490000, 5,
        5490001, 6
    };
    public static int[] eventUncommonReward = {
        1, 4,
        2, 8,
        3, 8,
        2022179, 5,
        5062000, 20,
        2430082, 20,
        2430092, 20,
        2022459, 2,
        2022460, 1,
        2022462, 1,
        2430103, 2,
        2430117, 2,
        2430118, 2,
        2430201, 4,
        2430228, 4,
        2430229, 4,
        2430283, 4,
        2430136, 4,
        2430476, 4,
        2430511, 4,
        2430206, 4,
        2430199, 1,
        1032062, 5,
        5220000, 28,
        2022459, 5,
        2022460, 5,
        2022461, 5,
        2022462, 5,
        2022463, 5,
        5050000, 2,
        4080100, 10,
        4080000, 10,
        2049100, 10,
        2430144, 10,
        2290285, 10,
        2028062, 10,
        2028061, 10,
        2530000, 5,
        2531000, 5,
        2041100, 1,
        2041101, 1,
        2041102, 1,
        2041103, 1,
        2041104, 1,
        2041105, 1,
        2041106, 1,
        2041107, 1,
        2041108, 1,
        2041109, 1,
        2041110, 1,
        2041111, 1,
        2041112, 1,
        2041113, 1,
        2041114, 1,
        2041115, 1,
        2041116, 1,
        2041117, 1,
        2041118, 1,
        2041119, 1,
        2041300, 1,
        2041301, 1,
        2041302, 1,
        2041303, 1,
        2041304, 1,
        2041305, 1,
        2041306, 1,
        2041307, 1,
        2041308, 1,
        2041309, 1,
        2041310, 1,
        2041311, 1,
        2041312, 1,
        2041313, 1,
        2041314, 1,
        2041315, 1,
        2041316, 1,
        2041317, 1,
        2041318, 1,
        2041319, 1,
        2049200, 1,
        2049201, 1,
        2049202, 1,
        2049203, 1,
        2049204, 1,
        2049205, 1,
        2049206, 1,
        2049207, 1,
        2049208, 1,
        2049209, 1,
        2049210, 1,
        2049211, 1
    };
    public static int[] eventRareReward = {
        2049100, 5,
        2430144, 5,
        2290285, 5,
        2028062, 5,
        2028061, 5,
        2530000, 2,
        2531000, 2,
        2049116, 1,
        2049401, 10,
        2049301, 20,
        2049400, 3,
        2340000, 1,
        3010130, 5,
        3010131, 5,
        3010132, 5,
        3010133, 5,
        3010136, 5,
        3010116, 5,
        3010117, 5,
        3010118, 5,
        1112405, 1,
        1112445, 1,
        1022097, 1,
        2040211, 1,
        2040212, 1,
        2049000, 2,
        2049001, 2,
        2049002, 2,
        2049003, 2,
        1012058, 2,
        1012059, 2,
        1012060, 2,
        1012061, 2,
        2022460, 4,
        2022461, 3,
        2022462, 4,
        2022463, 3,
        2040041, 1,
        2040042, 1,
        2040334, 1,
        2040430, 1,
        2040538, 1,
        2040539, 1,
        2040630, 1,
        2040740, 1,
        2040741, 1,
        2040742, 1,
        2040829, 1,
        2040830, 1,
        2040936, 1,
        2041066, 1,
        2041067, 1,
        2043023, 1,
        2043117, 1,
        2043217, 1,
        2043312, 1,
        2043712, 1,
        2043812, 1,
        2044025, 1,
        2044117, 1,
        2044217, 1,
        2044317, 1,
        2044417, 1,
        2044512, 1,
        2044612, 1,
        2044712, 1,
        2046000, 1,
        2046001, 1,
        2046004, 1,
        2046005, 1,
        2046100, 1,
        2046101, 1,
        2046104, 1,
        2046105, 1,
        2046200, 1,
        2046201, 1,
        2046202, 1,
        2046203, 1,
        2046208, 1,
        2046209, 1,
        2046210, 1,
        2046211, 1,
        2046212, 1,
        2046300, 1,
        2046301, 1,
        2046302, 1,
        2046303, 1,
        2047000, 1,
        2047001, 1,
        2047002, 1,
        2047100, 1,
        2047101, 1,
        2047102, 1,
        2047200, 1,
        2047201, 1,
        2047202, 1,
        2047203, 1,
        2047204, 1,
        2047205, 1,
        2047206, 1,
        2047207, 1,
        2047208, 1,
        2047300, 1,
        2047301, 1,
        2047302, 1,
        2047303, 1,
        2047304, 1,
        2047305, 1,
        2047306, 1,
        2047307, 1,
        2047308, 1,
        2047309, 1,
        1112427, 5,
        1112428, 5,
        1112429, 5,
        1012240, 10,
        1022117, 10,
        1032095, 10,
        1112659, 10,
        2070007, 10,
        2330007, 5,
        2070016, 5,
        2070018, 5,
        1152038, 1,
        1152039, 1,
        1152040, 1,
        1152041, 1,
        1122090, 1,
        1122094, 1,
        1122098, 1,
        1122102, 1,
        1012213, 1,
        1012219, 1,
        1012225, 1,
        1012231, 1,
        1012237, 1,
        2070023, 5,
        2070024, 5,
        2330008, 5,
        2003516, 5,
        2003517, 1,
        1132052, 1,
        1132062, 1,
        1132072, 1,
        1132082, 1,
        1112585, 1,
        //walker
        1072502, 1,
        1072503, 1,
        1072504, 1,
        1072505, 1,
        1072506, 1,
        1052333, 1,
        1052334, 1,
        1052335, 1,
        1052336, 1,
        1052337, 1,
        1082305, 1,
        1082306, 1,
        1082307, 1,
        1082308, 1,
        1082309, 1,
        1003197, 1,
        1003198, 1,
        1003199, 1,
        1003200, 1,
        1003201, 1,
        1662000, 1,
        1662001, 1,
        1672000, 1,
        1672001, 1,
        1672002, 1,
        //crescent moon
        1112583, 1,
        1032092, 1,
        1132084, 1,
        //mounts, 90 day
        2430290, 1,
        2430292, 1,
        2430294, 1,
        2430296, 1,
        2430298, 1,
        2430300, 1,
        2430302, 1,
        2430304, 1,
        2430306, 1,
        2430308, 1,
        2430310, 1,
        2430312, 1,
        2430314, 1,
        2430316, 1,
        2430318, 1,
        2430320, 1,
        2430322, 1,
        2430324, 1,
        2430326, 1,
        2430328, 1,
        2430330, 1,
        2430332, 1,
        2430334, 1,
        2430336, 1,
        2430338, 1,
        2430340, 1,
        2430342, 1,
        2430344, 1,
        2430347, 1,
        2430349, 1,
        2430351, 1,
        2430353, 1,
        2430355, 1,
        2430357, 1,
        2430359, 1,
        2430361, 1,
        2430392, 1,
        2430512, 1,
        2430536, 1,
        2430477, 1,
        2430146, 1,
        2430148, 1,
        2430137, 1,};
    public static int[] eventSuperReward = {
        2022121, 10,
        4031307, 50,
        3010127, 10,
        3010128, 10,
        3010137, 10,
        3010157, 10,
        2049300, 10,
        2040758, 10,
        1442057, 10,
        2049402, 10,
        2049304, 1,
        2049305, 1,
        2040759, 7,
        2040760, 5,
        2040125, 10,
        2040126, 10,
        1012191, 5,
        1112514, 1, //untradable/tradable
        1112531, 1,
        1112629, 1,
        1112646, 1,
        1112515, 1, //untradable/tradable
        1112532, 1,
        1112630, 1,
        1112647, 1,
        1112516, 1, //untradable/tradable
        1112533, 1,
        1112631, 1,
        1112648, 1,
        2040045, 10,
        2040046, 10,
        2040333, 10,
        2040429, 10,
        2040542, 10,
        2040543, 10,
        2040629, 10,
        2040755, 10,
        2040756, 10,
        2040757, 10,
        2040833, 10,
        2040834, 10,
        2041068, 10,
        2041069, 10,
        2043022, 12,
        2043120, 12,
        2043220, 12,
        2043313, 12,
        2043713, 12,
        2043813, 12,
        2044028, 12,
        2044120, 12,
        2044220, 12,
        2044320, 12,
        2044520, 12,
        2044513, 12,
        2044613, 12,
        2044713, 12,
        2044817, 12,
        2044910, 12,
        2046002, 5,
        2046003, 5,
        2046102, 5,
        2046103, 5,
        2046204, 10,
        2046205, 10,
        2046206, 10,
        2046207, 10,
        2046304, 10,
        2046305, 10,
        2046306, 10,
        2046307, 10,
        2040006, 2,
        2040007, 2,
        2040303, 2,
        2040403, 2,
        2040506, 2,
        2040507, 2,
        2040603, 2,
        2040709, 2,
        2040710, 2,
        2040711, 2,
        2040806, 2,
        2040903, 2,
        2040913, 2,
        2041024, 2,
        2041025, 2,
        2044815, 2,
        2044908, 2,
        1152046, 1,
        1152047, 1,
        1152048, 1,
        1152049, 1,
        1122091, 1,
        1122095, 1,
        1122099, 1,
        1122103, 1,
        1012214, 1,
        1012220, 1,
        1012226, 1,
        1012232, 1,
        1012238, 1,
        1032088, 1,
        1032089, 1,
        1032090, 1,
        1032091, 1,
        1132053, 1,
        1132063, 1,
        1132073, 1,
        1132083, 1,
        1112586, 1,
        1112593, 1,
        1112597, 1,
        1662002, 1,
        1662003, 1,
        1672003, 1,
        1672004, 1,
        1672005, 1,
        //130, 140 weapons
        1092088, 1,
        1092089, 1,
        1092087, 1,
        1102275, 1,
        1102276, 1,
        1102277, 1,
        1102278, 1,
        1102279, 1,
        1102280, 1,
        1102281, 1,
        1102282, 1,
        1102283, 1,
        1102284, 1,
        1082295, 1,
        1082296, 1,
        1082297, 1,
        1082298, 1,
        1082299, 1,
        1082300, 1,
        1082301, 1,
        1082302, 1,
        1082303, 1,
        1082304, 1,
        1072485, 1,
        1072486, 1,
        1072487, 1,
        1072488, 1,
        1072489, 1,
        1072490, 1,
        1072491, 1,
        1072492, 1,
        1072493, 1,
        1072494, 1,
        1052314, 1,
        1052315, 1,
        1052316, 1,
        1052317, 1,
        1052318, 1,
        1052319, 1,
        1052329, 1,
        1052321, 1,
        1052322, 1,
        1052323, 1,
        1003172, 1,
        1003173, 1,
        1003174, 1,
        1003175, 1,
        1003176, 1,
        1003177, 1,
        1003178, 1,
        1003179, 1,
        1003180, 1,
        1003181, 1,
        1302152, 1,
        1302153, 1,
        1312065, 1,
        1312066, 1,
        1322096, 1,
        1322097, 1,
        1332130, 1,
        1332131, 1,
        1342035, 1,
        1342036, 1,
        1372084, 1,
        1372085, 1,
        1382104, 1,
        1382105, 1,
        1402095, 1,
        1402096, 1,
        1412065, 1,
        1412066, 1,
        1422066, 1,
        1422067, 1,
        1432086, 1,
        1432087, 1,
        1442116, 1,
        1442117, 1,
        1452111, 1,
        1452112, 1,
        1462099, 1,
        1462100, 1,
        1472122, 1,
        1472123, 1,
        1482084, 1,
        1482085, 1,
        1492085, 1,
        1492086, 1,
        1532017, 1,
        1532018, 1,
        //mounts
        2430291, 1,
        2430293, 1,
        2430295, 1,
        2430297, 1,
        2430299, 1,
        2430301, 1,
        2430303, 1,
        2430305, 1,
        2430307, 1,
        2430309, 1,
        2430311, 1,
        2430313, 1,
        2430315, 1,
        2430317, 1,
        2430319, 1,
        2430321, 1,
        2430323, 1,
        2430325, 1,
        2430327, 1,
        2430329, 1,
        2430331, 1,
        2430333, 1,
        2430335, 1,
        2430337, 1,
        2430339, 1,
        2430341, 1,
        2430343, 1,
        2430345, 1,
        2430348, 1,
        2430350, 1,
        2430352, 1,
        2430354, 1,
        2430356, 1,
        2430358, 1,
        2430360, 1,
        2430362, 1,
        //rising sun
        1012239, 1,
        1122104, 1,
        1112584, 1,
        1032093, 1,
        1132085, 1
    };
    public static int[] tenPercent = {
        //10% scrolls
        2040002,
        2040005,
        2040026,
        2040031,
        2040100,
        2040105,
        2040200,
        2040205,
        2040302,
        2040310,
        2040318,
        2040323,
        2040328,
        2040329,
        2040330,
        2040331,
        2040402,
        2040412,
        2040419,
        2040422,
        2040427,
        2040502,
        2040505,
        2040514,
        2040517,
        2040534,
        2040602,
        2040612,
        2040619,
        2040622,
        2040627,
        2040702,
        2040705,
        2040708,
        2040727,
        2040802,
        2040805,
        2040816,
        2040825,
        2040902,
        2040915,
        2040920,
        2040925,
        2040928,
        2040933,
        2041002,
        2041005,
        2041008,
        2041011,
        2041014,
        2041017,
        2041020,
        2041023,
        2041058,
        2041102,
        2041105,
        2041108,
        2041111,
        2041302,
        2041305,
        2041308,
        2041311,
        2043002,
        2043008,
        2043019,
        2043102,
        2043114,
        2043202,
        2043214,
        2043302,
        2043402,
        2043702,
        2043802,
        2044002,
        2044014,
        2044015,
        2044102,
        2044114,
        2044202,
        2044214,
        2044302,
        2044314,
        2044402,
        2044414,
        2044502,
        2044602,
        2044702,
        2044802,
        2044809,
        2044902,
        2045302,
        2048002,
        2048005
    };
    public static int[] fishingReward = {
        0, 100, // Meso
        1, 100, // EXP
        2022179, 1, // Onyx Apple
        1302021, 5, // Pico Pico Hammer
        1072238, 1, // Voilet Snowshoe
        1072239, 1, // Yellow Snowshoe
        2049100, 2, // Chaos Scroll
        2430144, 1,
        2290285, 1,
        2028062, 1,
        2028061, 1,
        2049301, 1, // Equip Enhancer Scroll
        2049401, 1, // Potential Scroll
        1302000, 3, // Sword
        1442011, 1, // Surfboard
        4000517, 8, // Golden Fish
        4000518, 10, // Golden Fish Egg
        4031627, 2, // White Bait (3cm)
        4031628, 1, // Sailfish (120cm)
        4031630, 1, // Carp (30cm)
        4031631, 1, // Salmon(150cm)
        4031632, 1, // Shovel
        4031633, 2, // Whitebait (3.6cm)
        4031634, 1, // Whitebait (5cm)
        4031635, 1, // Whitebait (6.5cm)
        4031636, 1, // Whitebait (10cm)
        4031637, 2, // Carp (53cm)
        4031638, 2, // Carp (60cm)
        4031639, 1, // Carp (100cm)
        4031640, 1, // Carp (113cm)
        4031641, 2, // Sailfish (128cm)
        4031642, 2, // Sailfish (131cm)
        4031643, 1, // Sailfish (140cm)
        4031644, 1, // Sailfish (148cm)
        4031645, 2, // Salmon (166cm)
        4031646, 2, // Salmon (183cm)
        4031647, 1, // Salmon (227cm)
        4031648, 1, // Salmon (288cm)
        4001187, 20,
        4001188, 20,
        4001189, 20,
        4031629, 1 // Pot
    };

    public static boolean isReverseItem(int itemId) {
        switch (itemId) {
            case 1002790:
            case 1002791:
            case 1002792:
            case 1002793:
            case 1002794:
            case 1082239:
            case 1082240:
            case 1082241:
            case 1082242:
            case 1082243:
            case 1052160:
            case 1052161:
            case 1052162:
            case 1052163:
            case 1052164:
            case 1072361:
            case 1072362:
            case 1072363:
            case 1072364:
            case 1072365:

            case 1302086:
            case 1312038:
            case 1322061:
            case 1332075:
            case 1332076:
            case 1372045:
            case 1382059:
            case 1402047:
            case 1412034:
            case 1422038:
            case 1432049:
            case 1442067:
            case 1452059:
            case 1462051:
            case 1472071:
            case 1482024:
            case 1492025:

            case 1342012:
            case 1942002:
            case 1952002:
            case 1962002:
            case 1972002:
            case 1532016:
            case 1522017:
                return true;
            default:
                return false;
        }
    }

    public static boolean isTimelessItem(int itemId) {
        switch (itemId) {
            case 1032031: //shield earring, but technically
            case 1102172:
            case 1002776:
            case 1002777:
            case 1002778:
            case 1002779:
            case 1002780:
            case 1082234:
            case 1082235:
            case 1082236:
            case 1082237:
            case 1082238:
            case 1052155:
            case 1052156:
            case 1052157:
            case 1052158:
            case 1052159:
            case 1072355:
            case 1072356:
            case 1072357:
            case 1072358:
            case 1072359:
            case 1092057:
            case 1092058:
            case 1092059:

            case 1122011:
            case 1122012:

            case 1302081:
            case 1312037:
            case 1322060:
            case 1332073:
            case 1332074:
            case 1372044:
            case 1382057:
            case 1402046:
            case 1412033:
            case 1422037:
            case 1432047:
            case 1442063:
            case 1452057:
            case 1462050:
            case 1472068:
            case 1482023:
            case 1492023:
            case 1342011:
            case 1532015:
            case 1522016:
                //raven.
                return true;
            default:
                return false;
        }
    }

    public static boolean isRing(int itemId) {
        return itemId >= 1112000 && itemId < 1113000;
    }// 112xxxx - pendants, 113xxxx - belts

    //if only there was a way to find in wz files -.-
    public static boolean isEffectRing(int itemid) {
        return isFriendshipRing(itemid) || isCrushRing(itemid) || isMarriageRing(itemid);
    }

    public static boolean isMarriageRing(int itemId) {
        switch (itemId) {
            case 1112803:
            case 1112806:
            case 1112807:
            case 1112809:
                return true;
        }
        return false;
    }

    public static boolean isFriendshipRing(int itemId) {
        switch (itemId) {
            case 1112800:
            case 1112801:
            case 1112802:
            case 1112810: //new
            case 1112811: //new, doesnt work in friendship?
            case 1112812: //new, im ASSUMING it's friendship cuz of itemID, not sure.
            case 1112816: //new, i'm also assuming
            case 1112817:

            case 1049000:
                return true;
        }
        return false;
    }

    public static boolean isCrushRing(int itemId) {
        switch (itemId) {
            case 1112000: // sparkling ring is actually a couple ring.
            case 1112001:
            case 1112002:
            case 1112003:
            case 1112005: //new
            case 1112006: //new
            case 1112007:
            case 1112012:
            case 1112015: //new

            case 1048000:
            case 1048001:
            case 1048002:
                return true;
        }
        return false;
    }
    public static int[] Equipments_Bonus = {1122017};

    public static int Equipment_Bonus_EXP(int itemid) { // TODO : Add Time for more exp increase
        switch (itemid) {
            case 1122017:
                return 10;
        }
        return 0;
    }
    public static int[] blockedMaps = {180000001, 910310300, 109050000, 280030000, 240060200, 280090000, 280030001, 240060201, 950101100, 950101010};
    //If you can think of more maps that could be exploitable via npc,block nao pliz!
   public static int[] blockedems = {1212062,1222050,1222051,1222055,1222057,1232050,1232051,1232052,1232053,1232054,1232055,1232056,1242045,1242046,1242047,1242049,1242050,1242051,1242052,1242053,1242054,1242055,1242056,1242057,1242058,1242059,1302097,1302121,1302122,1302123,1302130,1302202,1302203,1302204,1302205,1302206,1302258,1302259,1302260,1302261,1302262,1302263,1302264,1302265,1302266,1302268,1302269,1302270,1302273,1312047,1312107,1312108,1312109,1312111,1312145,1312146,1312147,1312150,1312152,1322075,1322081,1322082,1322147,1322148,1322149,1322191,1322192,1322193,1322194,1322195,1322196,1322199,1322201,1332089,1332090,1332091,1332092,1332098,1332178,1332179,1332180,1332181,1332182,1332183,1332216,1332217,1332218,1332219,1332222,1332224,1342060,1342061,1342062,1342063,1342080,1353000,1353001,1353002,1353003,1353004,1362083,1362084,1362087,1362089,1372052,1372054,1372055,1372057,1372127,1372128,1372129,1372170,1372171,1372174,1372176,1382065,1382071,1382072,1382073,1382079,1382082,1382153,1382154,1382155,1382156,1382157,1382202,1382203,1382206,1382207,1402060,1402065,1402066,1402067,1402071,1402139,1402140,1402141,1402187,1402188,1402191,1402195,1412045,1412050,1412095,1412096,1412097,1412098,1412128,1412129,1412132,1412134,1422048,1422098,1422099,1422100,1422101,1422131,1422132,1422136,1422138,1432036,1432053,1432060,1432063,1432127,1432128,1432129,1432130,1432160,1432161,1432164,1432166,1442064,1442085,1442112,1442165,1442166,1442167,1442168,1442211,1442212,1442219,1442221,1442222,1452065,1452074,1452075,1452076,1452082,1452087,1452157,1452158,1452159,1452160,1452161,1452198,1452199,1452202,1452204,1462064,1462067,1462068,1462074,1462147,1462148,1462149,1462150,1462151,1462186,1462187,1462190,1462192,1472070,1472080,1472085,1472090,1472091,1472092,1472099,1472169,1472170,1472171,1472172,1472173,1472207,1472208,1472211,1472213,1482033,1482038,1482039,1482040,1482048,1482130,1482131,1482132,1482133,1482134,1482161,1482162,1482165,1482167,1492033,1492039,1492040,1492041,1492130,1492131,1492132,1492133,1492134,1492172,1492173,1492176,1492178,1522087,1522088,1522091,1522093,1532091,1532092,1532095,1532097,1662024,1672027,1672028,1690000,1690001,1690002,1690003,1690004,1690005,1690006,1690007,1690008,1690009,1690010,1690100,1690101,1690102,1690103,1690104,1690105,1690106,1690107,1690108,1690109,1690110,1702178,1702214,1702231,1702234,1702236,1702286,1702311,1702331,1702332,1702333,1702355,1702356,1702394,1702395,1702397,1702398,1702399,1702400,1802101,1802200,1802208,1802209,1802210,1802211,1802212,1802213,1802214,1802215,1802216,1802217,1802218,1802219,1802394,1802395,1802396,1802418,1802419,1802420,1802430,1802431,1802432,1902003,1902022,1902023,1902025,1902026,1902046,1902051,1912001,1912015,1912016,1912018,1912019,1912039,1912044,1932042,1932152,1932153,1932154,1932156,1932157,1932158,1932159,1932161,1932162,1942000,1942001,1942002,1942003,1942004,1952000,1952001,1952002,1952003,1952004,1962000,1962001,1962002,1962003,1962004,1972000,1972001,1972002,1972003,1972004,1982000,1982002,1983009,1983033,1983051,1000060,1001087,1001090,1002535,1002537,1002538,1002539,1002540,1002541,1002563,1002564,1002606,1002651,1002652,1002671,1002732,1002767,1002768,1002769,1002772,1002775,1002781,1002782,1002783,1002786,1002814,1002815,1002816,1002817,1002864,1002865,1002866,1002868,1002886,1002904,1002964,1002965,1002966,1002967,1002983,1002987,1003000,1003002,1003003,1003004,1003040,1003041,1003042,1003046,1003054,1003102,1003121,1003182,1003185,1003236,1003263,1003295,1003371,1003372,1003373,1003374,1003378,1003379,1003394,1003395,1003396,1003397,1003423,1003424,1003489,1003490,1003517,1003532,1003670,1003671,1003672,1003673,1003674,1003699,1003735,1003736,1003737,1003738,1003739,1003740,1003741,1003752,1003753,1003754,1003755,1003756,1003758,1003762,1003763,1003764,1003770,1003771,1003772,1003773,1003774,1003775,1003776,1003777,1003778,1003779,1003780,1003781,1003786,1003787,1003788,1003792,1003802,1003803,1003897,1012045,1012046,1012064,1012065,1012066,1012067,1012068,1012069,1012091,1012092,1012093,1012094,1012095,1012115,1012116,1012117,1012118,1012119,1012120,1012135,1012138,1012148,1012159,1012178,1012193,1012255,1012256,1012257,1012261,1012262,1012263,1012264,1012338,1012348,1012349,1012350,1012351,1012352,1012353,1012354,1012355,1012356,1012357,1012358,1012359,1012360,1012361,1012362,1012363,1012364,1012365,1022090,1022091,1022092,1022098,1022099,1022106,1022127,1022128,1022130,1022131,1022160,1022161,1022162,1022163,1022164,1022165,1022166,1022167,1032037,1032117,1032118,1032119,1032120,1032123,1032124,1032125,1032126,1032161,1032168,1032169,1032170,1032171,1032172,1032173,1032174,1032176,1032177,1032178,1040148,1041193,1042148,1042171,1042228,1042229,1042233,1042234,1042266,1050140,1050157,1050158,1050159,1050160,1050171,1050211,1050212,1050213,1050214,1050216,1050217,1050218,1050219,1050251,1050252,1050253,1050254,1050255,1050257,1050258,1050259,1050260,1050261,1050262,1050263,1050264,1050265,1050266,1050267,1050268,1050269,1050270,1050271,1050273,1050274,1050275,1050276,1050277,1050278,1050279,1050280,1050281,1050282,1051161,1051174,1051195,1051196,1051197,1051198,1051206,1051212,1051257,1051258,1051259,1051260,1051266,1051267,1051268,1051269,1051307,1051308,1051309,1051310,1051311,1051313,1051314,1051315,1051316,1051317,1051318,1051319,1051320,1051321,1051322,1051323,1051324,1051325,1051326,1051327,1051328,1051332,1051333,1051335,1051336,1051337,1051338,1051339,1051340,1051341,1051342,1051343,1051344,1051345,1052070,1052136,1052204,1052205,1052206,1052207,1052212,1052220,1052233,1052324,1052329,1052359,1052369,1052409,1052435,1052442,1052443,1052551,1052552,1052565,1052566,1052567,1052568,1052569,1052570,1052576,1052577,1052578,1052579,1052580,1052581,1052582,1052583,1052584,1052585,1052586,1052587,1052588,1052589,1052590,1052594,1052595,1060139,1062099,1062117,1062149,1062150,1070032,1070033,1070034,1070035,1070036,1070037,1070038,1070039,1070040,1070041,1070042,1070043,1070044,1070045,1070046,1070047,1070048,1070049,1070050,1070051,1070052,1070053,1070054,1070055,1070056,1071049,1071050,1071051,1071052,1071053,1071054,1071055,1071056,1071057,1071058,1071059,1071060,1071061,1071062,1071063,1071064,1071065,1071066,1071067,1071068,1071069,1071070,1071071,1071072,1071073,1072270,1072271,1072389,1072393,1072396,1072397,1072398,1072410,1072456,1072495,1072532,1072535,1072614,1072615,1072616,1072617,1072620,1072623,1072624,1072625,1072626,1072632,1072638,1072639,1072768,1072769,1072774,1072775,1072776,1072777,1072778,1072782,1072783,1072784,1072786,1072787,1072788,1072789,1072790,1072791,1072797,1072798,1072799,1072803,1082165,1082166,1082253,1082274,1082395,1082396,1082397,1082398,1082402,1082403,1082404,1082405,1082406,1082414,1082415,1082498,1082499,1082501,1082505,1082506,1082507,1082508,1082509,1082510,1082511,1082515,1082516,1082519,1092065,1102162,1102170,1102171,1102209,1102221,1102250,1102252,1102257,1102260,1102318,1102328,1102329,1102330,1102331,1102333,1102339,1102340,1102341,1102342,1102360,1102361,1102396,1102423,1102424,1102425,1102426,1102427,1102428,1102429,1102430,1102431,1102432,1102433,1102434,1102435,1102436,1102437,1102438,1102494,1102495,1102497,1102506,1102507,1102511,1102512,1102514,1102515,1102516,1102517,1102518,1102530,1102532,1112016,1112127,1112129,1112130,1112131,1112132,1112137,1112240,1112415,1112416,1112417,1112418,1112419,1112420,1112496,1112497,1112498,1112684,1112724,1112728,1112741,1112794,1112795,1112796,1112927,1112932,1113011,1113012,1113013,1113014,1113015,1113016,1113017,1122016,1122065,1122078,1122079,1122127,1122158,1122159,1122160,1122163,1122164,1122165,1122166,1122167,1122168,1122169,1122170,1122173,1122175,1122176,1122177,1122178,1122179,1122180,1122181,1122182,1122220,1122221,1132099,1132100,1132101,1132123,1132124,1132125,1132126,1132129,1132130,1132131,1132132,1132209,1132216,1142147,1142148,1142159,1142160,1142161,1142196,1142284,1142285,1142302,1142517,1142518,1142553,1142554,1142555,1142556,1142557,1142558,1142559,1142560,1142569,1142571,1142572,1142573,1142575,1142576,1142577,1142578,1142579,1142580,1142581,1142582,1142583,1142584,1142585,1142586,1142588,1142589,1152064,1152065,1152066,1152067,1152070,1152071,1152072,1152073,1152119,1152125,1182048,1182049,1182050,1182053,1190200,1190201,1212055,1212056,1212060};
   public static int[] blockedgms = {1202070,1202071,1202072,1202073,1202074,1202075,1202076,1202077,1202078,1202079,1202080,1202081,1202082,1212057,1212063,1212068,1212069,1222052,1222058,1222063,1222064,1302229,1302275,1312118,1312144,1312153,1322164,1322188,1322203,1332195,1332225,1342071,1342082,1352800,1352801,1352802,1352803,1352804,1362090,1372141,1372177,1382170,1382208,1402153,1402196,1412106,1412135,1422109,1422128,1422140,1432140,1432167,1442184,1442223,1452172,1452205,1462161,1462193,1472181,1472214,1482142,1482168,1492154,1492179,1522073,1522094,1532076,1532098,1542000,1542001,1542002,1542003,1542004,1542005,1542006,1542007,1542008,1542009,1542010,1542011,1542012,1542013,1542014,1542015,1542016,1542017,1542018,1542019,1542020,1542021,1542022,1542023,1542024,1542025,1542026,1542027,1542028,1542033,1542034,1542035,1542036,1542037,1542038,1542039,1542040,1542044,1542045,1542046,1542047,1542048,1542049,1542050,1542051,1542052,1542053,1542054,1542055,1542056,1542057,1542058,1542059,1542060,1542061,1552000,1552001,1552002,1552003,1552004,1552005,1552006,1552007,1552008,1552009,1552010,1552011,1552012,1552013,1552014,1552015,1552016,1552017,1552018,1552019,1552020,1552021,1552022,1552023,1552024,1552025,1552026,1552027,1552028,1552033,1552034,1552035,1552036,1552037,1552038,1552039,1552040,1552044,1552045,1552046,1552047,1552048,1552049,1552050,1552051,1552052,1552053,1552054,1552055,1552056,1552057,1552058,1552059,1552060,1552061,1702351,1702374,1702376,1702380,1702381,1802387,1802388,1812010,1932116,1932117,1932123,1932124,1932125,1942000,1942001,1942002,1942003,1942004,1952000,1952001,1952002,1952003,1952004,1962000,1962001,1962002,1962003,1962004,1972000,1972001,1972002,1972003,1972004,1983052, 9960528, 9960529, 9960530, 9960531, 9960532, 9960533, 9960534, 9960535, 9960536, 9960537, 9960538,1000063,1000064,1000065,1000066,1000067,1000068,1003554,1003555,1003556,1003557,1003567,1003568,1003570,1003571,1003572,1003573,1003574,1003575,1003601,1003602,1003603,1003604,1003605,1003643,1003654,1003655,1003656,1003657,1003658,1003725,1003726,1003791,1003797,1003798,1003799,1003800,1003801,1003804,1003825,1003826,1003827,1003828,1003898,1032179,1042253,1042254,1042255,1042256,1042257,1042258,1042259,1052463,1052464,1052465,1052466,1052471,1052472,1052473,1052474,1052475,1052479,1052480,1052481,1052482,1052509,1052510,1052511,1052512,1052513,1052531,1062164,1062165,1062166,1062167,1062168,1062169,1062170,1072668,1072669,1072670,1072671,1072673,1072674,1072676,1072678,1072684,1072685,1072686,1072687,1072711,1072712,1072713,1072714,1072715,1072750,1072812,1072813,1082434,1082435,1082436,1082437,1082442,1082443,1082450,1082451,1082452,1082453,1082472,1082473,1082474,1082475,1082476,1082528,1102456,1102457,1102458,1102459,1102460,1102490,1102493,1102533,1102585,1113018,1122204,1122222,1132156,1132157,1132158,1132159,1132160,1132211,1132212,1132213,1132214,1132215,1132222,1142459,1142460,1142461,1142462,1142463,1142464,1142465,1142466,1142467,1142468,1142469,1142470,1142471,1142473,1142474,1142475,1142490,1142491,1142492,1142493,1142494,1142506,1142507,1142508,1142509,1142510,1142592,1142595,1142605,1152094,1152095,1152096,1152097,1152098,1152120,1152121,1152122,1152123,1152124,1152126,1182054,1202063,1202064,1202065,1202066,1202067,1202068,1202069};
   public static int[] blockedjms = {1052260,1052261,1052262,1052263,1052264,1052265,1052266,1052267,1052269,1052276,1052310,1052311,1052325,1052326,1052351,1052352,1052353,1052361,1052362,1052363,1052364,1052365,1052366,1052399,1052400,1052401,1052402,1052403,1052406,1052407,1052413,1052414,1052428,1052436,1052437,1052441,1052450,1052451,1052452,1052453,1052454,1052462,1052476,1052477,1052483,1052484,1052485,1052486,1052504,1052505,1052506,1052507,1052508,1052514,1052572,1052573,1052574,1052575,1052591,1052592,1052593,1052596,1052597,1052600,1052602,1052604,1052610,1052614,1052615,1052616,1052617,1060115,1060146,1060147,1060148,1061167,1061168,1061169,1062090,1070021,1070022,1070030,1071033,1071034,1071047,1072372,1072391,1072411,1072412,1072413,1072414,1072415,1072424,1072435,1072479,1072496,1072511,1072512,1072513,1072523,1072524,1072525,1072526,1072527,1072528,1072529,1072530,1072611,1072612,1072640,1072653,1072654,1072655,1072656,1072657,1072667,1072709,1072716,1072731,1072772,1072773,1072801,1072802,1072804,1072805,1072806,1072807,1072816,1072822,1080002,1080005,1081008,1081011,1082265,1082269,1082292,1082316,1082317,1082318,1082319,1082320,1082321,1082411,1082425,1082426,1082427,1082428,1082429,1082471,1082523,1082526,1092048,1092055,1092066,1102088,1102090,1102161,1102189,1102201,1102244,1102247,1102268,1102332,1102334,1102346,1102375,1102454,1102455,1102462,1102463,1102464,1102492,1102509,1102510,1102519,1102522,1102535,1102536,1102538,1102539,1102540,1102541,1102549,1102554,1102565,1102566,1102567,1102568,1102569,1102570,1102571,1102573,1102574,1112133,1112313,1112314,1112406,1112409,1112410,1112411,1112412,1112430,1112749,1112814,1112815,1112818,1112819,1112822,1112931,1112933,1112934,1112936,1112938,1113007,1113028,1122060,1122061,1122062,1122063,1122064,1122066,1122067,1122068,1122069,1122072,1122190,1122191,1122192,1122193,1122247,1132163,1132223,1142102,1142104,1142105,1142106,1142121,1142162,1142163,1142164,1142185,1142221,1142222,1142223,1142224,1142225,1142248,1142251,1142252,1142253,1142261,1142262,1142294,1142303,1142327,1142361,1142368,1142370,1142380,1142458,1142552,1142570,1142596,1142609,1142615,1142617,1152002,1152003,1152100,1152128,1152129,1182055,1302103,1302115,1302116,1302117,1302118,1302136,1302137,1302139,1302140,1302148,1302158,1302215,1312050,1312051,1312053,1312054,1312063,1312113,1322078,1322079,1322094,1322152,1332062,1332096,1332097,1332105,1332106,1332108,1332109,1332189,1332190,1342067,1352805,1372064,1372065,1372067,1372068,1372133,1382069,1382077,1382078,1382081,1382083,1382084,1382086,1382087,1382089,1382090,1382161,1402052,1402069,1402070,1402079,1402080,1402082,1402083,1402144,1412049,1412052,1412053,1412101,1422051,1422052,1422054,1422055,1422104,1432062,1432064,1432065,1432069,1432070,1432072,1432073,1432116,1432134,1442062,1442086,1442089,1442097,1442098,1442100,1442101,1442172,1452072,1452080,1452081,1452088,1452094,1452095,1452097,1452098,1452164,1462065,1462072,1462073,1462079,1462080,1462082,1462083,1462117,1462154,1472087,1472096,1472097,1472105,1472106,1472108,1472109,1472176,1482044,1482045,1482047,1482049,1482050,1482067,1482068,1482070,1482071,1482137,1492045,1492046,1492049,1492050,1492051,1492052,1492067,1492068,1492070,1492071,1492137,1522069,1532072,1702241,1702242,1702243,1702244,1702245,1702247,1702265,1702267,1702294,1702307,1702325,1702326,1702327,1702338,1702339,1702343,1702369,1702404,1702408,1702409,1702417,1802075,1802201,1802202,1802203,1802204,1802205,1802206,1802207,1802222,1802223,1802224,1802225,1802226,1802227,1802355,1802356,1802357,1802358,1802359,1802360,1802361,1802362,1802363,1802364,1902027,1902043,1902044,1902053,1902056,1902057,1902058,1912020,1912036,1912037,1912046,1912049,1912050,1912051,1932101,1942000,1942001,1942002,1942003,1942004,1952000,1952001,1952002,1952003,1952004,1962000,1962001,1962002,1962003,1962004,1972000,1972001,1972002,1972003,1972004,1982001,1983026,1983040,1983041,1992016,1992017,1992018,1992019,1992020,1992021,1992022,1992023,1992024,1992025,1992026, 9960486, 9960487, 9960488, 9960489, 9960490, 9960491, 9960492, 9960493, 9960494, 9960495, 9960496, 9960497, 9960498, 9960499, 9960500, 9960501, 9960502, 9960503, 9960504, 9960505, 9960506, 9960507, 9960508, 9960509, 9960510, 9960511, 9960512, 9960513, 9960514, 9960515, 9960516, 9960517, 9960518, 9960519, 9960520, 9960521, 9960522, 9960523, 9960524, 9960525, 9960526, 9960527, 9960539, 9960540, 9960541, 9960542, 9960543, 9960544, 9960545, 9960546, 9960547, 9960548, 9960549, 9960550, 9960551, 9960552, 9960553, 9960554, 9960555, 9960556, 9960557, 9960558, 9960559, 9960560, 9960561, 9960562, 9960563, 9960564, 9960565, 9960566, 9960567, 9960568, 9960569, 9960570, 9960571, 9961026, 9961027, 9961028, 9961029, 9961030, 9961031, 9961032, 9961033, 9961034, 9961035, 9961036, 9961037, 9961038, 9961039, 9961040, 9961041, 9961042, 9961053, 9961054, 9961055, 9961056, 9961057, 9961058, 9961059, 9961136, 9961137, 9961138, 9961139, 9961140, 9961141, 9961142, 9961143, 9961144, 9961145, 9961146, 9961147, 9961148, 9961149, 9961150, 9961151, 9961152, 9961153, 9961154, 9961155, 9961156, 9961157, 9961158, 9961159, 9961160, 9961161, 9961162, 9961163, 9961164, 9961165, 9961166, 9961167, 9961168, 9961169, 9961170, 9961171, 9961172, 9961173, 9961174, 9961175, 9961176,1000047,1001067,1001072,1001086,1002546,1002561,1002581,1002588,1002604,1002664,1002668,1002744,1002751,1002787,1002809,1002810,1002838,1002883,1002892,1002893,1002918,1002931,1002932,1002933,1002934,1002935,1002936,1002946,1002963,1002977,1002982,1003017,1003018,1003019,1003020,1003021,1003037,1003045,1003061,1003062,1003063,1003064,1003065,1003066,1003067,1003081,1003085,1003086,1003088,1003093,1003097,1003098,1003099,1003100,1003113,1003125,1003126,1003127,1003128,1003129,1003162,1003164,1003165,1003183,1003184,1003231,1003257,1003258,1003259,1003260,1003261,1003262,1003346,1003347,1003348,1003351,1003363,1003365,1003366,1003369,1003370,1003375,1003380,1003381,1003382,1003383,1003384,1003385,1003408,1003425,1003426,1003427,1003428,1003429,1003434,1003437,1003438,1003440,1003441,1003442,1003456,1003457,1003464,1003465,1003466,1003467,1003468,1003469,1003470,1003471,1003472,1003473,1003474,1003475,1003476,1003477,1003478,1003480,1003481,1003488,1003511,1003512,1003513,1003514,1003515,1003553,1003598,1003599,1003600,1003606,1003746,1003747,1003749,1003750,1003751,1003757,1003795,1003796,1003810,1003811,1003812,1003813,1003814,1003815,1003821,1003829,1003830,1003832,1003833,1003834,1003835,1003842,1003844,1003845,1003846,1003847,1003866,1003868,1003869,1003870,1003871,1003872,1003880,1012130,1012162,1012164,1012175,1012194,1012195,1012320,1012322,1012323,1012324,1012375,1022067,1022077,1022096,1022105,1022107,1022111,1022112,1022113,1022116,1022173,1022176,1032065,1032066,1032067,1032068,1032069,1032115,1032175,1040155,1040156,1040157,1040158,1041157,1041158,1041159,1041160,1042139,1042195,1042196,1042197,1042201,1049001,1050130,1050167,1050172,1050207,1050223,1050250,1051194,1051207,1051213,1051234,1051251,1051273,1051306,1052080,1052088,1052150,1052181,1052184,1052185,1052187,1052188,1052189,1052190,1052191,1052219,1052221,1052222,1052223,1052227,1052237,1052238,1052239,1052240,1052241,1052242,1052243,1052247,1052249,1052250,1052252,1052254,1052256,1052257,1052258,1052259};
   public static int[] blockedkms = {1051348,1051349,1051350,1051351,1051352,1052598,1052599,1052601,1052603,1052611,1052612,1052613,1052618,1052619,1052624,1052626,1052627,1052628,1052629,1052630,1060182,1061206,1062171,1062172,1062174,1072785,1072809,1072810,1072811,1072817,1072818,1072819,1072821,1072823,1072824,1072829,1072830,1072831,1072832,1082512,1082513,1082514,1082518,1082524,1082525,1082527,1082533,1102534,1102537,1102542,1102543,1102544,1102545,1102546,1102547,1102548,1102550,1102551,1102556,1102562,1102563,1102564,1102572,1102575,1102576,1102577,1102582,1102583,1102587,1102588,1102589,1102590,1112144,1112256,1112935,1113022,1113023,1113024,1113025,1113026,1113027,1113034,1113035,1113036,1113037,1113039,1113040,1113041,1114000,1122224,1122225,1122226,1122227,1122228,1122229,1122230,1122231,1122232,1122233,1122234,1122235,1122236,1122237,1122238,1122239,1122240,1122241,1122242,1122243,1122244,1122245,1122246,1122248,1122249,1122252,1122253,1122254,1132228,1132229,1132230,1142590,1142591,1142593,1142594,1142597,1142598,1142599,1142603,1142604,1142606,1142607,1142608,1142613,1142614,1142616,1142618,1142619,1142620,1142622,1142623,1142627,1152127,1152135,1152136,1182056,1182058,1182059,1182061,1212065,1212066,1212067,1212071,1222060,1222061,1222062,1222066,1232057,1232058,1232059,1232060,1232061,1232062,1242060,1242061,1242062,1242063,1242064,1242065,1242066,1242067,1302276,1302277,1302278,1302279,1312154,1312155,1312156,1312157,1322204,1322205,1322206,1322207,1332226,1332227,1332228,1332229,1342083,1362091,1362092,1362093,1362094,1372178,1372179,1372180,1372181,1382209,1382211,1382212,1382213,1402197,1402198,1402199,1402200,1402202,1412136,1412137,1412138,1412139,1422141,1422142,1422143,1422144,1432168,1432169,1432170,1432171,1442224,1442225,1442226,1442227,1452206,1452207,1452208,1452209,1462194,1462195,1462196,1462197,1472215,1472216,1472217,1472218,1482169,1482170,1482171,1482172,1492180,1492181,1492182,1492183,1522095,1522096,1522097,1522098,1532099,1532100,1532101,1532102,1662025,1662026,1662027,1662032,1672029,1702405,1702406,1702410,1702411,1702412,1702413,1702414,1702416,1702418,1702419,1702420,1702421,1702423,1702424,1702426,1702427,1702428,1702429,1702430,1702431,1702433,1702436,1702437,1702438,1702439,1802424,1802425,1802426,1802427,1802428,1802429,1802433,1802434,1802435,1802436,1802444,1802445,1802446,1802447,1932164,1932165,1932166,1932167,1932168,1932169,1932170,1932171,1932172,1932173,1932174,1932175,1932176,1983056,1983057,1983058,1983059,1983060,1983061,1003769,1003805,1003806,1003807,1003808,1003809,1003816,1003817,1003818,1003819,1003820,1003822,1003836,1003837,1003838,1003839,1003848,1003849,1003850,1003851,1003852,1003853,1003854,1003855,1003856,1003857,1003858,1003859,1003860,1003863,1003864,1003865,1003867,1003873,1003874,1003875,1003876,1003877,1003878,1003879,1003881,1003882,1003883,1003884,1003890,1003892,1003899,1003900,1003901,1003902,1003903,1012367,1012368,1012369,1012370,1012371,1012372,1012373,1012374,1012376,1012377,1012379,1012384,1012385,1012386,1012387,1022168,1022172,1022174,1022175,1022177,1022185,1022186,1022187,1032180,1032182,1032183,1032185,1032186,1032192,1032193,1032194,1042260,1042261,1042262,1042264,1042265,1042267,1042268,1050272,1050283,1050284,1050285,1051334,1051347};
   public static int[] blockedmsea = {1802397,1802398,1802399,1802400,1802401,1802402,1802403,1802404,1802405,1802406,1802407,1802408,1802409,1802410,1802411,1802412,1802413,1802414,1802415,1802416,1802417,1802437,1802438,1802439,1802440,1802441,1802442,1802443,1983055,1003724,1003885,1003886,1003887,1003888,1012344,1012345,1012380,1012381,1012382,1022159,1022178,1022179,1022180,1032187,1032188,1032189,1032190,1051303,1052620,1052621,1052622,1052623,1071045,1072825,1072826,1072827,1072828,1082529,1082530,1082531,1082532,1102578,1102579,1102580,1102581,1113030,1113031,1113032,1113033,1122255,1132224,1132225,1132226,1132227,1152130,1152131,1152132,1152133,1182024,1182025,1182026,1182027,1182028,1182029,1182030,1182031,1182032,1182033,1182034,1182035,1182036,1182037,1182038,1182039,1182040,1182041,1182042,1182043,1182044,1182045,1182046,1182047,1212070,1222065,1402201,1662023,1672024,1672025,1672026,1702425};
   
   public static int getExpForLevel(int i, int itemId) {
        if (isReverseItem(itemId)) {
            return getReverseRequiredEXP(i);
        } else if (getMaxLevel(itemId) > 0) {
            return getTimelessRequiredEXP(i);
        }
        return 0;
    }

    public static int getMaxLevel(int itemId) {
        Map<Integer, Map<String, Integer>> inc = MapleItemInformationProvider.getInstance().getEquipIncrements(itemId);
        return inc != null ? (inc.size()) : 0;
    }

    public static int getStatChance() {
        return 25;
    }

    public static MonsterStatus getStatFromWeapon(int itemid) {
        switch (itemid) {
            case 1302109:
            case 1312041:
            case 1322067:
            case 1332083:
            case 1372048:
            case 1382064:
            case 1402055:
            case 1412037:
            case 1422041:
            case 1432052:
            case 1442073:
            case 1452064:
            case 1462058:
            case 1472079:
            case 1482035:
                return MonsterStatus.DARKNESS;
            case 1302108:
            case 1312040:
            case 1322066:
            case 1332082:
            case 1372047:
            case 1382063:
            case 1402054:
            case 1412036:
            case 1422040:
            case 1432051:
            case 1442072:
            case 1452063:
            case 1462057:
            case 1472078:
            case 1482036:
                return MonsterStatus.SPEED;
        }
        return null;
    }

    public static int getXForStat(MonsterStatus stat) {
        switch (stat) {
            case DARKNESS:
                return -70;
            case SPEED:
                return -50;
        }
        return 0;
    }

    public static int getSkillForStat(MonsterStatus stat) {
        switch (stat) {
            case DARKNESS:
                return 1111003;
            case SPEED:
                return 3121007;
        }
        return 0;
    }
    public static int[] normalDrops = {
        4001009, //real
        4001010,
        4001011,
        4001012,
        4001013,
        4001014, //real
        4001021,
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4001038, //fake
        4001039,
        4001040,
        4001041,
        4001042,
        4001043, //fake
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007, //end
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007, //end
        4000164, //start
        2000000,
        2000003,
        2000004,
        2000005,
        4000019,
        4000000,
        4000016,
        4000006,
        2100121,
        4000029,
        4000064,
        5110000,
        4000306,
        4032181,
        4006001,
        4006000,
        2050004,
        3994102,
        3994103,
        3994104,
        3994105,
        2430007}; //end
    public static int[] rareDrops = {
        2022179,
        2049100,
        2049100,
        2430144,
        2028062,
        2028061,
        2290285,
        2049301,
        2049401,
        2022326,
        2022193,
        2049000,
        2049001,
        2049002};
    public static int[] superDrops = {
        2040804,
        2049400,
        2028062,
        2028061,
        2430144,
        2430144,
        2430144,
        2430144,
        2290285,
        2049100,
        2049100,
        2049100,
        2049100};

    public static int getSkillBook(int job) {
        if (job >= 2210 && job <= 2218) {
            return job - 2209;
        }
        switch (job) {
            case 2310:
            case 3110:
            case 3210:
            case 3310:
            case 3510:
            case 570: // Jett
            case 2410: // Phantom
            case 5110: // Mihile
                return 1;
            case 2311:
            case 3111:
            case 3211:
            case 3311:
            case 3511:
            case 571: // Jett
            case 2411: // Phantom
            case 5111: // Mihile
                return 2;
            case 2312:
            case 3112:
            case 3212:
            case 3312:
            case 3512:
            case 572: // Jett
            case 2412: // Phantom
            case 5112: // Mihile
                return 3;
        }
        return 0;
    }

    public static int getSkillBook(int job, int level) {
        if (job >= 2210 && job <= 2218) {
            return job - 2209;
        }
        switch (job) {
            case 2300:
            case 2310:
            case 2311:
            case 2312:
            case 3100:
            case 3200:
            case 3300:
            case 3500:
            case 3110:
            case 3210:
            case 3310:
            case 3510:
            case 3111:
            case 3211:
            case 3311:
            case 3511:
            case 3112:
            case 3212:
            case 3312:
            case 3512:
            case 2410:
            case 2411:
            case 2412:
            case 570: // Jett
            case 571: // Jett
            case 572: // Jett
            case 5100: // Mihile 
            case 5110: // Mihile
            case 5111: // Mihile
            case 5112: // Mihile
                return (level <= 30 ? 0 : (level >= 31 && level <= 70 ? 1 : (level >= 71 && level <= 120 ? 2 : (level >= 120 ? 3 : 0))));
        }
        return 0;
    }

    public static int getSkillBookForSkill(int skillid) {
        return getSkillBook(skillid / 10000);
    }

    public static int getLinkedMountItem(int sourceid) {
        switch (sourceid % 1000) {
            case 1:
            case 24:
            case 25:
                return 1018;
            case 2:
            case 26:
                return 1019;
            case 3:
                return 1025;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return (sourceid % 1000) + 1023;
            case 9:
            case 10:
            case 11:
                return (sourceid % 1000) + 1024;
            case 12:
                return 1042;
            case 13:
                return 1044;
            case 14:
                return 1049;
            case 15:
            case 16:
            case 17:
                return (sourceid % 1000) + 1036;
            case 18:
            case 19:
                return (sourceid % 1000) + 1045;
            case 20:
                return 1072;
            case 21:
                return 1084;
            case 22:
                return 1089;
            case 23:
                return 1106;
            case 29:
                return 1151;
            case 30:
            case 50:
                return 1054;
            case 31:
            case 51:
                return 1069;
            case 32:
                return 1138;
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
                return (sourceid % 1000) + 1009;
            case 52:
                return 1070;
            case 53:
                return 1071;
            case 54:
                return 1096;
            case 55:
                return 1101;
            case 56:
                return 1102;
            case 58:
                return 1118;
            case 59:
                return 1121;
            case 60:
                return 1122;
            case 61:
                return 1129;
            case 62:
                return 1139;
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
                return (sourceid % 1000) + 1080;
            case 85:
            case 86:
            case 87:
                return (sourceid % 1000) + 928;
            case 88:
                return 1065;
            case 27:
                return 1932049; //airplane
            case 28:
                return 1932050; //airplane
            case 114:
                return 1932099; //bunny buddy
            //33 = hot air
            //37 = bjorn
            //38 = speedy chariot
            //57 = law officer
            //they all have in wz so its ok
        }
        return 0;
    }

    public static int getMountItem(int sourceid, MapleCharacter chr) {
        switch (sourceid) {
            case 5221006:
                return 1932000;
            case 33001001: //temp.
                if (chr == null) {
                    return 1932015;
                }
                switch (chr.getIntNoRecord(JAGUAR)) {
                    case 20:
                        return 1932030;
                    case 30:
                        return 1932031;
                    case 40:
                        return 1932032;
                    case 50:
                        return 1932033;
                    case 60:
                        return 1932036;
                }
                return 1932015;
            case 35001002:
            case 35120000:
                return 1932016;
			//case 30011109:
			//	return 1932085;
        }
        if (!isBeginnerJob(sourceid / 10000)) {
            if (sourceid / 10000 == 8000 && sourceid != 80001000) { //todoo clean up
                Skill skil = SkillFactory.getSkill(sourceid);
                if (skil != null && skil.getTamingMob() > 0) {
                    return skil.getTamingMob();
                } else {
                    int link = getLinkedMountItem(sourceid);
                    if (link > 0) {
                        if (link < 10000) {
                            return getMountItem(link, chr);
                        } else {
                            return link;
                        }
                    }
                }
            }
            return 0;
        }
        switch (sourceid % 10000) {
            case 1013:
            case 1046:
                return 1932001;
            case 1015:
            case 1048:
                return 1932002;
            case 1016:
            case 1017:
            case 1027:
                return 1932007;
            case 1018:
                return 1932003;
            case 1019:
                return 1932005;
            case 1025:
                return 1932006;
            case 1028:
                return 1932008;
            case 1029:
                return 1932009;
            case 1030:
                return 1932011;
            case 1031:
                return 1932010;
            case 1033:
                return 1932013;
            case 1034:
                return 1932014;
            case 1035:
                return 1932012;
            case 1036:
                return 1932017;
            case 1037:
                return 1932018;
            case 1038:
                return 1932019;
            case 1039:
                return 1932020;
            case 1040:
                return 1932021;
            case 1042:
                return 1932022;
            case 1044:
                return 1932023;
            //case 1045:
            //return 1932030; //wth? helicopter? i didnt see one, so we use hog
            case 1049:
                return 1932025;
            case 1050:
                return 1932004;
            case 1051:
                return 1932026;
            case 1052:
                return 1932027;
            case 1053:
                return 1932028;
            case 1054:
                return 1932029;
            case 1063:
                return 1932034;
            case 1064:
                return 1932035;
            case 1065:
                return 1932037;
            case 1069:
                return 1932038;
            case 1070:
                return 1932039;
            case 1071:
                return 1932040;
            case 1072:
                return 1932041;
            case 1084:
                return 1932043;
            case 1089:
                return 1932044;
            case 1096:
                return 1932045;
            case 1101:
                return 1932046;
            case 1102:
                return 1932061;
            case 1106:
                return 1932048;
            case 1118:
                return 1932060;
            case 1115:
                return 1932052;
            case 1121:
                return 1932063;
            case 1122:
                return 1932064;
            case 1123:
                return 1932065;
            case 1128:
                return 1932066;
            case 1130:
                return 1932072;
            case 1136:
                return 1932078;
            case 1138:
                return 1932080;
            case 1139:
                return 1932081;
            //FLYING
            case 1143:
            case 1144:
            case 1145:
            case 1146:
            case 1147:
            case 1148:
            case 1149:
            case 1150:
            case 1151:
            case 1152:
            case 1153:
            case 1154:
            case 1155:
            case 1156:
            case 1157:
                return 1992000 + (sourceid % 10000) - 1143;
            default:
                return 0;
        }
    }
    public static boolean isTotem(int itemId) {
        return itemId / 10000 == 122;
    }
    public static boolean isKatara(int itemId) {
        return itemId / 10000 == 134;
    }
    
        public static boolean isSoulShield(int itemId) {
        return itemId / 10000 == 109;
    }

    public static boolean isDagger(int itemId) {
        return itemId / 10000 == 133;
    }
    
    public static boolean isApplicableSkill(int skil) {
        return (skil < 70000000 && (skil % 10000 < 8000 || skil % 10000 > 8006) && !isAngel(skil)) || skil >= 92000000 || (skil >= 80000000 && skil < 80010000); //no additional/decent skills
    }

    public static boolean isApplicableSkill_(int skil) { //not applicable to saving but is more of temporary
        for (int i : PlayerStats.pvpSkills) {
            if (skil == i) {
                return true;
            }
        }
        return (skil >= 90000000 && skil < 92000000) || (skil % 10000 >= 8000 && skil % 10000 <= 8003) || isAngel(skil);
    }

    public static boolean isTablet(int itemId) {
        return itemId / 1000 == 2047;
    }

    public static boolean isGeneralScroll(int itemId) {
        return itemId / 1000 == 2046;
    }

    public static int getSuccessTablet(int scrollId, int level) {
        if (scrollId % 1000 / 100 == 2) { //2047_2_00 = armor, 2047_3_00 = accessory
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 55;
                case 2:
                    return 43;
                case 3:
                    return 33;
                case 4:
                    return 26;
                case 5:
                    return 20;
                case 6:
                    return 16;
                case 7:
                    return 12;
                case 8:
                    return 10;
                default:
                    return 7;
            }
        } else if (scrollId % 1000 / 100 == 3) {
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 35;
                case 2:
                    return 18;
                case 3:
                    return 12;
                default:
                    return 7;
            }
        } else {
            switch (level) {
                case 0:
                    return 70;
                case 1:
                    return 50; //-20
                case 2:
                    return 36; //-14
                case 3:
                    return 26; //-10
                case 4:
                    return 19; //-7
                case 5:
                    return 14; //-5
                case 6:
                    return 10; //-4
                default:
                    return 7;  //-3
            }
        }
    }

    public static int getCurseTablet(int scrollId, int level) {
        if (scrollId % 1000 / 100 == 2) { //2047_2_00 = armor, 2047_3_00 = accessory
            switch (level) {
                case 0:
                    return 10;
                case 1:
                    return 12;
                case 2:
                    return 16;
                case 3:
                    return 20;
                case 4:
                    return 26;
                case 5:
                    return 33;
                case 6:
                    return 43;
                case 7:
                    return 55;
                case 8:
                    return 70;
                default:
                    return 100;
            }
        } else if (scrollId % 1000 / 100 == 3) {
            switch (level) {
                case 0:
                    return 12;
                case 1:
                    return 18;
                case 2:
                    return 35;
                case 3:
                    return 70;
                default:
                    return 100;
            }
        } else {
            switch (level) {
                case 0:
                    return 10;
                case 1:
                    return 14; //+4
                case 2:
                    return 19; //+5
                case 3:
                    return 26; //+7
                case 4:
                    return 36; //+10
                case 5:
                    return 50; //+14
                case 6:
                    return 70; //+20
                default:
                    return 100;  //+30
            }
        }
    }

    public static boolean isAccessory(int itemId) {
        return (itemId >= 1010000 && itemId < 1040000) || (itemId >= 1122000 && itemId < 1153000) || (itemId >= 1112000 && itemId < 1113000);
    }

     //Whoever coded the original method, remind me to bitchslap you if we ever meet in person.
    public static boolean potentialIDFits(final int potentialID, final int newstate, final int i) {
        /* Potential rules:
         * optionType must fit (this is checked for elsewhere, doesn't need to be implemented here)
         * disallow potentials >= 60000 or < 10000 (invalid)
         * Items can have potentials of either their current rank or one rank below, except for the first line
         */
        if (potentialID >= 60000 || potentialID < 10000) {
            return false; //NO
        }
        int lowerBound = (newstate - 16) * 10000; //30000
        int upperBound = lowerBound + 9999; //39999
        int secondLowerBound = lowerBound - 10000; //20000
        //First line must ALWAYS be of the target rank.
        if (i == 0 && potentialID <= upperBound && potentialID >= lowerBound) {
            return true;
        } else if (i > 0 && potentialID <= upperBound && potentialID >= secondLowerBound) { //Other lines can be of up to one rank below.
            return true;
        }
        return false;
    }

    public static boolean optionTypeFits(int optionType, int itemId) {
        switch (optionType) {
            case 10: // weapons
                return isWeapon(itemId);
            case 11: // all equipment except weapons
                return !isWeapon(itemId);
            case 20: // all armors
                return !isAccessory(itemId) && !isWeapon(itemId);
            case 40: // accessories
                return isAccessory(itemId);
            case 51: // hat
                return itemId / 10000 == 100;
            case 52: // top and overall
                return itemId / 10000 == 104 || itemId / 10000 == 105;
            case 53: // bottom and overall
                return itemId / 10000 == 106 || itemId / 10000 == 105;
            case 54: // glove
                return itemId / 10000 == 108;
            case 55: // shoe
                return itemId / 10000 == 107;
            default:
                return true;
        }
    }
	
	public static int getNebuliteGrade(int id) {
		if (id / 10000 != 306) {
			return -1;
		}
		if (id >= 3060000 && id < 3061000) {
			return 0;
		} else if (id >= 3061000 && id < 3062000) {
			return 1;
		} else if (id >= 3062000 && id < 3063000) {
			return 2;
		} else if (id >= 3063000 && id < 3064000) {
			return 3;
		}
		return 4;
    }

    public static boolean isMountItemAvailable(int mountid, int jobid) {
        if (jobid != 900 && mountid / 10000 == 190) {
            switch (mountid) {
                case 1902000:
                case 1902001:
                case 1902002:
                    return isAdventurer(jobid);
                case 1902005:
                case 1902006:
                case 1902007:
                    return isKOC(jobid);
                case 1902015:
                case 1902016:
                case 1902017:
                case 1902018:
                    return isAran(jobid);
                case 1902040:
                case 1902041:
                case 1902042:
                    return isEvan(jobid);
            }

            if (isResist(jobid)) {
                return false; //none lolol
            }
        }
        if (mountid / 10000 != 190) {
            return false;
        }
        return true;
    }

    public static boolean isMechanicItem(int itemId) {
        return itemId >= 1610000 && itemId < 1660000;
    }

    public static boolean isEvanDragonItem(int itemId) {
        return itemId >= 1940000 && itemId < 1980000; //194 = mask, 195 = pendant, 196 = wings, 197 = tail
    }

    public static boolean canScroll(int itemId) {
        return itemId / 100000 != 19 && itemId / 100000 != 16; //no mech/taming/dragon
    }

    public static boolean canHammer(int itemId) {
        switch (itemId) {
            case 1122000:
            case 1122076: //ht, chaos ht
                return false;
        }
        if (!canScroll(itemId)) {
            return false;
        }
        return true;
    }
    public static int[] owlItems = new int[]{
        1082002, // work gloves
        2070005,
        2070006,
        1022047,
        1102041,
        2044705,
        2340000, // white scroll
        2040017,
        1092030,
        2040804};

    public static int getMasterySkill(int job) {
        if (job >= 1410 && job <= 1412) {
            return 14100000;
        } else if (job >= 410 && job <= 412) {
            return 4100000;
        } else if (job >= 520 && job <= 522) {
            return 5200000;
        }
        return 0;
    }

    public static int getExpRate_Below10(int job) {
        if (GameConstants.isEvan(job)) {
            return 1;
        } else if (GameConstants.isAran(job) || GameConstants.isKOC(job) || GameConstants.isResist(job)) {
            return 5;
        }
        return 10;
    }

    public static int getExpRate_Quest(int level) {
        return (level >= 30 ? (level >= 70 ? (level >= 120 ? 10 : 5) : 2) : 1);
    }

    public static String getCashBlockedMsg(int id) {
        switch (id) {
            //case 5220083:
            case 5220084:
            case 5220092:
                //cube
                return "This item is blocked from the Cash Shop.";
        }
        return "This item is blocked from the Cash Shop.";
    }

    public static int getCustomReactItem(int rid, int original) {
        if (rid == 2008006) { //orbis pq LOL
            return (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 4001055);
            //4001056 = sunday. 4001062 = saturday
        } else {
            return original;
        }
    }

    public static int getJobNumber(int jobz) {
        int job = (jobz % 1000);
        if (job / 100 == 0 || isBeginnerJob(jobz)) {
            return 0; //beginner
        } else if ((job / 10) % 10 == 0 || job == 501) {
            return 1;
        } else {
            return 2 + (job % 10);
        }
    }

    public static boolean isBeginnerJob(int job) {
        return job == 0 || job == 1 || job == 1000 || job == 2000 || job == 2001 || job == 3000 || job == 3001 || job == 2002;
    }

    public static boolean isForceRespawn(int mapid) {
        switch (mapid) {
            case 103000800: //kerning PQ crocs
            case 925100100: //crocs and stuff
                return true;
            case 926110100:
                return false;
            default:
                return mapid / 100000 == 9800 && (mapid % 10 == 1 || mapid % 1000 == 100);
        }
    }

    public static int getFishingTime(boolean vip, boolean gm) {
        return gm ? 1000 : (vip ? 30000 : 60000);
    }

    public static int getCustomSpawnID(int summoner, int def) {
        switch (summoner) {
            case 9400589:
            case 9400748: //MV
                return 9400706; //jr
            default:
                return def;
        }
    }
    
    public static boolean isAswanMap(int mapId) {
        if (mapId >= 262021000 && mapId <= 262023130) {
            return true;
        }
        return false;
    }

    public static boolean canForfeit(int questid) {
        switch (questid) {
            case 20000:
            case 20010:
            case 20015: //cygnus quests
            case 20020:
                return false;
            default:
                return true;
        }
    }

    public static double getAttackRange(MapleStatEffect def, int rangeInc) {
        double defRange = ((400.0 + rangeInc) * (400.0 + rangeInc));
        if (def != null) {
            defRange += def.getMaxDistanceSq() + (def.getRange() * def.getRange());
        }
        //rangeInc adds to X
        //400 is approximate, screen is 600.. may be too much
        //200 for y is also too much
        //default 200000
        return defRange + 120000.0;
    }

    public static double getAttackRange(Point lt, Point rb) {
        double defRange = (400.0 * 400.0);
        int maxX = Math.max(Math.abs(lt == null ? 0 : lt.x), Math.abs(rb == null ? 0 : rb.x));
        int maxY = Math.max(Math.abs(lt == null ? 0 : lt.y), Math.abs(rb == null ? 0 : rb.y));
        defRange += (maxX * maxX) + (maxY * maxY);
        //rangeInc adds to X
        //400 is approximate, screen is 600.. may be too much
        //200 for y is also too much
        //default 200000
        return defRange + 120000.0;
    }

    public static int getLowestPrice(int itemId) {
        switch (itemId) {
            case 2340000: //ws
            case 2531000:
            case 2530000:
                return 50000000;
        }
        return -1;
    }

    public static boolean isNoDelaySkill(int skillId) {
        return skillId == 22161005 || skillId == 12111007 || skillId == 5110001 || skillId == 21101003 || skillId == 15100004 || skillId == 33101004 || skillId == 32111010 || skillId == 2111007 || skillId == 2211007 || skillId == 2311007 || skillId == 32121003 || skillId == 35121005 || skillId == 35111004 || skillId == 35121013 || skillId == 35121003 || skillId == 22150004 || skillId == 22181004 || skillId == 11101002 || skillId == 13101002 || skillId == 24121000;
    }

    public static boolean isNoSpawn(int mapID) {
        return mapID == 809040100 || mapID == 925020010 || mapID == 925020011 || mapID == 925020012 || mapID == 925020013 || mapID == 925020014 || mapID == 980010000 || mapID == 980010100 || mapID == 980010200 || mapID == 980010300 || mapID == 980010020;
    }

    public static int getExpRate(int job, int def) {
        return def;
    }

    public static int getModifier(int itemId, int up) {
        if (up <= 0) {
            return 0;
        }
        switch (itemId) {
            case 2022459:
            case 2860179:
            case 2860193:
            case 2860207:
                return 130;
            case 2022460:
            case 2022462:
            case 2022730:
                return 150;
            case 2860181:
            case 2860195:
            case 2860209:
                return 200;
        }
        if (itemId / 10000 == 286) { //familiars
            return 150;
        }
        return 200;
    }

    public static short getSlotMax(int itemId) {
        switch (itemId) {
            case 4030003:
            case 4030004:
            case 4030005:
                return 1;
            case 4001168:
            case 4031306:
            case 4031307:
            case 3993000:
            case 3993002:
            case 3993003:
                return 100;
            case 5220010:
            case 5220013:
                return 1000;
            case 5220020:
                return 2000;
        }
        return 0;
    }

  /*  public static boolean isDropRestricted(int itemId) {
        return itemId == 3012000 || itemId == 4030004 || itemId == 1052098 || itemId == 1052202;
    }

    public static boolean isPickupRestricted(int itemId) {
        return itemId == 4030003 || itemId == 4030004;
    }
    * 
    */

    public static short getStat(int itemId, int def) {
        switch (itemId) {
            case 1002419:
                return 5;
            case 1002959:
                return 25;
            case 1142002:
                return 10;
            case 1122121:
                return 7;
        }
        return (short) def;
    }

    public static short getHpMp(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 500;
            case 1142002:
            case 1002959:
                return 1000;
        }
        return (short) def;
    }

    public static short getATK(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 3;
            case 1002959:
                return 4;
            case 1142002:
                return 9;
        }
        return (short) def;
    }

    public static short getDEF(int itemId, int def) {
        switch (itemId) {
            case 1122121:
                return 250;
            case 1002959:
                return 500;
        }
        return (short) def;
    }

    public static boolean isDojo(int mapId) {
        return mapId >= 925020100 && mapId <= 925023814;
    }

    public static int getPartyPlayHP(int mobID) {
        switch (mobID) {
            case 4250000:
                return 836000;
            case 4250001:
                return 924000;
            case 5250000:
                return 1100000;
            case 5250001:
                return 1276000;
            case 5250002:
                return 1452000;

            case 9400661:
                return 15000000;
            case 9400660:
                return 30000000;
            case 9400659:
                return 45000000;
            case 9400658:
                return 20000000;
        }
        return 0;
    }

    public static int getPartyPlayEXP(int mobID) {
        switch (mobID) {
            case 4250000:
                return 5770;
            case 4250001:
                return 6160;
            case 5250000:
                return 7100;
            case 5250001:
                return 7975;
            case 5250002:
                return 8800;

            case 9400661:
                return 40000;
            case 9400660:
                return 70000;
            case 9400659:
                return 90000;
            case 9400658:
                return 50000;
        }
        return 0;
    }

    public static int getPartyPlay(int mapId) {
        switch (mapId) {
            case 300010000:
            case 300010100:
            case 300010200:
            case 300010300:
            case 300010400:
            case 300020000:
            case 300020100:
            case 300020200:
            case 300030000:

            case 683070400:
            case 683070401:
            case 683070402:
                return 25;
        }
        return 0;
    }

    public static int getPartyPlay(int mapId, int def) {
        int dd = getPartyPlay(mapId);
        if (dd > 0) {
            return dd;
        }
        return def / 2;
    }

    public static boolean isHyperTeleMap(int mapId) {
        for (int i : hyperTele) {
            if (i == mapId) {
                return true;
            }
        }
        return false;
    }

    public static int getCurrentDate() {
        String time = FileoutputUtil.CurrentReadable_Time();
        return Integer.parseInt(new StringBuilder(time.substring(0, 4)).append(time.substring(5, 7)).append(time.substring(8, 10)).append(time.substring(11, 13)).toString());
    }

    public static int getCurrentDate_NoTime() {
        String time = FileoutputUtil.CurrentReadable_Time();
        return Integer.parseInt(new StringBuilder(time.substring(0, 4)).append(time.substring(5, 7)).append(time.substring(8, 10)).toString());
    }

    public static void achievementRatio(MapleClient c) {
        //PQs not affected: Amoria, MV, CWK, English, Zakum, Horntail(?), Carnival, Ghost, Guild, LudiMaze, Elnath(?) 
        switch (c.getPlayer().getMapId()) {
            case 240080600:
            case 920010000:
            case 930000000:
            case 930000100:
            case 910010000:
            case 922010100:
            case 910340100:
            case 925100000:
            case 926100000:
            case 926110000:
            case 932000100:
            case 923040100:
            case 921160100:
                c.getSession().write(CField.achievementRatio(0));
                break;
            case 930000200:
            case 922010200:
            case 922010300:
            case 922010400:
            case 922010401:
            case 922010402:
            case 922010403:
            case 922010404:
            case 922010405:
            case 925100100:
            case 926100001:
            case 926110001:
            case 921160200:
                c.getSession().write(CField.achievementRatio(10));
                break;
            case 930000300:
            case 910340200:
            case 922010500:
            case 922010600:
            case 925100200:
            case 925100201:
            case 925100202:
            case 926100100:
            case 926110100:
            case 932000200:
            case 923040200:
            case 921160300:
            case 921160310:
            case 921160320:
            case 921160330:
            case 921160340:
            case 921160350:
            case 921120005: // right
                c.getSession().write(CField.achievementRatio(25));
                break;
            case 930000400:
            case 926100200:
            case 926110200:
            case 926100201:
            case 926110201:
            case 926100202:
            case 926110202:
            case 921160400:
                c.getSession().write(CField.achievementRatio(35));
                break;
            case 910340300:
            case 922010700:
            case 930000500:
            case 925100300:
            case 925100301:
            case 925100302:
            case 926100203:
            case 926110203:
            case 932000300:
            case 240080700:
            case 240080800:
            case 923040300:
            case 921160500:
            case 921120100: // right
                c.getSession().write(CField.achievementRatio(50));
                break;
            case 910340400:
            case 922010800:
            case 930000600:
            case 925100400:
            case 926100300:
            case 926110300:
            case 926100301:
            case 926110301:
            case 926100302:
            case 926110302:
            case 926100303:
            case 926110303:
            case 926100304:
            case 926110304:
            case 932000400:
            case 923040400:
            case 921160600:
                c.getSession().write(CField.achievementRatio(70));
                break;
            case 910340500:
            case 922010900:
            case 930000700:
            case 920010800:
            case 925100500:
            case 926100400:
            case 926110400:
            case 926100401:
            case 926110401:
            case 921120400:
            case 921160700:
            case 921120200: // ehh.. i guess.
                c.getSession().write(CField.achievementRatio(85));
                break;
            case 922011000:
            case 922011100:
            case 930000800:
            case 920011000:
            case 920011100:
            case 920011200:
            case 920011300:
            case 925100600:
            case 926100500:
            case 926110500:
            case 926100600:
            case 926110600:
            case 921120500:
            case 921120600:
            case 921120300: // this should be the correct map not 500
                c.getSession().write(CField.achievementRatio(100));
                break;
        }
    }

   public static boolean isAngel(int sourceid) {
        return isBeginnerJob(sourceid / 10000) && (sourceid % 10000 == 1085 || sourceid % 10000 == 1087 || sourceid % 10000 == 1090 || sourceid % 10000 == 1179);
    }

    public static int getAngelicSkill(int equipId) {
        switch (equipId) {
            case 1112585:
                return 1085;
            case 1112586:
                return 1087;
            case 1112594:
                return 1090;
            case 1112663:
                return 1179;
        }
        return 0;
    }

    public static int getAngelicBuff(int skill) {
        switch (skill % 10000) {
            case 1085:
                return 2022746;
            case 1087:
                return 2022747;
            case 1090:
                return 2022764;//no idea lol
            case 1179:
                return 2022823;
        }
        return 0;
    }
    
    
    public static boolean isFishingMap(int mapid) {
        return mapid == 749050500 || mapid == 749050501 || mapid == 749050502 || mapid == 970020000 || mapid == 970020005;
    }

    public static int getRewardPot(int itemid, int closeness) {
        switch (itemid) {
            case 2440000:
                switch (closeness / 10) {
                    case 0:
                    case 1:
                    case 2:
                        return 2028041 + (closeness / 10);
                    case 3:
                    case 4:
                    case 5:
                        return 2028046 + (closeness / 10);
                    case 6:
                    case 7:
                    case 8:
                        return 2028049 + (closeness / 10);
                }
                return 2028057;
            case 2440001:
                switch (closeness / 10) {
                    case 0:
                    case 1:
                    case 2:
                        return 2028044 + (closeness / 10);
                    case 3:
                    case 4:
                    case 5:
                        return 2028049 + (closeness / 10);
                    case 6:
                    case 7:
                    case 8:
                        return 2028052 + (closeness / 10);
                }
                return 2028060;
            case 2440002:
                return 2028069;
            case 2440003:
                return 2430278;
            case 2440004:
                return 2430381;
            case 2440005:
                return 2430393;
        }
        return 0;
    }

    public static boolean isEventMap(int mapid) {
        return (mapid >= 109010000 && mapid < 109050000) || (mapid > 109050001 && mapid < 109090000) || (mapid >= 809040000 && mapid <= 809040100);
    }

    public static boolean isMagicChargeSkill(int skillid) {
        switch (skillid) {
            case 2121001: // Big Bang
            case 2221001:
            case 2321001:
            case 22121000: //breath
            case 22151001:
                return true;
        }
        return false;
    }

    public static boolean isTeamMap(int mapid) {
        return mapid == 960010104 || mapid == 109080000 || mapid == 109080001 || mapid == 109080002 || mapid == 109080003 || mapid == 109080010 || mapid == 109080011 || mapid == 109080012 || mapid == 109090300 || mapid == 109090301 || mapid == 109090302 || mapid == 109090303 || mapid == 109090304 || mapid == 910040100 || mapid == 960020100 || mapid == 960020101 || mapid == 960020102 || mapid == 960020103 || mapid == 960030100 || mapid == 689000000 || mapid == 689000010;
    }

    public static int getStatDice(int stat) {
        switch (stat) {
            case 2:
                return 30;
            case 3:
                return 20;
            case 4:
                return 15;
            case 5:
                return 20;
            case 6:
                return 30;
        }
        return 0;
    }

    public static int getDiceStat(int buffid, int stat) {
        if (buffid == stat || buffid % 10 == stat || buffid / 10 == stat) {
            return getStatDice(stat);
        } else if (buffid == (stat * 100)) {
            return getStatDice(stat) + 10;
        }
        return 0;
    }

    public static int getMPByJob(int job) {
        switch (job) {
			case 3100: 
				return 30;
            case 3110:
                return 50;
            case 3111:
                return 100;
            case 3112:
                return 120;
        }
		return 30; // beginner or 3100
    }
      // was 9270035 but we now have @wiz :P
    public static final int[] publicNpcIds = {9070004, 9071003, 9010022, 9000087, 9000088, 9270035, 9900002, 9900000, 9900001};
    public static final String[] publicNpcs = {"Move to the #cBattle Square# to fight other players", 
        "Move to #cMonster Park# to team up to defeat monsters.", "Move to a variety of #cparty quests#.", 
        "Move to #cFree Market# to trade items with players.", "Move to #cArdentmill#, the crafting town.", "#cUniversal NPC#.", "#cJob Changer#.", 
        "Male #Styler#.", "Female #cStyler#."};
    //questID; FAMILY USES 19000x, MARRIAGE USES 16000x, EXPED USES 16010x
    //dojo = 150000, bpq = 150001, master monster portals: 122600
    //compensate evan = 170000, compensate sp = 170001
    public static int OMOK_SCORE = 122200;
    public static int MATCH_SCORE = 122210;
    public static int HP_ITEM = 122221;
    public static int MP_ITEM = 122223;
    public static int JAIL_TIME = 123455;
    public static int JAIL_QUEST = 123456;
    public static int REPORT_QUEST = 123457;
    public static int ULT_EXPLORER = 111111;
    //codex = -55 slot
    //crafting/gathering are designated as skills(short exp then byte 0 then byte level), same with recipes(integer.max_value skill level)
    public static int ENERGY_DRINK = 122500;
    public static int HARVEST_TIME = 122501;
    public static int PENDANT_SLOT = 122700;
    public static int CURRENT_SET = 122800;
    public static int BOSS_PQ = 150001;
    public static int JAGUAR = 111112;
    public static int DOJO = 150100;
    public static int DOJO_RECORD = 150101;
    public static int DOJO_REGULAR = 150136;
    public static int DOJO_RANKED = 150137;
    public static int HOBLIN = 150138;
    public static int ARIANT = 150139;
    public static int PARTY_REQUEST = 122900;
    public static int PARTY_INVITE = 122901;
    public static int QUICK_SLOT = 123000;
    public static int ITEM_TITLE = 124000;
    public static int[] blockedItems = {};

    public static boolean isIllegal(int itemId) {
            if (itemId == 1004043 || itemId == 1004042 || itemId == 1004040 || itemId == 1302998) {
                return true;
            } else {
                return false;
            }
    }
    
    public static void getBlockedItems(final int[] items) {
        //for (final int item : items) {
                blockedItems = items;
        //}
    }

    public static boolean isIllegalMap(int id) {
        if (id == 180000000) {
            return true;
        } else {
            return false;
        }
    }

/*      */ 


/*      */   public static boolean isJett(int job) {
/*  218 */     return (job == 508) || (job / 10 == 57);
/*      */   }

/*      */   public static int getBuffDelay(int skill) {
/* 3724 */     switch (skill) {
/*      */     case 24111002:
/*      */     case 24111003:
/*      */     case 24111005:
/*      */     case 24121004:
/*      */     case 24121008:
/* 3730 */       return 1000;
/*      */     }
/* 3732 */     return 0;
/*      */   }
/*      */

/*      */   public static int getJudgmentStat(int buffid, int stat) {
/* 3551 */     switch (stat) {
/*      */     case 1:
/* 3553 */       return buffid == 20031209 ? 5 : 10;
/*      */     case 2:
/* 3555 */       return buffid == 20031209 ? 10 : 20;
/*      */     case 3:
/* 3557 */       return 2020;
/*      */     case 4:
/* 3559 */       return 100;
/*      */     }
/* 3561 */     return 0;
/*      */   }

/*      */   public static boolean isPhantom(int job) {
/*  222 */     return (job == 2003) || (job / 100 == 24);
/*      */   }

/*      */   public static boolean isMihile(int job) {
/*  222 */     return (job == 5000) || (job / 100 == 51);
/*      */   }

        
    public static int[] getInnerSkillbyRank(int rank) {
         if (rank == 0) {
             return rankC;
         } else if (rank == 1) {
             return rankB;
         } else if (rank == 2) {
             return rankA;
         } else if (rank == 3) {
             return rankS;
         } else {
             return null;
         }
     }
/* 4000 */   public static final int[] normalMagicWheel = { 4006000, 2050004, 3994102, 3994103, 3994104, 3994102, 3994103, 3994104, 3994105, 2430007 };
/*      */ 
/* 4011 */   public static final int[] rareMagicWheel = { 2028061, 2290285, 2049301, 2049401, 2022326, 2022193, 2049000, 2049001, 2049002 };
/*      */ 
/* 4021 */   public static final int[] superMagicWheel = { 2040804, 2049400, 2028062, 2028061, 2430144, 2430144, 2430144, 2430144, 2290285, 2049100, 2049100, 2049100, 2049100 };
/*      */ 
 /*      */   public static boolean isSuperMagicWheel(int itemid)
/*      */   {
/* 4037 */     for (int i : superMagicWheel) {
/* 4038 */       if (i == itemid) {
/* 4039 */         return true;
/*      */       }
/*      */     }
/* 4042 */     return false;
/*      */   }

    public static boolean isGMScroll(int itemId) {
        switch (itemId) {
            case 0: // load
            case 1: // all
            case 2: // scrolls
            case 3: // here
                return true;
            default:
                return false;
        }
    }
 }


