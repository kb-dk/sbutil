package dk.statsbiblioteket.sbutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;

/**
 * Simple utility listener to load certain properties before Spring Starts up.
 *
 * Add this entry to your web.xml:
 * <pre>
 *   &lt;listener&gt;
 *       &lt;listener-class&gt;dk.statsbiblioteket.sbutil.ExternalConfigLoaderContextListener&lt;/listener-class&gt;
 *   &lt;/listener&gt;
 * </pre>
 *
 * It will look for a context param  by the name dk.statsbiblioteket.sbutil.ExternalConfigLoaderContextListener.configDir
 * If this is a relative path, it is relative from the root folder of your webapp.
 * It expects to find a file called logback.xml in that folder.
 */
public class ExternalConfigLoaderContextListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(ExternalConfigLoaderContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String configdir = getClass().getCanonicalName()+".configDir";
        String configLocation = sce.getServletContext().getInitParameter(configdir);
        if (configLocation == null) {
            configLocation = System.getenv(configdir);
        }
        if (!configLocation.startsWith("/")) {
            String webxmlrealPath = sce.getServletContext().getRealPath("/WEB-INF/web.xml");
            configLocation = new File(new File(webxmlrealPath).getParentFile().getParentFile(), configLocation).getAbsolutePath();
        }

        try {
            new LogBackConfigLoader(new File(configLocation, "logback.xml").getAbsolutePath());
        } catch (Exception e) {
            logger.error("Unable to read config file from " + configLocation, e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
