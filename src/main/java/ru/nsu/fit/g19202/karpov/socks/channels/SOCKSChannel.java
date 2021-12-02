package ru.nsu.fit.g19202.karpov.socks.channels;

import ru.nsu.fit.g19202.karpov.socks.exceptions.SOCKSException;
import ru.nsu.fit.g19202.karpov.socks.server.SOCKServer;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface SOCKSChannel extends AutoCloseable {
    public void perform() throws IOException, SOCKSException;
    public SelectionKey register(SOCKServer server) throws SOCKSException, IOException;
}
