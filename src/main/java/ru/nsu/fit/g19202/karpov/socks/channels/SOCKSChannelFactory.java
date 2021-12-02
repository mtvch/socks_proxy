package ru.nsu.fit.g19202.karpov.socks.channels;

import ru.nsu.fit.g19202.karpov.socks.channels.partners.SOCKSClient;
import ru.nsu.fit.g19202.karpov.socks.channels.partners.SOCKSHost;
import ru.nsu.fit.g19202.karpov.socks.exceptions.SOCKSException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public abstract class SOCKSChannelFactory {
    public static SOCKSChannel createChannel(String channelType, Object[] args) throws IOException, SOCKSException {
        switch (channelType) {
            case "acceptor":
                if (!(args[0] instanceof InetSocketAddress)) {
                    throw new IllegalArgumentException();
                }
                return new SOCKSAcceptor((InetSocketAddress) args[0]);
            case "client":
                if (!(args[0] instanceof SocketChannel)) {
                    throw new IllegalArgumentException();
                }
                return new SOCKSClient((SocketChannel) args[0]);
            case "host":
                if (!(args[0] instanceof InetSocketAddress)) {
                    throw new IllegalArgumentException();
                }
                return new SOCKSHost((InetSocketAddress) args[0]);
            case "dns":
                return new SOCKSDns();
            default:
                return null;
        }
    }
}
