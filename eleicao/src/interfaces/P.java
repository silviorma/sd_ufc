package interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface P extends Remote {
	public void setLeader(long pid) throws RemoteException;
	public String startElection(long pidOrigem) throws RemoteException;
	public long getPID() throws RemoteException;
}
