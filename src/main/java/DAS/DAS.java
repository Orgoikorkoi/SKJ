import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class DAS
{
    public static void main(String[] args)
    {
        int port;
        int number;

        if (args.length != 2)
        {
            System.err.println("Invalid number of arguments");
            return;
        }

        try
        {
            port = Integer.parseInt(args[0]);
            number = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException e)
        {
            System.err.println("Invalid arguments type");
            return;
        }

        try
        {
            DatagramSocket socket = new DatagramSocket(port);
            MasterMode(socket, port, number);
        }
        catch (SocketException e)
        {
            SlaveMode(port, number);
        }
    }

    private static void MasterMode(DatagramSocket socket, int port, int number)
    {
        ArrayList<Integer> numbersList = new ArrayList<>();
        numbersList.add(number);
        byte[] buffer = new byte[1024];

        try
        {
            while (true)
            {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());

                try
                {
                    int messageNumber = Integer.parseInt(message);

                    if (messageNumber == 0)
                    {
                        int average = CalculateAverage(numbersList);
                        System.out.println("Average: " + average);
                        SendMessageToLocalDevices(socket, port, "Average: " + average);
                    }
                    else if (messageNumber == -1)
                    {
                        System.out.println(messageNumber);
                        SendMessageToLocalDevices(socket, port, String.valueOf(messageNumber));
                        socket.close();
                        break;
                    }
                    else
                    {
                        System.out.println(messageNumber);
                        numbersList.add(messageNumber);
                    }
                }
                catch (NumberFormatException e)
                {
                    continue;
                }
            }
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
        }
    }


    private static void SlaveMode(int port, int number)
    {
        try (DatagramSocket socket = new DatagramSocket())
        {
            byte[] numberBytes = String.valueOf(number).getBytes();
            InetAddress address = InetAddress.getByName("127.0.0.1");

            DatagramPacket packet = new DatagramPacket(numberBytes, numberBytes.length, address, port);
            socket.send(packet);
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
        }
    }

    private static void SendMessageToLocalDevices(DatagramSocket socket, int port, String message)
    {
        try
        {
            byte[] messageBytes = message.getBytes();
            InetAddress address = InetAddress.getByName("255.255.255.255");

            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, address, port);
            socket.send(packet);
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
        }
    }

    private static int CalculateAverage(ArrayList<Integer> numbersList)
    {
        int sum = 0;
        int numberOfDigits = 0;

        for (int number : numbersList)
        {
            sum += number;
            numberOfDigits++;
        }

        if (numberOfDigits != 0)
        {
            return sum/numberOfDigits;
        }
        else
        {
            return 0;
        }
    }
}
