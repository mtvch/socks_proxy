package ru.nsu.fit.g19202.karpov.socks.channels.partners;

import ru.nsu.fit.g19202.karpov.socks.exceptions.SOCKSException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

public class SOCKSClient extends SOCKSPartner {
    private int hostPort;
    private InetAddress hostAddr = null;
    private String hostDomainName = null;
    private boolean cantResolveDomainName = false;
    private State state = State.INIT;
    private static final Logger logger = Logger.getLogger(SOCKSClient.class.getName());

    private enum State {
        INIT,
        CLIENT_GREETED_SERVER,
        SERVER_GREETED_CLIENT,
        CLIENT_REQUESTED_CONN,
        CONNECTED
    }

    public SOCKSClient(SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(false);
    }

    @Override
    public void perform() {
        try {
            switch (this.state) {
                case INIT -> receiveGreetingIfReady();
                case CLIENT_GREETED_SERVER -> greetClientIfReady();
                case SERVER_GREETED_CLIENT -> processClientConnRequestIfReady();
                case CLIENT_REQUESTED_CONN -> grantConnIfReady();
                case CONNECTED -> passData();
                default -> throw new IllegalStateException();
            }
        } catch (Exception e) {
            this.server.removeChannel(this);
            e.printStackTrace();
        }
    }

    public String getUnresolvedHostName() {
        if (this.hostAddr == null) {
            return this.hostDomainName;
        }
        return null;
    }

    public void resolveHostName(InetAddress addr) {
        this.hostAddr = addr;
    }

    public void setCantResolveHostName() {
        this.cantResolveDomainName = true;
    }

    private void receiveGreetingIfReady() throws IOException {
        if (this.server.isReadable(this)) {
            this.buf.clear();
            int bytesRead = this.socketChannel.read(this.buf);
            if (bytesRead == -1) {
                processClientClosedConn();
                return;
            }
            this.buf.flip();
            logger.info("Received greeting from client: " + bufContent());
            if (this.buf.limit() < 3) {
                logger.warning("Not enough bytes in greeting");
                this.server.removeChannel(this);
                return;
            }
            processClientVersion(this.buf.get());
            byte numberAuthMethods = this.buf.get();
            processNumberAuthMethods(numberAuthMethods);
            byte[] methods = new byte[numberAuthMethods];
            this.buf.get(methods);
            processAuthMethods(methods);
            this.state = State.CLIENT_GREETED_SERVER;
        }
    }

    private void processClientVersion(byte version) {
         if (version != 0x05) {
             logger.warning("Client's version is not 0x05");
             this.server.removeChannel(this);
         }
    }

    private void processNumberAuthMethods(byte number) {
         if (number < 0x01) {
             logger.warning("Client does not support any auth methods");
             this.server.removeChannel(this);
         }
    }

     private void processAuthMethods(byte[] methods) throws IOException {
         boolean isNoAuthSupported = false;
         for (byte m : methods)
             if (m == 0x00) {
                 isNoAuthSupported = true;
                 break;
             }
         if (!isNoAuthSupported) {
             logger.warning("Client does not support No Auth method");
             sendNoAuthIfReady();
             this.server.removeChannel(this);
         }
     }

     private void sendNoAuthIfReady() throws IOException {
         byte[] resp = {0x5, (byte) 0xFF};
         sendIfReady(resp);
     }

     private void sendAddrTypeNotSupportedIfReady() throws IOException {
        byte[] resp = {0x5, 0x08, 0x00};
        sendIfReady(resp);
     }

     private void sendCommandNotSupportedIfReady() throws IOException {
        byte[] resp = {0x5, 0x07, 0x00};
        sendIfReady(resp);
     }

     private void sendGeneralFailureIfReady() throws IOException {
        byte[] resp = {0x5, 0x01, 0x00};
        sendIfReady(resp);
     }

     private void sendHostUnreachableIfReady() throws IOException {
        byte[] resp = {0x5, 0x04, 0x00};
        sendIfReady(resp);
     }

     private void sendIfReady(byte[] msg) throws IOException {
        if (this.server.isWritable(this)) {
            send(msg);
        }
     }

     private void send(byte[] msg) throws IOException {
        this.buf.clear();
        this.buf.put(msg);
        this.buf.flip();
        this.socketChannel.write(this.buf);
     }

     private void greetClientIfReady() throws IOException {
        if (this.server.isWritable(this)) {
            this.buf.clear();
            byte[] resp = {0x5, 0x00};
            this.buf.put(resp);
            this.buf.flip();
            logger.info("Greeting client: " + bufContent());
            this.socketChannel.write(this.buf);
            this.state = State.SERVER_GREETED_CLIENT;
        }
     }

     private String bufContent() {
        byte[] data = new byte[this.buf.limit()];
        this.buf.duplicate().get(data);
        return Arrays.toString(data);
     }

     private void processClientConnRequestIfReady() throws IOException {
        if (this.server.isReadable(this)) {
            this.buf.clear();
            int bytesRead = this.socketChannel.read(this.buf);
            if (bytesRead == -1) {
                processClientClosedConn();
                return;
            }
            this.buf.flip();
            logger.info("Received conn request from client: " + bufContent());
            if (this.buf.limit() < 4) {
                logger.warning("Not enough data");
                this.server.removeChannel(this);
                return;
            }
            processClientVersion(this.buf.get());
            processClientCommand(this.buf.get());
            processReservedByte(this.buf.get());
            processDestAddr();
            processDestPort();
            this.state = State.CLIENT_REQUESTED_CONN;
        }
     }

     private void processClientClosedConn() {
        logger.warning("Client closed connection");
        this.server.removeChannel(this);
     }

     public void processDestPort() {
         byte[] portBytes = new byte[2];
         this.buf.get(portBytes);
         this.hostPort = (portBytes[0] << 8) | (portBytes[1] & 0xFF);
     }

     public void processDestAddr() throws IOException {
         switch (this.buf.get()) {
             case 0x01 -> processIPv4Addr();
             case 0x03 -> processDomainNameAddr();
             default -> {
                 logger.warning("Address type not suported");
                 sendAddrTypeNotSupportedIfReady();
                 this.server.removeChannel(this);
             }
         }
     }

     public void processDomainNameAddr() {
         byte nameLength = this.buf.get();
         if (nameLength < 1) {
             logger.warning("Invalid name length");
             this.server.removeChannel(this);
             return;
         }
         byte[] domainName = new byte[nameLength];
         buf.get(domainName);
         this.hostDomainName = new String(domainName, StandardCharsets.UTF_8);
         this.server.addDomainName(this.hostDomainName);
     }

     public void processIPv4Addr() throws IOException {
         byte[] ipAddr = new byte[4];
         this.buf.get(ipAddr);
         try {
             this.hostAddr = InetAddress.getByAddress(ipAddr);
         } catch(UnknownHostException e) {
             logger.warning("Unknown host");
             sendGeneralFailureIfReady();
         }
     }

     public void processReservedByte(byte b) {
         if (b != 0x00) {
             logger.warning("No reserved byte");
             this.server.removeChannel(this);
         }
     }

     public void processClientCommand(byte command) throws IOException {
         if (command != 0x01) {
             logger.warning("Command not supported");
             sendCommandNotSupportedIfReady();
         }
     }

     public void grantConnIfReady() throws IOException, SOCKSException {
        if (this.server.isWritable(this) && this.hostAddr != null) {
            SOCKSHost host = (SOCKSHost) this.server.addChannel("host", new InetSocketAddress(this.hostAddr, this.hostPort));
            byte[] bndAddr = host.getBoundAddr();
            int port = host.getBoundPort();
            byte[] msg = {0x05, 0x00, 0x00, 0x01, bndAddr[0], bndAddr[1], bndAddr[2], bndAddr[3], (byte)((port >> 8) & 0xff), (byte)((port >> 8) & 0xff)};
            send(msg);
            logger.info("Connection granted");
            this.server.addConn(this, host);
            this.state = State.CONNECTED;
            return;
        }
        if (this.cantResolveDomainName) {
            sendHostUnreachableIfReady();
            this.server.removeChannel(this);
        }
     }
}
