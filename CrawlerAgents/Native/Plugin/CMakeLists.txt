project(PluginLib)

include_directories(include)
include_directories(C:/lib/poco/Foundation/include C:/lib/poco/JSON/include C:/lib/poco/NetSSL_OpenSSL/include)

link_directories(C:/lib/poco/build_vs14_u3_x86_static/lib/RelWithDebInfo)

add_library(Plugin STATIC src/Plugin.cpp)
