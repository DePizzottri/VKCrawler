cmake_minimum_required(VERSION 3.0)

project(CrawlerApp)

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/App ${CMAKE_CURRENT_SOURCE_DIR}/App)

install(FILES App/Release/Crawler.exe DESTINATION ${PROJECT_SOURCE_DIR}/install)
install(FILES FakeFriendsAndInfoPlugin/Release/FakeFriendsAndInfoPlugin.dll DESTINATION ${PROJECT_SOURCE_DIR}/install RENAME FakeFriendsAndInfoPlugin.vcpl)
install(FILES FriendsListCrawlPlugin/Release/FriendsListCrawlPlugin.dll DESTINATION ${PROJECT_SOURCE_DIR}/install RENAME FriendsListCrawlPlugin.vcpl)
install(FILES config/example.properties DESTINATION ${PROJECT_SOURCE_DIR}/install RENAME Crawler.properties)

