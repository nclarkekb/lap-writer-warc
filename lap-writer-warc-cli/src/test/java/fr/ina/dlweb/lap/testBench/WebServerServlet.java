package fr.ina.dlweb.lap.testBench;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

/**
 * Date: 22/11/12
 * Time: 15:42
 *
 * @author drapin
 */
public class WebServerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestPath = req.getRequestURI().replaceFirst("^/", "");
        if (requestPath.isEmpty()) requestPath = "index.html";

        InputStream resourceStream = WebServerServlet.class.getResourceAsStream(requestPath);
        resp.setHeader("LAP-Foo", "Bar");
        resp.setHeader("LAP-123", "456");
        IOUtils.copy(resourceStream, resp.getWriter());
        resp.getWriter().close();
    }
}
