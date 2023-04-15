package client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        String cmd = "";
        while (!cmd.equals("%exit")) {
            System.out.println("\nPlease enter a command (commands found in README):");
            currentInput = in.nextLine();
            System.out.println("\nCommand Received: " + currentInput);
            cmd = processInputCmd(currentInput);
        }
        System.out.println("Ending current session.");
        in.close();
    }

    private String processInputCmd(String input) {
        String[] splittedInput = input.split("\\s+");
        String cmd = splittedInput[0];
        if (cmd.equals("%connect")) {
            BBPVersion = "BBP/1";
            try {
                connect(splittedInput[1], splittedInput[2]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
            
        } else if (cmd.equals("%exit")) {
            BBPVersion = "BBP/1";
            disconnect();
        } else if (cmd.equals("%join")) {
            BBPVersion = "BBP/1";
            try {
                join(splittedInput[1]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%groupjoin")) {
            BBPVersion = "BBP/2";
            try {
                groupjoin(splittedInput[2], splittedInput[1]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%post")) {
            BBPVersion = "BBP/1";
            try {
                post(splittedInput[1], splittedInput[2]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%grouppost")) {
            BBPVersion = "BBP/2";
            try {
                grouppost(splittedInput[1], splittedInput[2], splittedInput[3]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%users")) {
            BBPVersion = "BBP/1";
            users();
        } else if (cmd.equals("%groupusers")) {
            BBPVersion = "BBP/2";
            try {
                groupusers(splittedInput[1]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%leave")) {
            BBPVersion = "BBP/1";
            leave();
        } else if (cmd.equals("%groupleave")) {
            BBPVersion = "BBP/2";
            try {
                groupleave(splittedInput[1]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%message")) {
            BBPVersion = "BBP/1";
            try {
                message(splittedInput[1]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%groupmessage")) {
            BBPVersion = "BBP/2";
            try {
                groupmessage(splittedInput[1], splittedInput[2]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%groups")) {
            BBPVersion = "BBP/2";
            groups();
        } else {
            System.out.println("Invalid command.");
        }

        return cmd;
    }

    private void connect(String address, String port) {
        try {
            controlSocket = new Socket(address, Integer.parseInt(port));
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
        currentResponse = sendCommand(command, Arrays.asList(200));
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
        currentResponse = sendCommand(command, Arrays.asList(200));
        if (currentResponse != null) {
           System.out.println("Successfully joined.");
        }
    }

    private void groupjoin(String name, String groupName) {
        String command = "JOIN " + BBPVersion + " NAME=" + name + " GROUP=" + groupName;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, Arrays.asList(200));
        if (currentResponse != null) {
           System.out.println("Successfully joined group " + groupName);
        }
    }

    private void post(String messageSubject, String messageContent) {
        String command = "POST " + BBPVersion + " MESSAGE_SUBJECT=" + messageSubject + " MESSAGE_CONTENT="
                + messageContent;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, Arrays.asList(201));
        if (currentResponse != null) {
            System.out.println("Successfully sent message.");
        }
    }

    private void grouppost(String group, String messageSubject, String messageContent) {
        String command = "POST " + BBPVersion + " GROUP=" + group + " MESSAGE_SUBJECT=" + messageSubject + " MESSAGE_CONTENT="
                + messageContent;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, Arrays.asList(201));
        if (currentResponse != null) {
            System.out.println("Successfully sent message to " + group);
        }
    }

    private void users() {
        String command = "MEMBERS " + BBPVersion;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, Arrays.asList(200, 204));
        if (currentResponse != null) {
            System.out.println("Current Users: ");
            System.out.println(currentResponse.substring(currentResponse.indexOf("MEMBERS=") + "MEMBERS=".length()));
        }

    }

    private void groupusers(String group) {
        String command = "MEMBERS " + BBPVersion + " GROUP=" + group;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, Arrays.asList(200, 204));
        if (currentResponse != null) {
            System.out.println("Current Users in " + group + ": ");
            System.out.println(currentResponse.substring(currentResponse.indexOf("MEMBERS=") + "MEMBERS=".length()));
        }
    }

    private void leave() {
        String command = "LEAVE " + BBPVersion;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, Arrays.asList(200));
        if (currentResponse != null) {
            System.out.println("Successfully left the group.");
        }
    }

    private void groupleave(String group) {
        String command = "LEAVE " + BBPVersion + " GROUP=" + group;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, Arrays.asList(200));
        if (currentResponse != null) {
            System.out.println("Successfully left group " + group);
        }
    }

    private void message(String id) {
        String command = "MESSAGE " + BBPVersion + " MESSAGE_ID=" + id;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, Arrays.asList(200));
        if (currentResponse != null) {
        System.out.println("Message Content: ");
            System.out.println("Message Content: " + currentResponse.substring(currentResponse.indexOf("MESSAGES=") + "MESSAGES=".length()));
        }
    }

    private void groupmessage(String group, String id) {
        String command = "MESSAGE " + BBPVersion + " GROUP=" + group + " MESSAGE_ID=" + id;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, Arrays.asList(200));
        if (currentResponse != null) {
            System.out.println("Message Content: " + currentResponse.substring(currentResponse.indexOf("MESSAGES=") + "MESSAGES=".length()));
        }
    }

    private void groups() {
        String command = "GROUPS " + BBPVersion;
        if (DEBUG) {
            System.out.println("Generated Command: " + command);
        }
        currentResponse = sendCommand(command, Arrays.asList(200));
        if (currentResponse != null) {
            System.out.println("Groups: ");
            System.out.println(currentResponse.substring(currentResponse.indexOf("GROUPS=") + "GROUPS=".length()));
        }
    }

    private String sendCommand(String command, List<Integer> expected_response_code) {
        bbpUpdates.pause();
        String response = "";
        try {
            controlWriter.println(command);
            sleep(500);
            response = bbpUpdates.getCurrentResponse();
            if (DEBUG) {
                System.out.println("Current BBP response: " + response);
            }
            HashMap<String, String> parsedResponse = bbpUpdates.parseResponse(response);

            // check validity of response
            if (!expected_response_code.stream().anyMatch(s -> s == Integer.parseInt(parsedResponse.get("status")))) {
                throw new IOException("Bad response: " + response);
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
            return null;
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException: " + ex);
            return null;
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
