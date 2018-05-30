package report;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/**
 *
 * @author Adi
 */
public class ConvergenceReport extends Report implements MessageListener{
    private Map<String, ConvergenceData> convergenceTime;
    private int nrofRelayed;
    private int nrofStarted;
    private int nrofSavedInBuffer;
    public ConvergenceReport() {
        init();
    }
    @Override
    protected void init() {
        super.init();
        nrofRelayed = 0;
        nrofStarted = 0;
        nrofSavedInBuffer = 0;
        convergenceTime = new HashMap<String, ConvergenceData>();
    }
    @Override
    public void newMessage(Message m) {
    }
    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
        nrofStarted++;
    }
    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    }
    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }
    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        nrofRelayed++;
        if (convergenceTime.containsKey(m.getId())) {
            ConvergenceData d = convergenceTime.get(m.getId());
            Set<DTNHost> nodeList = d.getNodeList();
            if (!nodeList.contains(to)) {
                nodeList.add(to);
                d.setNodeList(nodeList);
                d.setConvergenceTime(d.getConvergenceTime()+(getSimTime()-m.getCreationTime()));
                d.setLastNodeTime(getSimTime()-m.getCreationTime());
                convergenceTime.replace(m.getId(), d);
            }
        } else {
            ConvergenceData d = new ConvergenceData();
            d.setSource(from);
            d.setConvergenceTime(getSimTime()-m.getCreationTime());
            d.setLastNodeTime(getSimTime()-m.getCreationTime());
            Set<DTNHost> nodeList = new HashSet<>();
            nodeList.add(to);
            d.setNodeList(nodeList);
            convergenceTime.put(m.getId(), d);
        }
    }
    @Override
    public void done() {
        Settings s = new Settings();
        int nrofNode = s.getInt("Group.nrofHosts")-1;
        String report = "";
        /*write("Convergence Report\nSource Node\tMessage ID\tConvergenceTime\t\tLast Node Time\tNumber Of Node");
        for (Map.Entry<String, ConvergenceData> e : convergenceTime.entrySet()) {
            String m = e.getKey();
            ConvergenceData v = e.getValue();
            report = report+v.getSource()+"\t\t"+m+"\t\t"+v.getConvergenceTime()/v.getNodeList().size()+"\t"+v.getLastNodeTime()+"\t\t"+v.getNodeList().size()+"\n";
        }*/
        double avgConvTime = 0;
        double lastUpdateTime = 0;
        double nrofInfected = 0;
        for (Map.Entry<String, ConvergenceData> e : convergenceTime.entrySet()) {
            ConvergenceData v = e.getValue();
            avgConvTime += v.getConvergenceTime()/v.getNodeList().size();
            lastUpdateTime += v.getLastNodeTime();
            nrofInfected += v.getNodeList().size();
        }
        report += "\nMessage Started           = "+nrofStarted+"\n"
               + "Message Relayed           = "+nrofRelayed+"\n"
               + "Average Convergence Time  = "+avgConvTime/convergenceTime.size()+"\n"
               + "Average Last Update Time  = "+lastUpdateTime/convergenceTime.size()+"\n"
               + "Average Residue           = "+(nrofNode-nrofInfected/convergenceTime.size())/nrofNode+"\n"
               + "Transmission Load         = "+nrofRelayed/(nrofNode+1)+"\n"
               + "Resource Load             = "+nrofSavedInBuffer/(nrofNode+1);
        write(report);
        super.done();
    }

    @Override
    public void messageSavedToBuffer(Message m, DTNHost to) {
        nrofSavedInBuffer++;
    }

    @Override
    public void connectionUp(DTNHost thisHost) {}
}