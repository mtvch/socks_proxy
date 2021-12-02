package ru.nsu.fit.g19202.karpov.socks.server;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;

public abstract class SOCKServerFactory {
    public static SOCKServer createServer(String className, InetSocketAddress addr) throws InstantiationException, IllegalAccessException, IllegalArgumentException, NoSuchMethodException, SecurityException, ClassNotFoundException, InvocationTargetException {
        Class<?>[] args = {InetSocketAddress.class};
        Object Server = Class.forName(className).getDeclaredConstructor(args).newInstance(addr);
        if (!(Server instanceof SOCKServer)) {
            throw new IllegalArgumentException();
        }
        return (SOCKServer) Server;
    }
}
