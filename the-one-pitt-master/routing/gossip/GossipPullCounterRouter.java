package routing.gossip;
import core.*;
import java.util.*;
import routing.*;
/*
 * @author Wiryanto Setya Adi
 * @Sanata Dharma University
 */
public class GossipPullCounterRouter implements RoutingDecisionEngine{
    /**Set for Message Tombstone List */
    protected Set<String> tombstone;
    /**Set for Summary Vector with peer */
    protected Set<String> sumVectorList;
    /**Set for Network Size */
    protected Set<DTNHost> DTNList;
    /**Unsent Count from Setting */
    protected static final String UNSENT_COUNT_PROPERTY = "rejectCount";
    protected int unsentCount;
    /**Adaptive by Default */
    protected boolean adaptive = true;
    public GossipPullCounterRouter(Settings s) {
        if (s.contains(UNSENT_COUNT_PROPERTY)) {
            unsentCount = s.getInt(UNSENT_COUNT_PROPERTY);
            adaptive = false;
        }
    }
    public GossipPullCounterRouter(GossipPullCounterRouter proto) {
        this.unsentCount = proto.unsentCount;
        this.adaptive = proto.adaptive;
        sumVectorList = new HashSet<>();
        tombstone = new HashSet<>();
        DTNList = new HashSet<>();
    }
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        GossipPullCounterRouter partner = getOtherGossipRouter(peer);
        /**Summary Vector for Network Size */
        this.DTNList.add(peer);
        partner.DTNList.add(thisHost);
        this.DTNList.addAll(partner.DTNList);
        partner.DTNList.addAll(this.DTNList);
        /**Message Summary Vector */
        Collection <Message> thisMessage = thisHost.getMessageCollection();
        Collection <Message> peerMessage = peer.getMessageCollection();
        sumVectorList.clear();
        sumVectorCheck(peer, thisHost);
    }
    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }
    @Override
    public boolean newMessage(Message m) {
        /**Initialization for new message */
        if (adaptive) {
            unsentCount = (int) Math.log10(DTNList.size());
        }
        m.addProperty(UNSENT_COUNT_PROPERTY, unsentCount);
        tombstone.add(m.getId());
        return true;
    }
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo()==aHost;
    }
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        /**Initialization for new message */
        if (adaptive) {
            unsentCount = (int) Math.log10(DTNList.size());
        }
        m.updateProperty(UNSENT_COUNT_PROPERTY, unsentCount);
        tombstone.add(m.getId());
        return true;
    }
    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        return sumVectorList.contains(m.getId());
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
        return new GossipPullCounterRouter(this);
    }
    private void sumVectorCheck(DTNHost peer, DTNHost thisHost) {
        Collection <Message> thisHostMessage = thisHost.getMessageCollection();
        GossipPullCounterRouter de = getOtherGossipRouter(peer);
        List<Message> readyToDelete = new ArrayList<>();
        for (Message message : thisHostMessage) {
            /**Check if receiver node has already got the message */
            if (de.tombstone.contains(message.getId())) {
                /**Change message property on the sender node */
                message.updateProperty(UNSENT_COUNT_PROPERTY, (int)message.getProperty(UNSENT_COUNT_PROPERTY)-1);
                if ((int)message.getProperty(UNSENT_COUNT_PROPERTY)==0) {
                    readyToDelete.add(message);
                }
            } else {
                sumVectorList.add(message.getId());
            }
        }
        DecisionEngineRouter thisRouter = (DecisionEngineRouter) thisHost.getRouter();
        for (Message m : readyToDelete) {
            System.out.println("This Node = "+thisHost+", Peer = "+peer);
            System.out.println("Message going to Delete = "+m);
            System.out.println("Outgoing Message = "+thisRouter.getOutgoingMessages());
            if (thisRouter.isSending(m.getId())) {
                List<Connection> conList = thisHost.getConnections();
                System.out.println("Con List before Check = "+conList);
                for (Connection con : conList) {
                    if (con.getMessage()!=null&&con.getMessage().getId()==m.getId()) {
                        con.abortTransfer();
                        break;
                    }
                }
                System.out.println("Con List after Check = "+conList);
            }
            thisRouter.deleteMessage(m.getId(), false);
        }
        readyToDelete.clear();
    }
    private GossipPullCounterRouter getOtherGossipRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
        " with other routers of same type";
        return (GossipPullCounterRouter)((DecisionEngineRouter)otherRouter).getDecisionEngine();
    }
    public Set<String> getTombstone() {
        return tombstone;
    }
}