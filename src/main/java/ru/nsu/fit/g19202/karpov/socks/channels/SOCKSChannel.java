package ru.nsu.fit.g19202.karpov.socks.channels;

import ru.nsu.fit.g19202.karpov.socks.exceptions.SOCKSException;
import ru.nsu.fit.g19202.karpov.socks.server.SOCKServer;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public interface SOCKSChannel extends AutoCloseable {
    void perform() throws IOException, SOCKSException;
    SelectionKey register(SOCKServer server) throws SOCKSException, IOException;
}
