## node.js test VK crawlers ##

This crawlers writes to "users" collection of MongoDB "VK" database user identifiers starting from choosen one within BFS order. With dublicates.

crawler1.node.js - simple naive implementation of bfs, writes insert MongoDB command to stdout

crawler2.node.js - more smart: divides list into parts, writes json objects to stdout

crawler3.node.js - uses cache and evented queue

crawler4.node.js - writes directly to MongoDB