package client;

import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.Scanner;

public class BPPClient {
    private Socket controlSocket = null;
    private BufferedReader controlReader = null;
    private PrintWriter controlWriter = null;
    private String currentResponse;
    private String bppVersion;
    private boolean DEBUG = true; // Debug Flag

    public BPPClient() {
    }

    public void run() {
        Scanner in = new Scanner(System.in);
        String currentInput;
        boolean cont = true;
        while (cont) {
            System.out.println("Enter a BPP version (BPP/1 or BPP/2)");
            bppVersion = in.nextLine();
            System.out.println("Please enter a command (commands found in README):");
            currentInput = in.nextLine();
            System.out.println("\nCommand Received: " + currentInput);
            processInputCmd(currentInput);
            System.out.println("Would you like to continue sending commands? (enter true or false)");
            cont = in.nextBoolean();
            in.nextLine();
            if (cont) {
                System.out.println("Continuing session.");
            }
        }
        System.out.println("Ending current session.");
        in.close();
    }

    private void processInputCmd(String input) {
        String[] splittedInput = input.split("\\s+");
        if (splittedInput[0].equals("%connect")) {
            connect(splittedInput[1], splittedInput[2]);
        } else if (splittedInput[0].equals("%exit")) {
            disconnect();
        }
    }

    private String generateServerCommand(String command) {
        String generatedCommand;
        generatedCommand = command;
        generatedCommand += " " + bppVersion;
        if (command.equals("EXIT")) {
        }
        if (DEBUG) {
            System.out.println("Generated Command: " + generatedCommand);
        }
        return generatedCommand;
    }

    private void connect(String address, String port) {
        try {
            controlSocket = new Socket(address, Integer.parseInt(port));
            InputStream is = controlSocket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            controlReader = new BufferedReader(isr);
            controlWriter = new PrintWriter(controlSocket.getOutputStream(), true);
            System.out.println("Successfully connected to server.");
        } catch (UnknownHostException ex) {
            System.out.println("UnknownHostException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    private void disconnect() {
        String command = generateServerCommand("EXIT");
        currentResponse = sendCommand(command, 200);
        try {
            controlReader.close();
            controlWriter.close();
            controlSocket.close();
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    private String sendCommand(String command, int expected_response_code) {
        String response = "";
        try {
            // send command to the ftp server
            controlWriter.println(command);
            // get response from ftp server
            response = controlReader.readLine();
            if (DEBUG) {
                System.out.println("Current FTP response: " + response);
            }

            String[] splittedResponse = response.split(" ");
            
            // check validity of response
            if (!(splittedResponse[2].equals("STATUS=" + expected_response_code))) {
            throw new IOException(
            "Bad response: " + response);
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        return response;
    }

    public static void main(String argv[]) throws Exception {
        // Infinitely run a terminal searching for user inputted commands
        BPPClient client = new BPPClient();
        client.run();

    }

}
