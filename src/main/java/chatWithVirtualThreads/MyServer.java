package chatWithVirtualThreads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.concurrent.Executors;

public class MyServer {

    public void start(final int portNumber) {
        try (var serverSocket = new ServerSocket(portNumber)) {
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                while (true) {
                    var clientSocket = serverSocket.accept();
                    executor.submit(() -> {
                        System.out.println("Client connected!");
                        var clientIp = clientSocket.getInetAddress().getHostAddress();
                        var clientPort = clientSocket.getPort();
                        try (var clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                             var clientOutput = new PrintWriter(clientSocket.getOutputStream(), true)) {
                            for (String inputLine; (inputLine = clientInput.readLine()) != null; ) {
                                System.out.printf("(%s:%d): %s%n", clientIp, clientPort, inputLine);
                                clientOutput.println(new StringBuilder(inputLine).reverse());
                            }

                        } catch (IOException e) {
                            throw new RuntimeException();
                        }
                    });
                }
            }

        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
