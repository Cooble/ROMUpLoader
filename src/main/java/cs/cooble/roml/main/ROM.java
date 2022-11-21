package cs.cooble.roml.main;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by Matej on 1.4.2017.
 */
public class ROM {
    private static HashMap<Integer, Byte> src = new HashMap<>();

    public static void put(int address, byte value) {
        src.put(address, value);
    }

    public static void put(String address, String value) {
        put(Controller.toIntFromBin(address),(byte)Controller.toIntFromBin(value));
    }

    public static HashMap<Integer, Byte> getSrc() {
        return src;
    }

    public static String toStringo() {
        StringBuilder builder = new StringBuilder();
        src.forEach(new BiConsumer<Integer, Byte>() {
            @Override
            public void accept(Integer integer, Byte aByte) {
                builder.append(Controller.toBinString(integer,13) + " : " + Controller.toBinString(aByte,8) + "\n");
            }
        });
        return builder.toString();
    }

    public static void clear() {
        src.clear();
    }
}
