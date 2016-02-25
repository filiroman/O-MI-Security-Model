package com.aaltoasia.omi.accontrol.db.objects;

/**
 * Created by romanfilippov on 18/11/15.
 */
public class OMIUser {

    /** type of a user */
    public enum OMIUserType {
        OAuth,
        Shibboleth,
        Unknown;

    }

    // is not stored anywhere in DB for now
    public boolean isUserAuthorized;
    // is not stored anywhere in DB for now
    public OMIUserType userType;

    public int id;
    public String username;
    public String email;

    public OMIUser(OMIUserType userType)
    {
        this.userType = userType;
    }


}
