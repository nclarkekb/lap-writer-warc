package dk.netarkivet.lap;

import java.io.File;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.antiaction.common.templateengine.TemplateMaster;
import com.antiaction.common.templateengine.login.LoginTemplateHandler;
import com.antiaction.common.templateengine.storage.TemplateFileStorageManager;

public class LAPEnvironment {

    /** Logging mechanism. */
    private static Logger logger = Logger.getLogger(LAPEnvironment.class.getName());

    /** servletConfig. Currently not used. */
    public ServletConfig servletConfig = null;

    /*
     * Templates.
     */

    public TemplateMaster templateMaster = null;

    private String login_template_name = null;

    public LoginTemplateHandler<User> loginHandler = null;

    /*
     * LAP.
     */

    public MultiSessionManager sessionManager;

    public String lapHost;

    public int lapWriterPort;

    public int lapWebPort;

    public int lapTimeout = 10;

    public File targetDir;

    public LAPWarcWriterThread wt;

    public LAPEnvironment(ServletContext servletContext, ServletConfig theServletConfig) throws ServletException {
        this.servletConfig = theServletConfig;
        templateMaster = TemplateMaster.getInstance("default");
        templateMaster.addTemplateStorage(TemplateFileStorageManager.getInstance(servletContext.getRealPath("/")));

        login_template_name = servletConfig.getInitParameter("login-template");

        if (login_template_name != null && login_template_name.length() > 0) {
            logger.info("Using '" +  login_template_name + "' as login template.");
        } else {
            throw new ServletException("'login_template_name' must be configured!");
        }

        loginHandler = new LoginTemplateHandler<User>();
        loginHandler.templateMaster = templateMaster;
        loginHandler.templateName = login_template_name;
        loginHandler.title = "DAB - Login";
        loginHandler.adminPath = "/lap/";

        lapHost = theServletConfig.getInitParameter("lap-host");
        String lapWriterPortStr = theServletConfig.getInitParameter("lap-writer-port");
        String lapWebPortStr = theServletConfig.getInitParameter("lap-web-port");
        String lapTimeoutStr = theServletConfig.getInitParameter("lap-timeout");
        if (lapHost == null || lapHost.length() == 0) {
        	throw new ServletException("'lap-host' init parameter required!");
        }
        if (lapWriterPortStr == null || lapWriterPortStr.length() == 0) {
        	throw new ServletException("'lap-writer-port' init parameter required!");
        }
        if (lapWebPortStr == null || lapWebPortStr.length() == 0) {
        	throw new ServletException("'lap-web-port' init parameter required!");
        }
        if (lapTimeoutStr != null && lapTimeoutStr.length() > 0) {
        	try {
        		lapTimeout = Integer.parseInt(lapTimeoutStr);
        	} catch (NumberFormatException e) {
            	throw new ServletException("'lap-timeout' init parameter is not a valid inteter!");
        	}
        }
        try {
        	lapWriterPort = Integer.parseInt(lapWriterPortStr);
        } catch (NumberFormatException e) {
        	throw new ServletException("'lap-writer-port' init parameter is not a valid inteter!");
        }
        try {
        	lapWebPort = Integer.parseInt(lapWebPortStr);
        } catch (NumberFormatException e) {
        	throw new ServletException("'lap-web-port' init parameter is not a valid inteter!");
        }
        String targetDirStr = theServletConfig.getInitParameter("warc-dir");
        if (targetDirStr == null || targetDirStr.length() == 0) {
        	throw new ServletException("'warc-dir' init parameter required!");
        }
        targetDir = new File(targetDirStr);
        if (!targetDir.exists() || !targetDir.isDirectory()) {
        	throw new ServletException("'warc-dir' is not a valid directory!");
        }

        sessionManager = new MultiSessionManager();

        wt = new LAPWarcWriterThread(lapHost, lapWriterPort, lapTimeout, sessionManager, false);
    }

    public void cleanup() {
    	if (wt != null) {
            wt.stop();
    	}
        templateMaster = null;
        servletConfig = null;
    }

}
