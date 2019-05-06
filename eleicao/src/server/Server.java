package server;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteObject;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import interfaces.P;

public class Server implements P {
	
	private Map<String, P> peers;
	private long pid;
	private P leader;
	public static int port = 8888;
	
	public Server() {
		String process_name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
		
		this.pid = Long.parseLong(process_name.split("@")[0]);
		
		System.out.println("Criando processo " + this.pid);
	}
	
	public void mountListPeers() {
		peers = new HashMap<String, P>();
		
		try {
			Registry reg = LocateRegistry.getRegistry();
			System.out.println(reg.list());
			for(String proc : reg.list()) {
				P stub = (P) reg.lookup(proc);
				peers.put(proc, stub);
			}
		} catch (RemoteException ex) {
			
		} catch (NotBoundException nbex) {
			
		}
	}

	@Override
	public void setLeader(long pid) throws RemoteException {
		leader = peers.get(""+pid);
		System.out.println("Novo líder em " + getPID() + " => " + pid);
	}

	@Override
	public String startElection() throws RemoteException {
		this.mountListPeers();
		boolean greater = true;
		
		for(String key : peers.keySet()) {
			// check if the peers is alive
			try {
				if(peers.get(key).getPID() > this.pid) {
					String response = peers.get(key).startElection();
					// found someone with the greater PID
					if(response != null && response.equals("OK")) {
						greater = false;
						break;
					}
				}
			} catch (RemoteException rmex) {
				System.out.println("Serviço " + key + " não disponível");
			}
		}
		
		if(greater) {
			setLeader(getPID());
			for(String key : peers.keySet()) {
				try {
					peers.get(key).setLeader(getPID());
					System.out.println("Configurando o novo líder no processo " + key);
				} catch (RemoteException rex) {
					System.out.println("Não foi possível configurar o líder para " + key );
				}
			}
			return "OK";
		}
		
		return null;
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
		
		try {
			Registry reg = null;
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
			System.out.println(reg);
			reg.rebind(""+server.getPID(), stub);
		} catch(RemoteException rex) {
		} 
		
		Thread.sleep(10000);
		server.mountListPeers();
		
		System.out.println("Iniciando a execução....");
		
		while(true) {
			Thread.sleep(5000);
			// check if the leader is alive
			try {
				System.out.println("Líder atual no processo " + server.getPID() + ": " + server.getLeader().getPID());
			} catch (Exception e) {
				try {
					server.startElection();
				} catch (RemoteException ex) {
					ex.printStackTrace();
				}
				/*for(String key : server.getPeers().keySet()) {
					// check if the peers is alive
					try {
						if(server.getPeers().get(key).getPID() > server.getPID()) {
							String response = server.getPeers().get(key).startElection();
							// found someone with the greater PID
							if(response != null && response.equals("OK"))
								break;
							
						}
					} catch (RemoteException rmex) {
						System.out.println("Serviço " + key + " não disponível");
					}
				}*/
			}
		}
		
	}
	
}
