package cs.cooble.roml.cli;

import com.fazecast.jSerialComm.SerialPort;
import cs.cooble.roml.main.Controller;

import java.io.BufferedInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by Matej on 26.6.2018.
 */
public class RomProtocol2 {
    private SerialPort port;
    private volatile int EEPROM_SIZE;

    private PrintWriter out;
    private Scanner in;


    public RomProtocol2(SerialPort port) {
        this.port = port;
        EEPROM_SIZE = 8192;
    }

    /**
     * @throws NullPointerException when timeout or invalid
     */
    public void begin() {
        out = new PrintWriter(port.getOutputStream());
        in = new Scanner(port.getInputStream());
        while (true){
            println("s");
            try {
                EEPROM_SIZE = Integer.parseInt(readLine().substring(1));
                return;
            }
            catch (Exception e){}
        }
    }

    /**
     * @throws NullPointerException when timeout or invalid
     */
    public int readOneByte(int address) {
        println("r" + Controller.toHexString(address & 0xffff, 4)); //asks r + address

        while (true){
            println("r" + Controller.toHexString(address & 0xffff, 4)); //asks r + address
            try {
                return (Integer.decode("0x" + readLine().substring(1)) & 255);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void clearROM(){
        println("cccccc");
        waitFor("y");
    }

    /**
     * @return null if timeout or invalid
     */
    public void readWholeMemory(byte[] buff) {
        println("t");
        waitFor("a");

        int index = 0;
        while (in.hasNextLine() && index != buff.length) {
            String mes = in.nextLine();
            if (!mes.startsWith("t"))
                continue;
            buff[index] = (byte) (Integer.decode("0x" + mes.substring(1)) & 255);
            index++;
        }
        waitFor("ty");
    }

    public boolean writeOneByte(int address, byte value) {
        println("w" + Controller.toHexString(address & 0xffff, 4) + Controller.toHexString(value & 0xff, 2)); //asks r + address
        return waitFor("y","nn");
    }

    public void writeWholeMemory2(byte[] buffer) {
        if (buffer.length != EEPROM_SIZE)
            throw new IllegalArgumentException("Buffer size differs from EEPROM size!   ");
        println("u");
        waitFor("y");
        for (int i = 0; i < buffer.length/16; i++) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < 16; j++) {
                byte aBuffer = buffer[i*16+j];
                builder.append("w" + Controller.toHexString(aBuffer, 2));
            }
            println(builder.toString());
            waitFor("y");
            System.out.println((i*16)+" written byte ");
        }
        println("e");
    }
    public void writeWholeMemory(byte[] buffer) {
        ArrayList<Integer> errors = new ArrayList<>(buffer.length);
        if (buffer.length != EEPROM_SIZE)
            throw new IllegalArgumentException("Buffer size differs from EEPROM size!   ");
        for (int i = 0; i < buffer.length; i++) {
            if(!writeOneByte(i,buffer[i])){
                errors.add(i);
                System.out.println("Error "+i);
            }
        }
        System.out.println("Errors: "+errors.size());
    }
  /*  public void writeWholeMemoryFast(byte[] buffer) {
        ArrayList<Integer> errors = new ArrayList<>(buffer.length);
        if (buffer.length != EEPROM_SIZE)
            throw new IllegalArgumentException("Buffer size differs from EEPROM size!   ");
        for (int i = 0; i < buffer.length/8; i++) {
            StringBuilder builder = new StringBuilder();
            for (int k = 0; k < 8; k++) {
                int address = i*(buffer.length/8)+k;
                builder.append("w");
                builder.append(Controller.toHexString(address & 0xffff, 4)).append(Controller.toHexString(buffer[address] & 0xff, 2));
            }
            println(builder.toString());
            String feedback = readLine();
            for (int k = 0; k < feedback.length(); k++) {
                if(feedback.charAt(k)!='y'){
                    errors.add(i*(buffer.length/8)+k);
                }
            }

        }
        System.out.println("Errors: "+errors.size());
    }*/

    public int getEEPROMSize() {
        return EEPROM_SIZE;
    }

    private void println(String s) {
        System.out.println("-> "+s);
        out.println("o"+s+"p");
        out.flush();
    }

    private String readLine() {
        String s = in.nextLine();
        System.out.println("<- "+s);
        return s;
    }

    private void waitFor(String message) {
        while (in.hasNextLine()) {
            String e = in.nextLine();
            System.out.println("<- "+e);
            if (e.equals(message))
                return;
        }
    }
    private Boolean waitFor(String message,String message2) {
        while (in.hasNextLine()) {
            String e = in.nextLine();
            System.out.println("<- "+e);
            if (e.equals(message))
                return true;
            if(e.equals(message2))
                return false;
        }
        return null;
    }

}
