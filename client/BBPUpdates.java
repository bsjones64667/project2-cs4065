package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

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
                String[] splittedResponse = currentResponse.split(" ");
                String status = currentResponse.substring(currentResponse.indexOf("STATUS=") + "STATUS=".length());
                if (splittedResponse[0].equals("POST")) {
                    System.out.println("Received new message:");
                    System.out.println(
                            currentResponse.substring(currentResponse.indexOf("MESSAGES=") + "MESSAGES=".length()));
                } else if (splittedResponse[0].equals("LEAVE") && currentResponse.indexOf("MEMBERS=") != -1) {
                    System.out.println(currentResponse);
                    System.out.println("Member left group:");
                    System.out.println(
                            currentResponse.substring(currentResponse.indexOf("MEMBERS=") + "MEMBERS=".length()));
                } else if (splittedResponse[0].equals("JOIN") && currentResponse.indexOf("MEMBERS=") != -1 && status.startsWith("201")) {
                    System.out.println(currentResponse);
                    System.out.println("Member joined group:");
                    System.out.println(
                            currentResponse.substring(currentResponse.indexOf("MEMBERS=") + "MEMBERS=".length()));
                }
            } catch (IOException ex) {
                System.out.println("IOException: " + ex);
            } catch (NullPointerException ex) {
                System.out.println("NullPointerException: " + ex);
            }

        }
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
