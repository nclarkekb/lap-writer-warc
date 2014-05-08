package dk.netarkivet.lap;

import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.antiaction.common.templateengine.TemplateMaster;
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

    public MultiSessionManager sessionManager;

    public LAPEnvironment(ServletContext servletContext, ServletConfig theServletConfig) throws ServletException {
        this.servletConfig = theServletConfig;
        templateMaster = TemplateMaster.getInstance("default");
        templateMaster.addTemplateStorage(TemplateFileStorageManager.getInstance(servletContext.getRealPath("/")));
        sessionManager = new MultiSessionManager();
    }

    public void cleanup() {
        templateMaster = null;
        servletConfig = null;
    }

}
