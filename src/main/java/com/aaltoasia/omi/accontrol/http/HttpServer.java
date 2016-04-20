package com.aaltoasia.omi.accontrol.http;

import com.aaltoasia.omi.accontrol.AuthService;
import com.aaltoasia.omi.accontrol.ConfigHelper;
import com.aaltoasia.omi.accontrol.PermissionService;
import com.aaltoasia.omi.accontrol.AuthServlet;
import com.aaltoasia.omi.accontrol.db.DBHelper;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

/**
 * Created by romanfilippov on 13/01/16.
 */
public class HttpServer implements Runnable
{
    int port;

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    public HttpServer(int port){
        this.port = port;
    }

    public void run() {
        start();
    }

    public static class LoginFilter implements Filter {
        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;
            HttpSession session = request.getSession(false);
            String loginURI = request.getContextPath() + "/Login";
            String permissionServiceURI = request.getContextPath() + "/PermissionService";

            boolean loggedIn = (session != null) && (session.getAttribute("userID") != null);
            boolean loginRequest = request.getRequestURI().equals(loginURI);
            boolean permissionRequest = request.getRequestURI().contains(permissionServiceURI);

            String email = null;

            if (!loggedIn) {

                String certEmail = request.getHeader("X-SSL-CLIENT");
                String certVerify = request.getHeader("X-SSL-VERIFY");

                if (certEmail != null && certVerify != null) {

                    logger.debug("Client certificate detected. Email="+certEmail+"/Verified="+certVerify);

                    if (certVerify.equalsIgnoreCase("SUCCESS"))
                        email = certEmail.substring(certEmail.indexOf("emailAddress=") + "emailAddress=".length());

                } else {
                    logger.debug("Client certificate is not found.");
                }

                loggedIn = (email != null);

            } else {
                email = session.getAttribute("userID").toString();
            }

            boolean hasRights = false;

            if (loggedIn) {

                hasRights = AuthService.getInstance().isAdministrator(email);

//                if (hasRights)
//                    logger.info("User has rights to enter AC Console");
//                else
//                    logger.info("User does not have rights to enter AC Console");

            }

            if (hasRights || loginRequest || permissionRequest) {
//                if (loggedIn)
//                    logger.info("Attribute:"+ session.getAttribute("userID"));

                chain.doFilter(request, response);
            } else {
                response.sendRedirect(loginURI);
            }
        }

        @Override
        public void init(FilterConfig arg0) throws ServletException {

        }

        @Override
        public void destroy() {}
    }

    private void start()
    {
        //Server Setup
        Server server = new Server();
//        server.dumpStdErr();

        // Create HTTP Config
        HttpConfiguration httpConfig = new HttpConfiguration();

        // Add support for X-Forwarded headers
        httpConfig.addCustomizer( new org.eclipse.jetty.server.ForwardedRequestCustomizer() );

        // Create the http connector
        HttpConnectionFactory connectionFactory = new HttpConnectionFactory( httpConfig );
        ServerConnector connector = new ServerConnector(server, connectionFactory);

        connector.setPort(port);

        server.setConnectors( new ServerConnector[] { connector } );
//        server.addConnector(connector);

        // The filesystem paths we will map
        String homePath = System.getProperty("user.home");
        String pwdPath = System.getProperty("user.dir") + File.separator + "webclient";

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setResourceBase(pwdPath);
        context.setContextPath("/security");
        server.setHandler(context);

        SessionManager sessionManager = new HashSessionManager();
        sessionManager.setMaxInactiveInterval(60 * 15); //session time out of 15 minutes
        HashSessionIdManager idManager = new HashSessionIdManager();
        sessionManager.getSessionCookieConfig().setPath("/");
        sessionManager.setSessionIdManager(idManager);
        SessionHandler sessionHandler = new SessionHandler(sessionManager);
        context.setSessionHandler(sessionHandler);

        ServletHolder holderHome = new ServletHolder("static-home", DefaultServlet.class);
        holderHome.setInitParameter("resourceBase",pwdPath);
        holderHome.setInitParameter("dirAllowed","false");
        holderHome.setInitParameter("pathInfoOnly","true");
        context.addServlet(holderHome,"/AC/*");

        ServletHolder permissionService = new ServletHolder(new PermissionService());
        context.addServlet(permissionService, "/PermissionService/*");

        ServletHolder testAuth = new ServletHolder(new AuthServlet());
        context.addServlet(testAuth, "/Login/*");


        context.addFilter(LoginFilter.class, "/*",
                EnumSet.of(DispatcherType.REQUEST));

        // Initialize DB Singleton
        DBHelper.getInstance();

        try
        {
            server.start();
            server.join();
        }
        catch (Throwable t)
        {
            logger.warn(t.getMessage());
        }
    }

    public static void main(String[] args) {

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        String logLevel = System.getProperty("loglevel");
        if (logLevel != null) {
            if (logLevel.equalsIgnoreCase("error"))
                root.setLevel(Level.ERROR);
            else if (logLevel.equalsIgnoreCase("warn"))
                root.setLevel(Level.WARN);
            else if (logLevel.equalsIgnoreCase("info"))
                root.setLevel(Level.INFO);
            else
                root.setLevel(Level.DEBUG);
        } else {
            root.setLevel(Level.DEBUG);
        }

        //Start HTTP Server
        (new Thread(new HttpServer(ConfigHelper.port))).start();
    }
}

