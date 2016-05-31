package com.aaltoasia.omi.accontrol;

import com.aaltoasia.omi.accontrol.db.objects.OMIUser;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by romanfilippov on 12/11/15.
 */
public class AuthServlet extends HttpServlet {


    private FacebookAuth auth;

    public void init() throws ServletException
    {
        auth = FacebookAuth.getInstance();
    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException
    {
        // Set response content type
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        String title = "O-MI Login";
        String docType =
                "<!doctype html public \"-//w3c//dtd html 4.0 " +
                        "transitional//en\">\n";

        String auth_type = request.getParameter("auth_type");
        String accessCode = request.getParameter("code");
        if (accessCode != null)
        {
            auth.getAccessToken(accessCode);
            String userInfo = auth.getUserInformation();
            OMIUser newUser = auth.createUserForInfo(userInfo);
            boolean authenticated = auth.registerOrLoginUser(newUser);
            System.out.println(userInfo);

            if (authenticated) {
                // set session
                HttpSession session = request.getSession(true);
                session.setAttribute("userID", newUser.email);
            }

            out.println(docType +
                    "<html>\n" +
                    "<head><title>" + title + "</title></head>\n" +
                    "<body>\n" +
                    "<h1 align=\"center\">" + title + "</h1>\n" +
                    "<div align=\"center\">" +
                     userInfo +
                    "</div>" +
                    (authenticated ? "<h1 align=\"center\">User authenticated<br/><a href=\"../\">Go to O-MI Node</a></h1>" : "") +
                    "</body></html>");
        } else if (auth_type == null) {

//            RequestDispatcher view = request.getRequestDispatcher("/AC/auth.html");
//            view.forward(request, response);
            response.sendRedirect(request.getContextPath()+ "/AC/auth.html");

//            out.println(docType +
//                            "<html>\n" +
//                            "<head><title>" + title + "</title></head>\n" +
//                            "<body>\n" +
//                            "<h1 align=\"center\">" + title + "</h1>\n" +
//                            "<div align=\"center\">" +
//                            "<form>" +
//                            "<input type=\"hidden\" name=\"auth_type\" value=\"facebook\"/>" +
//                            "<input type=\"submit\" value=\"Facebook\"/>" +
//                            "</form>" +
//                            "<form>" +
//                            "<input type=\"hidden\" name=\"auth_type\" value=\"shibboleth\"/>" +
//                            "<input type=\"button\" value=\"Shibboleth\" onclick=\"alert('Shibboleth is not supported yet!');\"/>" +
//                            "</form></div>" +
//                            "</body></html>");
        } else {
            if (auth_type.equalsIgnoreCase("facebook")) {
                String authURL = auth.getAuthorizationURL();
                response.sendRedirect(authURL);
            }
            // TODO: place shibboleth auth handler here

        }
    }

    public void destroy()
    {
        // do nothing.
    }
}
