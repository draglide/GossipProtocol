package report;
import core.DTNHost;
/**
 *
 * @author Adi
 */
public class ConvergenceData {
    private DTNHost source;
    private double convergenceTime;
    private int nrofNode;
    private DTNHost lastNode;
    public ConvergenceData() {
        convergenceTime = 0.0;
        nrofNode = 0;
        lastNode = null;
        source = null;
    }
    public double getConvergenceTime() {
        return convergenceTime;
    }
    public void setConvergenceTime(double convergenceTime) {
        this.convergenceTime = convergenceTime;
    }
    public int getNrofNode() {
        return nrofNode;
    }
    public void setNrofNode(int nrofNode) {
        this.nrofNode = nrofNode;
    }
    public DTNHost getLastNode() {
        return lastNode;
    }
    public void setLastNode(DTNHost lastNode) {
        this.lastNode = lastNode;
    }
    public DTNHost getSource() {
        return source;
    }
    public void setSource(DTNHost source) {
        this.source = source;
    }
}