package client;

import database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import scripting.NPCConversationManager;

/**
 *
 * @author Eric
 */
public class MapleClans {
    
    /**
     * 
     * @return Player's clan id
     */
    public int getClanId() {
        return 1;
    }
    
    /**
     * 
     * @return MapleCharacter instance
     */
    public MapleCharacter getPlayer() {
        return null;
    }
    
    /**
     * 
     * @return top five clans and their stats
     * @throws SQLException 
     */
    public String getClanRanks() throws SQLException {
        StringBuilder ret = new StringBuilder();
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name, level, members, wins FROM clans ORDER BY wins desc LIMIT 5");
        ResultSet rs = ps.executeQuery();
            try {
                int calc = 1;
                while (rs.next()) {
                    int wins_Integer = rs.getInt("wins");
                    String wins = NumberFormat.getNumberInstance(Locale.US).format(wins_Integer);
                    ret.append("\r\n#e").append(calc).append("#n. #r").append(rs.getString("name")).append("#k\r\nClan Level : ").append(rs.getInt("level")).append(" | Clan Members : ").append(rs.getInt("members")).append(" | Wins : ").append(wins);
                    calc++;
                }
            } catch (SQLException ex) {
                Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
         ps.close();
         rs.close();
        return ret.toString();
    }
    
    /**
     * 
     * @return member names within the instanced user's clan
     * @throws SQLException 
     */
    public String getClanRoster() throws SQLException {
        StringBuilder ret = new StringBuilder();
        PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("SELECT name FROM characters WHERE clanid = ? ORDER BY name desc LIMIT 5");
        ps.setInt(1, getClanId());
        ResultSet rs = ps.executeQuery();
            try {
                int calc = 1;
                while (rs.next()) {
                    ret.append("\r\n#e").append(calc).append("#n. #b").append(rs.getString("name"));
                    calc++;
                }
            } catch (SQLException ex) {
                Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, ex);
            }
         ps.close();
         rs.close();
        return ret.toString();
    }
    
    /**
     * 
     * @return clans official message
     */
    public String getClanMessage() {
	Connection con = DatabaseConnection.getConnection();
	PreparedStatement ps;
	try {
            ps = con.prepareStatement("SELECT message FROM clans WHERE id = ?");
            ps.setInt(1, getClanId());
            ResultSet rs = ps.executeQuery();
		if (!rs.next()) {
               	    rs.close();
		    ps.close();
		    return "";
		}
            String name = rs.getString("message");
                rs.close();
                ps.close();
                return name;
            } catch (SQLException e) {
		System.out.print("ERROR" + e);
            }
		return "";
    }
    
    /**
     * 
     * @return instanced character's clan name
     */
    public String getClanName() {
        return getClanName(getClanId());
    }
    
    /**
     * 
     * @param id
     * @return the clan name of <id>
     */
    public static String getClanName(int id) {
	Connection con = DatabaseConnection.getConnection();
	PreparedStatement ps;
	try {
            ps = con.prepareStatement("SELECT name FROM clans WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
		if (!rs.next()) {
               	    rs.close();
		    ps.close();
		    return "Clan";
		}
            String name = rs.getString("name");
                rs.close();
                ps.close();
                return name;
            } catch (SQLException e) {
		System.out.print("ERROR" + e);
            }
		return "Clan";
    }
    
    /**
     * 
     * @param name
     * @return the clan id of <name>
     */
    public static int getClanIdByName(String name) {
        Connection con = DatabaseConnection.getConnection();
	PreparedStatement ps;
            try {
                ps = con.prepareStatement("SELECT id FROM clans WHERE name = ?");
		ps.setString(1, name);
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
    
    /**
     * 
     * @return instanced character's clanTag
     */
    public String getClantag() {
	Connection con = DatabaseConnection.getConnection();
	PreparedStatement ps;
	try {
            ps = con.prepareStatement("SELECT clantag FROM clans WHERE id = ?");
            ps.setInt(1, getClanId());
            ResultSet rs = ps.executeQuery();
		if (!rs.next()) {
               	    rs.close();
		    ps.close();
		    return "none";
		}
            String name = rs.getString("clantag");
                rs.close();
                ps.close();
                return name;
            } catch (SQLException e) {
		System.out.print("ERROR" + e);
            }
		return "none";
    }
    
    /**
     * 
     * @param chr
     * @return clanId of <chr>
     */
    public int getClanId(MapleCharacter chr) {
	return chr.getClanId();
    }
    
    /**
     * Updates the clans message to the new desired message
     * @param message 
     */
    public void setClanMessage(String message) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE clans SET message = ? WHERE id = ?");
            ps.setString(1, message);
            ps.setInt(2, getClanId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    /**
     * Sets or updates the instanced character's clanTag
     * @param tag 
     */
    public void setClanTag(String tag) {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE clans SET clantag = ? WHERE id = ?");
            ps.setString(1, tag);
            ps.setInt(2, getClanId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    /**
     * Levels up the instanced character's clan
     */
    public void levelUpClan() {
        try {
            PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE clans SET level = level+1 WHERE id = ?");
            ps.setInt(1, getClanId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    /**
     * Creates a clan
     * @param name 
     */
    public void createClan(String name) {
        try {
            java.sql.Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO clans ( name, level, members, wins, message, clantag, leaderid ) VALUES ( ?, ?, ?, ?, ?, ?, ? )");
            ps.setString(1, name);
            ps.setInt(2, 1);
            ps.setInt(3, 1);
            ps.setInt(4, 0);
            ps.setString(5, "");
            ps.setString(6, "");
            ps.setInt(7, getPlayer().getId());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            Logger.getLogger(NPCConversationManager.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    /**
     * The Clan Leader of the instanced character
     * @return 
     */
    public int getClanLeader() {
        Connection con = DatabaseConnection.getConnection();
	PreparedStatement ps;
            try {
                ps = con.prepareStatement("SELECT leaderid FROM clans WHERE id = ?");
		ps.setInt(1, getClanId());
		ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        rs.close();
			ps.close();
			return -1;
                    }
		int leaderid = rs.getInt("leaderid");
                        rs.close();
                        ps.close();
			return leaderid;
		} catch (SQLException e) {
			System.out.print("ERROR" + e);
		}
		return -1;
    }
    
    /**
     * 
     * @param chr
     * @return clan window for invitation
     */
    public String getClanRequest(MapleCharacter chr) {
        String information = "#e";
        information += chr.getName() + "#n, the leader of #e" + getClanName(chr.getClanId()) + "#n, has sent you a #bClan Invitation#k.\r\n";
        information += "If you choose to accept, you will become a(n) #e" + getClanName(chr.getClanId()) + "#n member from here on out.";
        return information;
    }
}
