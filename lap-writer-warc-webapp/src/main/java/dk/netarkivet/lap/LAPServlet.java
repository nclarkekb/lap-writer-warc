package dk.netarkivet.lap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.antiaction.common.servlet.AutoIncrement;
import com.antiaction.common.servlet.PathMap;
import com.antiaction.common.templateengine.login.LoginTemplateCallback;

public class LAPServlet extends HttpServlet implements ResourceManagerAbstract, LoginTemplateCallback<User> {

    private static Logger logger = Logger.getLogger(LAPServlet.class.getName());

    /**
	 * UID.
	 */
	private static final long serialVersionUID = 4894977060310522858L;

    public static LAPEnvironment environment;

    private PathMap<Resource> pathMap;

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        environment = new LAPEnvironment(getServletContext(), servletConfig);

        pathMap = new PathMap<Resource>();

        StaticResource staticResource = new StaticResource();
        staticResource.resources_init(environment);
        staticResource.resources_add(this);

        SessionResource sessionResource = new SessionResource();
        sessionResource.resources_init(environment);
        sessionResource.resources_add(this);

        logger.log(Level.INFO, this.getClass().getName() + " initialized.");
    }

    protected AutoIncrement resourceAutoInc = new AutoIncrement();

    public int resource_add(ResourceAbstract resources, String path,
            boolean bSecured) {
        int resource_id = resourceAutoInc.getId();
        Resource resource = new Resource();
        resource.resource_id = resource_id;
        resource.resources = resources;
        resource.bSecured = bSecured;
        pathMap.add(path, resource);
        return resource_id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.GenericServlet#destroy()
     */
    @Override
    public void destroy() {
        if (environment != null) {
            environment.cleanup();
            environment = null;
        }
        logger.log(Level.INFO, this.getClass().getName() + " destroyed.");
        super.destroy();
    }

	/*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest
     * , javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession();
        try {
            // debug
            // System.out.println( req.getContextPath() );
            // System.out.println( req.getServletPath() );
            // System.out.println( req.getPathInfo() );
            // System.out.println( req.getRealPath( req.getContextPath() ) );
            // System.out.println( req.getRealPath( req.getPathInfo() ) );

            User current_user = null;

            // If we have a valid session look for an already logged in current user.
            if (session != null) {
                current_user = (User) session.getAttribute("user");
            }

            // Look for cookies in case of no current user in session.
            if (current_user == null && session != null && session.isNew()) {
                current_user = environment.loginHandler.loginFromCookie(req, resp, session, this);
            }

            String action = req.getParameter("action");

            // Logout, login or administration.
            if (action != null && "logout".compareToIgnoreCase(action) == 0) {
                environment.loginHandler.logoff(req, resp, session);
            } else {
                String pathInfo = req.getPathInfo();
                if (pathInfo == null || pathInfo.length() == 0) {
                    pathInfo = "/";
                }

                // debug
                //logger.log(Level.INFO, req.getMethod() + " " + req.getPathInfo());

                List<Integer> numerics = new ArrayList<Integer>();
                Resource resource = pathMap.get(pathInfo, numerics);

                if (resource != null) {
                    if (resource.bSecured && current_user == null) {
                        environment.loginHandler.loginFromForm(req, resp, session, this);
                    } else if (!resource.bSecured) {
                        resource.resources.resource_service(this.getServletContext(), req, resp, current_user, resource.resource_id, numerics, pathInfo);
                    } else {
                        resource.resources.resource_service(this.getServletContext(), req, resp, current_user, resource.resource_id, numerics, pathInfo);
                    }
                } else {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, pathInfo);
                }
            }
        } catch (Throwable t) {
        	logger.log(Level.SEVERE, t.toString(), t);
        }
    }

    @Override
    public User validateUserCookie(String token) {
        return null;
    }

    @Override
    public User validateUserCredentials(String id, String password) {
        User current_user = null;
    	/*
        Connection conn = null;
        try {
            conn = environment.dataSource.getConnection();
            current_user = User.getAdminByCredentials(conn, id, password);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, e.toString(), e);
        } finally {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, e.toString(), e);
            }
        }
        if (current_user != null) {
            if (!current_user.active) {
            	current_user = null;
                logger.info("User account with id '" + id + "' is not active");
            }
        } else {
            logger.info("No known user '" + id + "' with the given credentials");
        }
        */
        current_user = new User();
        return current_user;
    }

    @Override
    public String getTranslated(String text_idstring) {
        return null;
    }

    class Resource {

        int resource_id;

        ResourceAbstract resources;

        boolean bSecured;

    }

}
