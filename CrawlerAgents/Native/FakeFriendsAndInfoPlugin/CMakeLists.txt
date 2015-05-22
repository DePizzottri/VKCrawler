cmake_minimum_required(VERSION 3.0)

project(FakeFriendsAndInfoPlugin)

add_definitions(-DPERFORMANCE_COUNT)

include_directories(include)

include_directories(D:/lib/Poco_2013/poco/Foundation/include D:/lib/Poco_2013/poco/JSON/include D:/lib/Poco_2013/poco/Net/include D:/lib/Poco_2013/poco/Util/include)
link_directories(D:/lib/Poco_2013/poco/lib)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../Plugin/include)
#add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../Plugin ${CMAKE_CURRENT_SOURCE_DIR}/../Plugin)
add_library(FakeFriendsAndInfoPlugin SHARED src/FakeFriendsAndInfoPlugin.cpp)

target_link_libraries(FakeFriendsAndInfoPlugin Plugin)