package report;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import java.util.HashMap;
import java.util.Map;
/**
 *
 * @author Adi
 */
public class MTLoadReport extends Report implements MessageListener{
    private Map<DTNHost, Integer> transmissionLoad;
    private Map<DTNHost, Integer> memoryLoad;
    public MTLoadReport() {
        init();
    }
    @Override
    protected void init() {
        super.init();
        transmissionLoad = new HashMap<>();
        memoryLoad = new HashMap<>();
    }
    @Override
    public void newMessage(Message m) {}
    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        if (transmissionLoad.containsKey(from)) {
            transmissionLoad.replace(from, (transmissionLoad.get(from)+1));
        } else {
            transmissionLoad.put(from, 1);
        }
    }
    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {}
    @Override
    public void messageSavedToBuffer(Message m, DTNHost to) {
        if (memoryLoad.containsKey(to)) {
            memoryLoad.replace(to, (memoryLoad.get(to)+1));
        } else {
            memoryLoad.put(to, 1);
        }
    }
    @Override
    public void done() {
        String temp = "Transmission Load\n";
        for (Map.Entry<DTNHost, Integer> entry : transmissionLoad.entrySet()) {
            temp += entry.getKey()+"\t"+entry.getValue()+"\n";
        }
        temp += "\n\nMemory Load\n";
        for (Map.Entry<DTNHost, Integer> entry : memoryLoad.entrySet()) {
            temp += entry.getKey()+"\t"+entry.getValue()+"\n";
        }
        write(temp);
        super.done(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void connectionUp(DTNHost thisHost) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}