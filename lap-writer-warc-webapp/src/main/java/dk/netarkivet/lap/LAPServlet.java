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

import com.antiaction.common.servlet.AutoIncrement;
import com.antiaction.common.servlet.PathMap;

public class LAPServlet extends HttpServlet implements ResourceManagerAbstract {

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
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() == 0) {
            pathInfo = "/";
        }

        logger.log(Level.INFO, req.getMethod() + " " + req.getPathInfo());

        List<Integer> numerics = new ArrayList<Integer>();
        Resource resource = pathMap.get(pathInfo, numerics);

        if (resource != null) {
            resource.resources.resource_service(req, resp, getServletContext(),
                    resource.resource_id, numerics, pathInfo);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, pathInfo);
        }
    }

    class Resource {

        int resource_id;

        ResourceAbstract resources;

        boolean bSecured;

    }

}
