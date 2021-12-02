package ru.nsu.fit.g19202.karpov.socks.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import ru.nsu.fit.g19202.karpov.socks.channels.partners.SOCKSPartner;
import ru.nsu.fit.g19202.karpov.socks.exceptions.SOCKSException;
import ru.nsu.fit.g19202.karpov.socks.channels.SOCKSChannel;
import ru.nsu.fit.g19202.karpov.socks.channels.SOCKSChannelFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class SOCKServerDefault implements SOCKServer {
    private Selector selector;
    private InetSocketAddress addr;
    private BiMap<SOCKSPartner, SOCKSPartner> conns;
    private BiMap<SelectionKey, SOCKSChannel> channels;
    private Queue<String> domainNames;

    {
        conns = HashBiMap.create();
        channels = HashBiMap.create();
        domainNames = new LinkedList<>();
    }

    public SOCKServerDefault(InetSocketAddress addr) throws IOException, SOCKSException {
        try  {
            this.selector = Selector.open();
            this.addr = addr;
            addChannel("acceptor", this.addr);
            addChannel("dns");
        } catch (IOException | SOCKSException e) {
            try {
                this.close();
            } catch(Exception e2) {
                e2.printStackTrace();
            }
            throw e;
        }
    }

    @Override
    public SOCKSChannel addChannel(String channelType, Object... args) throws SOCKSException, IOException {
        SOCKSChannel channel = SOCKSChannelFactory.createChannel(channelType, args);
        SelectionKey channelKey = channel.register(this);
        this.channels.put(channelKey, channel);
        return channel;
    }

    @Override
    public void addConn(SOCKSPartner p1, SOCKSPartner p2) {
        this.conns.put(p1, p2);
    }

    @Override
    public SOCKSPartner getPartner(SOCKSPartner channel) {
        SOCKSPartner partner = this.conns.get(channel);
        if (partner != null) {
            return partner;
        }
        return this.conns.inverse().get(channel);
    }

    @Override
    public void loop() throws SOCKSException, IOException {
        while (true) {
            int readyChannels = this.selector.select();
            if (readyChannels == 0) {
                continue;
            }
            Set<SelectionKey> keys = this.selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = keys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                SOCKSChannel channel = channels.get(key);
                if (channel != null) {
                    channel.perform();
                }
                keyIterator.remove();
            }
        }
    }

    @Override
    public Selector getSelector() {
        return this.selector;
    }

    @Override
    public void addDomainName(String domainName) {
        this.domainNames.add(domainName);
    }

    @Override
    public String popDomainName() {
        return domainNames.poll();
    }

    @Override
    public boolean isReadable(SOCKSChannel channel) {
        SelectionKey key = this.channels.inverse().get(channel);
        return key.isValid() && key.isReadable();
    }

    @Override
    public boolean isWritable(SOCKSChannel channel) {
        SelectionKey key = this.channels.inverse().get(channel);
        return key.isValid() && key.isWritable();
    }

    @Override
    public Set<SOCKSChannel> getChannels() {
        return this.channels.values();
    }

    @Override
    public void removeChannel(SOCKSChannel channel) {
        SOCKSChannel res = this.conns.remove(channel);
        if (res == null) {
            res = this.conns.inverse().remove(channel);
        }
        if (res != null) {
            try {
                SelectionKey key = this.channels.inverse().remove(res);
                key.cancel();
                res.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            SelectionKey key = this.channels.inverse().remove(channel);
            key.cancel();
            channel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws Exception {
        for (var entry : this.channels.entrySet()) {
            entry.getValue().close();
        }
    }
}
