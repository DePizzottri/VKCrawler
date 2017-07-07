cmake_minimum_required(VERSION 3.0)

project(Crawler)

add_definitions(-DPERFORMANCE_COUNT)

include_directories(include)

include_directories(C:/lib/poco/Foundation/include C:/lib/poco/JSON/include C:/lib/poco/Net/include C:/lib/poco/Util/include C:/lib/poco/NetSSL_OpenSSL/include)
link_directories(C:/lib/poco/build_vs14_u3_x86_static/lib/RelWithDebInfo)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../Plugin/include)

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../FakeFriendsAndInfoPlugin ${CMAKE_CURRENT_SOURCE_DIR}/../FakeFriendsAndInfoPlugin)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../FriendsListCrawlPlugin ${CMAKE_CURRENT_SOURCE_DIR}/../FriendsListCrawlPlugin)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../WallPlugin ${CMAKE_CURRENT_SOURCE_DIR}/../WallPlugin)

set(CRAWLER_SRC src/App.cpp src/PluginsCache.cpp src/WEBGetTask.cpp src/WorkerTask.cpp)

add_executable(Crawler ${CRAWLER_SRC})
target_link_libraries(Crawler Plugin)
