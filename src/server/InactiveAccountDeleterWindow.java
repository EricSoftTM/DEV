/*
Written by Emilyx3
Because I wanted to play with GUIs ^^
This version cleans up:
Guilds
BBSThreads/BBSEntries
BuddyLists
Fame Log
Cheat Log
Skill Macros
Rings
Pets

PLEASE MAKE SURE THE HANDLERS (found under the ensureCompleteRemoval method) ARE COMPATIBLE WITH YOUR DATABASE. I AM NOT RESPONSIBLE FOR ANY DAMAGE YOU DO TO YOUR DATABASE =P

Note: The TextAreaOutputStream class and getMonthStrings method were NOT written by me (Because I'm lazy and I know how to Google xD).

NOW WITH MULTITHREADING =)
 */


/*
 * InactiveAccountDeleterWindow.java
 *
 * Created on Dec 20, 2009, 2:09:04 AM
 */

package server;

import database.DatabaseConnection;
import java.io.CharArrayWriter;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import javax.swing.JTextArea;



public class InactiveAccountDeleterWindow extends javax.swing.JFrame {
    Calendar c = Calendar.getInstance();
    boolean deleteConfirmed = false;
    int batchsize = 1000;
    HashMap<String,String> tableReferences = new HashMap<>(); //Table Name, Column that references Character ID.

    public InactiveAccountDeleterWindow() {
        
        c.setTimeInMillis(System.currentTimeMillis());
        initComponents();
            DatabaseConnection.getConnection();

    }
                       
    private void initComponents() {

        String[] monthStrings = getMonthStrings();
        Month = new javax.swing.JComboBox<>(monthStrings);
        Day = new javax.swing.JComboBox<>();
        Year = new javax.swing.JComboBox<>();
        DeleteStart = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        JTextArea ta = new JTextArea();
        TextAreaOutputStream taos = new TextAreaOutputStream( ta, 60 );
        PrintStream ps = new PrintStream( taos );
        System.setOut(ps);
        System.setErr(ps);
        jScrollPane1 = new javax.swing.JScrollPane(ta);
        jLabel4 = new javax.swing.JLabel();
        Hour = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        Minute = new javax.swing.JComboBox<>();
        Second = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Inactive Account Deletion");

        initMonths();
        Month.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MonthActionPerformed(evt);
            }
        });

        redoDays();
        Day.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DayActionPerformed(evt);
            }
        });

        initYears(false);
        Year.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                YearActionPerformed(evt);
            }
        });

        DeleteStart.setText("Start Delete");
        DeleteStart.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteStartActionPerformed(evt);
            }
        });

        jLabel1.setText("Month");

        jLabel2.setText("Day");

        jLabel3.setText("Year");

        jLabel4.setText("Delete all accounts that have not logged on since:");

        initHours();
        Hour.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                HourActionPerformed(evt);
            }
        });

        jLabel5.setText("Hour");

        initMinutes();
        Minute.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MinuteActionPerformed(evt);
            }
        });

        initSeconds();
        Second.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SecondActionPerformed(evt);
            }
        });

        jLabel6.setText("Minute");

        jLabel7.setText("Second");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 615, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Month, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Day, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(Year, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addComponent(Hour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(20, 20, 20)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(Minute, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addGap(236, 236, 236)
                                .addComponent(DeleteStart))
                            .addComponent(Second, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel4))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(DeleteStart)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(Month, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Day, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Year, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Minute, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Hour, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(Second, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }                      

    private void MonthActionPerformed(java.awt.event.ActionEvent evt) {                                      
        c.set(Calendar.MONTH, Month.getSelectedIndex());
        redoDays();
        deleteConfirmed = false;
    }                                     

    private void DeleteStartActionPerformed(java.awt.event.ActionEvent evt) {                                            
        PreparedStatement ps1 = null;
        int totalAccounts = 0;
        try {
            ps1 = DatabaseConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM accounts");
            ResultSet rs = ps1.executeQuery();
            rs.next();
            totalAccounts = rs.getInt(1);
            ps1.close();
        } catch (SQLException ex) {
        } finally {
            try{ps1.close();}catch(SQLException se){}
        }

        if (deleteConfirmed) {
            deleteConfirmed = !deleteConfirmed;
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps  = null;
            ResultSet rs;
            ArrayList<ArrayList<Integer>> accountIDLists = new ArrayList<>();
            accountIDLists.add(new ArrayList<Integer>());
            int curAccListIndex = 0;
            //ArrayList<Integer> accountIDs = new ArrayList<Integer>();
            try {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE lastlogin < ?");
                ps.setTimestamp(1, new Timestamp(c.getTimeInMillis()));
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (accountIDLists.get(curAccListIndex).size() < batchsize) {
                        accountIDLists.get(curAccListIndex).add(rs.getInt("id"));
                    }
                    else {
                        accountIDLists.add(new ArrayList<Integer>());
                        curAccListIndex++;
                        accountIDLists.get(curAccListIndex).add(rs.getInt("id"));
                    }
                }
            } catch (SQLException ex) {
            } finally {
                try{ps.close();}catch(SQLException se){}
            }
            int totAccs = 0;
            for (ArrayList<Integer> a : accountIDLists) {
                totAccs += a.size();
            }
            System.out.println("Inactive Accounts found: "+totAccs+" (Total Accounts: "+totalAccounts+")");
            System.out.println(accountIDLists.size()+" threads will be started.");
            int threadid = 1;
            for (ArrayList<Integer> a : accountIDLists) {
                new DeletionThread(a,threadid);
                threadid++;
            }

                           
        } else {
            deleteConfirmed = !deleteConfirmed;
            PreparedStatement ps2 = null;
            int accountsToBeDeleted = 0;
            try {
                ps2 = DatabaseConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM accounts WHERE lastlogin < ?");
                ps2.setTimestamp(1, new Timestamp(c.getTimeInMillis()));
                ResultSet rs = ps2.executeQuery();
                rs.next();
                accountsToBeDeleted = rs.getInt(1);
                ps2.close();
            } catch (SQLException ex) {
            } finally {
                try{ps2.close();}catch(SQLException se){}
            }
            System.out.println("Deleting all accounts that haven't been accessed since:");
            System.out.println(c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US)+" "+c.get(Calendar.DATE)+", "+c.get(Calendar.YEAR)+
                    " "+padSingleNumber(c.get(Calendar.HOUR_OF_DAY))+":"+padSingleNumber(c.get(Calendar.MINUTE))+":"+padSingleNumber(c.get(Calendar.SECOND)));
            System.out.println(accountsToBeDeleted+" Accounts will be deleted (Total Accounts: "+totalAccounts+")");

            System.out.println("Press again to confirm (!!)\r\n");
        }

        
    }                                           

    private void DayActionPerformed(java.awt.event.ActionEvent evt) {                                    
        c.set(Calendar.DATE, Day.getSelectedIndex() + 1);
        deleteConfirmed = false;
    }                                   

    private void YearActionPerformed(java.awt.event.ActionEvent evt) {                                     
        c.set(Calendar.YEAR, (Integer) Year.getSelectedItem());
        initYears(true);
        deleteConfirmed = false;
    }                                    

    private void MinuteActionPerformed(java.awt.event.ActionEvent evt) {                                       
        c.set(Calendar.MINUTE, (Integer) Minute.getSelectedItem());
        deleteConfirmed = false;
    }                                      

    private void HourActionPerformed(java.awt.event.ActionEvent evt) {                                     
        c.set(Calendar.HOUR, (Integer) Hour.getSelectedItem());
        deleteConfirmed = false;
    }                                    

    private void SecondActionPerformed(java.awt.event.ActionEvent evt) {                                       
        c.set(Calendar.SECOND, (Integer) Second.getSelectedItem());
        deleteConfirmed = false;
    }                                      

    private String padSingleNumber(int i) {
        if (i >= 0 && i <= 9) {
            return "0"+i;
        }
        else {
            return ""+i;
        }
    }

    private void initHours() {
        for (int i = 0;i < 24; i++) {
            Hour.addItem(new Integer(i));
        }
        Hour.setSelectedIndex(0);
        c.set(Calendar.HOUR, 0);
    }

    private void initMinutes() {
        for (int i = 0;i < 60; i++) {
            Minute.addItem(new Integer(i));
        }
        Minute.setSelectedIndex(0);
        c.set(Calendar.MINUTE, 0);
    }

    private void initSeconds() {
        for (int i = 0;i < 60; i++) {
            Second.addItem(new Integer(i));
        }
        Second.setSelectedIndex(0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private void initYears(boolean setmid) {
        int year = c.get(Calendar.YEAR) - 25;
        Year.removeAllItems();
        for (int i = 0;i < 50; i++) {
            Year.addItem(new Integer(year++));
        }
        //if (setmid)
            Year.setSelectedIndex(25);
    }

    private void redoDays() {
        int days = 0;
        switch (c.get(Calendar.MONTH)) {
            case 0:
            case 2:
            case 4:
            case 6:
            case 7:
            case 9:
            case 11:
                days = 31;break;
            case 3:
            case 5:
            case 8:
            case 10:
            case 12:
                days = 30;break;
            case 1: 
                if (c.get(Calendar.YEAR) % 400 == 0 || (c.get(Calendar.YEAR) % 4 == 0 && c.get(Calendar.YEAR) % 100 != 0)) {
            days = 29;
        }
                else {
            days = 28;
        }
        }
        if (Day.getItemCount() != 0) {
            Day.removeAllItems();
        }
        for (int i = 0;i < days; i++) {
            Day.addItem(new Integer(i + 1));
        }
        try {
            Day.setSelectedIndex(c.get(Calendar.DATE));
        } catch (NullPointerException npe) {
            Day.setSelectedIndex(0);
        } //TryCatch Abuse ^_^;;
        
    }

    private void initMonths() {
        try {
            Month.setSelectedIndex(c.get(Calendar.MONTH));
        } catch (NullPointerException npe) {
            Month.setSelectedIndex(0);
        } //TryCatch Abuse ^_^;;
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new InactiveAccountDeleterWindow().setVisible(true);
                System.out.println("Please select a date and hit \"Start Delete\"");
            }
        });
    }
                   
    private javax.swing.JComboBox<Integer>  Day;
    private javax.swing.JButton DeleteStart;
    private javax.swing.JComboBox<Integer>  Hour;
    private javax.swing.JComboBox<Integer>  Minute;
    private javax.swing.JComboBox<String>  Month;
    private javax.swing.JComboBox<Integer>  Second;
    private javax.swing.JComboBox<Integer>  Year;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;   


    static protected String[] getMonthStrings() {
        String[] months = new java.text.DateFormatSymbols().getMonths();
        int lastIndex = months.length - 1;

        if (months[lastIndex] == null
           || months[lastIndex].length() <= 0) { //last item empty
            String[] monthStrings = new String[lastIndex];
            System.arraycopy(months, 0,
                             monthStrings, 0, lastIndex);
            return monthStrings;
        } else { //last item not empty
            return months;
        }
    }
}

class DeletionThread extends Thread {
    int threadid = -1;
    ArrayList<Integer> accountIDs;
    Connection con = DatabaseConnection.getConnection();
     public DeletionThread(ArrayList<Integer> accs, int threadid) {
        accountIDs = accs;
        this.threadid = threadid;
        start();
    }

    @Override
    public void run() {
        int totalCharacters = 0;

            PreparedStatement ps = null;
            PreparedStatement ps2 = null;
            ResultSet rs;
            try {
                ps2 = DatabaseConnection.getConnection().prepareStatement("SELECT COUNT(*) FROM characters");
                ResultSet rs2 = ps2.executeQuery();
                rs2.next();
                totalCharacters = rs2.getInt(1);
                ps2.close();
            } catch (SQLException ex) {
            } finally {
                try{ps2.close();}catch(SQLException se){}
            }

            int charsDeleted = 0;
            ArrayList<Integer> characterIDs = new ArrayList<>();
            try {
                StringBuilder selStr = new StringBuilder("SELECT id FROM characters WHERE ");
                ps = con.prepareStatement("DELETE FROM characters WHERE accountid = ?");
                boolean first = true;
                for (Integer accID : accountIDs) {
                    ps.setInt(1, accID);
                    ps.addBatch();
                    if (first) {
                        selStr.append("accountid = ").append(accID);
                        first = false;
                    } else {
                        selStr.append(" || accountid = ").append(accID);
                    }
                }
                ps2 = con.prepareStatement(selStr.toString());
                rs = ps2.executeQuery();
                while (rs.next()) {
                    characterIDs.add(rs.getInt("id"));
                }

                int[] results = ps.executeBatch();
                for (int i : results) {
                    charsDeleted += i;
                }
            } catch (SQLException ex) {
            } finally {
                try{ps.clearBatch();ps.close();ps2.close();}catch(SQLException se){}
            }
            System.out.println("Thread "+threadid+": Characters Deleted: "+charsDeleted+" (Total Characters: "+totalCharacters+")");

            ensureCompleteRemoval(characterIDs);

            int accountsDeleted = 0;
            try {
                ps = con.prepareStatement("DELETE FROM accounts WHERE id = ?");
                for (Integer accID : accountIDs) {
                    ps.setInt(1, accID);
                    ps.addBatch();
                }
                int[] results = ps.executeBatch();
                for (int i : results) {
                    accountsDeleted += i;
                }
            } catch (SQLException ex) {
            } finally {
                try{ps.close();}catch(SQLException se){}
            }
            System.out.println("Thread "+threadid+": Accounts Deleted: "+accountsDeleted);
    }

    private void ensureCompleteRemoval(ArrayList<Integer> characterIDs) {
        handleGuilds(characterIDs);
        //handlePlayerBBS(characterIDs);
        handleBuddyLists(characterIDs);
       // handleFameLog(characterIDs);
        handleCheatLog(characterIDs);
       handleSkillMacros(characterIDs);
        handleSkills(characterIDs);
    //    handlePets(characterIDs);
    }

    private void handlePets(ArrayList<Integer> characterIDs) {
        PreparedStatement ps = null;
        StringBuilder selStr = new StringBuilder("SELECT petid FROM inventoryitems WHERE ");
        boolean first = true;
        for (Integer charID : characterIDs) {
            if (first) {
                first = false;
                selStr.append("characterid = ").append(charID);
            } else {
                selStr.append(" || characterid = ").append(charID);
            }
        }
        ArrayList<Integer> petDeleted = new ArrayList<>();
        try {
            ps = DatabaseConnection.getConnection().prepareStatement(selStr.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                petDeleted.add(rs.getInt("petid"));
            }
        } catch (SQLException se) {
            System.out.println("Thread "+threadid+": ERROR HANDLING PETS (Getting petids): "+se.toString());
        } finally {
            try{ps.close();}catch(SQLException se){}
        }

        if (!petDeleted.isEmpty()) {
            try {
                ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM pets WHERE petid = ?");
                for (Integer petID : petDeleted) {
                    ps.setInt(1, petID);
                    ps.addBatch();
                }
                int[] ret = ps.executeBatch();
                int count = 0;
                for (int i : ret) {
                    count += i;
                }
                System.out.println("Thread "+threadid+": Deleted "+count+" pet records.");
            } catch (SQLException se) {
                System.out.println("Thread "+threadid+": ERROR DELETING PET RECORDS: "+se.toString());
            } finally {
                try{ps.close();}catch(SQLException se){}
            }
        }
    }
    
    
    private void handleSkills(ArrayList<Integer> characterIDs) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM skills WHERE characterid = ?");
            for (Integer charID : characterIDs) {
                ps.setInt(1, charID);
                ps.addBatch();
            }
            int[] ret = ps.executeBatch();
            int count = 0;
            for (int i : ret) {
                count += i;
            }
            System.out.println("Thread "+threadid+": Deleted "+count+" skill macro records.");
        } catch (SQLException se) {
            System.out.println("Thread "+threadid+": ERROR HANDLING SKILL MACROS: "+se.toString());
        } finally {
            try{ps.close();}catch(SQLException se){}
        }
    }

    private void handleSkillMacros(ArrayList<Integer> characterIDs) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM skillmacros WHERE characterid = ?");
            for (Integer charID : characterIDs) {
                ps.setInt(1, charID);
                ps.addBatch();
            }
            int[] ret = ps.executeBatch();
            int count = 0;
            for (int i : ret) {
                count += i;
            }
            System.out.println("Thread "+threadid+": Deleted "+count+" skill macro records.");
        } catch (SQLException se) {
            System.out.println("Thread "+threadid+": ERROR HANDLING SKILL MACROS: "+se.toString());
        } finally {
            try{ps.close();}catch(SQLException se){}
        }
    }

    private void handleCheatLog(ArrayList<Integer> characterIDs) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM inventoryitems WHERE characterid = ?");
            for (Integer charID : characterIDs) {
                ps.setInt(1, charID);
                ps.addBatch();
            }
            int[] ret = ps.executeBatch();
            int count = 0;
            for (int i : ret) {
                count += i;
            }
            System.out.println("Thread "+threadid+": Deleted "+count+" items");
        } catch (SQLException se) {
            System.out.println("Thread "+threadid+": ERROR HANDLING items: "+se.toString());
        } finally {
            try{ps.close();}catch(SQLException se){}
        }
    }

    private void handleRings(ArrayList<Integer> characterIDs) {
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        ArrayList<Integer> deleteRings = new ArrayList<>();
        StringBuilder selStr = new StringBuilder("SELECT id FROM rings WHERE ");
        boolean first = true;
        try {
            ps2 = DatabaseConnection.getConnection().prepareStatement("DELETE FROM rings WHERE partnerchrid = ?");
            for (Integer charID : characterIDs) {
                ps2.setInt(1,charID);
                ps2.addBatch();
                if (first) {
                    first = false;
                    selStr.append("partnerchrid = ").append(charID);
                } else {
                    selStr.append(" || partnerchrid = ").append(charID);
                }
            }
            ps = DatabaseConnection.getConnection().prepareStatement(selStr.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                deleteRings.add(rs.getInt("id"));
            }

            int[] ret = ps2.executeBatch();
            int count = 0;
            for (int i : ret) {
                count += i;
            }
            System.out.println("Thread "+threadid+": Deleted "+count+" Ring Entries");
        } catch (SQLException se) {
            System.out.println("Thread "+threadid+": ERROR HANDLING RINGS (Getting ringids) "+se.toString());
        } finally {
            try{ps.close();ps2.close();}catch(SQLException se){}
        }

        if (!deleteRings.isEmpty()) {
            ArrayList<Integer> inventoryItemIDDeleted = new ArrayList<>();
            selStr = new StringBuilder("SELECT inventoryitemid FROM inventoryequipment WHERE ");
            first = true;
            try {
                for (Integer ringID : deleteRings) {
                    if (first) {
                        first = false;
                        selStr.append("ringid = ").append(ringID);
                    } else {
                        selStr.append(" || ringid = ").append(ringID);
                    }
                }
                ps = DatabaseConnection.getConnection().prepareStatement(selStr.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    inventoryItemIDDeleted.add(rs.getInt("inventoryitemid"));
                }
            } catch (SQLException se) {
                System.out.println("Thread "+threadid+": ERROR HANDLING RINGS (Getting inventoryitemids) "+se.toString());
            } finally {
                try{ps.close();}catch(SQLException se){}
            }

            if (!inventoryItemIDDeleted.isEmpty()) {
                try {
                    ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM inventoryitems WHERE inventoryitemid = ?");
                    for (Integer inventoryItemID : inventoryItemIDDeleted) {
                        ps.setInt(1, inventoryItemID);
                        ps.addBatch();
                    }
                    int[] ret = ps.executeBatch();
                    int count = 0;
                    for (int i : ret) {
                        count += i;
                    }
                    System.out.println("Thread "+threadid+": Deleted "+count+" inventoryitem Entries while deleting rings.");
                } catch (SQLException se) {
                    System.out.println("Thread "+threadid+": ERROR HANDLING RINGS (Deleting from inventoryitem) "+se.toString());
                } finally {
                    try{ps.close();}catch(SQLException se){}
                }
            } else {
                System.out.println("Thread "+threadid+": No inventoryitem entries to be deleted while deleting rings, moving along.");
            }
        } else {
            System.out.println("Thread "+threadid+": No rings to be deleted, moving along.");
        }
    }

    private void handleFameLog(ArrayList<Integer> characterIDs) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM famelog WHERE characterid = ? || characterid_to = ?");
            for (Integer charID : characterIDs) {
                ps.setInt(1, charID);
                ps.setInt(2, charID);
                ps.addBatch();
            }
            int[] ret = ps.executeBatch();
            int count = 0;
            for (int i : ret) {
                count += i;
            }
            System.out.println("Thread "+threadid+": Deleted "+count+" fame records.");
        } catch (SQLException se) {
            System.out.println("Thread "+threadid+": ERROR HANDLING FAMELOG: "+se.toString());
        } finally {
            try{ps.close();}catch(SQLException se){}
        }
    }

    private void handleBuddyLists(ArrayList<Integer> characterIDs) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM buddies WHERE characterid = ? || buddyid = ?");
            for (Integer charID : characterIDs) {
                ps.setInt(1, charID);
                ps.setInt(2, charID);
                ps.addBatch();
            }
            int[] ret = ps.executeBatch();
            int count = 0;
            for (int i : ret) {
                count += i;
            }
            System.out.println("Thread "+threadid+": Deleted "+count+" buddy records.");
        } catch (SQLException se) {
            System.out.println("Thread "+threadid+": ERROR HANDLING BUDDYLISTS: "+se.toString());
        } finally {
            try{ps.close();}catch(SQLException se){}
        }
    }

    private void handleGuilds(ArrayList<Integer> characterIDs) {
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        StringBuilder selStr = new StringBuilder("SELECT guildid FROM guilds WHERE ");
        boolean first = true;

        ArrayList<Integer> deletedGuilds = new ArrayList<>();
        for (Integer charID : characterIDs) {
            if (first) {
                first = false;
                selStr.append("leader = ").append(charID);
            } else {
                selStr.append(" || leader = ").append(charID);
            }
        }
        try {
            ps = DatabaseConnection.getConnection().prepareStatement(selStr.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                deletedGuilds.add(rs.getInt("guildid"));
            }
        } catch (SQLException se) {
            System.out.println("Thread "+threadid+": ERROR HANDLING GUILDS: "+se.toString());
        } finally {
            try{ps.close();}catch(SQLException se){}
        }

        if (!deletedGuilds.isEmpty()) {
         //   handleGuildBBS(deletedGuilds);

            selStr = new StringBuilder("UPDATE characters SET guildid = 0, guildrank = 5 WHERE ");
            first = true;
            try {
                ps2 = DatabaseConnection.getConnection().prepareStatement("DELETE FROM guilds WHERE guildid = ?");
                for (Integer guildID : deletedGuilds) {
                    ps2.setInt(1, guildID);
                    ps2.addBatch();
                    if (first) {
                        first = false;
                        selStr.append("guildid = ").append(guildID);
                    } else {
                        selStr.append(" || guildid = ").append(guildID);
                    }
                }
                ps = DatabaseConnection.getConnection().prepareStatement(selStr.toString());
                int ret2 = ps.executeUpdate();
                int[] ret = ps2.executeBatch();

                int count = 0;
                for (int i : ret) {
                    count += i;
                }

                System.out.println("Thread "+threadid+": Reset guildid and giuldrank for "+ret2+" characters.");
                System.out.println("Thread "+threadid+": Deleted "+count+" guilds.");
            } catch (SQLException se) {
                System.out.println("Thread "+threadid+": ERROR DELETING GUILDS/RESETTING GUILDS: "+se.toString());
            } finally {
                try{ps.close();ps2.close();}catch(SQLException se){}
            }
        } else {
            System.out.println("Thread "+threadid+": No guilds need to be deleted, moving along.");
        }

    }

    private void handleGuildBBS(ArrayList<Integer> guildIDs) {
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        StringBuilder selStr = new StringBuilder("SELECT threadid FROM bbs_threads WHERE ");
        boolean first = true;
        ArrayList<Integer> deletedThreads = new ArrayList<>();
        for (Integer guildID : guildIDs) {
            if (first) {
                first = false;
                selStr.append("guildid = ").append(guildID);
            } else {
                selStr.append(" || guildid = ").append(guildID);
        }
        }
        try {
            ps = DatabaseConnection.getConnection().prepareStatement(selStr.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                deletedThreads.add(rs.getInt("threadid"));
            }
        } catch (SQLException se) {
            System.out.println("Thread "+threadid+": ERROR HANDLING GUILD BBS THREADS: "+se.toString());
        } finally {
            try{ps.close();}catch(SQLException se){}
        }

        if (!deletedThreads.isEmpty()) {
            try {
                ps = DatabaseConnection.getConnection().prepareStatement("DELETE FROM bbs_replies WHERE threadid = ?");
                ps2 = DatabaseConnection.getConnection().prepareStatement("DELETE FROM bbs_threads WHERE threadid = ?");
                for (Integer threadID : deletedThreads) {
                    ps.setInt(1, threadID);
                    ps2.setInt(2, threadID);
                    ps.addBatch();
                    ps2.addBatch();
                }
                int[] ret = ps.executeBatch();
                int count = 0;
                for (int i : ret) {
                    count += i;
                }
                System.out.println("Thread "+threadid+": Deleted "+count+" bbs_replies while deleting guilds.");

                int[] ret2 = ps2.executeBatch();
                count = 0;
                for (int i : ret2) {
                    count += i;
                }
                System.out.println("Thread "+threadid+": Deleted "+count+" bbs_threads while deleting guilds.");

            } catch (SQLException se) {
                System.out.println("Thread "+threadid+": ERROR DELETING GUILD BBS THREADS/REPLIES: "+se.toString());
            } finally {
                try{ps.close();ps2.close();}catch(SQLException se){}
            }
        } else {
            System.out.println("Thread "+threadid+": No BBS Threads/Replies need to be deleted while deleting Guilds.");
        }
    }

    
}

class TextAreaOutputStream extends OutputStream {
    private JTextArea                       textArea;
    private int                             maxLines;
    private LinkedList<Integer>                      lineLengths;
    private int                             curLength;
    private byte[]                          oneByte;

    public TextAreaOutputStream(JTextArea ta) {
        this(ta,1000);
    }

    public TextAreaOutputStream(JTextArea ta, int ml) {
        if ( ml<1 ) {
            ml = 50;
        }
        textArea=ta;
        maxLines=ml;
        lineLengths = new LinkedList<>();
        curLength=0;
        oneByte=new byte[1];
    }
    public synchronized void clear() {
        lineLengths = new LinkedList<>();
        curLength=0;
        textArea.setText("");
    }

    public synchronized int getMaximumLines() { return maxLines; }

    public synchronized void setMaximumLines(int val) { maxLines=val; }

    @Override
    public void close() {
        if(textArea!=null) {
            textArea=null;
            lineLengths=null;
            oneByte=null;
        }
    }

    @Override
    public void flush() {}

    @Override
    public void write(int val) {
        oneByte[0]=(byte)val;
        write(oneByte,0,1);
    }

    @Override
    public void write(byte[] ba) {
        write(ba,0,ba.length);
    }

    @Override
    public synchronized void write(byte[] ba,int str,int len) {
        try {
            curLength+=len;
            if (bytesEndWith(ba,str,len,LINE_SEP)) {
                lineLengths.addLast(new Integer(curLength));
                curLength=0;
                if(lineLengths.size()>maxLines) {
                    textArea.replaceRange(null,0,((Integer)lineLengths.removeFirst()).intValue());
                }
            }
            for (int xa=0; xa<10; xa++) {
                try { textArea.append(new String(ba,str,len)); break; }
                catch(Throwable thr) {                                                 // sometimes throws a java.lang.Error: Interrupted attempt to aquire write lock
                    if(xa==9) {}
                    else      { Thread.sleep(200);    }
                    }
            }
        } catch(Throwable thr) {
            CharArrayWriter caw=new CharArrayWriter();
            thr.printStackTrace(new PrintWriter(caw,true));
            textArea.append(System.getProperty("line.separator","\n"));
            textArea.append(caw.toString());
        }
    }

    private boolean bytesEndWith(byte[] ba, int str, int len, byte[] ew) {
        if (len < LINE_SEP.length) { return false; }
        for (int xa=0,xb=(str+len-LINE_SEP.length); xa<LINE_SEP.length; xa++,xb++) {
            if(LINE_SEP[xa]!=ba[xb]) { return false; }
        }
        return true;
    }

    static private byte[] LINE_SEP = System.getProperty("line.separator","\n").getBytes();
}