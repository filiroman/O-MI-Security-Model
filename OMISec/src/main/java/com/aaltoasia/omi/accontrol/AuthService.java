package com.aaltoasia.omi.accontrol;

import com.aaltoasia.omi.accontrol.db.DBHelper;
import com.aaltoasia.omi.accontrol.db.objects.OMIInfoItem;
import com.aaltoasia.omi.accontrol.db.objects.OMIObject;
import com.aaltoasia.omi.accontrol.db.objects.OMIUser;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Created by romanfilippov on 18/11/15.
 */
public class AuthService {

    private static final AuthService instance = new AuthService();
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private AuthService() {

        logger.setLevel(Level.INFO);
        if (DBHelper.getInstance() == null)
        {
            logger.warning("Can not initiate DB. Exiting...");

            System.exit(0);
        }
    }

    public static AuthService getInstance() {
        return instance;
    }

    public OMIUser authenticateUser(String userData)
    {
        OMIUser user = new OMIUser(OMIUser.OMIUserType.OAuth);
        user.isUserAuthorized = true;

        return user;
    }

    private void writePermissionToDB(String objName, String xPath, int groupID, boolean objectRule)
    {
        boolean writable = objName.indexOf("[W]") > -1;
        boolean readable = objName.indexOf("[R]") > -1;
        //objName = objName.replace("[W]","").replace("[R]","");
        if (writable)
            DBHelper.getInstance().updateOrCreateRule(xPath, groupID, true, objectRule);
        else if (readable) {
            DBHelper.getInstance().updateOrCreateRule(xPath, groupID, false, objectRule);
        }
    }

    private void writeObjectPermission(OMIObject obj, int groupID)
    {
        String objName = obj.getId();
        if (objName != null) {
            writePermissionToDB(objName, obj.xPath, groupID, true);
        }

        for (OMIInfoItem infoItem:obj.getInfoItems()) {
            writePermissionToDB(infoItem.getName(), infoItem.xPath, groupID, false);
        }

        writePermissions(obj.getSubObjects(), groupID);
    }

    public void writePermissions(ArrayList<OMIObject> mainObj, int groupID)
    {
        for (OMIObject obj:mainObj) {
            writeObjectPermission(obj, groupID);
        }
    }

    public boolean checkPermissions(ArrayList<String> paths, String userEmail, boolean isWrite) {
        return DBHelper.getInstance().checkUserPermissions(paths, userEmail, isWrite);
    }

    public boolean isAdministrator(String email) {
        return DBHelper.getInstance().checkAdminPermissions(email);
    }

}
