package cs.cooble.roml.main;

import java.util.function.Consumer;

/**
 * Created by Matej on 2.4.2017.
 */
public class ReadConsumer implements Consumer<String> {
    private boolean startRead;
    private byte[] src = new byte[(int) Math.pow(2, 13)];
    private int currentIndex;

    @Override
    public void accept(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == 'm') {
                //  System.out.println("startread");

                startRead = true;
                currentIndex = 0;

            } else if (s.charAt(i) == 'e') {
                startRead = false;
                //  System.out.println("Received whole snake "+currentIndex);

                //proccesData();

            } else if (startRead) {
                // if(s.charAt(i)!='\n') {
                System.out.println("Data accepted: " + s.charAt(i) + " : " + Controller.toBinString((byte) s.charAt(i),8) + " : " + (byte) s.charAt(i));
                src[currentIndex] = (byte) s.charAt(i);
                currentIndex++;
                currentIndex %= src.length;
                // }

            }
            // System.out.println("Received: "+s.charAt(i)+" which is byte value of: "+(byte)s.charAt(i));
        }
    }

    /**
     * @return null if still reading
     */
    public byte[] proccesData() {
        if (startRead)
            return null;
        byte[] out = new byte[currentIndex];
        System.arraycopy(src, 0, out, 0, currentIndex);
        return out;
    }

    public boolean isReady() {
        return !startRead && currentIndex != 0;
    }

    public void clear() {
        currentIndex = 0;
        startRead = false;
    }

    /**
     * waits for incoming data
     *
     * @param i timeout ms
     * @return false if timeout
     */
    public boolean waitPls(int i) {
        int o = 0;
        while (!isReady() && o < i) {
            o++;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return isReady();
    }
}
