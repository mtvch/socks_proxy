package ru.nsu.fit.g19202.karpov.socks.server;

import ru.nsu.fit.g19202.karpov.socks.channels.partners.SOCKSPartner;
import ru.nsu.fit.g19202.karpov.socks.exceptions.SOCKSException;
import ru.nsu.fit.g19202.karpov.socks.channels.SOCKSChannel;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.Set;

public interface SOCKServer extends AutoCloseable {
    SOCKSChannel addChannel(String channelType, Object... args) throws SOCKSException, IOException;
    void addConn(SOCKSPartner c1, SOCKSPartner c2);
    SOCKSPartner getPartner(SOCKSPartner channel);
    void removeChannel(SOCKSChannel channel);
    Set<SOCKSChannel> getChannels();
    void loop() throws SOCKSException, IOException;
    Selector getSelector();

    void addDomainName(String domainName);

    String popDomainName();
    boolean isReadable(SOCKSChannel channel);
    boolean isWritable(SOCKSChannel channel);
}
