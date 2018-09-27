# Stormy Haystack
A strongly consistent distributed key-value store utilizing the [Kompcis framework](kompics.sics.se/current/)

## Architecture
The system relies on two types of nodes to function:
* a coordinator node for managing the *routing*, *partitioning*, and *reconfiguration* (currently unimplemented)
* one or more data nodes which *receive requests* from the coordinator and decide with their partition on the *sequence of events<* using a total order broadcast abstraction

![Architecture overview](../master/prelim-report-arch.png)

# Authors
- Johan Mickos ([jarlopez](https://github.com/jarlopez))
- Khaled Jendi ([jSchnitzer1](https://github.com/jSchnitzer1))
