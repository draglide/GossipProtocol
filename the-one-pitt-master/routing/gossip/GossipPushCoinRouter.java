package routing.gossip;
import core.*;
import java.util.*;
import routing.RoutingDecisionEngine;
/*
 * @author Wiryanto Setya Adi
 * @Sanata Dharma University
 */
public class GossipPushCoinRouter implements RoutingDecisionEngine{
    /**Deletion Probability Initialization Constant */
    protected static final double DEFAULT_K = 2.0;
    /**Set for Message Tombstone List */
    protected Set<String> tombstone;
    protected static final String K_SETTING = "K";
    protected double k;
    public GossipPushCoinRouter(Settings s) {
        if (s.contains(K_SETTING)) {
            k = s.getDouble(K_SETTING);
        } else {
            k = DEFAULT_K;
        }
    }
    protected GossipPushCoinRouter(GossipPushCoinRouter proto) {
        tombstone = new HashSet<>();
        this.k = proto.k;
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
        tombstone.add(m.getId());
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo()==aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        if (tombstone.contains(m.getId())) {
            List<DTNHost> listHop = m.getHops();
            Iterator it = listHop.iterator();
            DTNHost lastHop = null;
            while (it.hasNext()) {
                DTNHost temp = (DTNHost) it.next();
                if (thisHost!=temp) {
                    lastHop = temp;
                }
            }
            Collection<Message> messageCollection = lastHop.getMessageCollection();
            it = messageCollection.iterator();
            while (it.hasNext()) {
                Message temp = (Message) it.next();
                if (temp.getId()==m.getId()) {
                    if (Math.random()<=1/k) {
                        lastHop.deleteMessage(temp.getId(), false);
                    }
                    break;
                }
            }
            return false;
        }
        tombstone.add(m.getId());
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
        return new GossipPushCoinRouter(this);
    }
}