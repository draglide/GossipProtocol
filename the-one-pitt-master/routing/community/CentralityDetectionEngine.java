/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.community;
/*
 * Declares a RoutingDecisionEngine object to also perform centrality
 * detection in some fashion. This is needed for Centrality Detection Reports
 * to print out the global and local centrality of each node 
 * 
 */
public interface CentralityDetectionEngine {
    //returns the global centrality of a node
    public double getGlobalDegreeCentrality();
    //returns the local centrality of a node
    public double getLocalDegreeCentrality();
    //temporary inserted
    //return the array of the global centrality of a node
    //public int [] getArrayCentrality ();
}