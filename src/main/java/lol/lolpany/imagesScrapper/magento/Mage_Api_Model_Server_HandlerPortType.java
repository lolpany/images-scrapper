/**
 * Mage_Api_Model_Server_HandlerPortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package lol.lolpany.imagesScrapper.magento;

public interface Mage_Api_Model_Server_HandlerPortType extends java.rmi.Remote {

    /**
     * Call api functionality
     */
    public Object call(String sessionId, String resourcePath, Object args) throws java.rmi.RemoteException;

    /**
     * Multiple calls of resource functionality
     */
    public Object[] multiCall(String sessionId, Object[] calls, Object options) throws java.rmi.RemoteException;

    /**
     * End web service session
     */
    public boolean endSession(String sessionId) throws java.rmi.RemoteException;

    /**
     * Login user and retrive session id
     */
    public String login(String username, String apiKey) throws java.rmi.RemoteException;

    /**
     * Start web service session
     */
    public String startSession() throws java.rmi.RemoteException;

    /**
     * List of available resources
     */
    public Object[] resources(String sessionId) throws java.rmi.RemoteException;

    /**
     * List of resource faults
     */
    public Object[] globalFaults(String sessionId) throws java.rmi.RemoteException;

    /**
     * List of global faults
     */
    public Object[] resourceFaults(String resourceName, String sessionId) throws java.rmi.RemoteException;
}
