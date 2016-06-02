package com.aaltoasia.omi.accontrol;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Created by romanfilippov on 30/05/16.
 */
public class RegService extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private String getRequestBody(HttpServletRequest request) {
        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                jb.append(line);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }

        return jb.toString();
    }

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

//        response.setContentType("application/json");
//        response.setCharacterEncoding("UTF-8");

        String regUser = request.getParameter("regUser");
        String authUser = request.getParameter("authUser");

        //String body = getRequestBody(request);

        if (regUser != null) {

            String userName = request.getParameter("username");
            String userEmail = request.getParameter("email");
            String userPass = request.getParameter("passwd");

            boolean registered = AuthService.getInstance().registerUser(userName, userEmail, userPass);
            if (registered) {
                // set session
                HttpSession session = request.getSession(true);
                session.setAttribute("userID", userEmail);
                response.sendRedirect("/");
            }

        } else if (authUser != null) {


            //JsonObject userDetails = new JsonParser().parse(body).getAsJsonObject();
            String userName = request.getParameter("username");
            String userPass = request.getParameter("passwd");

            boolean authenticated = AuthService.getInstance().checkUserCredentials(userName, userPass);

            if (authenticated)
            {
                // set session
                HttpSession session = request.getSession(true);
                session.setAttribute("userID", userName);
                response.sendRedirect("/");
            }
        }

    }
}
