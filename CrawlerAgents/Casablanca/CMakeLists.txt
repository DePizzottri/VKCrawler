cmake_minimum_required(VERSION 2.8)

project(Crawler_FriendsList)

if(UNIX)
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fno-strict-aliasing")
endif()

include_directories(${CASABLANCA_INCLUDE_DIR})
link_directories(${CASABLANCA_LIB_DIR})

add_executable(Crawler_FL src/friends_list.cpp)
target_link_libraries(Crawler_FL ${CASABLANCA_LIB_DIR}/cpprest_2_8.lib
)
