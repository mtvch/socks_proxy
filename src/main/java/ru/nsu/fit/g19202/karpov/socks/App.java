package ru.nsu.fit.g19202.karpov.socks;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.logging.Logger;

import ru.nsu.fit.g19202.karpov.socks.server.SOCKServer;
import ru.nsu.fit.g19202.karpov.socks.server.SOCKServerFactory;


public class App  {
    public static final String CONFIG_FILE_NAME = "config.txt";
    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        try (InputStream configStream = App.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
            Properties config = new Properties();
			config.load(configStream);
            startServer(config, new InetSocketAddress(port));
        } catch (Exception e) {
			e.printStackTrace();
		}
    }

    private static void startServer(Properties config, InetSocketAddress port) throws Exception {
        try (SOCKServer server = SOCKServerFactory.createServer(config.getProperty("SOCKServer"), port)) {
            logger.info("Starting server on port " + port.getPort());
            server.loop();
        }
    }
}

