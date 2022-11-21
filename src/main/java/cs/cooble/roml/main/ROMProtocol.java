package cs.cooble.roml.main;

import cs.cooble.roml.communication.PortCommunication;

import java.io.PrintWriter;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * Created by Matej on 24.6.2018.
 * Talks with flasher on Serial Interface using ROMProtocol
 */
public class ROMProtocol {

    private static final int timeOut = 500;
    private PortCommunication port;
    private int EEPROM_SIZE;


    public ROMProtocol(PortCommunication port) {
        this.port = port;
        EEPROM_SIZE=8192;
    }

    /**
     * @throws NullPointerException when timeout or invalid
     */
    public void begin() {
        port.clearMessages();
        if (!port.isPortOpened())
            throw new IllegalArgumentException("Starting protocol when port is closed");

        port.sendData('s');
        EEPROM_SIZE = Integer.parseInt(port.readMessage(timeOut).substring(1));


    }

    /**
     * @throws NullPointerException when timeout or invalid
     */
    public byte readOneByte(int address) {
        port.clearMessages();
        port.sendData("r" + Controller.toHexString(address & 0xffff, 4)); //asks r + address
        return (byte) port.readMessage(timeOut).charAt(1);
    }

    /**
     * @return null if timeout or invalid
     */
   /* public byte[] readWholeMemory(byte[] buff, int timeOut) {
        port.clearMessages();
        int index=-1;

        port.clearMessages();
        port.sendData("t");
        while (index++<getEEPROMSize()){
            if(!port.hasMessage(100)) {
                System.err.println("no respond in time t 2 "+index);
                return null;
            }
            String mes = port.readMessage();
            if(!mes.startsWith("t")) {
                System.err.println("no respond in time t 3");
                return null;
            }

            buff[index]= (byte)(Integer.decode("0x"+mes.substring(1))&255);
        }
        if(!port.waitUntilMessage("ty", 100)) {
            System.err.println("no respond in time t 4");
            return null;
        }
        return buff;
    } */
    /**
     * @return null if timeout or invalid
     */
    public byte[] readWholeMemory(byte[] buff, int timeOut) {
        System.out.println("reding");
        Scanner scanner = port.getListeningScanner();
        PrintWriter writer = port.getPrintWriter();
        writer.println("t");
        writer.flush();
        while (scanner.hasNextLine())
            if(scanner.nextLine().equals("a"))
                break;

        int index = 0;
        while (scanner.hasNextLine()){
            String mes = scanner.nextLine();
            if(!mes.startsWith("t"))
                continue;
            String[] vals = mes.split("t");
            for (String val : vals) {
                buff[index] = (byte) (Integer.decode("0x" + val) & 255);
                index++;
            }

        }
        return buff;
    }

    public boolean writeOneByte(int address, byte value) {
        port.clearMessages();
        port.sendData("w" + Controller.toHexString(address & 0xffff, 4) + Controller.toHexString(value & 0xff, 2)); //asks r + address
        return port.waitUntilMessage("y", timeOut);
    }

    public boolean writeWholeMemory(byte[] buffer) {
        port.clearMessages();
        if (buffer.length != EEPROM_SIZE)
            throw new IllegalArgumentException("Buffer size differs from EEPROM size!   ");
        port.sendData("ffffff");

        if(!port.waitUntilMessage("y",100))
            return false;

        for (byte aBuffer : buffer) {
            port.sendData("w" + Controller.toHexString(aBuffer, 2));
            if(!port.waitUntilMessage("y",100))
                return false;
        }
        port.sendData('e');
        return true;
    }

    public int getEEPROMSize() {
        return EEPROM_SIZE;
    }
}
