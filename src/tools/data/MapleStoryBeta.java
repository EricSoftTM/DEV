package tools.data;

import constants.ServerConstants;
import handling.MapleServerHandler;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginInformationProvider;
import handling.login.LoginServer;
import handling.mina.MapleCodecFactory;
import handling.world.World;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import server.CashItemFactory;

public class MapleStoryBeta {

    public static final MapleStoryBeta instance = new MapleStoryBeta();
    private static IoAcceptor acceptor;

    public void run() throws InterruptedException {
        System.out.println("Binding Ports for MapleStory Beta v40..");
        acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("codec", (IoFilter) new ProtocolCodecFilter(new MapleCodecFactory()));
        acceptor.setHandler(new MapleServerHandler(-1, -1, false));
        ((SocketSessionConfig) acceptor.getSessionConfig()).setTcpNoDelay(true);
        try {
            acceptor.bind(new InetSocketAddress(8383)); // Center
            //acceptor.bind(new InetSocketAddress(8484)); // Login
            acceptor.bind(new InetSocketAddress(8585)); // Game
            acceptor.bind(new InetSocketAddress(8787)); // Shop0
            acceptor.bind(new InetSocketAddress(8888)); // Claim
            acceptor.bind(new InetSocketAddress(8989)); // MapGen0
            acceptor.bind(new InetSocketAddress(8999)); // MTS (ITC0)
            acceptor.bind(new InetSocketAddress(7070)); // Admin Port
            acceptor.bind(new InetSocketAddress(7071)); // Admin Port
            acceptor.bind(new InetSocketAddress(8580)); // Orion Login0 ??
            System.out.println("Ports are now binded. Currently listening.\r\n8383 - Center,\r\n8484 - Login,\r\n8585 - Game,\r\n8787 - Shops,\r\n8888 - Claim,\r\n8989 - MapGen,\r\n8999 - ITC (MTS),\r\n7070 - Admin Port (One),\r\n7071 - Admin Port (Two),\r\n8580 - Orion Login.");
        } catch (IOException e) {
            System.err.println("Binding to ports 8484, 8485 failed" + e);
        }
        System.out.println("Sockets binded! Now Listening on ports: 8484");
    }

    public static void main(final String args[]) throws InterruptedException {
        instance.run();
    }
}
