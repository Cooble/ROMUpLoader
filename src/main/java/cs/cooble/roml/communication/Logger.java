package cs.cooble.roml.communication;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by Matej on 1.4.2017.
 */
public class Logger {
   // private String[] strings;
    private Consumer<String> consumer;
    private static final int maxLinesLength = 5000;
    public static final String CLEAR_COMMAND="clearmeplease$$$$#@";

    private int currentIndex;
    public Logger(Consumer<String> consumer){
      //  this.strings=new String[maxLinesLength];
        this.consumer = consumer;
        clear();
    }

    private void sendToConsumer(){
       /* StringBuilder builder = new StringBuilder();
        for (int i = 0; i < currentIndex+1; i++) {
            String string = strings[i];
          lder.append(string).append("\n");
        }*/

      //  consumer.accept(builder.toString());
    }
    public void addMessage(String s){
       /* currentIndex++;
        if(currentIndex==maxLinesLength)
            clear();
        strings[currentIndex]=s;*/

        consumer.accept("\n"+s);

    }
    public void clear(){
       addMessage(CLEAR_COMMAND);
    }
    public void appendMessage(String s){
        consumer.accept(s);//todo not working
    }
}
