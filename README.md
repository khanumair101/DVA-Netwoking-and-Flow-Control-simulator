# DVA-Netwoking-and-Flow-Control-simulator

### Network Traffic Simulation: A Technical Overview 🖥️

This project is a **network traffic simulator** that models how data packets traverse a fixed network topology. It's built around two core components: a **Distance Vector Computation** module and a **Flow Routing and Forwarding** algorithm. The user can interact with the simulation by modifying certain parameters, which in turn influences how packets are routed and how network performance is measured.

***

### Distance Vector Algorithm and Topology Configuration

At its foundation, the simulation uses the **Distance Vector (DV) algorithm** to determine the most efficient paths between network nodes (routers). The network's physical layout, or **topology**, is pre-defined and static. However, the user has the flexibility to adjust the **path costs** (or "weights") between each router. These costs are a crucial input for the DV algorithm.

* **Distance Vector Computation:** This module is responsible for calculating the shortest paths from every router to every other router in the network. Once the user has specified the path costs, the program runs the DV algorithm to generate a **routing table** for each router. This table contains the shortest distance to all other destinations and the next-hop router to reach them. The resulting routing tables are then saved to an external text file for later use.

***

### Traffic Simulation and Packet Flow

The second major component is the **Flow Routing and Forwarding** module, which simulates the actual movement of data packets. This part of the program takes the previously generated routing tables as input to build an internal representation of the network topology.

* **User-Defined Flows:** The user can specify any number of **data flows**. A flow is defined by its source router, its destination router, and the size of the packets to be transmitted.
* **Packet-Level Simulation:** The program then simulates the journey of each individual packet, step by step. It provides a detailed log of the packet's path, including every router it visits, the time spent in **queues**, and the **delay** incurred at each step. This level of granularity allows for a deep analysis of network performance under different traffic loads and path costs.

***

### Core Technical Skills and Concepts

The successful implementation of this simulator requires proficiency in several key programming and networking concepts:

* **Socket Programming:** This is used to mimic the communication between routers, allowing them to exchange distance vector information and forward packets as they would in a real network.
* **Multi-Threading:** This is essential for simulating concurrent processes, such as multiple routers handling packets simultaneously or different data flows occurring at the same time.
* **Decentralized Programming:** The DV algorithm is a classic example of a **decentralized algorithm**, where each router only has knowledge of its direct neighbors. The simulation reflects this by having each router make independent routing decisions based on its local information.
* **Object-Oriented Design (OOD):** The project is structured using OOD principles. Routers, packets, and network links are likely modeled as **objects**, each with their own properties and methods, which makes the code modular and easier to manage.
* **I/O Management:** The program needs to handle **file I/O** to read user-defined parameters and to write the computed routing tables to a file.
* **Algorithm Implementation:** The project's core functionality is dependent on the correct and efficient implementation of the **Distance Vector, Routing, and Forwarding** algorithms, along with a mechanism for **Flow Control**.
