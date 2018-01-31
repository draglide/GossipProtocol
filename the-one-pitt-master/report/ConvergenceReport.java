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
public class ConvergenceReport extends Report implements MessageListener{
    private Map<String, ConvergenceData> convergenceTime;
    public ConvergenceReport() {
        init();
    }
    @Override
    protected void init() {
        super.init();
        convergenceTime = new HashMap<String, ConvergenceData>();
    }
    @Override
    public void newMessage(Message m) {
    }
    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }
    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    }
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        if (convergenceTime.containsKey(m.getId())) {
            ConvergenceData d = convergenceTime.get(m.getId());
            d.setNrofNode(d.getNrofNode()+1);
            d.setConvergenceTime(d.getConvergenceTime()+(getSimTime()-m.getCreationTime()));
            d.setLastNode(to);
            convergenceTime.replace(m.getId(), d);
        } else {
            ConvergenceData d = new ConvergenceData();
            d.setSource(from);
            d.setConvergenceTime(getSimTime()-m.getCreationTime());
            d.setLastNode(to);
            d.setNrofNode(1);
            convergenceTime.put(m.getId(), d);
        }
    }
    @Override
    public void done() {
        write("Convergence Report\nSource Node\tMessage ID\tConvergenceTime\t\tLast Node\tNumber Of Node");
        String report = "";
        for (Map.Entry<String, ConvergenceData> e : convergenceTime.entrySet()) {
            String m = e.getKey();
            ConvergenceData v = e.getValue();
            report = report+v.getSource()+"\t\t"+m+"\t\t"+v.getConvergenceTime()/v.getNrofNode()+"\t"+v.getLastNode()+"\t\t"+v.getNrofNode()+"\n";
        }
        write(report);
        super.done();
    }
}