package cs.cooble.roml.main;

import cs.cooble.roml.communication.PortCommunication;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Created by Matej on 1.4.2017.
 */
public final class Memory implements Consumer<String> {

    private byte[] src;

    private byte[] buffer;
    private int buffered_index;
    private boolean hasIncomingMesseage;

    private Queue<Integer> changes = new LinkedList<>();

    private PortCommunication communication;

    public Memory(PortCommunication communication){
        this.communication = communication;
        communication.addInputStringConsumer(this);
        src = new byte[(int) Math.pow(2,16)];
        System.out.println("Lengh"+src.length);
        buffer = new byte[src.length];
    }

    public int getSize(){
        return src.length;
    }
    public byte getByte(int address){
        return src[address];
    }
    public void writeByte(int address,byte val){
        boolean change = val!=src[address];
        src[address]=val;
        if(change)
            changes.add(address);
    }

    public void writeToROM(){
        communication.sendData('w');//readEpromSize writing
        while (changes.size()!=0){
            int index  =changes.remove();
            communication.sendData((char)(index>>8));//a0
            communication.sendData((char)index);//a1
            communication.sendData((char) src[index]);//val
        }
        communication.sendData('s');//stop
    }
    public void readFromROM(int fromA,int toA){
        communication.sendData('r');//readEpromSize reading
        //startaddress
        communication.sendData((char)(fromA>>8));//a0
        communication.sendData((char)fromA);//a1

        //endaddress
        communication.sendData((char)(toA>>8));//a0
        communication.sendData((char)toA);//a1

        hasIncomingMesseage=false;

        boolean success = waitTilMesseage(2000);//timeout= 1s
        if(success){
            int bufindex=0;
            for (int i = fromA; i < toA; i++) {
                src[i]=buffer[bufindex];
                bufindex++;
            }
        }
        else System.out.println("Fuck, timeout for recieving data Memory");

    }

    private void toBuffer(byte b){
        buffer[buffered_index]=b;
        buffered_index++;
        buffered_index%=buffer.length;
    }

    @Override
    public void accept(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if(c=='a'){//readEpromSize response
                buffered_index=0;
                hasIncomingMesseage=false;
            }
            else if(c=='b') {//end response
                hasIncomingMesseage=true;

            }else
                toBuffer((byte) c);
        }
    }
    private boolean waitTilMesseage(long timeOut){
        int time=0;
        while (!hasIncomingMesseage&&time<timeOut){
            time++;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return timeOut!=time;
    }
}
