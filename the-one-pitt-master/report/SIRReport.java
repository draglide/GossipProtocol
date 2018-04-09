package report;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.UpdateListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static routing.DecisionEngineRouter.PUBSUB_NS;
import routing.gossip.*;
/**
 *
 * @author Adi
 */
public class SIRReport extends Report implements UpdateListener{
    public static final String REPORT_INTERVAL = "Interval";
    public static final int DEFAULT_REPORT_INTERVAL = 360;
    public static final String ENGINE_SETTING = "decisionEngine";
    private double lastRecord = Double.MIN_VALUE;
    private int interval;
    private Tombstone tb;
    private Map<String, Map<Integer, Integer>> susceptible = new HashMap<String, Map<Integer, Integer>>();
    private Map<String, Map<Integer, Integer>> infective = new HashMap<String, Map<Integer, Integer>>();
    private Map<String, Map<Integer, Integer>> removed = new HashMap<String, Map<Integer, Integer>>();
    private int updateCounter = 0;
    public SIRReport() {
        super();
        Settings settings = getSettings();
        if (settings.contains(REPORT_INTERVAL)) {
            interval = settings.getInt(REPORT_INTERVAL);
        } else {
            interval = -1;
        }
        if (interval < 0) {
            interval = DEFAULT_REPORT_INTERVAL;
        }
    }
    @Override
    public void updated(List<DTNHost> hosts) {
        double simTime = getSimTime();
        if (isWarmup()) {
            return;
        }
        if (simTime - lastRecord >= interval) {
            printLine(hosts);
            System.out.println(updateCounter);
            updateCounter++;
            this.lastRecord = simTime - simTime % interval;
        }
    }
    private void printLine(List<DTNHost> hosts) {
        int nrofHost = hosts.size();
        for (DTNHost host : hosts) {
            Settings routeSettings = new Settings(PUBSUB_NS);
            tb = (Tombstone)routeSettings.createIntializedObject(
                    "routing." + routeSettings.getSetting(ENGINE_SETTING));
            Set<String> tombstone = tb.getTombstone();
            for (String m : tombstone) {
                Map<Integer, Integer> temp;
                if (susceptible.containsKey(m)) {
                    temp = susceptible.get(m);
                    if (temp.containsKey(updateCounter)) {
                        temp.replace(updateCounter, (temp.get(updateCounter)-1));
                    } else {
                        temp.put(updateCounter, (nrofHost-1));
                    }
                } else {
                    temp = new HashMap<>();
                    temp.put(updateCounter, (nrofHost-1));
                    susceptible.put(m, temp);
                }
            }
            for (Message m : host.getMessageCollection()) {
                Map<Integer, Integer> temp;
                if (infective.containsKey(m.getId())) {
                    temp = infective.get(m.getId());
                    if (temp.containsKey(updateCounter)) {
                        temp.replace(updateCounter, (temp.get(updateCounter)+1));
                    } else {
                        temp.put(updateCounter, 1);
                    }
                    infective.replace(m.getId(), temp);
                } else {
                    temp = new HashMap<>();
                    temp.put(updateCounter, 1);
                    infective.put(m.getId(), temp);
                }
            }
        }
    }
    @Override
    public void done() {
        for (Map.Entry<String, Map<Integer, Integer>> entry : susceptible.entrySet()) {
            Map<Integer, Integer> value = entry.getValue();
            for (int i = 0; i < updateCounter; i++) {
                if (value.containsKey(i)) {
                    continue;
                }
                value.put(i, 500);
            }
            susceptible.replace(entry.getKey(), value);
            
        }
        for (Map.Entry<String, Map<Integer, Integer>> entry : infective.entrySet()) {
            Map<Integer, Integer> value = entry.getValue();
            for (int i = 0; i < updateCounter; i++) {
                if (value.containsKey(i)) {
                    continue;
                }
                value.put(i, 0);
            }
            infective.replace(entry.getKey(), value);
        }
        write("Susceptible");
        for (Map.Entry<String, Map<Integer, Integer>> entry : susceptible.entrySet()) {
            String printHost = entry.getKey()+"";
            Map<Integer, Integer> value = entry.getValue();
            for (Map.Entry<Integer, Integer> values : value.entrySet()) {
                printHost = printHost +"\t"+values.getValue();
            }
            write(printHost);
        }
        write("\nInfective");
        for (Map.Entry<String, Map<Integer, Integer>> entry : infective.entrySet()) {
            String printHost = entry.getKey()+"";
            Map<Integer, Integer> value = entry.getValue();
            for (Map.Entry<Integer, Integer> values : value.entrySet()) {
                printHost = printHost +"\t"+values.getValue();
            }
            write(printHost);
        }
	super.done();
    }
}