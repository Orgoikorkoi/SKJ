import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class CCS {
    public static void main(String[] args) {
        int port = -1; // Zainicjalizowanie wartości domyślnej

        if (args.length != 1) {
            System.err.println("Usage: java CCS <port>");
            System.exit(1);
        }

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number. Please provide an integer.");
            System.exit(1);
        }

        try {
            DatagramSocket udpSocket = new DatagramSocket(port);
            ServerSocket tcpSocket = new ServerSocket(port);

            StartServer(udpSocket, tcpSocket, port);
        } catch (IOException e) {
            System.err.println("Error initializing sockets: " + e.getMessage());
        }
    }


    private static void StartServer(DatagramSocket udpSocket, ServerSocket tcpSocket, int port) {
        AtomicInteger totalClients = new AtomicInteger(0);
        AtomicInteger recentClients = new AtomicInteger(0);
        AtomicInteger totalOperations = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger sumOfResults = new AtomicInteger(0);

        ScheduledExecutorService reportExecutor = Executors.newScheduledThreadPool(1);
        ExecutorService clientExecutor = Executors.newCachedThreadPool();

        reportExecutor.scheduleAtFixedRate(() -> ReportStatistics(totalClients, recentClients, totalOperations, errorCount, sumOfResults), 0, 10000, TimeUnit.MILLISECONDS);

        new Thread(() -> HandleUDPServiceDiscovery(udpSocket)).start();

        try {
            while (true) {
                Socket clientSocket = tcpSocket.accept();
                clientExecutor.submit(() -> HandleClient(clientSocket, totalClients, recentClients, totalOperations, errorCount, sumOfResults));
                totalClients.incrementAndGet();
                recentClients.incrementAndGet();
            }
        } catch (IOException e) {
            System.err.println("Error accepting client: " + e.getMessage());
        }
    }

    private static void HandleUDPServiceDiscovery(DatagramSocket udpSocket) {
        byte[] buffer = new byte[1024];
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("CCS DISCOVER")) {
                    byte[] response = "CCS FOUND".getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(response, response.length, packet.getAddress(), packet.getPort());
                    udpSocket.send(responsePacket);
                }
            }
        } catch (IOException e) {
            System.err.println("Error in UDP service discovery: " + e.getMessage());
        }
    }

    private static void HandleClient(Socket clientSocket, AtomicInteger totalClients, AtomicInteger recentClients, AtomicInteger totalOperations, AtomicInteger errorCount, AtomicInteger sumOfResults) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length != 3) {
                    out.println("ERROR");
                    errorCount.incrementAndGet();
                    continue;
                }

                String operation = parts[0];
                int arg1, arg2;

                try {
                    arg1 = Integer.parseInt(parts[1]);
                    arg2 = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    out.println("ERROR");
                    errorCount.incrementAndGet();
                    continue;
                }

                int result;
                try {
                    switch (operation) {
                        case "ADD":
                            result = arg1 + arg2;
                            break;
                        case "SUB":
                            result = arg1 - arg2;
                            break;
                        case "MUL":
                            result = arg1 * arg2;
                            break;
                        case "DIV":
                            if (arg2 == 0) throw new ArithmeticException("Division by zero");
                            result = arg1/arg2;
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid operation");
                    }
                    out.println(result);
                    totalOperations.incrementAndGet();
                    sumOfResults.addAndGet(result);
                } catch (Exception e) {
                    out.println("ERROR");
                    errorCount.incrementAndGet();
                }
            }

        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    private static void ReportStatistics(AtomicInteger totalClients, AtomicInteger recentClients, AtomicInteger totalOperations, AtomicInteger errorCount, AtomicInteger sumOfResults) {
        System.out.println("Total Clients: " + totalClients.get());
        System.out.println("New Clients (last interval): " + recentClients.getAndSet(0));
        System.out.println("Total Operations: " + totalOperations.get());
        System.out.println("Error Count: " + errorCount.get());
        System.out.println("Sum of Results: " + sumOfResults.get());
        System.out.println();
    }
}