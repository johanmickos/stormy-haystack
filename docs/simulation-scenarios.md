## Simulation Scenarios
The following are test scenarios for key-value store components:

####1- Eventually perfect failure detector simulator
The test of Eventually-Perfect-Failure-Detector component guarantees the following properties are satisfied:
1.	EPFD1: Strong completeness: Every crashed process is eventually detected by all correct processes
2.	EPFD2: Eventual strong accuracy: Eventually, no correct process is suspected by any correct process.

**Scenario:**
1.	The scenario will create five nodes to be tested through EPFD component (server → main → scala-2.11 → stormy → epfd → EPFD).
2.	Kill the first node which has NetAddress [192.168.0.1].
3.	Kill the second node which has NetAddress [192.168.0.2].
4.	Check the log that after killing each node, it is suspected by other correct nodes, so that EPFD1
5.	Verify a suspected node is set back to restore set after a heartbeat message is delivered. Furthermore, when EPFD created, all correct nodes can inaccurately suspect other correct nodes, and restore them after receiving heartbeats and timeout is increased.

####2- Eventual leader detector simulator
The test of Eventually Leader Detector component guarantees the following properties are satisfied:
1.  ELD1: eventual completeness: Eventually every correct node trusts some correct node.
2.	ELD2: eventual agreement: Eventually no two correct nodes trust different correct node.

**Scenario:**
1.	The scenario will create five nodes to be tested through EPFD component (server → main → scala-2.11 → stormy → eld → ELD).
2.	Kill the first node which has the highest rank.
3.	Kill the second node which has the second highest rank. 
4.	Recreate last killed node.
5.	Recreate the first killed node.
6.	Check the log that ELD1 and ELD2 properties are satisfied since all correct nodes consider the node with highest number and ever node with the highest rank will be considered as the leader.


