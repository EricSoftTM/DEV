package constants;

public class ServerConstants {

    public static boolean TESPIA = false; // true = uses GMS test server, for MSEA it does nothing though
    public static final short MAPLE_VERSION = 117;
    public static final String MAPLE_PATCH = "2";
    public static String SERVER_IP = "127.0.0.1";
    public static String SERVER_NAME = "Development";
    public static String WzRevision = "1.0";
    public static String SourceRev = "1.0"; // initial revision
    public static final String MasterPass = "ericftw";
    public static boolean dropUndroppables = true;
    public static boolean moreThanOne = true;
    public static final boolean BlockCS = false;
    public static final int Currency = 4000999;
    public static final boolean MerchantsUseCurrency = false; // Log Packets = true | Allow people to connect = false
    public static boolean Use_Localhost = false; // Only allow accounted admins to connect pass login server
    public static boolean Admin_Only = false; 
    public static boolean release = true;
    public static int ERIC_ACC_ID = 2197;
    public static final boolean TRIPLE_TRIO = true;
    public static int CHANNEL_LOAD = 150; // players per channel
    public static final int number1 = (142449577 + 753356065 + 611816275);
    public static final short number2 = 18773, updateNumber = 18774, linkNumber = 18775, messageNumber = 18776;
    public static final long number3 = 202227478981090217L;
    public static int[] VIP_ROCK_BLOCK = {180000000, 180000001};
    public static final byte[] NEXON_IP = new byte[]{(byte) 8, (byte) 31, (byte) 98, (byte) 53};
    // TODO: messages handled per world:)
    public static String serverMessage = "Welcome to Development v117.2 Closed Alpha! * Type @commands for a list of commands * Report all bugs to our forums, or to Eric.";
    public static String WELCOME_MESSAGE = "Welcome to #rDevelopment#k - Closed Alpha!\r\n#gReport #ebugs#n to the forums!#k\r\n#bFacebook - #ehttp://www.facebook.com/Development#n#k";
    
    public static String getTip() {
        // Update these occasionally <3
        String[] tips = {
            "#rNEW items are available!#k", "#bNEW commands are available!#k", "#rVote for Munny!#k",
            "#bWe love our beauties! <3#k", "#rAuto Events#k give you Munny!", "#bMinigames are now available!#k",
            "#We support #rWindows 8#k!", ("Our #bWZ's REV#k is #r" + WzRevision),
            ("Our #bSource's REV#k is #r" + SourceRev), "#bDuh hello?#k", "Follow us on #bFacebook!#k",
            "#rY#k#bO#k#rL#k#bO#k", "#ruw0tm8?#k", "ALL HAIL #rTROLLS#k!!!", "#rEric#k is cool", "#bKevin#k is cool", "#rPaul#k is cool"
        };
        int tip = (int)Math.floor(Math.random() * tips.length);
        return tips[tip];
    }

    public static enum PlayerGMRank {

        NORMAL('@', 0),
        DONATOR('!', 1),
        SUPERDONATOR('!', 2),
        INTERN('!', 3),
        GM('!', 4),
        SUPERGM('!', 5),
        ADMIN('!', 6), 
        GOD('!', 100);
        private char commandPrefix;
        private int level;

        PlayerGMRank(char ch, int level) {
            commandPrefix = ch;
            this.level = level;
        }

        public char getCommandPrefix() {
            return commandPrefix;
        }

        public int getLevel() {
            return level;
        }
    }

    public static enum CommandType {

        NORMAL(0),
        TRADE(1);
        private int level;

        CommandType(int level) {
            this.level = level;
        }

        public int getType() {
            return level;
        }
    }
}
