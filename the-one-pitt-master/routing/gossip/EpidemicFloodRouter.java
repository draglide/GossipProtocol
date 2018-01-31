package routing.gossip;
import core.*;
import routing.RoutingDecisionEngine;
/**
 * @author Adi
 * @Sanata Dharma University
 */
public class EpidemicFloodRouter implements RoutingDecisionEngine{
    
    public EpidemicFloodRouter(Settings s) {
    }
    protected EpidemicFloodRouter(EpidemicFloodRouter proto) {
    }
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }
    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }
    @Override
    public boolean newMessage(Message m) {
        return true;
    }
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo()==aHost;
    }
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return true;
    }
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        return true;
    }
    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }
    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }
    @Override
    public RoutingDecisionEngine replicate() {
        return new EpidemicFloodRouter(this);
    }
}