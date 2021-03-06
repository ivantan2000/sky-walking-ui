package org.skywalking.apm.ui.tools;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.skywalking.apm.ui.config.UIConfig;
import org.skywalking.apm.ui.creator.UrlCreator;
import org.springframework.context.ApplicationContext;

/**
 * @author pengys5
 */
public enum CollectorUIServerGetterTimer {
    INSTANCE;

    private Logger logger = LogManager.getFormatterLogger(CollectorUIServerGetterTimer.class);

    private Gson gson = new Gson();

    public void start(ApplicationContext applicationContext) {
        logger.info("collector ui server getter timer start");
        final long timeInterval = 10 * 1000;
        
        UIConfig uiConfig = applicationContext.getBean(UIConfig.class);
        UrlCreator urlCreator = applicationContext.getBean(UrlCreator.class);

        Thread persistenceThread = new Thread(() -> {
            while (true) {
                try {
                    List<String> servers=getServer(uiConfig);
                    urlCreator.updateServerList(servers);
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                } finally {
                    try {
                        Thread.sleep(timeInterval);
                    } catch (InterruptedException e) {
                    }
                }
            }
        });
        persistenceThread.setName("timerPersistence");
        persistenceThread.start();
    }

    private List<String> getServer(UIConfig uiConfig) {
        for (String server : uiConfig.getServers()) {
            try {
                String uiServerResponse = HttpClientTools.INSTANCE.get("http://" + server + "/ui/jetty", null);
                logger.debug("uiServerResponse: %s", uiServerResponse);
                JsonArray serverArray = gson.fromJson(uiServerResponse, JsonArray.class);
                List<String> servers = new ArrayList<>();
                serverArray.forEach(serverElement -> servers.add(serverElement.getAsString()));
                return servers;
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }
}
