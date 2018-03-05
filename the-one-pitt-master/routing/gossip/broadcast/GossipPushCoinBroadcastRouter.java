package routing.gossip.broadcast;
/*
 * @author Wiryanto Setya Adi
 * @Sanata Dharma University
 */
import core.*;
import java.util.*;
import routing.*;
public class GossipPushCoinBroadcastRouter implements RoutingDecisionEngine{
    /**Set for Message Tombstone List */
    protected Set<String> tombstone;
    /**Map for Death Certificate */
    protected Map<DTNHost, Double> deathCertificate;
    /**Rejected Count from Setting */
    protected static final String K_SETTING = "K";
    protected static final double DEFAULT_K = 2.0;
    protected double k;
    public GossipPushCoinBroadcastRouter(Settings s) {
        if (s.contains(K_SETTING)) {
            k = s.getInt(K_SETTING);
        } else {
            k = DEFAULT_K;
        }
    }
    protected GossipPushCoinBroadcastRouter(GossipPushCoinBroadcastRouter proto) {
        this.k = proto.k;
        tombstone = new HashSet<>();
        deathCertificate = new HashMap<>();
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
        GossipPushCoinBroadcastRouter partner = getOtherGossipRouter(peer);
        /**Summary Vector DC */
        sumVectorDC(myHost, peer);
        /**Delete Obsolete Update */
        checkDC(deathCertificate, myHost);
        checkDC(partner.deathCertificate, peer);
    }

    @Override
    public boolean newMessage(Message m) {
        /**Initialization for new message */
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
                    if (Math.random()<=1/k) {
                        lastHop.deleteMessage(temp.getId(), false);
                    }
                    break;
                }
            }
            return false;
        }
        /**Initialization for relayed message */
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
        return new GossipPushCoinBroadcastRouter(this);
    }
    private GossipPushCoinBroadcastRouter getOtherGossipRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
        " with other routers of same type";
        return (GossipPushCoinBroadcastRouter)((DecisionEngineRouter)otherRouter).getDecisionEngine();
    }
    private void sumVectorDC(DTNHost thisHost, DTNHost peer){
        GossipPushCoinBroadcastRouter partner = getOtherGossipRouter(peer);
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