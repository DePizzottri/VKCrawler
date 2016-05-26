cmake_minimum_required(VERSION 2.8)

project(Crawler_FriendsList)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fno-strict-aliasing")

include_directories(~/code/casablanca/Release/include)
link_directories(~/code/casablanca/Release/build.release.static/Binaries)

add_executable(Crawler_FL src/friends_list.cpp)
target_link_libraries(BingRequest )
