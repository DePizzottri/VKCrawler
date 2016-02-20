package vkcrawler.bfs.SprayJsonSupport

import spray.json._
import spray.json.DefaultJsonProtocol
import spray.httpx.SprayJsonSupport

object FriendsJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val friendsListFormat = jsonFormat2(vkcrawler.bfs.BFS.Friends)
}

object NewUsersJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val newUsersListFormat = jsonFormat1(vkcrawler.bfs.BFS.NewUsers)
}
