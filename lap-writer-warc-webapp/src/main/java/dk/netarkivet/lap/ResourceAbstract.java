package dk.netarkivet.lap;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface ResourceAbstract {

    public void resources_init(LAPEnvironment environment);

    public void resources_add(ResourceManagerAbstract resourceManager);

    public void resource_service(ServletContext servletContext, 
    		HttpServletRequest req, HttpServletResponse resp, User current_user, 
            int resource_id, List<Integer> numerics, String pathInfo)
            throws IOException;

}
