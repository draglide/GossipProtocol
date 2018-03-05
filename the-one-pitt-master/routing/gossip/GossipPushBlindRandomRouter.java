package routing.gossip;
import core.*;
import java.util.*;
import routing.DecisionEngineRouterFIX;
import routing.RoutingDecisionEngineFIX;
/*
 * @author Wiryanto Setya Adi
 * @Sanata Dharma University
 */
public class GossipPushBlindRandomRouter implements RoutingDecisionEngineFIX{
    protected Set<String> tombstone;
    public GossipPushBlindRandomRouter(Settings s) {
    }
    protected GossipPushBlindRandomRouter(GossipPushBlindRandomRouter proto) {
        tombstone = new HashSet<>();
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
    public RoutingDecisionEngineFIX replicate() {
        return new GossipPushBlindRandomRouter(this);
    }
    @Override
    public void update(DTNHost thisHost) {
        Collection<Message> msgs = thisHost.getMessageCollection();
        List<String> readyToDelete = new ArrayList<>();
        for (Message m : msgs) {
            if (Math.random()<=1/2.0) {
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