cmake_minimum_required(VERSION 3.0)

project(FakeFriendsAndInfoPlugin)

add_definitions(-DPERFORMANCE_COUNT)

include_directories(include)

include_directories(C:/lib/poco/Foundation/include C:/lib/poco/JSON/include C:/lib/poco/Net/include C:/lib/poco/Util/include C:/lib/poco/NetSSL_OpenSSL/include)
link_directories(C:/lib/poco/build_vs14_u3_x86_static/lib/RelWithDebInfo)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../Plugin/include)
#add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../Plugin ${CMAKE_CURRENT_SOURCE_DIR}/../Plugin)
add_library(FakeFriendsAndInfoPlugin SHARED src/FakeFriendsAndInfoPlugin.cpp)

target_link_libraries(FakeFriendsAndInfoPlugin Plugin)