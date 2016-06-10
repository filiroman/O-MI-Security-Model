package com.aaltoasia.omi.accontrol.http;

import com.aaltoasia.omi.accontrol.*;
import com.aaltoasia.omi.accontrol.config.ConfigHelper;
import com.aaltoasia.omi.accontrol.db.DBHelper;
import com.aaltoasia.omi.accontrol.openid.OpenIDAuth;
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
    private int port;

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private Server server;

    private static final HttpServer instance = new HttpServer(ConfigHelper.port);
    private HttpServer(int port){
        this.port = port;
    }
    public static HttpServer getInstance() {

        return instance;
    }

    public void run() {
        start();
    }

    public static class LoginFilter implements Filter {

        public static int nthIndexOf(String text, char needle, int number)
        {
            int n = number;
            for (int i = 0; i < text.length(); i++)
            {
                if (text.charAt(i) == needle)
                {
                    n--;
                    if (n == 0)
                    {
                        return i;
                    }
                }
            }
            return -1;
        }

        private final String[] allowedExtensions = new String[]{".css", ".js", ".png", ".jpg", ".ttf", ".woff", ".woff2"};

        private boolean isResourceExtension (String URIpath)
        {
            for (String extension: allowedExtensions) {
                if (URIpath.endsWith(extension))
                    return true;
            }
            return false;
        }

        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) res;
            HttpSession session = request.getSession(false);
            String loginURI = request.getContextPath() + "/Login";
            String loginServlet = request.getContextPath() + "/AC/auth.html";
            String permissionServiceURI = request.getContextPath() + "/PermissionService";
            String registrationServiceURI = request.getContextPath() + "/RegService";

            String requestURI = request.getRequestURI();

            boolean loggedIn = (session != null) && (session.getAttribute("userID") != null);
            boolean loginRequest = requestURI.equals(loginURI);
            boolean permissionRequest = requestURI.contains(permissionServiceURI);
            boolean loginServletRequest = requestURI.equals(loginServlet);
            boolean pathContainsAC = requestURI.contains("/AC/");
            boolean resourceRequest = isResourceExtension(requestURI);
            boolean registrationRequest = requestURI.contains(registrationServiceURI);

            String email = null;

            if (!loggedIn) {

                String certEmail = request.getHeader("X-SSL-CLIENT");
                String certVerify = request.getHeader("X-SSL-VERIFY");

                if (certEmail != null && certVerify != null) {

                    logger.debug("Client certificate detected. Email="+certEmail+"/Verified="+certVerify);

                    if (certVerify.equalsIgnoreCase("SUCCESS")) {
                        int firstIndex = certEmail.indexOf("emailAddress=") + "emailAddress=".length();
                        int lastIndex = nthIndexOf(certEmail,'/',2);

                        if (lastIndex < firstIndex) 
                            email = certEmail.substring(firstIndex);
                        else
                            email = certEmail.substring(firstIndex, lastIndex);
                    }

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

            boolean allowedRequests = hasRights ||
                                      loginRequest ||
                                      permissionRequest ||
                                      loginServletRequest ||
                                      (pathContainsAC && resourceRequest) ||
                                      registrationRequest;

            if (allowedRequests) {
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
        server = new Server();
//        server.dumpStdErr();

        // Create HTTP Config
        HttpConfiguration httpConfig = new HttpConfiguration();

        // Add support for X-Forwarded headers
        httpConfig.addCustomizer( new ForwardedRequestCustomizer() );

        // Create the http connector
        HttpConnectionFactory connectionFactory = new HttpConnectionFactory( httpConfig );
        ServerConnector connector = new ServerConnector(server, connectionFactory);

        connector.setPort(port);

        server.setConnectors( new ServerConnector[] { connector } );
//        server.addConnector(connector);

        // The filesystem paths we will map
//        String homePath = System.getProperty("user.home");
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

        ServletHolder regService = new ServletHolder(new RegService());
        context.addServlet(regService, "/RegService/*");

        ServletHolder testAuth = new ServletHolder(new AuthServlet());
        context.addServlet(testAuth, "/Login/*");


        context.addFilter(LoginFilter.class, "/*",
                EnumSet.of(DispatcherType.REQUEST));

        // Initialize DB Singleton
        if (DBHelper.getInstance() == null)
            return;

        if (FacebookAuth.getInstance() == null)
            return;

        if (OpenIDAuth.getInstance() == null)
            return;

        server.setStopAtShutdown(true);

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

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
           logger.error("Error while stopping Jetty:",e);
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
        (new Thread(HttpServer.getInstance())).start();
    }
}

