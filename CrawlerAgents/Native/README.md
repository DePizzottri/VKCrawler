Native Crawler service
======================

Depends on Poco 1.6+
Builds with CMake

Native crawler agent have a plugin architecture: every task type implemented by external plugin (*.vcpl).
Server meet info consist of types of presented plugin. Every plugin must implement processing of only one task type.
Not configurable yet.

App
---

Main application, initialize plugins, gets tasks, manage tasks to plugins (downloading new plugins and updates in the future).
Task processing starts in the thread pool.

Pluing
------

Static library. Every plugin must inherit AbstractPlugin and link with this library.

FriendsListCrawlPlugin
----------------------

Plugin crawls static info and social graph
