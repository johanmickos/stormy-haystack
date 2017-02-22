# Stormy Haystack
A DHT implemented at KTH.

## Goals
The goal of the project is to implement and test and simple partitioned, distributed
in-memory key-value store with linearisable operation semantics. You have significant
freedoms in your choice of implementation, but you will need to motivate all your decisions
in the report. Some of the issues to consider are:
- Networking Protocol
- Bootstrapping
- Group Membership
- Failure Detector
- Routing Protocol
- Replication Algorithm

You will also have to write verification scenarios for your implementation. Distributed
algorithms are notoriously difficult to test and verify, so do not take this part of the
tasks lightly.
You are free to write your project in either Java or Scala Kompics.


## Preliminary Architecture Overview
![Suggested architecture](https://github.com/jarlopez/stormy-haystack/blob/master/prelim-report-arch.png)

## Tasks

### Introduction to Kompics (0 Points)
Implement all the PingPong examples from the Kompics tutorial at:
http://kompics.sics.se and/or complete Programming Exercise 1 in Canvas.
This task is optional and does not give any points. However, if you haven’t worked with
Kompics before you should most definitely do it. If you plan on doing the project in
Java prioritise the PingPong, if you plan on doing it in Scala, rather to the Programming
Exercise first.
It is recommended (but not required), that you continue to do the Programming Exercises
in Canvas as the course progresses, as they contain helpful information that you
can use for your own solutions.
If you have questions you can ask them during the tutorial exercise session.

### Infrastructure (10 Points)
For this task you have to design the basic infrastructural layers for the key-value store.
Your system should support a partitioned key-space of some kind (e.g. hash-partitioned
strings or range-partitioned integers). The partitions need to be distributed over the
available nodes such that all value are replicated with a specific replication degree δ.
You are free to keep δ as a configuration value or hardcode it, as long as it fulfils the
requirements of your chosen replication algorithm (task 2.2), so plan ahead.
For this task you need to be able to set up the system, assign nodes to partitions and
replication groups, lookup (preloaded) values1
from an external system (client), and
detect (but not necessarily handle) failures of nodes. Additionally, you should be able
to broadcast information within replication groups and possibly across.
On the verification side you have to write simulator scenarios that test all the features
mentioned above. You should also be able to run in a simple deployment (it’s fine to
run multiple JVMs on the same machine and consider them separate nodes).
For the report describe and motivate all your decisions and tests.

#### Note
Since not all of the subtasks of this section are of particular interest to this course,
we are providing a template project you can use as a starting point for your code. You
can find it at https://gits-15.sys.kth.se/lkroll/id2203project17. You are not
required to use all or even any of the code in there, it is merely provided as a convenience
to avoid that people waste too much time on unrelated coding work.

### KV-Store (20 Points)
After you have gotten the basic infrastructure working, you have to add a P UT(key, value)
operation, that updates (or adds) a value at a key, to the GET from the previous task.
As mentioned in the goals, the operations should fulfil the linearisable property, so make
sure choose you the right replication algorithm.
For more points, also implement a compare-and-swap (CAS(key, referenceV alue, newV alue) ∶
success?) operation that compares the current value at the key to a given reference value
and only updates with the new value if the old value and the reference value are the
same.
As before, be sure to write test scenarios for the simulator that check the correctness
of your implementation. Especially be very careful to explain how you are verifying the
linearisability of the store

### Reconfiguration (10 Points + 10 Bonus Points)
At this point your store is fairly static and can’t really deal with node failures apart from
complaining about them. For this task you should implement reconfiguration support
for your replication groups and your routing protocol. You should be able to deal with
both nodes leaving the system (treat a voluntary leave the same way as a failure, for
simplicity) and new nodes joining the system. There are many ways to interpret the
semantics of this, including making some of the partitions unavailable while they are
under-replicated. Any approach you take is acceptable as long as you document it
properly in the report. However, you have to make sure that reconfiguration does not
violate linearisability for correct nodes.
To get full points for this task you’ll have to write test scenarios that reconfigure the
system and verify that for all correct nodes the operations are still linearisable.
#### Note
This task is fairly open ended and can get quite difficult, depending on the choices
you have made before. For the 10 required points, you are expected to make a good
effort in writing some code and demonstrate in the report that you have understood
what is involved in a proper implementation of this. For actually getting it to work
correctly, you are awarded 10 bonus points.
