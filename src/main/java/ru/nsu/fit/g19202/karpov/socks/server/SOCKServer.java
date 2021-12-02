package ru.nsu.fit.g19202.karpov.socks.server;

import ru.nsu.fit.g19202.karpov.socks.channels.partners.SOCKSPartner;
import ru.nsu.fit.g19202.karpov.socks.exceptions.SOCKSException;
import ru.nsu.fit.g19202.karpov.socks.channels.SOCKSChannel;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.Set;

public interface SOCKServer extends AutoCloseable {
    public SOCKSChannel addChannel(String channelType, Object... args) throws SOCKSException, IOException;
    public void addConn(SOCKSPartner c1, SOCKSPartner c2);
    public SOCKSPartner getPartner(SOCKSPartner channel);
    public void removeChannel(SOCKSChannel channel);
    public Set<SOCKSChannel> getChannels();
    public void loop() throws SOCKSException, IOException;
    public Selector getSelector();

    void addDomainName(String domainName);

    public String popDomainName();
    public boolean isReadable(SOCKSChannel channel);
    public boolean isWritable(SOCKSChannel channel);
}
