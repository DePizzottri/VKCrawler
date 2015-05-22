cmake_minimum_required(VERSION 3.0)

project(Crawler)

add_definitions(-DPERFORMANCE_COUNT)

include_directories(include)

include_directories(D:/lib/Poco_2013/poco/Foundation/include D:/lib/Poco_2013/poco/JSON/include D:/lib/Poco_2013/poco/Net/include D:/lib/Poco_2013/poco/Util/include)
link_directories(D:/lib/Poco_2013/poco/lib)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../Plugin/include)

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../FakeFriendsAndInfoPlugin ${CMAKE_CURRENT_SOURCE_DIR}/../FakeFriendsAndInfoPlugin)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../FriendsListCrawlPlugin ${CMAKE_CURRENT_SOURCE_DIR}/../FriendsListCrawlPlugin)

set(CRAWLER_SRC src/App.cpp src/PluginsCache.cpp src/WEBGetTask.cpp src/WorkerTask.cpp)

add_executable(Crawler ${CRAWLER_SRC})
target_link_libraries(Crawler Plugin)
