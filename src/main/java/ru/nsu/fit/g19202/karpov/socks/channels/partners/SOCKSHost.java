package ru.nsu.fit.g19202.karpov.socks.channels.partners;

import ru.nsu.fit.g19202.karpov.socks.exceptions.SOCKSException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class SOCKSHost extends SOCKSPartner {
    public SOCKSHost(InetSocketAddress addr) throws IOException {
        this.socketChannel = SocketChannel.open();
        this.socketChannel.configureBlocking(false);
        this.socketChannel.bind(new InetSocketAddress(0));
        this.socketChannel.connect(addr);
    }

    public byte[] getBoundAddr() throws IOException {
        return ((InetSocketAddress) this.socketChannel.getLocalAddress()).getAddress().getAddress();
    }

    public int getBoundPort() throws IOException {
        return ((InetSocketAddress) this.socketChannel.getLocalAddress()).getPort();
    }

    @Override
    public void perform() throws IOException {
        passData();
    }
}
