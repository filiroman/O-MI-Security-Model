package com.aaltoasia.omi.accontrol;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by romanfilippov on 15/12/15.
 */
public class LoginService {

    private static final LoginService instance = new LoginService();
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private LoginService() {
        logger.setLevel(Level.INFO);
    }

    public static LoginService getInstance() {
        return instance;
    }

//    public void logOrRegisterUser()
}
