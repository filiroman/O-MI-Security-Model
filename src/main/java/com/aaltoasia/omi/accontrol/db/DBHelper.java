package com.aaltoasia.omi.accontrol.db;

import com.aaltoasia.omi.accontrol.ConfigHelper;
import com.aaltoasia.omi.accontrol.db.objects.OMIGroup;
import com.aaltoasia.omi.accontrol.db.objects.OMIRule;
import com.aaltoasia.omi.accontrol.db.objects.OMIUser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by romanfilippov on 19/11/15.
 */
public class DBHelper {

    private int DEFAULT_GROUP_ID;
    private Connection connection;

    private static final Logger logger = LoggerFactory.getLogger(DBHelper.class.getName());
    private final String pass_salt = "INSERT_SALT_PHRASE_HERE";

    private static final DBHelper instance = createInstance();
    private DBHelper() {
//        logger.setLevel(Level.INFO);
        configureDB();
    }

    private static DBHelper createInstance()
    {
        try {
            return new DBHelper();
        } catch (Exception e) {
            logger.error("When initializing DBHelper module ",e);

        }
        return null;
    }

    public static DBHelper getInstance() {

        return instance;
    }

    private void createTables() throws SQLException
    {
        Statement stmt = connection.createStatement();
        String sql = "CREATE TABLE USERS " +
                "(ID INTEGER PRIMARY KEY     NOT NULL," +
                " USERNAME       VARCHAR(256)    NOT NULL,"+
                " PASSWORD       VARCHAR(256)    ,"+
                " EMAIL       VARCHAR(256)    UNIQUE NOT NULL)";
        stmt.executeUpdate(sql);

        sql = "CREATE TABLE RULES " +
                "(ID INTEGER PRIMARY KEY     NOT NULL," +
                " HID            TEXT     NOT NULL," +
                " GROUP_ID          INT    NOT NULL," +
                " WRITE_PERMISSIONS   INT     NOT NULL," +
                " OBJECT_RULE INT NOT NULL)";
        stmt.executeUpdate(sql);

        sql = "CREATE TABLE GROUPS " +
                "(ID INTEGER PRIMARY KEY     NOT NULL," +
                " GROUP_NAME          VARCHAR(256)    UNIQUE NOT NULL)";
        stmt.executeUpdate(sql);

        sql = "CREATE TABLE USERS_GROUPS_RELATION " +
                "(ID INTEGER PRIMARY KEY     NOT NULL," +
                " USER_ID          INT    NOT NULL," +
                " GROUP_ID          INT    NOT NULL)";
        stmt.executeUpdate(sql);

        sql = "CREATE TABLE ADMINISTRATORS " +
                "(ID INTEGER PRIMARY KEY NOT NULL," +
                "EMAIL VARCHAR(256) UNIQUE NOT NULL)";
        stmt.executeUpdate(sql);
        stmt.close();

        DEFAULT_GROUP_ID = createGroup("Default");
    }

    public boolean checkUserCredentials (OMIUser user) {

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM USERS WHERE USERNAME=? OR EMAIL=?");
            stmt.setString(1, user.username);
            stmt.setString(2, user.username);
            ResultSet rs = stmt.executeQuery();
            boolean res = rs.next();
            if (res) {
                String db_pass = rs.getString("PASSWORD");
                String entered_pass = get_SHA_256_SecurePassword(user.password, pass_salt.getBytes());
                return (entered_pass.equals(db_pass));
            } else {
                stmt.close();
                return false;
            }

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    private void createAdmin(String email) {

        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO ADMINISTRATORS(EMAIL) VALUES(?)")){
            stmt.setString(1,email);
            stmt.executeUpdate();

            int res = stmt.getGeneratedKeys().getInt(1);
            stmt.close();
            logger.info("Administrator with email:"+email+" successfully created. ID="+res);

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
        }
    }

    public boolean checkAdminPermissions(String userID)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM ADMINISTRATORS WHERE EMAIL=?");
            stmt.setString(1, userID);
            ResultSet rs = stmt.executeQuery();
            boolean res = rs.next();
            stmt.close();
            return res;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    private void removeOldAdministrators() {
        try {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("DELETE FROM ADMINISTRATORS");
            stmt.close();

        } catch (SQLException ex)
        {
            logger.error("While deleting administrators: ",ex);
        }
    }

    private void createAdministrators()
    {
        removeOldAdministrators();

        // Number of first lines in file that are used for comments
        int commentLines = 1;
        int i = 0;

        try(BufferedReader br = new BufferedReader(new FileReader(ConfigHelper.adminFileName))) {
            String line = br.readLine();

            while (line != null) {

                if (i<commentLines) {
                    i++;
                } else {
                    createAdmin(line);
                }

                line = br.readLine();
            }
        } catch (Exception ex) {
            logger.error("Error:",ex);
        }
    }

    public int createGroup(String groupName)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO GROUPS(GROUP_NAME) VALUES(?)");
            stmt.setString(1,groupName);
            stmt.executeUpdate();

            int res = stmt.getGeneratedKeys().getInt(1);
            stmt.close();
            logger.info("Group with name:"+groupName+" successfully created. ID="+res);
            return res;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return -1;
        }
    }

    public boolean updateGroup(int groupID, String groupName)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE GROUPS SET GROUP_NAME = ? WHERE ID = ?;");
            stmt.setString(1,groupName);
            stmt.setInt(2,groupID);
            stmt.executeUpdate();
            stmt.close();
            logger.info("Group with ID:"+groupID+" successfully updated.");
            return true;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    public boolean deleteGroup(int groupID)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM GROUPS WHERE ID=?;");
            stmt.setInt(1,groupID);
            stmt.executeUpdate();

            stmt = connection.prepareStatement("DELETE FROM RULES WHERE GROUP_ID=?;");
            stmt.setInt(1,groupID);
            stmt.executeUpdate();

            stmt = connection.prepareStatement("DELETE FROM USERS_GROUPS_RELATION WHERE GROUP_ID=?;");
            stmt.setInt(1,groupID);
            stmt.executeUpdate();

            logger.info("Group with ID="+groupID+" deleted successfully. Related rules were removed and users removed from the group");
            stmt.close();
            return true;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    public ArrayList<OMIGroup> getGroups ()
    {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery( "SELECT * FROM GROUPS" );
            ArrayList<OMIGroup> resultsArray = new ArrayList<OMIGroup>();
            while ( rs.next() ) {
                OMIGroup nextGroup = new OMIGroup();
                nextGroup.id = rs.getInt("ID");
                nextGroup.name = rs.getString("GROUP_NAME");


                //TODO: requires optimization!
                PreparedStatement prst = connection.prepareStatement("SELECT USER_ID FROM USERS_GROUPS_RELATION WHERE GROUP_ID=?;");
                prst.setInt(1, nextGroup.id);
                ResultSet rs2 = prst.executeQuery();

                ArrayList<Integer> userIDs = new ArrayList<Integer>();
                while ( rs2.next() ) {
                    userIDs.add(rs2.getInt("USER_ID"));
                }

                nextGroup.userIDs = userIDs;
                rs2.close();
                prst.close();

                resultsArray.add(nextGroup);
            }
            rs.close();
            stmt.close();
            logger.info("Groups fetch request finished. Size:"+resultsArray.size());
            return resultsArray;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return null;
        }
    }

    public ArrayList<OMIGroup> getGroups (int[] groupIDs)
    {
        try {
            if (groupIDs.length < 1)
            {
                return new ArrayList<OMIGroup>();
            }

            String query = "SELECT * FROM GROUPS WHERE ID IN(";

            for (int i = 0; i < groupIDs.length-1; i++) {
                query += "?,";
            }

            query += "?);";
            PreparedStatement stmt = connection.prepareStatement(query);

            for (int i = 0; i < groupIDs.length; i++) {
                stmt.setInt(i+1, groupIDs[i]);
            }

            ResultSet rs = stmt.executeQuery(query);
            ArrayList<OMIGroup> resultsArray = new ArrayList<OMIGroup>();
            while ( rs.next() ) {
                OMIGroup nextGroup = new OMIGroup();
                nextGroup.id = rs.getInt("ID");
                nextGroup.name = rs.getString("GROUP_NAME");

                resultsArray.add(nextGroup);
            }
            rs.close();
            stmt.close();
            logger.info("Groups fetch request finished. Size:"+resultsArray.size());
            return resultsArray;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return null;
        }
    }

    public int getGroupID(String groupName) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT ID FROM GROUPS WHERE GROUP_NAME=?");
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            if ( rs.next() ) {
                int res = rs.getInt("ID");
                stmt.close();
                return res;
            }
            stmt.close();
            return -1;
        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return -1;
        }
    }

    public boolean addUserToGroup (int userID, int groupID)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO USERS_GROUPS_RELATION(USER_ID,GROUP_ID) VALUES(?,?)");
            stmt.setInt(1,userID);
            stmt.setInt(2,groupID);
            stmt.executeUpdate();
            stmt.close();

            logger.info("User with ID="+userID+" successfully added to the group with ID="+groupID);
            return true;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    public boolean addUsersToGroup (int[] userIDs, int groupID)
    {
        try {
            String query = "INSERT INTO USERS_GROUPS_RELATION(USER_ID,GROUP_ID) VALUES ";

            for (int i = 0; i < userIDs.length-1; i++) {
                query += "(?,?),";
            }

            query += "(?,?);";

            PreparedStatement stmt = connection.prepareStatement(query);
            for (int i = 1; i < userIDs.length+1; i++) {
                stmt.setInt(2*i-1,userIDs[i-1]);
                stmt.setInt(2*i,groupID);
            }
            stmt.executeUpdate();
            stmt.close();

            logger.info("Users list successfully added to the group with ID="+groupID);
            return true;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    public boolean removeUserFromGroup (int userID, int groupID)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM USERS_GROUPS_RELATION WHERE USER_ID=? AND GROUP_ID=?");
            stmt.setInt(1,userID);
            stmt.setInt(2,groupID);
            stmt.executeUpdate();
            stmt.close();

            logger.info("User with ID="+userID+" successfully removed from the group with ID="+groupID);
            return true;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    public boolean removeUsersFromGroup (int groupID)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM USERS_GROUPS_RELATION WHERE GROUP_ID=?;");
            stmt.setInt(1, groupID);
            stmt.executeUpdate();
            stmt.close();

            logger.info("All users successfully removed from the group with ID="+groupID);
            return true;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    public boolean updateUsersForGroup (int[] userIDs, int groupID)
    {
//        try {

            // TODO: query optimization
//            String query = "DELETE FROM USERS_GROUPS_RELATION WHERE USER_ID NOT IN (";
//            for (int i = 0; i < userIDs.length-1; i++) {
//                query += "?,";
//            }
//
//            query += "?) AND GROUP_ID=?;";
//            PreparedStatement stmt = connection.prepareStatement(query);
//
//            for (int i = 0; i < userIDs.length; i++) {
//                stmt.setInt(i+1, userIDs[i]);
//            }
//
//            stmt.setInt(userIDs.length+1,groupID);
//            stmt.executeUpdate();


            // Delete all users from current group (may be optimized)
            removeUsersFromGroup(groupID);

            // Add users from response
            addUsersToGroup(userIDs, groupID);

            logger.info("User list successfully updated for the group with ID="+groupID);
            return true;

//        } catch (SQLException ex)
//        {
//            logger.error("Error:",ex);
//            return false;
//        }
    }

    public boolean updateOrCreateRule(String HID, int groupID, boolean writable, boolean objectRule)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("UPDATE RULES SET WRITE_PERMISSIONS = ?, OBJECT_RULE = ? WHERE HID = ? AND GROUP_ID = ?;");
            stmt.setBoolean(1,writable);
            stmt.setBoolean(2,objectRule);
            stmt.setString(3,HID);
            stmt.setInt(4,groupID);
            int rows = stmt.executeUpdate();
            if (rows == 0)
            {
                logger.info("Record for HID:"+HID+" not found. Creating new.");
                createRule(HID, groupID, writable, objectRule);
            } else {
                logger.info("Record for HID:"+HID+" was updated.");
            }
            stmt.close();
            return true;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    public boolean createRule(String HID, int groupID, boolean writable, boolean objectRule)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO RULES(HID,GROUP_ID,WRITE_PERMISSIONS,OBJECT_RULE) VALUES(?,?,?,?)");
            stmt.setString(1,HID);
            stmt.setInt(2,groupID);
            stmt.setBoolean(3,writable);
            stmt.setBoolean(4,objectRule);
            stmt.executeUpdate();
            stmt.close();

            logger.info("Record for HID:"+HID+" successfully created.");
            return true;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    public ArrayList<OMIRule> getRules(int groupID)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM RULES WHERE GROUP_ID=?;");
            stmt.setInt(1, groupID);
            ResultSet rs = stmt.executeQuery();
            ArrayList<OMIRule> resultsArray = new ArrayList<OMIRule>();
            while ( rs.next() ) {
                OMIRule nextRule = new OMIRule();
                nextRule.id = rs.getInt("ID");
                nextRule.hid = rs.getString("HID");
                nextRule.groupID = rs.getInt("GROUP_ID");
                nextRule.writePermissions = rs.getInt("WRITE_PERMISSIONS");

                resultsArray.add(nextRule);
            }
            rs.close();
            stmt.close();

            logger.info("Rules fetch request finished for group:"+groupID+". Size:"+resultsArray.size());
            return resultsArray;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return null;
        }
    }

    public boolean deleteRule(String HID, int groupID)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM RULES WHERE HID=? AND GROUP_ID=?");
            stmt.setString(1,HID);
            stmt.setInt(2,groupID);
            stmt.executeUpdate();
            stmt.close();

            logger.info("Record for HID:"+HID+" successfully deleted.");
            return true;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    public OMIUser getUser (String userID)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM USERS WHERE EMAIL=? OR USERNAME=?");
            stmt.setString(1,userID);
            stmt.setString(2,userID);
            ResultSet rs = stmt.executeQuery();
            OMIUser nextUser = new OMIUser();
            if ( rs.next() ) {
                nextUser.id = rs.getInt("ID");
                nextUser.username = rs.getString("USERNAME");
                nextUser.email = rs.getString("EMAIL");

            }
            rs.close();
            stmt.close();

            return nextUser;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return null;
        }
    }

    public ArrayList<OMIUser> getUsers ()
    {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery( "SELECT * FROM USERS;" );
            ArrayList<OMIUser> resultsArray = new ArrayList<OMIUser>();
            while ( rs.next() ) {
                OMIUser nextUser = new OMIUser();
                nextUser.id = rs.getInt("ID");
                nextUser.username = rs.getString("USERNAME");
                nextUser.email = rs.getString("EMAIL");

                resultsArray.add(nextUser);
            }
            rs.close();
            stmt.close();

            logger.info("Users fetch request finished. Size:"+resultsArray.size());
            return resultsArray;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return null;
        }
    }

    public boolean checkIfUserExists(OMIUser user) {

        try {

            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM USERS WHERE EMAIL=?");
            stmt.setString(1, user.email);
            ResultSet rs = stmt.executeQuery();

            return rs.isBeforeFirst();

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }

    }

    public int createUser(OMIUser user)
    {
        try {
            String statement = null;

            if (user.password != null)
                statement = "INSERT INTO USERS(USERNAME,EMAIL,PASSWORD) VALUES(?,?,?)";
            else
                statement = "INSERT INTO USERS(USERNAME,EMAIL) VALUES(?,?)";

            PreparedStatement stmt = connection.prepareStatement(statement);
            stmt.setString(1,user.username);
            stmt.setString(2,user.email);

            if (user.password != null) {
                String pass_hash = get_SHA_256_SecurePassword(user.password, pass_salt.getBytes());
                stmt.setString(3, pass_hash);
            }

            stmt.executeUpdate();

            int res = stmt.getGeneratedKeys().getInt(1);
            stmt = connection.prepareStatement("INSERT INTO USERS_GROUPS_RELATION(USER_ID,GROUP_ID) VALUES(?,?)");
            stmt.setInt(1, res);
            stmt.setInt(2, DEFAULT_GROUP_ID);

            stmt.executeUpdate();
            stmt.close();

            logger.info("User with name:"+user.username+" and email:" +user.email + " successfully created. ID="+res);
            return res;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return -1;
        }
    }

    public boolean  createUserIfNotExists(OMIUser user)
    {
        try {

            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM USERS WHERE EMAIL=? OR USERNAME=?");
            stmt.setString(1,user.email);
            stmt.setString(2,user.username);
            ResultSet rs = stmt.executeQuery();

            // No such users, insert one
            if (!rs.isBeforeFirst()) {

                String statement = null;

                if (user.password != null)
                    statement = "INSERT INTO USERS(USERNAME,EMAIL,PASSWORD) VALUES(?,?,?)";
                else
                    statement = "INSERT INTO USERS(USERNAME,EMAIL) VALUES(?,?)";

                stmt = connection.prepareStatement(statement);
                stmt.setString(1,user.username);
                stmt.setString(2,user.email);

                if (user.password != null) {
                    String pass_hash = get_SHA_256_SecurePassword(user.password, pass_salt.getBytes());
                    stmt.setString(3, pass_hash);
                }

                boolean res = (stmt.executeUpdate() != 0);

                if (!res)
                    return false;


                // Add new user to Default group (everybody belongs it)
                int userID = stmt.getGeneratedKeys().getInt(1);
                stmt = connection.prepareStatement("INSERT INTO USERS_GROUPS_RELATION(USER_ID,GROUP_ID) VALUES(?,?)");
                stmt.setInt(1, userID);
                stmt.setInt(2, DEFAULT_GROUP_ID);

                res = (stmt.executeUpdate() != 0);
                stmt.close();
                logger.info("New user created successfully. ID="+userID);
                return res;
            } else {
                return true;
            }

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    public ArrayList<String> getRulesByUser(String userEmail)
    {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT GROUP_ID FROM USERS_GROUPS_RELATION WHERE USER_ID=(SELECT ID FROM USERS WHERE EMAIL=?)")){
            stmt.setString(1,userEmail);
            ResultSet rs = stmt.executeQuery();
            //ArrayList<Integer> groupIDs = new ArrayList<>();

            String groupIDs = "(";

            boolean hasNext = rs.next();
            while (hasNext) {

                groupIDs += rs.getInt(1);
                hasNext = rs.next();
                if (hasNext) {
                    groupIDs += ",";
                }
            }
            groupIDs+= ")";
            stmt.close();


            Statement statement = connection.createStatement();
            rs = statement.executeQuery("SELECT HID FROM RULES WHERE GROUP_ID IN " + groupIDs);
            ArrayList<String> rules = new ArrayList<>();

            while (rs.next()) {
                rules.add(rs.getString(1));
            }
            statement.close();

            return rules;

        } catch (SQLException ex) {
            logger.error("Error:",ex);
            return null;
        }
    }

    public boolean checkUserPermissions(ArrayList<String> paths, String userEmail, boolean isWrite)
    {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT GROUP_ID FROM USERS_GROUPS_RELATION WHERE USER_ID=(SELECT ID FROM USERS WHERE EMAIL=?)");
            stmt.setString(1,userEmail);
            ResultSet rs = stmt.executeQuery();
            //ArrayList<Integer> groupIDs = new ArrayList<>();

            String groupIDs = "(";

            boolean hasNext = rs.next();
            while (hasNext) {

                groupIDs += rs.getInt(1);
                hasNext = rs.next();
                if (hasNext) {
                    groupIDs += ",";
                }
            }

//            while (rs.next()) {
//
//                if (rs.isLast()) {
//                    groupIDs += rs.getInt(1);
//                    queryPlaceHolder += "?";
//                }
//                else {
//                    groupIDs += rs.getInt(1) + ",";
//                    queryPlaceHolder += "?,";
//                }
//
//                //groupIDs.add(new Integer(rs.getInt(1)));
//            }

            groupIDs+= ")";

            logger.info("Resource access request for groups "+groupIDs);

            stmt.close();
            stmt = connection.prepareStatement("SELECT WRITE_PERMISSIONS FROM RULES WHERE " +
                    "(HID=? OR ((? LIKE '%'||HID||'%') AND OBJECT_RULE=1)) AND GROUP_ID IN " + groupIDs);

            for (String omiPath:paths) {
                stmt.setString(1, omiPath);
                stmt.setString(2, omiPath);
                rs = stmt.executeQuery();

                logger.info("Checking permissions for HID:"+omiPath);

                // No rules for that HID, deny all request
                if (!rs.isBeforeFirst()) {
                    logger.info("No permissions for HID:"+omiPath+" found. Request denied.");
                    stmt.close();
                    return false;
                } else {
                    boolean db_write = false;
                    while (rs.next()) {
                        db_write = rs.getInt(1) == 1;

                        if (db_write)
                            break;
                    }

                    // If in DB we have read permissions but write is requested
                    if (!db_write && isWrite) {
                        logger.info("Read permission for HID:"+omiPath+" found, but write requested. Request denied.");
                        stmt.close();
                        return false;
                    }

                    logger.info("Requested permissions for HID:"+omiPath+" successfully found. Request allowed.");
                }
            }

            return true;

        } catch (SQLException ex)
        {
            logger.error("Error:",ex);
            return false;
        }
    }

    private String get_SHA_256_SecurePassword(String passwordToHash, byte[] salt)
    {
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] bytes = md.digest(passwordToHash.getBytes());
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++)
            {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            logger.error("Error:",e);
        }
        return generatedPassword;
    }

    private boolean configureDB()
    {
        String dbName = ConfigHelper.dbName + ".db";
        String jdbcDriver = "jdbc:sqlite:"+ dbName;

        File file = new File(dbName);
        boolean dbExists = file.exists();

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(jdbcDriver);
        } catch ( Exception e ) {
            logger.error("While loading JDBC driver: ",e);
            return false;
        }

        if (!dbExists)
        {
            logger.info("Creating new database");

            try {
                createTables();
            } catch (SQLException ex)
            {
                logger.error("Error while creating tables: ", ex);
                return false;
            }

            logger.info("Created tables successfully.");
        } else {
            logger.info("Opened database successfully. Path:"+file.getAbsolutePath());

            DEFAULT_GROUP_ID = getGroupID("Default");
            if (DEFAULT_GROUP_ID == -1)
                DEFAULT_GROUP_ID = createGroup("Default");
        }

        createAdministrators();
        return true;
    }

}
