cmake_minimum_required(VERSION 3.0)

project(FriendsListCrawlPlugin)

add_definitions(-DPERFORMANCE_COUNT)

include_directories(include)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../Plugin/include)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../Plugin ${CMAKE_CURRENT_SOURCE_DIR}/../Plugin)

include_directories(C:/lib/poco/Foundation/include C:/lib/poco/JSON/include C:/lib/poco/Net/include C:/lib/poco/Util/include C:/lib/poco/NetSSL_OpenSSL/include)
link_directories(C:/lib/poco/build_vs14_u3_x86_static/lib/RelWithDebInfo)

add_library(FriendsListCrawlPlugin SHARED src/FriendsListCrawlPlugin.cpp)

target_link_libraries(FriendsListCrawlPlugin Plugin)
