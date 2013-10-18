package dk.netarkivet.lap;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.antiaction.common.templateengine.TemplateMaster;

public interface ResourceAbstract {

    public void resources_init(DataSource dataSource,
            TemplateMaster templateMaster);

    public void resources_add(ResourceManagerAbstract resourceManager);

    public void resource_service(HttpServletRequest req,
            HttpServletResponse resp, ServletContext servletContext,
            int resource_id, List<Integer> numerics, String pathInfo)
            throws IOException;

}
