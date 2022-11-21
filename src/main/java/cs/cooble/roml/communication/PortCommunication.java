package cs.cooble.roml.communication;

import com.fazecast.jSerialComm.SerialPort;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * Created by Matej on 1.4.2017.
 * Gate between program and serial interface
 */
public class PortCommunication implements Consumer<String> {
    private SerialPort[] ports;
    private int currentIndex = -1;
    /**
     * receives program from serial interface
     */
    private ArrayList<Consumer<String>> consumersInputs;
    /**
     * sends program to serial interface
     */
    private ArrayList<Consumer<String>> consumersOutputs;
    private PrintWriter printWriter;
    private Scanner listeningScanner;
    private Thread listeningThread;


    public PortCommunication() {
        consumersInputs = new ArrayList<>();
        consumersInputs.add(this);
        consumersOutputs = new ArrayList<>();
        refreshPorts();
    }

    public void refreshPorts() {
        ports = SerialPort.getCommPorts();
    }

    public boolean openPort(int index) {
        if (currentIndex != -1)
            closePort();
        if (index > -1 && index < ports.length) {
            SerialPort serialPort = ports[index];
            serialPort.setBaudRate(115200);
            serialPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 10,10);
            boolean success = serialPort.openPort();
            currentIndex = (success ? index : -1);
            if (success) {
                startListening();
                startSending();
            }
            return success;
        }
        return false;
    }

    public void startSending() {
        printWriter = new PrintWriter(getCurrentPort().getOutputStream());
    }

    public void sendData(String string) {
        for (Consumer<String> cons : consumersOutputs) {
            cons.accept(string);
        }
        printWriter.println(string);
        printWriter.flush();

    }

    public void sendData(char c) {
        sendData(c + "");
    }

    public void startListening() {
        listeningThread = new Thread(() -> {
            listeningScanner = new Scanner(getCurrentPort().getInputStream());
            while ((listeningScanner.hasNextLine())) {
                String s = listeningScanner.nextLine();
                for (Consumer<String> cons : consumersInputs) {
                    cons.accept(s);
                }
            }
        });
        listeningThread.start();
    }

    public void closePort() {
        if (isPortOpened()) {
            getCurrentPort().closePort();
            listeningThread.interrupt();
            printWriter = null;
            currentIndex = -1;
        }
    }

    public void addInputStringConsumer(Consumer<String> consumer) {
        consumersInputs.add(consumer);
    }

    private boolean removeStringConsumer(Consumer<String> consumer, ArrayList<Consumer<String>> list) {
        for (int i = 0; i < list.size(); i++) {
            Consumer<String> cons = list.get(i);
            if (cons.equals(consumer)) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean removeOutputStringConsumer(Consumer<String> consumer) {
        return removeStringConsumer(consumer, consumersOutputs);
    }

    public boolean removeInputStringConsumer(Consumer<String> consumer) {
        if (consumer != this)
            return removeStringConsumer(consumer, consumersInputs);
        return false;
    }

    public void addOutputStringConsumer(Consumer<String> consumer) {
        consumersOutputs.add(consumer);
    }

    public String[] getPortsNames() {
        refreshPorts();
        String[] out = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            out[i] = ports[i].getSystemPortName();
        }
        return out;
    }

    public boolean isPortOpened() {
        return currentIndex != -1;
    }

    private SerialPort getCurrentPort() {
        if (currentIndex == -1)
            return null;
        else
            return ports[currentIndex];
    }

    private volatile Queue<String> queue = new LinkedList<>();

    public void waitUntilNextMessage(int timeout) {
        long start = System.currentTimeMillis();
        while (queue.size() == 0 && (System.currentTimeMillis() - start) < timeout) ;
    }

    public boolean waitUntilMessage(String targetMessage,int timeout) {
        long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < timeout){
            if(hasMessage())
                if(targetMessage.equals(readMessage()))
                    return true;
        }
        return false;
    }

    public String readMessage(int timeout) {
        waitUntilNextMessage(timeout);
        if (hasMessage())
            return readMessage();
        return null;
    }

    public String readMessage() {
        return queue.poll();
    }

    public boolean hasMessage() {
        return queue.peek() != null;
    }

    public void clearMessages() {
        queue.clear();
    }
    public boolean hasMessage(int timeOut){
        long start = System.currentTimeMillis();
        while (!hasMessage() && (System.currentTimeMillis() - start) < timeOut) ;
        return hasMessage();
    }

    @Override
    public void accept(String s) {
        queue.add(s);
    }

    public Scanner getListeningScanner() {
        return new Scanner(getCurrentPort().getInputStream());
    }

    public PrintWriter getPrintWriter() {
        return new PrintWriter(getCurrentPort().getOutputStream());
    }
}

//todo use hexadec instead of pure ascii



