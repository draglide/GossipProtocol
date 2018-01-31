package routing.gossip.broadcast;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
/**
 *
 * @author Adi
 */
public class EpidemicBroadcast implements RoutingDecisionEngine{
    /**Map for Death Certificate */
    protected Map<DTNHost, Double> deathCertificate;
    public EpidemicBroadcast(Settings s) {
    }
    protected EpidemicBroadcast(EpidemicBroadcast proto) {
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
        DTNHost thisHost = con.getOtherNode(peer);
        EpidemicBroadcast partner = getOtherGossipRouter(peer);
        /**Summary Vector DC */
        sumVectorDC(thisHost, peer);
        /**Delete Obsolete Update */
        checkDC(deathCertificate, thisHost);
        checkDC(partner.deathCertificate, peer);
    }
    @Override
    public boolean newMessage(Message m) {
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
        return new EpidemicBroadcast(this);
    }
    private void sumVectorDC(DTNHost thisHost, DTNHost peer){
        EpidemicBroadcast partner = getOtherGossipRouter(peer);
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
    private EpidemicBroadcast getOtherGossipRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
        " with other routers of same type";
        return (EpidemicBroadcast)((DecisionEngineRouter)otherRouter).getDecisionEngine();
    }
}