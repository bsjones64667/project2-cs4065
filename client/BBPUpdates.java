package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BBPUpdates implements Runnable {
    private Socket controlSocket = null;
    private BufferedReader controlReader = null;
    private String currentResponse;
    private InputStreamReader isr;
    private volatile boolean paused = false;
    private volatile boolean running = true;
    private final Object pauseLock = new Object();

    BBPUpdates(Socket socket) {
        try {
            controlSocket = socket;
            InputStream is = controlSocket.getInputStream();
            isr = new InputStreamReader(is);
            controlReader = new BufferedReader(isr);
        } catch (UnknownHostException ex) {
            System.out.println("UnknownHostException: " + ex);
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
        System.out.println("Opened server reader.");
    }

    public void close() {
        try {
            controlReader.close();
            // controlSocket.close();
        } catch (IOException ex) {
            System.out.println("IOException: " + ex);
        }
    }

    public void updateCont(boolean newValue) {
        running = newValue;
    }

    public String getCurrentResponse() {
        return currentResponse;
    }

    @Override
    public void run() {
        while (running) {
            synchronized (pauseLock) {
                if (!running) { // may have changed while waiting to
                    // synchronize on pauseLock
                    break;
                }
                if (paused) {
                    try {
                        pauseLock.wait(); // will cause this Thread to block until
                        // another thread calls pauseLock.notifyAll()
                        // Note that calling wait() will
                        // relinquish the synchronized lock that this
                        // thread holds on pauseLock so another thread
                        // can acquire the lock to call notifyAll()
                        // (link with explanation below this code)
                    } catch (InterruptedException ex) {
                        break;
                    }
                    if (!running) { // running might have changed since we paused
                        break;
                    }
                }
            }
            try {
               currentResponse = controlReader.readLine();
                HashMap<String, String> parsedResponse = parseResponse(currentResponse);
                if (parsedResponse.get("command").equals("POST") && parsedResponse.get("status").equals("201")) {
                    System.out.println("Received new message from group " + parsedResponse.get("groups") + ":");
                    System.out.println(parsedResponse.get("messages"));
                    System.out.println("\n");
                } else if (parsedResponse.get("command").equals("LEAVE") && currentResponse.indexOf("MEMBERS=") != -1) {
                    System.out.println("Member left group " + parsedResponse.get("groups") + ":");
                    System.out.println(parsedResponse.get("members"));
                    System.out.println("\n");
                } else if (parsedResponse.get("command").equals("JOIN") && currentResponse.indexOf("MEMBERS=") != -1 && parsedResponse.get("status").equals("201")) {
                    System.out.println("Member joined group " + parsedResponse.get("groups") + ":");
                    System.out.println(parsedResponse.get("members"));
                    System.out.println("\n");
                }
            } catch (IOException ex) {
               System.out.println("IOException: " + ex);
            } catch (NullPointerException ex) {
                System.out.println("NullPointerException: " + ex);
            }

        }
    }

    public HashMap<String, String> parseResponse(String response) {
      // Define the regex pattern to match the string
      String pattern = "(?<command>\\S+)\\s+(?<version>\\S+)\\s+STATUS=(?<status>\\d+)?(?:\\s+MEMBERS=(?<members>[^=\\s]+(?:\\s(?!GROUPS=|MESSAGES=)[^=\\s]+)*))?(?:\\s+GROUPS=(?<groups>[^=\\s]+(?:\\s(?!MEMBERS=|MESSAGES=)[^=\\s]+)*))?(?:\\s+MESSAGES=(?<messages>[^=\\s]+(?:\\s(?!MEMBERS=|GROUPS=)[^=\\s]+)*))?";

      // Use Pattern.matcher() to extract the pattern from the input string
      Pattern regex = Pattern.compile(pattern);
      Matcher matcher = regex.matcher(response);

      HashMap<String, String> result = new HashMap<String, String>();
      // Create a dictionary from the matched groups
      while (matcher.find()) {
          String command = matcher.group("command");
          String version = matcher.group("version");
          String status = matcher.group("status");
          result.put("command", command);
          result.put("version", version);
          result.put("status", status);

          String members = matcher.group("members");
            if (members != null) {
                members = members.replaceAll("(^\\[)|(\\]$)", "");
                result.put("members", members);
            }

            String groups = matcher.group("groups");
            if (groups != null) {
               groups = groups.replaceAll("(^\\{)|(\\}$)", "");
               result.put("groups", groups);
            }

            String messages = matcher.group("messages");
            if (messages != null) {
                messages = messages.replaceAll("(^\\{)|(\\}$)", "");
                result.put("messages", messages);
            }
      }

      return result;
  }

    public void stop() {
        running = false;
        // you might also want to interrupt() the Thread that is
        // running this Runnable, too, or perhaps call:
        resume();
        // to unblock
    }

    public void pause() {
        // you may want to throw an IllegalStateException if !running
        paused = true;
    }

    public void resume() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Unblocks thread
        }
    }

}
