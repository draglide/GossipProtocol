package routing.community;
public interface CentralityDetection {
    public double getCentrality(double[][] matrixEgoNetwork);
    public CentralityDetection replicate();
}