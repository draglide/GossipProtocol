package routing.gossip;
/*
 * @author Wiryanto Setya Adi
 * @Sanata Dharma University
 */
import core.*;
import java.util.*;
import routing.*;
public class OldGossipPushCounterRouterImprovedWithACK implements RoutingDecisionEngineFIX{
    /**Rumor spreading Initialization Constant */
    protected static final int RUMOR_INIT = 1;
    /**Set for Message Tombstone List */
    protected Set<String> tombstone;
    /**Set for Network Size */
    protected Set<DTNHost> DTNList;
    /**Set for ACK*/
    protected Set<String> ACK;
    /**Set for readyToDeleteMessage*/
    protected Set<Message> readyToDelete;
    protected static final String RUMOR_MONGERING_PROPERTY = "RUMOR_COPY";
    public OldGossipPushCounterRouterImprovedWithACK(Settings s) {
    }
    protected OldGossipPushCounterRouterImprovedWithACK(OldGossipPushCounterRouterImprovedWithACK proto) {
        tombstone = new HashSet<>();
        DTNList = new HashSet<>();
        ACK = new HashSet<>();
        readyToDelete = new HashSet<>();
    }
    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        readyToDelete.clear();
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        OldGossipPushCounterRouterImprovedWithACK partner = getOtherGossipRouter(peer);
        //delete ACKed Message
        if (ACK.hashCode()!=partner.ACK.hashCode()) {
            Collection<Message> peerMessageCollection = peer.getMessageCollection();
            for (Message message : peerMessageCollection) {
                if (ACK.contains(message.getId())) {
                    partner.readyToDelete.add(message);
                }
            }
            Collection<Message> thisMessageCollection = myHost.getMessageCollection();
            for (Message message : thisMessageCollection) {
                if (partner.ACK.contains(message.getId())) {
                    readyToDelete.add(message);
                }
            }
        }
        //summary Vector for ACK
        this.ACK.addAll(partner.ACK);
        partner.ACK.addAll(this.ACK);
        //summary Vector for Network Size
        this.DTNList.add(peer);
        partner.DTNList.add(myHost);
        this.DTNList.addAll(partner.DTNList);
        partner.DTNList.addAll(this.DTNList);
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(RUMOR_MONGERING_PROPERTY, RUMOR_INIT);
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
                    temp.updateProperty(RUMOR_MONGERING_PROPERTY, (int)temp.getProperty(RUMOR_MONGERING_PROPERTY)-(int)Math.pow(Math.log10(DTNList.size()),2));
                    if ((int)temp.getProperty(RUMOR_MONGERING_PROPERTY)==0) {
                        lastHop.deleteMessage(temp.getId(), false);
                    }
                    break;
                }
            }
            return false;
        }
        m.updateProperty(RUMOR_MONGERING_PROPERTY, (int)m.getProperty(RUMOR_MONGERING_PROPERTY)+1);
        tombstone.add(m.getId());
        return true;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        if (isFinalDest(m, otherHost)) {
            /*ACK should Send */
            ACK.add(m.getId());
            OldGossipPushCounterRouterImprovedWithACK partner = getOtherGossipRouter(otherHost);
            partner.ACK.add(m.getId());
            return true;
        }
        return (int)m.getProperty(RUMOR_MONGERING_PROPERTY)>0;
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
        return new OldGossipPushCounterRouterImprovedWithACK(this);
    }
    private OldGossipPushCounterRouterImprovedWithACK getOtherGossipRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works " + 
        " with other routers of same type";
        return (OldGossipPushCounterRouterImprovedWithACK)((DecisionEngineRouter)otherRouter).getDecisionEngine();
    }
    @Override
    public void update(DTNHost thisHost) {
    }
}