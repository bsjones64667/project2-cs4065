package client;

import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.Scanner;

public class BBPClient extends Thread {
    private Socket controlSocket = null;
    private PrintWriter controlWriter = null;
    private String currentResponse;
    private boolean DEBUG = true; // Debug Flag
    private Scanner in;
    private String BBPVersion;
    private InputStreamReader isr;
    private BBPUpdates bbpUpdates;
    private Thread t;

    public BBPClient() {
    }

    public void run() {
        in = new Scanner(System.in);
        String currentInput;
        boolean cont = true;
        while (cont) {
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
            BBPVersion = "BBP/1";
            connect(splittedInput[1], splittedInput[2]);
        } else if (splittedInput[0].equals("%exit")) {
            BBPVersion = "BBP/1";
            disconnect();
        } else if (splittedInput[0].equals("%join")) {
            System.out.println("Enter the name you will use during this session:");
            String memberName = in.nextLine();
            BBPVersion = "BBP/1";
            join(memberName);
        } else if (splittedInput[0].equals("%groupjoin")) {
            System.out.println("Enter the name you will use during this session:");
            String memberName = in.nextLine();
            BBPVersion = "BBP/2";

            groupjoin(memberName, splittedInput[1]);
        } else if (splittedInput[0].equals("%post")) {
            BBPVersion = "BBP/1";
            post(splittedInput[1], splittedInput[2]);
        } else if (splittedInput[0].equals("%grouppost")) {
            BBPVersion = "BBP/2";
            grouppost(splittedInput[1], splittedInput[2], splittedInput[3]);
        } else if (splittedInput[0].equals("%users")) {
            BBPVersion = "BBP/1";
            users();
        } else if (splittedInput[0].equals("%groupusers")) {
            BBPVersion = "BBP/2";
            groupusers(splittedInput[1]);
        } else if (splittedInput[0].equals("%leave")) {
            BBPVersion = "BBP/1";
            leave();
        } else if (splittedInput[0].equals("%groupleave")) {
            BBPVersion = "BBP/2";
            groupleave(splittedInput[1]);
        } else if (splittedInput[0].equals("%message")) {
            BBPVersion = "BBP/1";
            message(splittedInput[1]);
        } else if (splittedInput[0].equals("%groupmessage")) {
            BBPVersion = "BBP/2";
            groupmessage(splittedInput[1], splittedInput[2]);
        } else if (splittedInput[0].equals("%groups")) {
            BBPVersion = "BBP/2";
            groups();
        }
    }

    // private String generateServerCommand(String command) {
    // String generatedCommand;
    // generatedCommand = command;
    // generatedCommand += " " + BBPVersion;
    // if (command.equals("EXIT")) {
    // }
    // else if (command.equals("JOIN")) {
    // generatedCommand += " " + "NAME=" + memberName;
    // if (BBPVersion.equals("BBP/2")) {
    // generatedCommand += " " + "GROUP=" + group;
    // }
    // }
    // if (DEBUG) {
    // System.out.println("Generated Command: " + generatedCommand);
    // }
    // return generatedCommand;
    // }

    private void connect(String address, String port) {
        try {
            controlSocket = new Socket(address, Integer.parseInt(port));
            // InputStream is = controlSocket.getInputStream();
            // isr = new InputStreamReader(is);
            // controlReader = new BufferedReader(isr);
            controlWriter = new PrintWriter(controlSocket.getOutputStream(), true);
            bbpUpdates = new BBPUpdates(controlSocket);
            t = new Thread(bbpUpdates);
            t.start();
        } catch (UnknownHostException ex) {
            System.out.println("UnknownHostException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        System.out.println("Successfully connected to server.");
    }

    private void disconnect() {
        String command = "EXIT " + BBPVersion;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 200);
        try {
            controlWriter.close();
            controlSocket.close();
            bbpUpdates.stop();
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    private void join(String name) {
        String command = "JOIN " + BBPVersion + " NAME=" + name;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 200);
        System.out.println("Successfully joined.");
    }

    private void groupjoin(String name, String groupName) {
        String command = "JOIN " + BBPVersion + " NAME=" + name + " GROUP=" + groupName;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 200);
        System.out.println("Successfully joined group " + groupName);
    }

    private void post(String messageSubject, String messageContent) {
        String command = "POST " + BBPVersion + " MESSAGE_SUBJECT=" + messageSubject + " MESSAGE_CONTENT="
                + messageContent;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 201);
        System.out.println("Successfully sent message.");
    }

    private void grouppost(String group, String messageSubject, String messageContent) {
        String command = "POST " + BBPVersion + " GROUP=" + group + " MESSAGE_SUBJECT=" + messageSubject + " MESSAGE_CONTENT="
                + messageContent;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 201);
        System.out.println("Successfully sent message to " + group);
    }

    private void users() {
        String command = "MEMBERS " + BBPVersion;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 200);
        System.out.println("Current Users: ");
        System.out.println(currentResponse.substring(currentResponse.indexOf("MEMBERS=") + "MEMBERS=".length()));

    }

    private void groupusers(String group) {
        String command = "MEMBERS " + BBPVersion + " GROUP=" + group;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 200);
        System.out.println("Current Users in " + group + ": ");
        System.out.println(currentResponse.substring(currentResponse.indexOf("MEMBERS=") + "MEMBERS=".length()));
    }

    private void leave() {
        String command = "LEAVE " + BBPVersion;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 200);
        System.out.println("Successfully left the group.");
    }

    private void groupleave(String group) {
        String command = "LEAVE " + BBPVersion + " GROUP=" + group;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 200);
        System.out.println("Successfully left group " + group);
    }

    private void message(String id) {
        String command = "MESSAGE " + BBPVersion + " MESSAGE_ID=" + id;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 200);
        System.out.println("Message Content: ");
        System.out.println("Message Content: " + currentResponse.substring(currentResponse.indexOf("MESSAGES=") + "MESSAGES=".length()));
    }

    private void groupmessage(String group, String id) {
        String command = "MESSAGE " + BBPVersion + " GROUP=" + group + " MESSAGE_ID=" + id;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 200);
        System.out.println("Message Content: " + currentResponse.substring(currentResponse.indexOf("MESSAGES=") + "MESSAGES=".length()));
    }

    private void groups() {
        String command = "GROUPS " + BBPVersion;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, 200);
        System.out.println("Groups: ");
        System.out.println(currentResponse.substring(currentResponse.indexOf("GROUPS=") + "GROUPS=".length()));
    }

    private String sendCommand(String command, int expected_response_code) {
        bbpUpdates.pause();
        String response = "";
        try {
            controlWriter.println(command);
            sleep(100);
            response = bbpUpdates.getCurrentResponse();
            if (DEBUG) {
                System.out.println("Current BBP response: " + response);
            }

            String[] splittedResponse = response.split(" ");

            // check validity of response
            if (!(splittedResponse[2].equals("STATUS=" + expected_response_code))) {
                throw new IOException(
                        "Bad response: " + response);
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException: " + ex);
        }
        bbpUpdates.resume();
        return response;
    }

    public static void main(String argv[]) throws Exception {
        // Infinitely run a terminal searching for user inputted commands
        BBPClient client = new BBPClient();
        client.start();

    }

}
