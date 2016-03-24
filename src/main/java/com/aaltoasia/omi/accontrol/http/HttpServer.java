package com.aaltoasia.omi.accontrol.http;

import com.aaltoasia.omi.accontrol.AuthService;
import com.aaltoasia.omi.accontrol.PermissionService;
import com.aaltoasia.omi.accontrol.AuthServlet;
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

            boolean hasRights = false;

            if (loggedIn) {
                String email = session.getAttribute("userID").toString();

                hasRights = AuthService.getInstance().isAdministrator(email);

                if (hasRights)
                    logger.info("User has rights to enter AC Console");
                else
                    logger.info("User does not have rights to enter AC Console");

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

        // add a simple Servlet at "/dynamic/*"
//        ServletHolder holderDynamic = new ServletHolder("dynamic", DynamicServlet.class);
//        context.addServlet(holderDynamic, "/dynamic/*");

        ServletHolder holderHome = new ServletHolder("static-home", DefaultServlet.class);
        holderHome.setInitParameter("resourceBase",pwdPath);
        holderHome.setInitParameter("dirAllowed","false");
        holderHome.setInitParameter("pathInfoOnly","true");
        context.addServlet(holderHome,"/AC/*");

        ServletHolder permissionService = new ServletHolder(new PermissionService());
        context.addServlet(permissionService, "/PermissionService/*");

        ServletHolder testAuth = new ServletHolder(new AuthServlet());
        context.addServlet(testAuth, "/Login/*");

        // Lastly, the default servlet for root content (always needed, to satisfy servlet spec)
        // It is important that this is last.
//        ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
//        holderPwd.setInitParameter("dirAllowed","false");
//        context.addServlet(holderPwd,"/");

        context.addFilter(LoginFilter.class, "/*",
                EnumSet.of(DispatcherType.REQUEST));

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
        //Start HTTP Server
        (new Thread(new HttpServer(8088))).start();
    }
}

