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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BBPClient extends Thread  {
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
        help();
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
            Pattern pattern = Pattern.compile("^%connect\\s+-a\\s+(\\S+)\\s+-p\\s+(\\d+)");
            Matcher matcher = pattern.matcher(input);
            try {
               if (!matcher.find()) {
                  System.out.println("Invalid command. Use %help to see valid commands");
                  return cmd;
               }
                connect(matcher.group(1), matcher.group(2));
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
            
        } else if (cmd.equals("%exit")) {
            BBPVersion = "BBP/1";
            disconnect();
        } else if (cmd.equals("%join")) {
            BBPVersion = "BBP/1";
            Pattern pattern = Pattern.compile("^%join\\s+-n\\s+(\\S+(?:\\s+\\S+)*)");
            Matcher matcher = pattern.matcher(input);
            try {
               if (!matcher.find()) {
                  System.out.println("Invalid command. Use %help to see valid commands");
                  return cmd;
               }
                join(matcher.group(1));
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%groupjoin")) {
            BBPVersion = "BBP/2";
            Pattern pattern = Pattern.compile("^%groupjoin\\s+-g\\s+(\\d+)\\s+-n\\s+(\\S+(?:\\s+\\S+)*)");
            Matcher matcher = pattern.matcher(input);
            try {
               if (!matcher.find()) {
                  System.out.println("Invalid command. Use %help to see valid commands");
                  return cmd;
               }
                groupjoin(matcher.group(2), matcher.group(1));
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%post")) {
            BBPVersion = "BBP/1";
            Pattern pattern = Pattern.compile("^%post\\s+-s\\s+(\\S+(?:\\s+\\S+)*)\\s+-c\\s+(\\S+(?:\\s+\\S+)*)");
            Matcher matcher = pattern.matcher(input);
            try {
               if (!matcher.find()) {
                  System.out.println("Invalid command. Use %help to see valid commands");
                  return cmd;
               }
                post(matcher.group(1), matcher.group(2));
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%grouppost")) {
            BBPVersion = "BBP/2";
            Pattern pattern = Pattern.compile("^%grouppost\\s+-g\\s+(\\d+)\\s+-s\\s+(\\S+(?:\\s+\\S+)*)\\s+-c\\s+(\\S+(?:\\s+\\S+)*)");
            Matcher matcher = pattern.matcher(input);
            try {
               if (!matcher.find()) {
                  System.out.println("Invalid command. Use %help to see valid commands");
                  return cmd;
               }
                grouppost(matcher.group(1), matcher.group(2), matcher.group(3));
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%users")) {
            BBPVersion = "BBP/1";
            users();
        } else if (cmd.equals("%groupusers")) {
            BBPVersion = "BBP/2";
            Pattern pattern = Pattern.compile("^%groupusers\\s+-g\\s+(\\d+)");
            Matcher matcher = pattern.matcher(input);
            try {
               if (!matcher.find()) {
                  System.out.println("Invalid command. Use %help to see valid commands");
                  return cmd;
               }
                groupusers(matcher.group(1));
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%leave")) {
            BBPVersion = "BBP/1";
            leave();
        } else if (cmd.equals("%groupleave")) {
            BBPVersion = "BBP/2";
            Pattern pattern = Pattern.compile("^%groupleave\\s+-g\\s+(\\d+)");
            Matcher matcher = pattern.matcher(input);
            try {
               if (!matcher.find()) {
                  System.out.println("Invalid command. Use %help to see valid commands");
                  return cmd;
               }
                groupleave(matcher.group(1));
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%message")) {
            BBPVersion = "BBP/1";
            Pattern pattern = Pattern.compile("^%message\\s+-m\\s+(\\d+)");
            Matcher matcher = pattern.matcher(input);
            try {
               if (!matcher.find()) {
                  System.out.println("Invalid command. Use %help to see valid commands");
                  return cmd;
               }
                message(matcher.group(1));
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%groupmessage")) {
            BBPVersion = "BBP/2";
            Pattern pattern = Pattern.compile("^%groupmessage\\s+-g\\s+(\\d+)\\s+-m\\s+(\\d+)");
            Matcher matcher = pattern.matcher(input);
            try {
               if (!matcher.find()) {
                  System.out.println("Invalid command. Use %help to see valid commands");
                  return cmd;
               }
                groupmessage(matcher.group(1), matcher.group(2));
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println("Not enough parameters for the command.");
                System.out.println("ArrayIndexOutOfBoundsException: " + ex);
            }
        } else if (cmd.equals("%groups")) {
            BBPVersion = "BBP/2";
            groups();
        } else if (cmd.equals("%help")) {
            help();
        } else {
            System.out.println("Invalid command. Use %help to see valid commands");
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
        currentResponse = sendCommand(command, Arrays.asList(200, 201));
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
        currentResponse = sendCommand(command, Arrays.asList(200, 201));
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
          sleep(  100);
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

    private void help() {
      System.out.println("Commands (disregard brackets for parsing purposes):");
      System.out.println("\t%connect -a [address] -p [port]");
      System.out.println("\t%join -n [member_name]");
      System.out.println("\t%post -s [message_subject] -c [message_content]");
      System.out.println("\t%users");
      System.out.println("\t%leave");
      System.out.println("\t%message -m [message_id]");
      System.out.println("\t%exit");
      System.out.println("\t%groups");
      System.out.println("\t%groupjoin -g [group_id] -n [member_name]");
      System.out.println("\t%grouppost -g [group_id] -s [message_subject] -c [message_content]");
      System.out.println("\t%groupusers -g [group_id]");
      System.out.println("\t%groupleave -g [group_id]");
      System.out.println("\t%groupmessage -g [group_id] -m [message_id]");
    }

    public static void main(String argv[]) throws Exception {
        // Infinitely run a terminal searching for user inputted commands
        BBPClient client = new BBPClient();
        client.start();

    }

}
