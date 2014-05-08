package dk.netarkivet.lap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.antiaction.common.filter.Caching;
import com.antiaction.common.templateengine.Template;
import com.antiaction.common.templateengine.TemplateParts;
import com.antiaction.common.templateengine.TemplatePlaceBase;
import com.antiaction.common.templateengine.TemplatePlaceHolder;
import com.antiaction.common.templateengine.TemplatePlaceTag;

public class SessionResource implements ResourceAbstract {

	private int R_INDEX = -1;

	private int R_ADD = -1;

	protected LAPEnvironment environment;

	public SessionResource() {
    }

    @Override
    public void resources_init(LAPEnvironment environment) {
		this.environment = environment;
    }

    @Override
    public void resources_add(ResourceManagerAbstract resourceManager) {
        R_INDEX = resourceManager.resource_add(this, "/", false);
        R_ADD = resourceManager.resource_add(this, "/add/", false);
    }

    @Override
    public void resource_service(HttpServletRequest req, HttpServletResponse resp, ServletContext servletContext,
            int resource_id, List<Integer> numerics, String pathInfo)
            throws IOException {
        String method = req.getMethod();
        if (resource_id == R_INDEX) {
        	if ("GET".equals(method)) {
        		session_list(req, resp);
        	}
        } else if (resource_id == R_ADD) {
        	if ("GET".equals(method) || "POST".equals(method)) {
            	session_add(req, resp);
        	}
        }
    }

    protected void session_list(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletOutputStream out = resp.getOutputStream();
        resp.setContentType("text/html; charset=utf-8");

        Caching.caching_disable_headers(resp);

        Template template = environment.templateMaster.getTemplate("session_list.html");

        TemplatePlaceHolder titlePlace = TemplatePlaceBase.getTemplatePlaceHolder("title");
        TemplatePlaceHolder navbarPlace = TemplatePlaceBase.getTemplatePlaceHolder("navbar");
        TemplatePlaceHolder userPlace = TemplatePlaceBase.getTemplatePlaceHolder("user");
        TemplatePlaceHolder statemenuPlace = TemplatePlaceBase.getTemplatePlaceHolder("state_menu");
        TemplatePlaceHolder menuPlace = TemplatePlaceBase.getTemplatePlaceHolder("menu");
        TemplatePlaceHolder headingPlace = TemplatePlaceBase.getTemplatePlaceHolder("heading");
        TemplatePlaceHolder actionButtonsPlace = TemplatePlaceBase.getTemplatePlaceHolder("action_buttons");
        TemplatePlaceHolder paginationPlace = TemplatePlaceBase.getTemplatePlaceHolder("pagination");
        TemplatePlaceHolder pagination2Place = TemplatePlaceBase.getTemplatePlaceHolder("pagination2");
        TemplatePlaceTag myformTag = TemplatePlaceTag.getInstance("form", "myform");
        TemplatePlaceHolder domainsPlace = TemplatePlaceBase.getTemplatePlaceHolder("domains");
        TemplatePlaceHolder contentPlace = TemplatePlaceBase.getTemplatePlaceHolder("content");

        List<TemplatePlaceBase> placeHolders = new ArrayList<TemplatePlaceBase>();
        placeHolders.add(titlePlace);
        placeHolders.add(navbarPlace);
        placeHolders.add(userPlace);
        placeHolders.add(statemenuPlace);
        placeHolders.add(menuPlace);
        placeHolders.add(headingPlace);
        placeHolders.add(actionButtonsPlace);
        placeHolders.add(paginationPlace);
        placeHolders.add(pagination2Place);
        placeHolders.add(myformTag);
        placeHolders.add(domainsPlace);
        placeHolders.add(contentPlace);

        TemplateParts templateParts = template.filterTemplate(placeHolders, resp.getCharacterEncoding());

        try {
            for (int i = 0; i < templateParts.parts.size(); ++i) {
                out.write(templateParts.parts.get(i).getBytes());
            }
            out.flush();
            out.close();
        } catch (IOException e) {
        }
    }

    protected void session_add(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ServletOutputStream out = resp.getOutputStream();
        resp.setContentType("text/html; charset=utf-8");

        Caching.caching_disable_headers(resp);

        String method = req.getMethod();
        if ("POST".equals(method)) {
        	File targetDir = new File("");
        	String filePrefix = req.getParameter("fileprefix");
        	String compressionStr = req.getParameter("compression");
        	boolean bCompression = false;
        	if (compressionStr != null) {
        		bCompression = true;
        	}
        	long maxFileSize = 1024 * 1024 * 1024;
        	String deduplicationStr = req.getParameter("deduplication");
        	boolean bDeduplication = false;
        	if (deduplicationStr != null) {
        		bDeduplication = true;
        	}
            String isPartOf = req.getParameter("ispartof");
            String description = req.getParameter("description");
            String operator = req.getParameter("operator");
            String httpheader = req.getParameter("httpheader");
            Session session = new Session(targetDir, filePrefix, bCompression, maxFileSize, bDeduplication,
            		isPartOf, description, operator, httpheader);
            String ip = req.getRemoteAddr();
            environment.sessionManager.addSession(ip, session);
        }

        Template template = environment.templateMaster.getTemplate("session_add.html");

        TemplatePlaceHolder titlePlace = TemplatePlaceBase.getTemplatePlaceHolder("title");
        TemplatePlaceHolder navbarPlace = TemplatePlaceBase.getTemplatePlaceHolder("navbar");
        TemplatePlaceHolder userPlace = TemplatePlaceBase.getTemplatePlaceHolder("user");
        TemplatePlaceHolder statemenuPlace = TemplatePlaceBase.getTemplatePlaceHolder("state_menu");
        TemplatePlaceHolder menuPlace = TemplatePlaceBase.getTemplatePlaceHolder("menu");
        TemplatePlaceHolder headingPlace = TemplatePlaceBase.getTemplatePlaceHolder("heading");
        TemplatePlaceHolder actionButtonsPlace = TemplatePlaceBase.getTemplatePlaceHolder("action_buttons");
        TemplatePlaceHolder paginationPlace = TemplatePlaceBase.getTemplatePlaceHolder("pagination");
        TemplatePlaceHolder pagination2Place = TemplatePlaceBase.getTemplatePlaceHolder("pagination2");
        TemplatePlaceTag myformTag = TemplatePlaceTag.getInstance("form", "myform");
        TemplatePlaceHolder domainsPlace = TemplatePlaceBase.getTemplatePlaceHolder("domains");
        TemplatePlaceHolder contentPlace = TemplatePlaceBase.getTemplatePlaceHolder("content");

        List<TemplatePlaceBase> placeHolders = new ArrayList<TemplatePlaceBase>();
        placeHolders.add(titlePlace);
        placeHolders.add(navbarPlace);
        placeHolders.add(userPlace);
        placeHolders.add(statemenuPlace);
        placeHolders.add(menuPlace);
        placeHolders.add(headingPlace);
        placeHolders.add(actionButtonsPlace);
        placeHolders.add(paginationPlace);
        placeHolders.add(pagination2Place);
        placeHolders.add(myformTag);
        placeHolders.add(domainsPlace);
        placeHolders.add(contentPlace);

        TemplateParts templateParts = template.filterTemplate(placeHolders, resp.getCharacterEncoding());

        try {
            for (int i = 0; i < templateParts.parts.size(); ++i) {
                out.write(templateParts.parts.get(i).getBytes());
            }
            out.flush();
            out.close();
        } catch (IOException e) {
        }
    }

}
