package routing.gossip;
import core.*;
import java.util.*;
import routing.*;
/*
 * @author Wiryanto Setya Adi
 * @Sanata Dharma University
 */
public class GossipPullBlindRandomRouter implements RoutingDecisionEngineFIX{
    protected Set<String> tombstone;
    protected Set<String> sumVectorList;
    public GossipPullBlindRandomRouter(Settings s) {
    }
    public GossipPullBlindRandomRouter(GossipPullBlindRandomRouter proto) {
        sumVectorList = new HashSet<>();
        tombstone = new HashSet<>();
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
    public RoutingDecisionEngineFIX replicate() {
        return new GossipPullBlindRandomRouter(this);
    }
    private void sumVectorCheck(DTNHost peer, DTNHost thisHost) {
        Collection <Message> thisHostMessage = thisHost.getMessageCollection();
        GossipPullBlindRandomRouter de = getOtherGossipRouter(peer);
        for (Message message : thisHostMessage) {
            if (!de.tombstone.contains(message.getId())) {
                sumVectorList.add(message.getId());
            }
        }
    }
    private GossipPullBlindRandomRouter getOtherGossipRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouterFIX : "This router only works " + 
        " with other routers of same type";
        return (GossipPullBlindRandomRouter)((DecisionEngineRouterFIX)otherRouter).getDecisionEngine();
    }

    @Override
    public void update(DTNHost thisHost) {
        Collection<Message> msgs = thisHost.getMessageCollection();
        List<String> readyToDelete = new ArrayList<>();
        for (Iterator<Message> it = msgs.iterator(); it.hasNext();) {
            Message m = it.next();
            if (Math.random()<=1/2.0) {
                readyToDelete.add(m.getId());
            }
        }
        DecisionEngineRouterFIX thisRouter = (DecisionEngineRouterFIX) thisHost.getRouter();
        for (String m : readyToDelete) {
            if (thisRouter.isSending(m)) {
                List<Connection> conList = thisHost.getConnections();
                for (Connection con : conList) {
                    if (con.getMessage()!=null&&con.getMessage().getId()==m) {
                        con.abortTransfer();
                        break;
                    }
                }
            }
            thisHost.deleteMessage(m, false);
        }
        readyToDelete.clear();
    }
}