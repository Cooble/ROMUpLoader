package cs.cooble.roml.cli;

import com.fazecast.jSerialComm.SerialPort;
import cs.cooble.roml.main.Controller;
import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by Matej on 26.6.2018.
 * Simple CLI interface using EEPROM Protocol
 * to write content of ROM to file
 * to write to ROM from file
 */
public class Main extends Application {
    private static Scanner scanner;
    private static SerialPort port;
    private static RomProtocol2 protocol;
    private static byte[] buffer;
    private static String defaultpath = "D:\\Datasheets\\mypc";

    public static void main(String[] args) {
        launch();
    }

    public static void startCLI() {
        scanner = new Scanner(System.in);
        choosePort();
        protocol = new RomProtocol2(port);
        System.out.println("Beginning protocol..");
        protocol.begin();
        buffer = new byte[protocol.getEEPROMSize()];
        menu();


    }

    private static void menu() {
        while (true) {

            System.out.println("Choose operation");
            System.out.println("0) Exit");
            System.out.println("1) Ask for size");
            System.out.println("2) Read byte");
            System.out.println("3) Read into file");
            System.out.println("4) Write to ROM from file");
            System.out.println("5) Clear ROM");
            System.out.println("6) Gen random file");
            System.out.println("7) Compare files");
            System.out.println("8) Write byte");
            int i = scanner.nextInt();

            switch (i) {
                case 0:
                    System.exit(0);
                    break;
                case 1:
                    System.out.print("Size is: ");
                    System.out.println(protocol.getEEPROMSize());
                    break;
                case 2:
                    scanner.reset();
                    System.out.println("Enter address");
                    String s = null;
                    while (s == null || Objects.equals(s, "")) {
                        s = scanner.nextLine();
                    }
                    s = Controller.replaceNumbersCharBin(s);
                    s = Controller.replaceNumbersCharDec(s);
                    int address = Integer.decode("0x" + s);
                    if (address > -1 && address < protocol.getEEPROMSize()) {
                        System.out.print("Reading byte: ");
                        System.out.println(protocol.readOneByte(address));
                    } else System.out.println("Invalid address");
                    break;
                case 3:
                    System.out.println("Enter file loc");
                    File f = saveDialog("Enter file to be saved to", defaultpath);
                    if (f == null)
                        break;
                    if (!f.exists())
                        try {
                            f.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    System.out.println("Reading whole ROM...");
                    protocol.readWholeMemory(buffer);
                    System.out.println("Saving content to " + f.getAbsolutePath());
                    saveFile(buffer, f);
                    break;

                case 4:
                    System.out.println("Enter file loc");
                    f = chooseDialog("Enter file to be loaded from", defaultpath);
                    if (f == null)
                        break;

                    clearBuffer();
                    System.out.println("Loading from file...");
                    readFile(buffer, f);
                    System.out.println("writing to ROM...");
                    protocol.writeWholeMemory(buffer);
                    System.out.println("Done");
                    break;
                case 5:
                    System.out.println("Clearing");
                    protocol.clearROM();
                    System.out.println("Done clearing");
                    break;
                case 6:
                    System.out.println("Enter file loc");
                    f = saveDialog("Enter file to be saved to", defaultpath);
                    if (f == null)
                        break;
                    if (!f.exists())
                        try {
                            f.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    Random r = new Random(0);
                    for (int ii = 0; ii < buffer.length; ii++) {
                        buffer[ii] = (byte) r.nextInt(255);
                    }
                    System.out.println("Saving random content to " + f.getAbsolutePath());
                    saveFile(buffer, f);
                    break;
                case 7:
                    System.out.println("Enter file loc");
                    f = chooseDialog("Enter file to be saved to", defaultpath);
                    if (f == null)
                        break;
                    if (!f.exists())
                        break;
                    System.out.println("Enter 2nd file loc");
                    File f2 = chooseDialog("Enter file to be saved to", defaultpath);
                    if (f2 == null)
                        break;
                    if (!f2.exists())
                        break;

                    byte[] buf0 = new byte[buffer.length];
                    byte[] buf1 = new byte[buffer.length];
                    readFile(buf0, f);
                    readFile(buf1, f2);
                    int errors = 0;
                    for (int ii = 0; ii < buf0.length; ii++) {
                        if (buf0[ii] != buf1[ii])
                            errors++;
                    }
                    System.out.println("Files differ by " + errors + "errors");
                    break;
                case 8:
                    scanner.reset();
                    System.out.println("Enter address");
                    s = null;
                    while (s == null || Objects.equals(s, "")) {
                        s = scanner.nextLine();
                    }
                    s = Controller.replaceNumbersCharBin(s);
                    s = Controller.replaceNumbersCharDec(s);
                    address = Integer.decode("0x" + s);

                    System.out.println("Enter value");
                    s = null;
                    while (s == null || Objects.equals(s, "")) {
                        s = scanner.nextLine();
                    }
                    s = Controller.replaceNumbersCharBin(s);
                    s = Controller.replaceNumbersCharDec(s);
                    int val = Integer.decode("0x" + s);

                    if (address > -1 && address < protocol.getEEPROMSize()) {
                        System.out.println("writing byte with: " + (protocol.writeOneByte(address, (byte) (val & 255)) ? "Success" : "Failure"));
                    } else System.out.println("Invalid address");
                    break;
            }
            System.out.println("Type anything to continue");
            scanner.next();
            clearScreen();
        }
    }

    private static int portIndex;

    private static void choosePort() {
        SerialPort[] ports = SerialPort.getCommPorts();
        port = null;
        while (port == null) {
            clearScreen();
            System.out.println("Choose port");
            System.out.println("0) Exit");
            for (int i = 0; i < ports.length; i++)
                System.out.println((i + 1) + ") " + ports[i].getSystemPortName());
            int i = scanner.nextInt();
            if (i > -1 && i < ports.length + 1) {
                if (i == 0)
                    System.exit(0);
                System.out.println("Connecting to " + ports[i - 1].getSystemPortName());
                SerialPort current = ports[i - 1];
                current.setBaudRate(115200);
                current.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 2, 2);
                if (current.openPort()) {
                    portIndex = i - 1;
                    port = current;
                    System.out.println("Connected!");
                } else System.out.println("Cannot connect!");
            } else System.out.println("Invalid number");
        }
    }

    private static void clearScreen() {
        for (int i = 0; i < 20; i++) {
            System.out.println();
        }
        for (int i = 0; i < 20; i++) {
            System.out.print("=");
        }
        System.out.println();
    }

    private static void clearBuffer() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 0;
        }
    }

    private static void readFile(byte[] buffer, File file) {
        int index = 0;
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
                        if (divide.length == 2) {
                            //  System.out.println("Loaded "+index+": "+divide[1]+ " which is "+(byte) (Controller.toIntFromBin(divide[1]) & 255));
                            index++;
                            buffer[Controller.toIntFromBin(divide[0])] = (byte) (Controller.toIntFromBin(divide[1]) & 255);
                        }
                        line = br.readLine();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveFile(byte[] buffer, File file) {
        file.delete();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (PrintWriter writer = new PrintWriter(file)) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            writer.println("#Content of EEPROM " + dateFormat.format(date));
            writer.println();
            writer.println();
            for (int ii = 0; ii < buffer.length; ii++) {
                writer.println(Controller.toBinString(ii, 13) + " : " + Controller.toBinString(buffer[ii], 8));
                //  writer.println(Controller.toBinString(ii, 13) + " : " + "00011000");
            }
            System.out.println("File saved!");
        } catch (Exception e) {
            System.err.println("Failure during saving to file");
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        startCLI();
    }

    private static File chooseDialog(String title, String pathName) {
        // return new File("/C:/Users/Matej/Desktop/memory.txt/");
        final FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.setInitialDirectory(new File(pathName));
        return fc.showOpenDialog(null);
    }

    private static File saveDialog(String title, String pathName) {
        // return new File("/C:/Users/Matej/Desktop/memory.txt/");
        final FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.setInitialDirectory(new File(pathName));
        return fc.showSaveDialog(null);
    }

}
