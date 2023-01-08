package sockslib.forward;

import sockslib.server.Session;
import sockslib.server.Socks5Handler;
import sockslib.server.UDPRelayServer;
import sockslib.server.msg.CommandMessage;
import sockslib.server.msg.CommandResponseMessage;
import sockslib.server.msg.ServerReply;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class Socks5HandlerForward extends Socks5Handler {

    public static final ArrayList<UDPRelayServer> udpRelayServers = new ArrayList<>();
    public static InetAddress forwardTarget;
    public static int forwardPort = 19132;

    static {
        try {
            forwardTarget = InetAddress.getLocalHost();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static UDPRelayServer getUDPRelayServerByPort(int port) {
        for (UDPRelayServer server : udpRelayServers) {
            if (server.getServer().getLocalPort() == port) return server;
        }
        return null;
    }

    @Override
    public void doUDPAssociate(Session session, CommandMessage commandMessage) throws IOException {
        UDPRelayServer udpRelayServer =
                new UDPRelayServerForward(((InetSocketAddress) session.getClientAddress()).getAddress(),
                        commandMessage.getPort(), forwardTarget, forwardPort);
        InetSocketAddress socketAddress = (InetSocketAddress) udpRelayServer.start();
        logger.info("Create UDP relay server at[{}] for {}", socketAddress, commandMessage
                .getSocketAddress());
        udpRelayServers.add(udpRelayServer);
        session.write(new CommandResponseMessage(VERSION, ServerReply.SUCCEEDED, InetAddress
                .getLocalHost(), socketAddress.getPort()));
        try {
            // The client should never send any more data on the control socket, so read() should hang
            // until the client closes the socket (returning -1) or this thread is interrupted (throwing
            // InterruptedIOException).
            int nextByte = session.getInputStream().read();
            if (nextByte != -1) {
                logger.warn("Unexpected data on Session[{}]", session.getId());
            }
        } catch (IOException e) {
            // This is expected on a thread interrupt.
        }
        session.close();
        logger.info("Session[{}] closed", session.getId());
        udpRelayServers.remove(udpRelayServer);
        udpRelayServer.stop();
        logger.debug("UDP relay server for session[{}] is closed", session.getId());
    }
}
