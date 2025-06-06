package chatWithNio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;

public class NioServer {
    public void start(final int portNumber) {
        var clients = new HashSet<SocketChannel>();
        try (var serverSocketChannel = ServerSocketChannel.open();
             var selector = Selector.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(portNumber));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            var buffer = ByteBuffer.allocate(1024);
            while (true) {
                if (selector.select() == 0) {
                    continue;
                }
                for (var key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        if (key.channel() instanceof ServerSocketChannel channel) {
                            SocketChannel client = channel.accept();
                            Socket socket = client.socket();
                            var clientInfo = String.join("", socket.getInetAddress().getHostAddress(), ":", String.valueOf(socket.getPort()));
                            System.out.println(String.join("", "CONNECTED: ", clientInfo));
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ);
                            clients.add(client);
                        } else {
                            throw new RuntimeException("Unknown channel");
                        }
                    } else if (key.isReadable()) {
                        if (key.channel() instanceof SocketChannel client) {
                            var bytesRead = client.read(buffer);
                            if (bytesRead == -1) {
                                var clientInfo = String.join("", client.getRemoteAddress().toString(), ":", String.valueOf(client.socket().getPort()));
                                System.out.println(String.join("", "DISCONNECTED: ", clientInfo));
                                client.close();
                                clients.remove(client);
                                continue;
                            }
                            buffer.flip();
                            var data = new String(buffer.array(), buffer.position(), bytesRead);
                            System.out.print(data);
                            buffer.order(ByteOrder.LITTLE_ENDIAN);
                            buffer.putInt(bytesRead).flip();
                            while (buffer.hasRemaining()) {
                                client.write(buffer);
                            }
                            buffer.order(ByteOrder.BIG_ENDIAN);
                            buffer.clear();
                        } else {
                            throw new RuntimeException("Unknown channel");
                        }
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            for (var client : clients) {
                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
