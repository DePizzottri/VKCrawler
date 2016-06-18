name := """VKCrawler WEB Interface"""

import Common._

resolvers ++= Seq (
  Akka.resolver,
  Spray.resolver
)

libraryDependencies ++= Seq(
  Akka.actor,
  Akka.remote,
  Akka.protobuf
)

libraryDependencies ++= Seq(
  Spray.client,
  Spray.can,
  Spray.http,
  Spray.httpx,
  Spray.util,
  Spray.routing,
  //Spray.testkit,
  Spray.json
)

// libraryDependencies ++= Seq(
//   Kamon.core,
//   Kamon.akka,
//   Kamon.akka_remote,
//   Kamon.statsd,
//   Kamon.spray,
//   Kamon.aspectj
// )

libraryDependencies ++= Seq(
  Other.slf4j
  //Other.protobuf
)

scalacOptions ++= Seq("-feature", "-deprecation")


// aspectjSettings
//
// javaOptions <++= AspectjKeys.weaverOptions in Aspectj
//
// //workaround for Kamon/AspectJ+assembly
// val aopMerge: sbtassembly.MergeStrategy = new sbtassembly.MergeStrategy {
//   val name = "aopMerge"
//   import scala.xml._
//   import scala.xml.dtd._
//
//   def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
//     val dt = DocType("aspectj", PublicID("-//AspectJ//DTD//EN", "http://www.eclipse.org/aspectj/dtd/aspectj.dtd"), Nil)
//     val file = MergeStrategy.createMergeTarget(tempDir, path)
//     val xmls: Seq[Elem] = files.map(XML.loadFile)
//     val aspectsChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "aspects" \ "_")
//     val weaverChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "weaver" \ "_")
//     val options: String = xmls.map(x => (x \\ "aspectj" \ "weaver" \ "@options").text).mkString(" ").trim
//     val weaverAttr = if (options.isEmpty) Null else new UnprefixedAttribute("options", options, Null)
//     val aspects = new Elem(null, "aspects", Null, TopScope, false, aspectsChildren: _*)
//     val weaver = new Elem(null, "weaver", weaverAttr, TopScope, false, weaverChildren: _*)
//     val aspectj = new Elem(null, "aspectj", Null, TopScope, false, aspects, weaver)
//     XML.save(file.toString, aspectj, "UTF-8", xmlDecl = false, dt)
//     IO.append(file, IO.Newline.getBytes(IO.defaultCharset))
//     Right(Seq(file -> path))
//   }
// }

assemblyMergeStrategy in assembly := {
  case "application.conf" => MergeStrategy.first
  //case PathList("META-INF", "aop.xml") => aopMerge
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyMergeStrategy in assembly := {
  case PathList("org", "joda", "time", "base", "BaseDateTime.class") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

