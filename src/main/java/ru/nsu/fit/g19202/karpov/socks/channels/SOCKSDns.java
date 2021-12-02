package ru.nsu.fit.g19202.karpov.socks.channels;

import org.xbill.DNS.*;
import ru.nsu.fit.g19202.karpov.socks.channels.partners.SOCKSClient;
import ru.nsu.fit.g19202.karpov.socks.exceptions.SOCKSException;
import ru.nsu.fit.g19202.karpov.socks.server.SOCKServer;

import java.io.IOException;
import org.xbill.DNS.Record;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class SOCKSDns implements SOCKSChannel {
    private final DatagramChannel datagramChannel;
    private final InetSocketAddress resolver;
    private SOCKServer server;
    private final ByteBuffer buf;
    private static final Logger logger = Logger.getLogger(SOCKSClient.class.getName());

    public SOCKSDns() throws SOCKSException, IOException {
        List<InetSocketAddress> dnsServers = ResolverConfig.getCurrentConfig().servers();
        try {
            this.resolver = dnsServers.get(0);
            this.datagramChannel = DatagramChannel.open();
            this.datagramChannel.socket().bind(new InetSocketAddress(0));
            this.datagramChannel.configureBlocking(false);
            this.buf = ByteBuffer.allocate(512);
        } catch (IndexOutOfBoundsException e) {
            throw new SOCKSException("No dns resolvers found");
        }
    }

    @Override
    public void perform() throws IOException {
        if (this.server.isWritable(this)) {
            String name = server.popDomainName();
            if (name != null) {
                sendResolve(name);
            }
        }
        if (this.server.isReadable(this)) {
            recvResolved();
        }
    }

    @Override
    public SelectionKey register(SOCKServer server) throws IOException {
        this.server = server;
        return this.datagramChannel.register(server.getSelector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    public void close() throws Exception {
        this.datagramChannel.close();
    }

    private void sendResolve(String dnsName) throws IOException {
        Record queryRecord = Record.newRecord(Name.fromString(dnsName.concat(".")), Type.A, DClass.IN);
        Message queryMessage = Message.newQuery(queryRecord);
        this.buf.clear();
        this.buf.put(queryMessage.toWire());
        this.buf.flip();
        this.datagramChannel.send(this.buf, this.resolver);
        logger.info("Sent request to resolve domain name: " + dnsName);
    }

    private void recvResolved() throws IOException {
        this.buf.clear();
        Objects.requireNonNull(this.datagramChannel.receive(this.buf));
        this.buf.flip();
        Message msg = new Message(this.buf);
        String name = msg.getSection(0).get(0).getName().toString();
        Set<SOCKSChannel> channels = this.server.getChannels();
        SOCKSClient client = Objects.requireNonNull(getClientByHostName(channels, name));
        try {
            String ip = msg.getSection(1).get(0).rdataToString();
            logger.info("Resolved domain name: " + name);
            client.resolveHostName(InetAddress.getByName(ip));
        } catch (IndexOutOfBoundsException e) {
            logger.info("Can't resolve domain name: " + name);
            client.setCantResolveHostName();
            this.server.removeChannel(client);
        }
    }

    private SOCKSClient getClientByHostName(Set<SOCKSChannel> channels, String hostName) {
        String processedHostName = hostName.substring(0, hostName.length() - 1);
        for (var channel : channels) {
            if (channel instanceof SOCKSClient client) {
                if (processedHostName.equals(client.getUnresolvedHostName())) {
                    return client;
                }
            }
        }
        return null;
    }
}
