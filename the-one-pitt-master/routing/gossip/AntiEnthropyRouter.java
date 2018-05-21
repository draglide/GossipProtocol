package routing.gossip;
import core.*;
import java.util.*;
import routing.RoutingDecisionEngineFIX;
/*
 * @author Adi
 * @Sanata Dharma University
 */
public class AntiEnthropyRouter implements RoutingDecisionEngineFIX{
    private Set<String> sumVectorList;
    public AntiEnthropyRouter(Settings s) {
    }
    protected AntiEnthropyRouter(AntiEnthropyRouter proto) {
        this.sumVectorList = new HashSet<>();
    }
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        Collection <Message> peerMessage = peer.getMessageCollection();
        sumVectorList.clear();
        for (Message message : peerMessage) {
            sumVectorList.add(message.getId());
        }
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
        return !sumVectorList.contains(m.getId());
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
    public RoutingDecisionEngineFIX replicate() {
        return new AntiEnthropyRouter(this);
    }

    @Override
    public void update(DTNHost thisHost) {
       
    }
}