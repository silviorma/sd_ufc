package server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import interfaces.P;


public class Server implements P {
	
	/** mapping the pid of the remote process with the stub */
	private Map<String, P> peers;
	
	/** Process ID of the peer*/
	private long pid;
	
	/** The actual leader */
	private P leader;
	
	/** RMI registry */
	private Registry reg;
	
	/**
	 * Initialize the node assigning the pid value 
	 **/
	public Server() {
		String process_name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
		
		this.pid = Long.parseLong(process_name.split("@")[0]);
		
		System.out.println("Creating the process with PID " + this.pid);
		
		try {
			reg = LocateRegistry.getRegistry();
		} catch (RemoteException re) {
			try {
				reg = LocateRegistry.getRegistry(1099);
			} catch (RemoteException rex) {
				System.err.println("It's not possible get the RMI Registry.");
				rex.printStackTrace();
			}
		}
	}
	
	/**
	 * Mount the list of peers existing in the RMI Registry 
	 **/
	private void mountListPeers() {
		peers = new HashMap<String, P>();
		
		try {
			String listOfPeers = "";
			for(String proc : reg.list()) {
				P stub = (P) reg.lookup(proc);
				listOfPeers += proc + ",";
				peers.put(proc, stub);
			}
			
			System.out.println("List of the peers alive: " + listOfPeers.substring(0, listOfPeers.length() - 1));
		} catch (RemoteException ex) {
			ex.printStackTrace();
		} catch (NotBoundException nbex) {
			nbex.printStackTrace();
		}
	}

	/**
	 * Configure the new leader after the election process 
	 **/
	@Override
	public void setLeader(long pid) throws RemoteException {
		leader = peers.get("" + pid);
		System.out.println("New leader in the peer with PID " + getPID() + " => " + pid);
	}

	/**
	 * When a new peer comes in or when the leader falls, the new election process
	 * must be initiated. 
	 **/
	@Override
	public String startElection(long sourcePid) throws RemoteException {
		this.mountListPeers();
		
		// Check if exists some peer with greater pid than the own pid
		boolean greater = true;
		
		if(getPID() >= sourcePid) {
			for(String key : peers.keySet()) {
				// check if the peers is alive
				try {
					if(peers.get(key).getPID() > this.pid) {
						String response = peers.get(key).startElection(getPID());
						// found someone with the greater PID
						if(response != null && response.equals("OK")) {
							greater = false;
						}
					}
				} catch (RemoteException rmex) {
					// if the peer is not responding, the peer is removed of the RMI Registry
					try {
						reg.unbind(key);
					} catch (NotBoundException e) {
						e.printStackTrace();
					}
					System.out.println("The peer with pid " + key + " is not responding");
				}
			}
		} else
			return "OK";
		
		// if it is the greatest pid, then the other peers are notified
		if(greater) {
			setLeader(getPID());
			for(String key : peers.keySet()) {
				try {
					peers.get(key).setLeader(getPID());
				} catch (RemoteException rex) {
					System.out.println("It's not possible configure the leader in the process " + key );
				}
			}
			return "OK";
		}
			
		
		return null;
	}
	
	/**
	 * Ping the leader 
	 **/
	public boolean pingLeader() {
		if(leader == null)
			return false;
		
		try {
			System.out.println("Actual leader in the process with PID " + this.pid + ": " + leader.getPID());
			
			String listOfPeers = "";
			for(String proc : reg.list()) {
				P stub = (P) reg.lookup(proc);
				listOfPeers += proc + ",";
				peers.put(proc, stub);
			}
			
			System.out.println("List of the peers alive: " + listOfPeers.substring(0, listOfPeers.length() - 1));
			
			return true;
		} catch (Exception rmex) {
			System.err.println("The actual leader is not responding...");
			return false;
		}
			
	}

	@Override
	public long getPID() throws RemoteException {	
		return this.pid;
	}
	
	public Map<String, P> getPeers() {
		return peers;
	}

	public void setPeers(Map<String, P> peers) {
		this.peers = peers;
	}

	public P getLeader() {
		return leader;
	}

	public void setLeader(P leader) {
		this.leader = leader;
	}

	public static void main(String[] args) throws InterruptedException {
		
		Server server = new Server();
		
		int port = Integer.parseInt(args[0]);
		Registry reg = null;

		
		try {
			P stub = null;
			
			try {
				stub = (P) UnicastRemoteObject.exportObject(server, port);
				reg = LocateRegistry.createRegistry(1099);
			} catch (RemoteException re) {
				try {
					reg = LocateRegistry.getRegistry(1099);
				} catch (RemoteException rex) {
				}
			}
			reg.rebind(""+server.getPID(), stub);
		} catch(RemoteException rex) {
		} 
		
		Thread.sleep(5000);
		server.mountListPeers();
		
		System.out.println("Initializing ....");
		
		while(true) {
			Thread.sleep(5000);
			// check if the leader is alive
			if(!server.pingLeader()) {
				try {
					server.startElection(server.getPID());
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
			}
		}
		
	}
	
}
