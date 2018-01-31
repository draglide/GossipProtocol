package routing.gossip.broadcast;
import core.*;
import java.util.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
/**
 *
 * @author Adi
 */
public class GossipPullCounterBroadcastRouter implements RoutingDecisionEngine{
    /**Set for Message Tombstone List */
    protected Set<String> tombstone;
    /**Set for Summary Vector with peer */
    protected Set<String> sumVectorList;
    /**Map for Death Certificate */
    protected Map<DTNHost, Double> deathCertificate;
    protected static final String UNSENT_COUNT_PROPERTY = "rejectCount";
    protected static final int REJECT_COUNT = 1;
    protected int unsentCount;
    public GossipPullCounterBroadcastRouter(Settings s) {
        if (s.contains(UNSENT_COUNT_PROPERTY)) {
            unsentCount = s.getInt(UNSENT_COUNT_PROPERTY);
        } else {
            unsentCount = REJECT_COUNT;
        }
    }
    public GossipPullCounterBroadcastRouter(GossipPullCounterBroadcastRouter proto) {
        this.unsentCount = proto.unsentCount;
        sumVectorList = new HashSet<>();
        tombstone = new HashSet<>();
        deathCertificate = new HashMap<>();
    }
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }
    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        sumVectorList.clear();
    }
    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost thisHost = con.getOtherNode(peer);
        GossipPullCounterBroadcastRouter partner = getOtherGossipRouter(peer);
        /**Summary Vector DC */
        sumVectorDC(thisHost, peer);
        /**Delete Obsolete Update */
        checkDC(deathCertificate, thisHost);
        checkDC(partner.deathCertificate, peer);
        /**Summary Vector */
        sumVectorCheck(peer, thisHost);
    }
    @Override
    public boolean newMessage(Message m) {
        /**Initialization for new message */
        m.addProperty(UNSENT_COUNT_PROPERTY, unsentCount);
        tombstone.add(m.getId());
        if (deathCertificate.containsKey(m.getFrom()))
            deathCertificate.replace(m.getFrom(), m.getCreationTime());
        else
            deathCertificate.put(m.getFrom(), m.getCreationTime());
        return true;
    }
    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo()==aHost;
    }
    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        /**Initialization for new message */
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
    private void sumVectorDC(DTNHost thisHost, DTNHost peer){
        GossipPullCounterBroadcastRouter partner = getOtherGossipRouter(peer);
        for (Map.Entry<DTNHost, Double> entry : this.deathCertificate.entrySet()) {
            DTNHost key = entry.getKey();
            if (!partner.deathCertificate.containsKey(key)) {
                partner.deathCertificate.put(key, entry.getValue());
            } else {
                Double value = entry.getValue();
                if (partner.deathCertificate.get(key)<value) {
                    partner.deathCertificate.replace(key, value);
                }
            }
        }
        this.deathCertificate.clear();
        this.deathCertificate.putAll(partner.deathCertificate);
    }
    private void sumVectorCheck(DTNHost peer, DTNHost thisHost) {
        Collection<Message> thisHostMessage = thisHost.getMessageCollection();
        GossipPullCounterBroadcastRouter de = getOtherGossipRouter(peer);
        Set<String> readyToDelete = new HashSet<>();
        for (Message message : thisHostMessage) {
            /**Check if receiver node has already got the message */
            if (de.tombstone.contains(message.getId())) {
                /**Change message property on the sender node */
                message.updateProperty(UNSENT_COUNT_PROPERTY, (int)message.getProperty(UNSENT_COUNT_PROPERTY)-1);
                if ((int)message.getProperty(UNSENT_COUNT_PROPERTY)==0) {
                    readyToDelete.add(message.getId());
                }
            } else {
                sumVectorList.add(message.getId());
            }
        }
        DecisionEngineRouter thisRouter = (DecisionEngineRouter) thisHost.getRouter();
        for (String m : readyToDelete) {
            thisRouter.deleteMessage(m, false);
        }
        readyToDelete.clear();
        Collection<Message> peerHostMessage = peer.getMessageCollection();
        for (Message message : peerHostMessage) {
            /**Check if receiver node has already got the message */
            if (tombstone.contains(message.getId())) {
                /**Change message property on the sender node */
                message.updateProperty(UNSENT_COUNT_PROPERTY, (int)message.getProperty(UNSENT_COUNT_PROPERTY)-1);
                if ((int)message.getProperty(UNSENT_COUNT_PROPERTY)==0) {
                    readyToDelete.add(message.getId());
                }
            } else {
                de.sumVectorList.add(message.getId());
            }
        }
        DecisionEngineRouter peerRouter = (DecisionEngineRouter) peer.getRouter();
        for (String m : readyToDelete) {
            peerRouter.deleteMessage(m, false);
        }
        readyToDelete.clear();
    }
    @Override
    public RoutingDecisionEngine replicate() {
        return new GossipPullCounterBroadcastRouter(this);
    }
    private GossipPullCounterBroadcastRouter getOtherGossipRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
        " with other routers of same type";
        return (GossipPullCounterBroadcastRouter)((DecisionEngineRouter)otherRouter).getDecisionEngine();
    }
    public Set<String> getTombstone() {
        return tombstone;
    }
    private void checkDC(Map<DTNHost, Double> dc, DTNHost thisHost) {
        Collection<Message> cm = thisHost.getMessageCollection();
        Set<String> readyToDelete = new HashSet<>();
        for (Message m : cm) {
            if (dc.containsKey(m.getFrom())&&dc.get(m.getFrom())>m.getCreationTime()) {
                readyToDelete.add(m.getId());
            }
        }
        DecisionEngineRouter thisRouter = (DecisionEngineRouter) thisHost.getRouter();
        for (String m : readyToDelete) {
            thisRouter.deleteMessage(m, false);
        }
        readyToDelete.clear();
    }
}