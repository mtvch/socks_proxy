package ru.nsu.fit.g19202.karpov.socks.channels;

import ru.nsu.fit.g19202.karpov.socks.exceptions.SOCKSException;
import ru.nsu.fit.g19202.karpov.socks.server.SOCKServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.logging.Logger;

public class SOCKSAcceptor implements SOCKSChannel {
    private static final Logger logger = Logger.getLogger(SOCKSAcceptor.class.getName());

    private ServerSocketChannel serverSocketChannel;
    private SOCKServer server;

    public SOCKSAcceptor(InetSocketAddress addr) throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(Objects.requireNonNull(addr));
        this.serverSocketChannel.configureBlocking(false);
    }

    @Override
    public void perform() throws IOException, SOCKSException {
        SocketChannel clientChannel = Objects.requireNonNull(this.serverSocketChannel.accept());
        this.server.addChannel("client", clientChannel);
        logger.info("Accepted new client");
    }

    @Override
    public SelectionKey register(SOCKServer server) throws IOException {
        this.server = server;
        return this.serverSocketChannel.register(server.getSelector(), SelectionKey.OP_ACCEPT);
    }

    @Override
    public void close() throws Exception {
        this.serverSocketChannel.close();
    }
}
