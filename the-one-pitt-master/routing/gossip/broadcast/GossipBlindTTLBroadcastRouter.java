package routing.gossip.broadcast;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import routing.DecisionEngineRouterFIX;
import routing.MessageRouter;
import routing.RoutingDecisionEngineFIX;
/*
 * @author Wiryanto Setya Adi
 * @Sanata Dharma University
 */
public class GossipBlindTTLBroadcastRouter implements RoutingDecisionEngineFIX{
    /**Set for Message Tombstone List */
    protected Set<String> tombstone;
    /**Set for Summary Vector with peer */
    protected Set<String> sumVectorList;
    /**Map for Death Certificate */
    protected Map<DTNHost, Double> deathCertificate;
    protected static final String TRESHOLD_SETTING = "t";
    protected static final double DEFAULT_TRESHOLD = 3.0;
    protected double treshold;
    public GossipBlindTTLBroadcastRouter(Settings s) {
        if (s.contains(TRESHOLD_SETTING)) {
            treshold = s.getDouble(TRESHOLD_SETTING);
        } else {
            treshold = DEFAULT_TRESHOLD;
        }
    }
    public GossipBlindTTLBroadcastRouter(GossipBlindTTLBroadcastRouter proto) {
        this.treshold = proto.treshold;
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
        GossipBlindTTLBroadcastRouter partner = getOtherGossipRouter(peer);
        /**Summary Vector DC */
        sumVectorDC(thisHost, peer);
        /**Delete Obsolete Update */
        checkDC(deathCertificate, thisHost);
        checkDC(partner.deathCertificate, peer);
        /**Summary Vector */
        SumVectorCheck(thisHost, peer);
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
        return new GossipBlindTTLBroadcastRouter(this);
    }
    @Override
    public void update(DTNHost thisHost) {
        Collection<Message> msgs = thisHost.getMessageCollection();
        if (!msgs.isEmpty()) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (Message m : msgs) {
                if (m.getTtl()<min) {
                    min = m.getTtl();
                }
                if (m.getTtl()>max) {
                    max = m.getTtl();
                }
            }
            double treshold = (max - min)/this.treshold + min;
            List<String> readyToDelete = new ArrayList<>();
            for (Message m : msgs) {
                if (m.getTtl()<treshold) {
                    readyToDelete.add(m.getId());
                }
            }
            DecisionEngineRouterFIX thisRouter = (DecisionEngineRouterFIX) thisHost.getRouter();
            for (String m : readyToDelete) {
                thisRouter.deleteMessage(m, false);
            }
            readyToDelete.clear();
        }
    }
    private void sumVectorDC(DTNHost thisHost, DTNHost peer){
        GossipBlindTTLBroadcastRouter partner = getOtherGossipRouter(peer);
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
    private GossipBlindTTLBroadcastRouter getOtherGossipRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouterFIX : "This router only works " + 
        " with other routers of same type";
        return (GossipBlindTTLBroadcastRouter)((DecisionEngineRouterFIX)otherRouter).getDecisionEngine();
    }
    private void checkDC(Map<DTNHost, Double> dc, DTNHost thisHost) {
        Collection<Message> cm = thisHost.getMessageCollection();
        Set<String> readyToDelete = new HashSet<>();
        for (Message m : cm) {
            if (dc.containsKey(m.getFrom())&&dc.get(m.getFrom())>m.getCreationTime()) {
                readyToDelete.add(m.getId());
            }
        }
        DecisionEngineRouterFIX thisRouter = (DecisionEngineRouterFIX) thisHost.getRouter();
        for (String m : readyToDelete) {
            thisRouter.deleteMessage(m, false);
        }
        readyToDelete.clear();
    }
    private void SumVectorCheck(DTNHost thisHost, DTNHost peer) {
        Collection<Message> thisHostMessage = thisHost.getMessageCollection();
        GossipBlindTTLBroadcastRouter peerRouter = getOtherGossipRouter(peer);
        for (Message m : thisHostMessage) {
            if (!peerRouter.tombstone.contains(m.getId())) {
                sumVectorList.add(m.getId());
            }
        }
        Collection<Message> peerMessage = peer.getMessageCollection();
        for (Message m : peerMessage) {
            if (!this.tombstone.contains(m.getId())) {
                peerRouter.sumVectorList.add(m.getId());
            }
        }
    }
}