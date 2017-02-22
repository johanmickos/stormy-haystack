# Lab Notes
- To solve: failing nodes during bootstrap
- DON'T: "Don't do Chord" -- too impractcal and difficult with consistent store
- DO: Really simple overlay, don't spend too much time on it
What are alternative overlays (to Chord...)
    - Chord too complex!
    - Template has starter for simple one
        - i..e generate lookup table in beginning
        - update it when failures occur
        - not enough when doing reconfigs
        - possible to extend:
            - who's responsible for changing? (the one who notoices failures)
            - prevent inconcistent or conflicting copies in system?
                - in lookup table
            - problem of WHILE distributing LUT, we'll be in inconsistent state
                - can't drop completely
                - structure routing algo such that it deals with temporary state
                - stillllll much simpler wrt linearizability (Chord + linearizability == hard)
# Partner Notes
-Partitionining, replication
    - Look back at data backups from previous course
    - Group membership + leader election allows for replacing dead/old/broken leader with other
- Need to marry EPFD with broadcasting approach
- Figure out broadcasting wtihin groups and within system
- How does one create simulators?
- Broadcaster, failure detector seem to be quite obvious for infrastructure 1


# TODOS
## Infrastructure
- Analyze Lars's test/simulator
- Look through template, figure out component dependencies and data flow
    - Includes running Bootstrap Node, 2x Server Node, 1x Client and playing  with the running system
- Figure out where to put in FD, Broadcasting for partitions, etc.
- Figure out how to extends VSOM --> supporting multiple partitions

## KV Store (20pts)
- Support PUT requests!
    - Challenging with multiple partitions
    - Also support linearisability
- Need to determine replication algo. to satisfy above
- KISS: Keep It Simple, Stupid
- MORE Points: Compare-And-Swap

