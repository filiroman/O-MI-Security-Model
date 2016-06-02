package com.aaltoasia.omi.accontrol;

import com.aaltoasia.omi.accontrol.db.*;
import com.aaltoasia.omi.accontrol.db.objects.*;
import com.google.gson.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by romanfilippov on 23/11/15.
 */
public class PermissionService extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public void init() throws ServletException
    {
//        logger.setLevel(Level.INFO);
        // Do required initialization
    }


    // Wrap array with top-level key
    private String wrapJson(Object objectToSerialize, String keyName) {
        Gson gson = new Gson();
        JsonObject result = new JsonObject();
        //Obtain a serialized version of your object
        JsonElement jsonElement = gson.toJsonTree(objectToSerialize);
        result.add(keyName, jsonElement);
        return gson.toJson(result);
    }

    protected void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException
    {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String readUsers = request.getParameter("readUsers");
        String readGroups = request.getParameter("readGroups");
        String removeGroups = request.getParameter("removeGroups");
        String readRules = request.getParameter("readRules");
        String getUserInfo = request.getParameter("getUserInfo");

        PrintWriter out = response.getWriter();

        if (getUserInfo != null) {

            HttpSession session = request.getSession(false);
            if (session != null) {
                String userEmail = (String)session.getAttribute("userID");
                OMIUser user = DBHelper.getInstance().getUser(userEmail);
                Gson gson = new Gson();
                out.println(gson.toJson(user));
            }

        } else if (readUsers != null) {

            ArrayList<OMIUser> users = DBHelper.getInstance().getUsers();
            out.println(wrapJson(users, "users"));

        } else if (readGroups != null) {

            ArrayList<OMIGroup> groups = DBHelper.getInstance().getGroups();
            out.println(wrapJson(groups, "groups"));
        } else if (readRules != null) {

            int groupID = Integer.parseInt(request.getParameter("groupID"));
            ArrayList<OMIRule> rules = DBHelper.getInstance().getRules(groupID);

            out.println(wrapJson(rules, "rules"));
        } else if (removeGroups != null) {

            int groupID = Integer.parseInt(request.getParameter("groupID"));
            if (DBHelper.getInstance().deleteGroup(groupID))
                out.write("{\"result\":\"ok\"}");
            else
                out.write("{\"error\":\"group was not deleted\"}");

        }
    }

    protected void doPost(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException
    {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String writeRules = request.getParameter("writeRules");
        String writeGroups = request.getParameter("writeGroups");
        String writeUsers = request.getParameter("writeUsers");
        String readPaths = request.getParameter("getPaths");
        String ac = request.getParameter("ac");

        if (ac != null) {

            logger.info("Received Access Control request");
//            Cookie[] cookies = request.getCookies();
//            System.out.println("Cookies:");
//            for (Cookie ck: cookies) {
//                System.out.println(ck.getName()+ck.getValue());
//            }

            boolean haveCredentials = false;
            String userEmail = null;

            HttpSession httpSession = request.getSession(false);
            if (httpSession == null) {
                logger.info("Session is not found!");
                haveCredentials = false;
            } else {
                haveCredentials = true;
                userEmail = (String)httpSession.getAttribute("userID");
            }

            //logger.info("UserCredential: "+userEmail);
            boolean isWrite = request.getParameter("write").equalsIgnoreCase("true");

            StringBuffer jb = new StringBuffer();
            String line = null;
            try {
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null)
                    jb.append(line);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }


            JsonObject paths = new JsonParser().parse(jb.toString()).getAsJsonObject();

            if (!haveCredentials) {
                JsonPrimitive userCred = paths.getAsJsonPrimitive("user");
                if (userCred == null) {
                    response.getWriter().write("false");
                    return;
                }
                userEmail = userCred.getAsString();
            }

            JsonArray json_paths = paths.getAsJsonArray("paths");

            String logString = "";

            ArrayList<String> paths_to_check = new ArrayList<>(json_paths.size());
            for (int i = 0; i < json_paths.size(); i++) {
                String nextPath = json_paths.get(i).getAsString();
                logString += nextPath+"\n";
                paths_to_check.add(nextPath);
            }

            logger.info("Received resource access requests." +
                    "\nUserIdentifier:"+userEmail+
                    "\nPaths:\n"+logString+
                    "isWrite:"+isWrite);

            boolean result = AuthService.getInstance().checkPermissions(paths_to_check, userEmail, isWrite);
//                logger.info("RESULT:"+result);
            response.getWriter().write(result ? "true" : "false");

        } else if (readPaths != null) {

            logger.info("Received Tree Paths request");

            boolean haveCredentials = false;
            String userEmail = null;

            HttpSession httpSession = request.getSession(false);
            if (httpSession == null) {
                logger.info("Session is not found!");
                haveCredentials = false;
            } else {
                haveCredentials = true;
                userEmail = (String)httpSession.getAttribute("userID");
            }

            if (!haveCredentials)
            {
                // try to fetch username from POST body

                StringBuffer jb = new StringBuffer();
                String line = null;
                try {
                    BufferedReader reader = request.getReader();
                    while ((line = reader.readLine()) != null)
                        jb.append(line);
                    userEmail = jb.toString();
                    haveCredentials = true;
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }


            if (haveCredentials) {
                logger.info("Valid session or user credentials are found!");

                if (AuthService.getInstance().isAdministrator(userEmail))
                {
                    logger.info("Administrator mode ON");
                    response.getWriter().write("true");
                    return;
                }

                logger.info("Administrator mode OFF");

                ArrayList<String> paths = DBHelper.getInstance().getRulesByUser(userEmail);
                String res = wrapJson(paths, "paths");
                logger.info("Paths:" + res);
                response.getWriter().write(res);
            } else {
                response.getWriter().write("false");
            }

        } else if (writeRules != null) {

            String groupID = request.getParameter("groupID");
            logger.info("Received security policies for group with ID:" + groupID);

            StringBuffer jb = new StringBuffer();
            String line = null;
            try {
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null)
                    jb.append(line);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            StringReader reader = new StringReader(jb.toString());

            try {

                XMLInputFactory xif = XMLInputFactory.newInstance();
                XMLStreamReader xsr = xif.createXMLStreamReader(reader);

                while (xsr.hasNext()) {
                    if (xsr.isStartElement() && "Objects".equals(xsr.getLocalName())) {
                        break;
                    }
                    xsr.next();
                }


                JAXBContext jc = JAXBContext.newInstance(OMIObjects.class);
                Unmarshaller unmarshaller = jc.createUnmarshaller();

                OMIObjects objResponse = (OMIObjects) unmarshaller.unmarshal(xsr);
                String answer = "XML with permissions parsed successfully. Objects:" + objResponse.getObjects().size();
                logger.info(answer);

                writeXPath(objResponse);

                AuthService.getInstance().writePermissions(objResponse.getObjects(), Integer.parseInt(groupID));
                response.getWriter().write(answer);

            } catch (Exception ex) {
                logger.error(ex.getCause() + ex.getMessage());
                response.getWriter().write("ERROR!" + ex.getCause() + ex.getMessage());
            }
        } else if (writeGroups != null) {

            StringBuffer jb = new StringBuffer();
            String line = null;
            try {
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null)
                    jb.append(line);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }


            JsonObject newGroup = new JsonParser().parse(jb.toString()).getAsJsonObject();
            String groupName = newGroup.getAsJsonPrimitive("name").getAsString();

            JsonArray userIDs_json = newGroup.getAsJsonArray("values");
            int [] userIDs = new int[userIDs_json.size()];
            for (int i = 0; i < userIDs_json.size(); i++) {
                userIDs[i] = userIDs_json.get(i).getAsInt();
            }

            int newGroupID = -1;
            if (newGroup.getAsJsonPrimitive("id") != null)
            {
                newGroupID = newGroup.getAsJsonPrimitive("id").getAsInt();
            }

            if (newGroupID == -1) {
                logger.info("Creating new group for name:"+groupName);
                newGroupID = DBHelper.getInstance().createGroup(groupName);

                if (newGroupID == -1) {
                    response.getWriter().write("{\"error\":\"new group was not created\"}");
                } else {

                    if (userIDs.length > 0) {
                        if (!DBHelper.getInstance().addUsersToGroup(userIDs,newGroupID))
                            response.getWriter().write("{\"error\":\"user list can not be added to group with id="+newGroupID+"\"}");
                    }
                    response.getWriter().write("{\"result\":\"ok\",\"groupID\":\"" + newGroupID + "\"}");
                }
            } else {
                logger.info("Modifying group with ID:"+newGroupID);
                if (DBHelper.getInstance().updateGroup(newGroupID, groupName)) {
                    DBHelper.getInstance().updateUsersForGroup(userIDs, newGroupID);
                    response.getWriter().write("{\"result\":\"ok\"}");
                }
                else
                    response.getWriter().write("{\"error\":\"group was not updated\"}");
            }

        } else if (writeUsers != null) {

            logger.info("Received new user request!");

            StringBuffer jb = new StringBuffer();
            String line = null;
            try {
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null)
                    jb.append(line);
            } catch (Exception e) {
                logger.error(e.getMessage());
            }


            JsonObject newUser = new JsonParser().parse(jb.toString()).getAsJsonObject();
            String userName = newUser.getAsJsonPrimitive("username").getAsString();
            String userEmail = newUser.getAsJsonPrimitive("email").getAsString();

            OMIUser user = new OMIUser();
            user.email = userEmail;
            user.username = userName;

            boolean userExists = DBHelper.getInstance().checkIfUserExists(user);

            if (!userExists) {

                logger.info("Creating new user for name:"+userName +" / email:"+userEmail);

                int userID = DBHelper.getInstance().createUser(user);

                if (userID == -1) {
                    response.getWriter().write("{\"error\":{\"type\":\"name\", \"msg\":\"Error while creating new user\"}}");
                } else {

                    response.getWriter().write("{\"result\":\"ok\",\"userID\":\"" + userID + "\"}");
                }
            } else {

                logger.info("User exists already for email:"+userEmail);
                response.getWriter().write("{\"error\":{\"type\":\"email\", \"msg\":\"User with given email already registered\"}}");
            }
        }
    }

    public void writeObjectXPath(OMIObject obj, String currentPath) {

        String newPath = currentPath + "/" + obj.getId().replace("[RW]","").replace("[R]","").replace("[D]","");
        obj.xPath = newPath;

        for (OMIInfoItem infoItem:obj.getInfoItems()) {
            infoItem.xPath = newPath + "/" + infoItem.getName().replace("[RW]","").replace("[R]","").replace("[D]","");
        }

        for (OMIObject nextObject:obj.getSubObjects()) {
            writeObjectXPath(nextObject,newPath);
        }
    }

    public void writeXPath(OMIObjects objects) {

        String path = "Objects";
        objects.xPath = path;

        for (OMIObject obj:objects.getObjects()) {
            writeObjectXPath(obj,path);
        }

    }

    public void destroy()
    {
        // do nothing.
    }
}
