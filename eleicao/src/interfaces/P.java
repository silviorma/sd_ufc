package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

public interface P extends Remote {
	public void setLeader(long pid) throws RemoteException;
	public String startElection(long pidOrigem, Registry reg) throws RemoteException;
	public long getPID() throws RemoteException;
}
