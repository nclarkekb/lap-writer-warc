package dk.netarkivet.lap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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

        List<SessionConfig> sessionConfigList = environment.sessionManager.getSessionConfigs();

        StringBuilder sb = new StringBuilder();
        Iterator<SessionConfig> iter = sessionConfigList.iterator();
        while (iter.hasNext()) {
        	SessionConfig sc = iter.next();
        	sb.append(sc.dir);
        	sb.append(sc.filePrefix);
        	sb.append(sc.isPartOf);
        	sb.append(sc.operator);
        	sb.append(sc.description);
        	sb.append(sc.httpheader);
        	sb.append(sc.writerAgent);
        	sb.append(sc.bCompression);
        	sb.append(sc.bDeduplication);
        	sb.append(sc.maxFileSize);
        }

        if (contentPlace != null) {
        	contentPlace.setText(sb.toString());
        }

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

    	SessionConfig sessionConfig;

        String method = req.getMethod();
        if ("POST".equals(method)) {
        	File targetDir = environment.targetDir;
        	String dir = targetDir.getPath();
            String ip = req.getRemoteAddr();
        	String filePrefix = req.getParameter("prefix");
        	String compressionStr = req.getParameter("compression");
        	boolean bCompression = false;
        	if (compressionStr != null) {
        		bCompression = true;
        	}
        	long maxFileSize = 1073741824L;
        	try {
        		maxFileSize = Long.parseLong(req.getParameter("maxfilesize"));
        	} catch (NumberFormatException e) {
        	}
        	String deduplicationStr = req.getParameter("deduplication");
        	boolean bDeduplication = false;
        	if (deduplicationStr != null) {
        		bDeduplication = true;
        	}
            String isPartOf = req.getParameter("ispartof");
            String description = req.getParameter("description");
            String operator = req.getParameter("operator");
            String httpheader = req.getParameter("httpheader");
            sessionConfig = new SessionConfig(dir, targetDir, new String[] {ip}, filePrefix, bCompression, maxFileSize, bDeduplication, isPartOf, description, operator, httpheader);
            environment.sessionManager.addSession(sessionConfig);
        } else {
        	sessionConfig = new SessionConfig();
        	sessionConfig.ip = new String[1];
        	sessionConfig.ip[0] = req.getRemoteAddr();
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
        TemplatePlaceTag myformTag = TemplatePlaceTag.getInstance("form", "myform");
        TemplatePlaceHolder domainsPlace = TemplatePlaceBase.getTemplatePlaceHolder("domains");
        TemplatePlaceHolder contentPlace = TemplatePlaceBase.getTemplatePlaceHolder("content");

        TemplatePlaceTag ipsInputTag = TemplatePlaceTag.getInstance("input", "ips");
        TemplatePlaceTag prefixInputTag = TemplatePlaceTag.getInstance("input", "prefix");
        TemplatePlaceTag compressionInputTag = TemplatePlaceTag.getInstance("input", "compression");
        TemplatePlaceTag maxFileSizeInputTag = TemplatePlaceTag.getInstance("input", "maxfilesize");
        TemplatePlaceTag deduplicationInputTag = TemplatePlaceTag.getInstance("input", "deduplication");
        TemplatePlaceTag ispartofInputTag = TemplatePlaceTag.getInstance("input", "ispartof");
        TemplatePlaceTag descriptionInputTag = TemplatePlaceTag.getInstance("input", "description");
        TemplatePlaceTag operatorInputTag = TemplatePlaceTag.getInstance("input", "operator");
        TemplatePlaceTag httpheaderInputTag = TemplatePlaceTag.getInstance("input", "httpheader");

        List<TemplatePlaceBase> placeHolders = new ArrayList<TemplatePlaceBase>();
        placeHolders.add(titlePlace);
        placeHolders.add(navbarPlace);
        placeHolders.add(userPlace);
        placeHolders.add(statemenuPlace);
        placeHolders.add(menuPlace);
        placeHolders.add(headingPlace);
        placeHolders.add(actionButtonsPlace);
        placeHolders.add(paginationPlace);
        placeHolders.add(myformTag);
        placeHolders.add(domainsPlace);
        placeHolders.add(contentPlace);

        placeHolders.add(ipsInputTag);
        placeHolders.add(prefixInputTag);
        placeHolders.add(compressionInputTag);
        placeHolders.add(maxFileSizeInputTag);
        placeHolders.add(deduplicationInputTag);
        placeHolders.add(ispartofInputTag);
        placeHolders.add(descriptionInputTag);
        placeHolders.add(operatorInputTag);
        placeHolders.add(httpheaderInputTag);

        TemplateParts templateParts = template.filterTemplate(placeHolders, resp.getCharacterEncoding());

        if ( ipsInputTag != null && ipsInputTag.htmlItem != null ) {
        }
        if ( prefixInputTag != null && prefixInputTag.htmlItem != null ) {
        	prefixInputTag.htmlItem.setAttribute("value", sessionConfig.filePrefix);
        }
        if ( compressionInputTag != null && compressionInputTag.htmlItem != null ) {
        	if (sessionConfig.bCompression) {
        		compressionInputTag.htmlItem.setAttribute("checked", "1");
        	}
        }
        if ( maxFileSizeInputTag != null && maxFileSizeInputTag.htmlItem != null ) {
        	maxFileSizeInputTag.htmlItem.setAttribute("value", Long.toString(sessionConfig.maxFileSize));
        }
        if ( deduplicationInputTag != null && deduplicationInputTag.htmlItem != null ) {
        	if (sessionConfig.bDeduplication) {
        		deduplicationInputTag.htmlItem.setAttribute("checked", "1");
        	}
        }
        if ( ispartofInputTag != null && ispartofInputTag.htmlItem != null ) {
        	ispartofInputTag.htmlItem.setAttribute("value", sessionConfig.isPartOf);
        }
        if ( descriptionInputTag != null && descriptionInputTag.htmlItem != null ) {
        	descriptionInputTag.htmlItem.setAttribute("value", sessionConfig.description);
        }
        if ( operatorInputTag != null && operatorInputTag.htmlItem != null ) {
        	operatorInputTag.htmlItem.setAttribute("value", sessionConfig.operator);
        }
        if ( httpheaderInputTag != null && httpheaderInputTag.htmlItem != null ) {
        	httpheaderInputTag.htmlItem.setAttribute("value", sessionConfig.httpheader);
        }

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
