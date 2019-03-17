package cs.cooble.roml.main;

import cs.cooble.roml.communication.Logger;
import cs.cooble.roml.communication.PortCommunication;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.FileChooser;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Communication Protocol:
 * #Computer
 * *startStopPrintingChar  -> enable/disable printing
 * *dataStepsSend          -> request for sending number of max steps
 * *dataSendStartEnd       -> open and close Bracket for sending position to go to
 * <p>
 * #Printer
 * *dataReceivedChar       -> sends to computer while data was received by printer
 * *dataStepsReceiveStart  -> open Bracket for sending numbers of max steps
 * *dataStepsReceiveEnd    -> close Bracket for sending numbers of max steps
 */
public class Controller {
    private final MainApp main;
    private final ComboBox<String> comPortBox;
    private PortCommunication portCommunnication;
    private boolean autoScroll;
    private Logger loggerSerial;
    private Logger loggerUser;
    private HashMap<Integer, String> wrongs = new HashMap<>();
    private ROMProtocol protocol;
    private static final int limitLogger=3000;


    public Controller(final MainApp main, ComboBox<String> comPortBox) {
        this.main = main;
        this.comPortBox = comPortBox;
        Consumer<String> consumer = s -> {
            final String s1 = s;
            Platform.runLater(() -> {
                if(main.textArea.getText().length()>limitLogger)
                    main.textArea.setText("");
                if (s.contains(Logger.CLEAR_COMMAND))
                    main.textArea.setText("");
                else
                    main.textArea.appendText(s1);
              /*  if (autoScroll) {
                    main.textArea.appendText("");
                }*/
            });
        };
        Consumer<String> consumer2 = s -> {
            if(main.textArea2.getText().length()>limitLogger)
                main.textArea2.setText("");
            final String s1 = s;
            Platform.runLater(() -> {
                if (s.contains(Logger.CLEAR_COMMAND))
                    main.textArea2.setText("");
                else
                    main.textArea2.appendText(s1);
            });
        };
        loggerSerial = new Logger(consumer);
        loggerUser = new Logger(consumer2);
        portCommunnication = new PortCommunication();
        String[] serialPorts = portCommunnication.getPortsNames();
        for (String serialPort : serialPorts) {
            comPortBox.getItems().add(serialPort);
        }
        protocol = new ROMProtocol(portCommunnication);
    }

    private Consumer<String> lastUsedPortConsumerIn;
    private Consumer<String> lastUsedPortConsumerOut;

    private void onButtonPressed2(Button button) {
        String buttonText = button.getText();
        switch (buttonText) {
            case "Connect":
                int i = comPortBox.getSelectionModel().getSelectedIndex();
                if (i != -1) {
                    final String comName = comPortBox.getSelectionModel().getSelectedItem();
                    loggerSerial.addMessage("Opening port: " + comName);
                    if (portCommunnication.openPort(i)) {//success
                        Platform.runLater(() -> button.setText("Disconnect"));
                        Platform.runLater(() -> main.sendBtn.setDisable(false));
                        loggerSerial.addMessage("Port : " + comName + " opened successfully.");
                        reloadListeners(comName);
                        Thread t = new Thread(() -> {
                            try {
                                protocol.begin();
                            } catch (Exception e) {
                                for (int j = 0; j < 5000; j++) {
                                    if (portCommunnication.hasMessage()) {
                                        portCommunnication.readMessage();
                                        break;
                                    }
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException ee) {
                                        ee.printStackTrace();
                                    }
                                }
                                protocol.begin();
                            }
                        });
                        t.start();


                    } else {
                        loggerSerial.addMessage("Error while opening port: " + comName);
                    }
                }
                break;
            case "Disconnect":
                Platform.runLater(() -> button.setText("Connect"));

                portCommunnication.closePort();
                loggerSerial.addMessage("Port closed.");
                Platform.runLater(() -> main.sendBtn.setDisable(true));
                break;
            case "Send":
                String txt = main.textField.getText();
                if (txt.length() == 0)
                    return;

                txt = replaceNumbersCharBin(txt);
                txt = replaceNumbersCharDec(txt);

                portCommunnication.sendData(txt);
                if (!txt.equals(main.textField.getText())) {
                    txt = main.textField.getText();
                    loggerSerial.appendMessage("          (BeforeTranslation: " + txt + ")");
                }
                break;


        }
    }

    public void onButtonPressed(Button button) {
        runLater(() -> onButtonPressed2(button));
    }


    public static String replaceNumbersCharDec(String text) {
        int startIndex = -1;
        for (int j = 0; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c == '{') {
                startIndex = j;
            } else if (c == '}' && startIndex != -1) {
                String newText = text.substring(0, startIndex);
                int i = Integer.parseInt(text.substring(startIndex + 1, j));
                newText += toHexString(i, 2);
                newText += text.substring(j + 1);
                return replaceNumbersCharDec(newText);

            }
        }
        return text;
    }


    public static String replaceNumbersCharBin(String text) {
        int startIndex = 0;
        for (int j = 0; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c == '[') {
                startIndex = j;
            } else if (c == ']') {
                String newText = text.substring(0, startIndex);
                int i = toIntFromBin(text.substring(startIndex + 1, j));
                newText += toHexString(i, 2);
                newText += text.substring(j + 1);
                return replaceNumbersCharBin(newText);
            }
        }
        return text;
    }

    private static Consumer<String> serialOutputter;

    public void reloadListeners(String portName) {
        if (serialOutputter == null) {
            // serialOutputter = s -> System.out.println("rec: " + s);
            // portCommunnication.addportCommunnicationStringConsumer(serialOutputter);
        }
        if (lastUsedPortConsumerIn != null)
            portCommunnication.removeInputStringConsumer(lastUsedPortConsumerIn);
        if (lastUsedPortConsumerOut != null)
            portCommunnication.removeOutputStringConsumer(lastUsedPortConsumerOut);

        Consumer<String> newConsumerIn = s -> loggerSerial.addMessage("[" + portName + "] " + s);
        Consumer<String> newConsumerOut = s -> loggerSerial.addMessage("[YOU] " + s);

        portCommunnication.addInputStringConsumer(newConsumerIn);
        portCommunnication.addOutputStringConsumer(newConsumerOut);

        lastUsedPortConsumerIn = newConsumerIn;
        lastUsedPortConsumerOut = newConsumerOut;
    }

    public void setCheckBoxState(boolean isChecked) {
        autoScroll = isChecked;
    }

    public void clearLog() {
        loggerSerial.clear();
        loggerUser.clear();
    }

    public Logger getLoggerSerial() {
        return loggerSerial;
    }

    public Logger getLoggerUser() {
        return loggerUser;
    }

    public void onProgramQuited() {
        portCommunnication.closePort();
    }

    public void saveWrongs() {
        HashMap<Integer, Byte> srcROM = ROM.getSrc();
        HashMap<Integer, Byte> srcROMcopy = new HashMap<>();
        srcROM.forEach(new BiConsumer<Integer, Byte>() {
            @Override
            public void accept(Integer integer, Byte aByte) {
                srcROMcopy.put(integer, aByte);
            }
        });
        ROM.clear();
        wrongs.forEach(new BiConsumer<Integer, String>() {
            @Override
            public void accept(Integer integer, String s) {
                ROM.put(integer, srcROMcopy.get(integer));
            }
        });
        loggerUser.addMessage("Wrongs to be repaired: " + wrongs.size());
        loggerUser.addMessage(ROM.toStringo());
        loggerUser.addMessage("Wrongs to be repaired: " + wrongs.size());
        loggerUser.addMessage("Send 'w' to programmer and put 12V on OE pin. Then press 'writeToMemory'.");


    }

    public void checkIfSuccessWrite() {
        Thread thread = new Thread(this::checkIfSuccessWrite2);
        thread.start();
    }

    private void checkIfSuccessWrite2() {

        loggerUser.addMessage("Reading Data: ");
        byte[] buffer = new byte[protocol.getEEPROMSize()];
        buffer = protocol.readWholeMemory(buffer, 10000);
        if (buffer == null) {
            loggerUser.addMessage("Cannot read EEPROM!");
            return;
        }
        Map<Integer, Byte> src = ROM.getSrc();
        final List<Integer> wrongIndexes = new ArrayList<>();
        final byte[] finalBuffer = buffer;
        src.forEach(new BiConsumer<Integer, Byte>() {
            @Override
            public void accept(Integer integer, Byte wanted) {
                if (finalBuffer[integer] != wanted) {
                    wrongIndexes.add(integer);
                }
            }
        });
        if (wrongIndexes.size() == 0) {
            loggerUser.addMessage("All values are right! -> Success :D");
        } else {
            loggerUser.addMessage(wrongIndexes.size() + " inequalities were found:");
            loggerUser.addMessage("Here they are (" + wrongIndexes.size() + "/" + src.size() + "):");
            wrongIndexes.forEach(new Consumer<Integer>() {
                @Override
                public void accept(Integer integer) {
                    loggerUser.addMessage(toBinString(integer, 13) + " : " + toBinString(finalBuffer[integer], 8) + " (" + toBinString(src.get(integer), 8) + ")");
                }
            });
            loggerUser.addMessage("Wrongs: (" + wrongIndexes.size() + "/" + src.size() + ")");
            loggerUser.addMessage(wrongIndexes.size() + " inequalities were found:");
            loggerUser.addMessage("That is a pity!");
        }

    }

    public static String toBinString(int integer, int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(((integer & (1 << (length - 1 - i))) != 0) ? "1" : "0");
        }
        return builder.toString();
    }

    public static int toIntFromBin(String string) {
        int out = 0;
        for (int i = 0; i < string.length(); i++) {
            boolean one = string.charAt(i) == '1';
            if (one)
                out |= 1 << (string.length() - 1 - i);
        }
        return out;
    }


    public void writeToMemory() {
        Thread thread = new Thread(this::writeToMemoryReal);
        thread.start();
    }

    public void writeToMemoryReal() {
        String s = "Writing data to ROM: " + ROM.getSrc().size();
        loggerUser.addMessage(s);
        loggerUser.addMessage("=");
        HashMap<Integer, Byte> map = ROM.getSrc();
        map.forEach(new BiConsumer<Integer, Byte>() {
            @Override
            public void accept(Integer integer, Byte aByte) {
                protocol.writeOneByte(integer, aByte);
            }
        });
        loggerUser.addMessage("Writing completed! (" + map.size() + "/" + map.size() + ")");
    }

    public void writeWholeMemory() {
        runLater(this::writeWholeMemory2);
    }

    public void writeWholeMemory2() {
        if (ROM.getSrc().size() != protocol.getEEPROMSize()) {
            loggerUser.addMessage("file too small: " + ROM.getSrc().size() + ", need whole memory: " + protocol.getEEPROMSize() + " Bytes");
            return;
        }
        loggerUser.addMessage("Writing to whole ROM: " + ROM.getSrc().size());
        byte[] buf = new byte[ROM.getSrc().size()];

        for (int i = 0; i < protocol.getEEPROMSize(); i++) {
            buf[i] = ROM.getSrc().get(i);
        }
        if (protocol.writeWholeMemory(buf))
            loggerUser.addMessage("Writing whole rom complete!");
        else loggerUser.addMessage("Failure while writing:(");

    }

    private String toHexString(byte b) {
        //System.out.println("from "+b);
        String out = Integer.toHexString(b);
        if (out.length() > 2) {
            out = out.substring(out.length() - 2);
        } else if (out.length() == 1) {
            out = "0" + out;
        }
        // System.out.println("to "+out);
        return out;
    }

    private String toHexString(int b) {
        //System.out.println("from "+b);
        String out = Integer.toHexString(b);
        if (out.length() > 2) {
            out = out.substring(out.length() - 2);
        } else if (out.length() == 1) {
            out = "0" + out;
        }
        // System.out.println("to "+out);
        return out;
    }

    public static String toHexString(int val, int length) {
        String out = Integer.toHexString(val);
        while (out.length() < length)
            out = "0" + out;
        return out;
    }

    public static String toHexString(byte val, int length) {
        return toHexString(val & 255, length);
    }

    public void testButton() {
        runLater(()->{
            protocol.begin();
            System.out.println("size "+protocol.getEEPROMSize());
            byte[] bytes = new byte[protocol.getEEPROMSize()];
            if(protocol.readWholeMemory(bytes,5000)!=null){
                System.out.println("success read");
            }
            else{
                System.out.println("null bytes");
            }
          //  System.out.println("Testing:");
           // System.out.println("equls "+portCommunnication.waitUntilMessage("s8192", 5000));
        });

    }

    private void readFile(File file) {
        ROM.clear();
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = null;
                try {
                    line = br.readLine();
                    while (line != null) {
                        line = line.replace(':', ' ');
                        if (line.startsWith("//") || line.startsWith("#")) {
                            line = br.readLine();
                            continue;

                        }
                        String[] divide = line.split("  ");
                        if (divide.length == 2)
                            ROM.put(divide[0], divide[1]);
                        line = br.readLine();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                //System.out.println("Loaded:");
                loggerUser.addMessage(ROM.toStringo());
                //System.out.println(ROM.getWriteMesseageToHardware());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File chooseDialog(String title, String pathName) {
        // return new File("/C:/Users/Matej/Desktop/memory.txt/");
        final FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.setInitialDirectory(new File(pathName));
        return fc.showOpenDialog(null);
    }

    private File saveDialog(String title, String pathName) {
        // return new File("/C:/Users/Matej/Desktop/memory.txt/");
        final FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.setInitialDirectory(new File(pathName));
        return fc.showSaveDialog(null);
    }

    public void loadFile() {
        File file = chooseDialog("Chose memory file", "C:\\Users\\Matej\\Desktop");
        if (file == null)
            return;
        readFile(file);
        loggerUser.addMessage("File: " + file.getAbsolutePath() + " loaded to Buffer.");
    }

    public static void runLater(Runnable run) {
        new Thread(run).start();
    }

    public void readToFile() {
        File f = saveDialog("Choose file to save rom content", "C:\\Users\\Matej\\Desktop");
        if (f == null)
            return;
        if (!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        runLater(() -> readToFile2(f));
    }

    private void readToFile2(File f) {
        loggerUser.addMessage("Reading ROM");
        loggerUser.addMessage("mil " + System.currentTimeMillis());

        byte[] buffer = new byte[protocol.getEEPROMSize()];
        long m = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.err.println("Memory before " + m / 1000_000);
        buffer = protocol.readWholeMemory(buffer, 10000);
        long mm = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        // System.err.println("Memory after  " + mm / 1000_000);
        System.err.println("used memory: " + (mm - m) / 1000_000 + "MB");

        if (buffer == null) {
            loggerUser.addMessage("Cannot read EEPROM!");
            return;
        }
        loggerUser.addMessage("Writing to file ");
        try (PrintWriter writer = new PrintWriter(f)) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            writer.println("#Content of EEPROM " + dateFormat.format(date));
            writer.println();
            writer.println();
            for (int i = 0; i < buffer.length; i++) {
                writer.println(toBinString(i, 13) + " : " + toBinString(buffer[i], 8));
                //   writer.println(toBinString(i, 13) + " : " + "11001100");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        loggerUser.addMessage("File saved: " + f.getAbsolutePath());
        buffer = null;

        System.gc();
        mm = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("Memory total after  " + mm / 1000_000);
    }
}
