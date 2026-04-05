# Distributed Cache System (LLD Implementation)

This repository contains a Java low-level design implementation of a distributed cache with:

- client-facing cache API
- consistent-hash based routing
- per-node storage and replication
- pluggable eviction strategies (LRU, LFU, TTL)
- pluggable persistence policies (write-through, write-back)
- heartbeat-based health monitoring and failover support

## UML Class Diagram

```mermaid
classDiagram
    direction LR

    class Cache {
      -LoadBalancer loadBalancer
      +Response get(Key key)
      +Response put(Key key, Value val)
    }

    class Response {
      -Value value
      -boolean cacheHit
      -String nodeId
      -long latencyMs
      +Value getValue()
      +boolean isCacheHit()
      +String getNodeId()
      +long getLatencyMs()
    }

    class Key {
      <<interface>>
      +int getHashCode()
    }

    class Value {
      -Object data
      +Object getData()
      +String toString()
    }

    class LoadBalancer {
      -ConsistentHashRing ring
      -List~CacheNode~ nodes
      -Set~String~ activeNodeIds
      +void addNode(CacheNode node)
      +void removeNode(CacheNode node)
      +CacheNode route(Key key)
      +void onNodeDown(CacheNode node)
      +void onNodeUp(CacheNode node)
    }

    class ConsistentHashRing {
      -SortedMap~Integer, CacheNode~ ring
      -int replicas
      +void addNode(CacheNode node)
      +void removeNode(CacheNode node)
      +CacheNode getNode(Key key)
    }

    class CacheNode {
      -String nodeId
      -Map~Key, Value~ cache
      -EvictionStrategy eviction
      -PersistenceLayer persistence
      -List~CacheNode~ replicas
      -boolean isUp
      +String getNodeId()
      +boolean isUp()
      +Value get(Key key)
      +void put(Key key, Value value)
      +boolean containsKey(Key key)
      +void addReplica(CacheNode replicaNode)
      +void removeReplica(CacheNode replicaNode)
      +void markDown()
      +void markUp()
      +void sync()
      +void shutdown()
    }

    class EvictionStrategy {
      <<interface>>
      +void evict(Map~Key, Value~ cache)
      +boolean shouldEvict()
      +void onGet(Key key)
      +void onPut(Key key, Value value)
      +void onRemove(Key key)
    }

    class LRUEviction {
      -int capacity
      -LinkedHashMap~Key, Boolean~ accessOrder
    }

    class LFUEviction {
      -int capacity
      -Map~Key, Integer~ frequencies
      -Map~Key, Long~ versions
      -PriorityQueue minHeap
    }

    class TTLEviction {
      -long ttlMillis
      -Map~Key, Long~ expiryByKey
    }

    class PersistenceLayer {
      <<interface>>
      +WritePolicy getWritePolicy()
      +Value get(Key key)
      +void put(Key key, Value value)
      +void flush()
      +void shutdown()
    }

    class WriteThrough {
      -Map~Key, Value~ store
    }

    class WriteBack {
      -Map~Key, Value~ store
      -Map~Key, Value~ pendingWrites
      -ScheduledExecutorService flusher
    }

    class WritePolicy {
      <<enumeration>>
      WRITE_THROUGH
      WRITE_BACK
    }

    class NodeObserver {
      <<interface>>
      +void onNodeDown(CacheNode node)
      +void onNodeUp(CacheNode node)
    }

    class HealthMonitor {
      -Duration interval
      -List~NodeObserver~ observers
      -List~CacheNode~ monitoredNodes
      -Map~String, Boolean~ knownNodeStates
      +void addObserver(NodeObserver observer)
      +void removeObserver(NodeObserver observer)
      +void registerNode(CacheNode node)
      +void unregisterNode(CacheNode node)
      +void startHeartbeat()
      +void stopHeartbeat()
    }

    class Main

    Cache --> LoadBalancer
    Cache --> Response
    Cache ..> Key
    Response --> Value

    LoadBalancer --> ConsistentHashRing
    LoadBalancer --> CacheNode
    LoadBalancer ..|> NodeObserver

    ConsistentHashRing --> CacheNode
    ConsistentHashRing ..> Key : uses getHashCode()

    CacheNode *-- EvictionStrategy
    CacheNode *-- PersistenceLayer
    CacheNode --> CacheNode : replicas
    CacheNode ..> Key
    CacheNode ..> Value

    LRUEviction ..|> EvictionStrategy
    LFUEviction ..|> EvictionStrategy
    TTLEviction ..|> EvictionStrategy

    WriteThrough ..|> PersistenceLayer
    WriteBack ..|> PersistenceLayer
    PersistenceLayer --> WritePolicy

    HealthMonitor --> NodeObserver
    HealthMonitor ..> CacheNode : monitors

    Main ..> Cache
    Main ..> CacheNode
    Main ..> HealthMonitor
```

## Running the Demo

From this directory:

```bash
javac *.java
java Main
```

The demo in Main exercises:

- basic put/get through Cache
- replication path (primary to replica)
- LRU eviction behavior
- node down/up detection and routing failover

## Notes

- All classes are intentionally kept in a single folder and default package, per requirement.
- This is an LLD-style implementation focused on behavior and component interaction.
