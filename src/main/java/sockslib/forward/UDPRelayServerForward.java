package sockslib.forward;

import sockslib.server.UDPRelayServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class UDPRelayServerForward extends UDPRelayServer {

    private final InetAddress forwardAddress;
    private final int forwardPort;

    private InetAddress originalAddress = null;
    private int originalPort = 0;


    public UDPRelayServerForward(InetAddress clientInetAddress, int clientPort, InetAddress forwardAddress, int forwardPort) {
        super(new InetSocketAddress(clientInetAddress, clientPort));
        this.forwardAddress = forwardAddress;
        this.forwardPort = forwardPort;
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[bufferSize];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                server.receive(packet);
                if (isFromClient(packet)) {
                    originalAddress = packet.getAddress();
                    originalPort = packet.getPort();
                    packet.setAddress(forwardAddress);
                    packet.setPort(forwardPort);
                    datagramPacketHandler.decapsulate(packet);
                    server.send(packet);
                } else {
                    packet = datagramPacketHandler.encapsulate(packet, clientAddress, clientPort);
                    server.send(packet);
                }
            }
        } catch (IOException e) {
            if (e.getMessage().equalsIgnoreCase("Socket closed") && !running) {
                logger.debug("UDP relay server stopped");
            } else {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public InetAddress getOriginalAddress() {
        return originalAddress;
    }

    public int getOriginalPort() {
        return originalPort;
    }

    public InetAddress getForwardAddress() {
        return forwardAddress;
    }

    public int getForwardPort() {
        return forwardPort;
    }
}
