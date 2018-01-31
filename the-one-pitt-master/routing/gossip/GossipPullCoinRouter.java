package routing.gossip;
import core.*;
import java.util.*;
import routing.*;
/*
 * @author Wiryanto Setya Adi
 * @Sanata Dharma University
 */
public class GossipPullCoinRouter implements RoutingDecisionEngine{
    protected static final double DEFAULT_K = 2.0;
    protected Set<String> tombstone;
    protected Set<String> sumVectorList;
    protected static final String K_SETTING = "K";
    protected double k;
    public GossipPullCoinRouter(Settings s) {
        if (s.contains(K_SETTING)) {
            k = s.getDouble(K_SETTING);
        } else {
            k = DEFAULT_K;
        }
    }
    public GossipPullCoinRouter(GossipPullCoinRouter proto) {
        sumVectorList = new HashSet<>();
        tombstone = new HashSet<>();
        this.k = proto.k;
    }
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        Collection <Message> thisMessage = thisHost.getMessageCollection();
        Collection <Message> peerMessage = peer.getMessageCollection();
        sumVectorList.clear();
        if (thisMessage.hashCode() != peerMessage.hashCode()) {
            sumVectorCheck(peer, thisHost);
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
        tombstone.add(m.getId());
        return true;
    }
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo()==aHost;
    }
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
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
        return new GossipPullCoinRouter(this);
    }
    private void sumVectorCheck(DTNHost peer, DTNHost thisHost) {
        Collection <Message> thisHostMessage = thisHost.getMessageCollection();
        GossipPullCoinRouter de = getOtherGossipRouter(peer);
        List<Message> readyToDelete = new ArrayList<>();
        for (Message message : thisHostMessage) {
            if (de.tombstone.contains(message.getId())) {
                if (Math.random()<=1/k) {
                    readyToDelete.add(message);
                }
            } else {
                sumVectorList.add(message.getId());
            }
        }
        DecisionEngineRouter thisRouter = (DecisionEngineRouter) thisHost.getRouter();
        for (Message m : readyToDelete) {
            if (thisRouter.isSending(m.getId())) {
                List<Connection> conList = thisHost.getConnections();
                for (Connection con : conList) {
                    if (con.getMessage()!=null&&con.getMessage().getId()==m.getId()) {
                        con.abortTransfer();
                        break;
                    }
                }
            }
            thisHost.deleteMessage(m.getId(), false);
        }
        readyToDelete.clear();
    }
    private GossipPullCoinRouter getOtherGossipRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
        " with other routers of same type";
        return (GossipPullCoinRouter)((DecisionEngineRouter)otherRouter).getDecisionEngine();
    }
}