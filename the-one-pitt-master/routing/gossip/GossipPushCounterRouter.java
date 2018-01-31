package routing.gossip;
/*
 * @author Wiryanto Setya Adi
 * @Sanata Dharma University
 */
import core.*;
import java.util.*;
import routing.*;
public class GossipPushCounterRouter implements RoutingDecisionEngine{
    /**Set for Message Tombstone List */
    protected Set<String> tombstone;
    /**Set for Network Size */
    protected Set<DTNHost> DTNList;
    /**Rejected Count from Setting */
    protected static final String REJECTED_COUNT_PROPERTY = "rejectCount";
    protected int rejectCount;
    /**Adaptive by Default */
    protected boolean adaptive = true;
    public GossipPushCounterRouter(Settings s) {
        if (s.contains(REJECTED_COUNT_PROPERTY)) {
            rejectCount = s.getInt(REJECTED_COUNT_PROPERTY);
            adaptive = false;
        }
    }
    protected GossipPushCounterRouter(GossipPushCounterRouter proto) {
        this.adaptive = proto.adaptive;
        this.rejectCount = proto.rejectCount;
        tombstone = new HashSet<>();
        DTNList = new HashSet<>();
    }
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        GossipPushCounterRouter partner = getOtherGossipRouter(peer);
        /**Summary Vector for Network Size */
        this.DTNList.add(peer);
        partner.DTNList.add(myHost);
        this.DTNList.addAll(partner.DTNList);
        partner.DTNList.addAll(this.DTNList);
    }

    @Override
    public boolean newMessage(Message m) {
        /**Initialization for new message */
        if (adaptive) {
            rejectCount = (int) Math.log10(DTNList.size());
        }
        m.addProperty(REJECTED_COUNT_PROPERTY, rejectCount);
        tombstone.add(m.getId());
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo()==aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        /**Check if node has already got the message */
        if (tombstone.contains(m.getId())) {
            List<DTNHost> listHop = m.getHops();
            Iterator it = listHop.iterator();
            /**Get the sender node */
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
                    /**Change message property on the sender node */
                    temp.updateProperty(REJECTED_COUNT_PROPERTY, (int)temp.getProperty(REJECTED_COUNT_PROPERTY)-1);
                    if ((int)temp.getProperty(REJECTED_COUNT_PROPERTY)==0) {
                        lastHop.deleteMessage(temp.getId(), false);
                    }
                    break;
                }
            }
            return false;
        }
        /**Initialization for relayed message */
        if (adaptive) {
            rejectCount = (int) Math.log10(DTNList.size());
        }
        m.updateProperty(REJECTED_COUNT_PROPERTY, rejectCount);
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
        return new GossipPushCounterRouter(this);
    }
    private GossipPushCounterRouter getOtherGossipRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
        " with other routers of same type";
        return (GossipPushCounterRouter)((DecisionEngineRouter)otherRouter).getDecisionEngine();
    }
}