import os

import sys

os.system("mkdir bin")
os.system("mkdir bin\interfaces")
os.system("mkdir bin\server")


os.system("javac src\interfaces\P.java -d bin\\")
os.system("javac src\server\Peer.java -d bin\ -cp bin\\")

if sys.platform.find("win") != -1:

    os.system("start cmd call /k \"cd bin & java server.Peer 8888\"")
    os.system("start cmd call /k \"cd bin & java server.Peer 8889\"")
    os.system("start cmd call /k \"cd bin & java server.Peer 8890\"")
    os.system("start cmd call /k \"cd bin & java server.Peer 8891\"")
    os.system("start cmd call /k \"cd bin & java server.Peer 8892\"")
    
else:
    os.system("gnome-terminal -e 'bash -c \"rmiregistry; exec bash\"'")
    os.system("gnome-terminal -e 'bash -c \"cd bin; java server.Peer 8888; exec bash\"'")
    os.system("gnome-terminal -e 'bash -c \"cd bin; java server.Peer 8889; exec bash\"'")
    os.system("gnome-terminal -e 'bash -c \"cd bin; java server.Peer 8890; exec bash\"'")
    os.system("gnome-terminal -e 'bash -c \"cd bin; java server.Peer 8891; exec bash\"'")
    os.system("gnome-terminal -e 'bash -c \"cd bin; java server.Peer 8892; exec bash\"'")
