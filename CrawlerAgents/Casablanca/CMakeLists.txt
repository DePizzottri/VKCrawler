cmake_minimum_required(VERSION 2.8)

project(Crawlers)

if(UNIX)
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fno-strict-aliasing")
endif()

include_directories(${CASABLANCA_INCLUDE_DIR})
link_directories(${CASABLANCA_LIB_DIR})

add_executable(Crawler_FL src/friends_list.cpp)
add_executable(Crawler_UI src/user_info.cpp)

find_package(Boost COMPONENTS system)
FIND_PACKAGE(OpenSSL)

if(UNIX)
  target_link_libraries(Crawler_FL libcpprest.so ${Boost_LIBRARIES} ${OPENSSL_LIBRARIES})
  target_link_libraries(Crawler_UI libcpprest.so ${Boost_LIBRARIES} ${OPENSSL_LIBRARIES})
else()
  target_link_libraries(Crawler_FL ${CASABLANCA_LIB_DIR}/cpprest_2_8.lib)
  target_link_libraries(Crawler_UI ${CASABLANCA_LIB_DIR}/cpprest_2_8.lib)
endif()

