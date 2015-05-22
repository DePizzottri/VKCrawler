project(PluginLib)

include_directories(include)
include_directories(D:/lib/poco/Foundation/include D:/lib/poco/JSON/include)

link_directories(D:/lib/poco/lib)

add_library(Plugin STATIC src/Plugin.cpp)
