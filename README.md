# Stormy Haystack
A strongly consistent distributed key-value store utilizing the [Kompcis framework](kompics.sics.se/current/)

## Architecture
The system relies on two types of nodes to function:
* a coordinator node for managing the *routing*, *partitioning*, and *reconfiguration* (currently unimplemented)
* one or more data nodes which *receive requests* from the coordinator and decide with their partition on the *sequence of events<* using a total order broadcast abstraction

![Architecture overview](../master/prelim-report-arch.png)

## Distributed Algorithms & Abstractions
Kompics enables systems to be built using modular Lego-like builing blocks. At the base of the layers of abstractions you'll find the Timer and Network components. 

### Timer
Core Kompics component capable of signalling to dependent components when intervals and durations pass. Useful for scheduling heartbeats, timeouts, and repeated tasks.

### Network
Core Kompics component that abstracts away the underlying network protocols and point-to-point communcation setup. Enables distributed components to send and receive serialized messages across the network.

### Eventually Perfect Failure Detector (EPFD)
Failure detector which operates correctly under the partially synchronous model by suspecting dead nodes and restoring susicions when detecting alive nodes.

The EPFD broadcasts and responds to heartbeat requests at scheduled timeouts, scaling up the timeout interval each time a node is incorrectly suspected of being dead.

### Eventual Leader Detector (ELD)
Leader electing algorithm which requires an EPFD implementation. Each time the EPFD signals a node liveness change, the ELD updates the set of suspected nodes and chooses the new leader to be the node with the highest pre-computed rank.

Because the node ranks are determined during the system bootstrap phase (and because the designs do not support dynamic reconfiguration), the newly elected leader is guaranteed to eventually be chosen across all participating nodes.

### Abortable Sequence Consensus (ASC)
Consensus algorithm for ensuring that all nodes within a replication group agree on the same operations in the same order, thus ensuring strong consistency.

### Total Order Broadcast
Communcation wrapper around ASC to ensure that agreed-upon messages get sent across the network and delivered to the application.

# Authors
- Johan Mickos ([jarlopez](https://github.com/jarlopez))
- Khaled Jendi ([jSchnitzer1](https://github.com/jSchnitzer1))
