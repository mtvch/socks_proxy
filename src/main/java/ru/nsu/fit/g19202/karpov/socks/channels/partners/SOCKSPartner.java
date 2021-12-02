package ru.nsu.fit.g19202.karpov.socks.channels.partners;

import ru.nsu.fit.g19202.karpov.socks.channels.SOCKSChannel;
import ru.nsu.fit.g19202.karpov.socks.server.SOCKServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public abstract class SOCKSPartner implements SOCKSChannel {
    protected SOCKServer server;
    protected SocketChannel socketChannel;
    protected ByteBuffer buf;
    private Op lastOp = null;
    {
        buf = ByteBuffer.allocate(128);
    }

    private enum Op {
        READ,
        WRITE
    }

    @Override
    public SelectionKey register(SOCKServer server) throws IOException {
        this.server = server;
        return this.socketChannel.register(server.getSelector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    @Override
    public void close() throws Exception {
        this.socketChannel.close();
    }

    protected void passData() throws IOException {
        SOCKSPartner partner = this.server.getPartner(this);
        if (this.server.isReadable(this)) {
            if (this.lastOp == Op.WRITE || this.lastOp == null) {
                this.buf.compact();
                this.lastOp = Op.READ;
            }
            int bytesRead = this.socketChannel.read(this.buf);
            if (bytesRead < 0) {
                this.server.removeChannel(this);
                return;
            }
        }
        if (partner.socketChannel.finishConnect() && this.server.isWritable(partner)) {
            if (this.lastOp == Op.READ || this.lastOp == null) {
                this.buf.flip();
                this.lastOp = Op.WRITE;
            }
            partner.socketChannel.write(this.buf);
        }
    }
}
