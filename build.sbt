name := """VKCrawler"""
version := "0.3.0"
scalaVersion := "2.11.5"

lazy val common = Seq(
  version := "0.3.0",
  scalaVersion := "2.11.5",
  Revolver.settings
)

lazy val root = (project in file(".")).aggregate(InfoNGraphRefiner, FLNGTaskMaster, Services, DataScheme)

lazy val DataScheme = (project in file("lib/DataScheme")).settings(common: _*).settings(assemblySettings)

lazy val Services = (project in file("lib/Services")).settings(common: _*).settings(assemblySettings)

lazy val InfoNGraphRefiner = (project in file("Refineries/InfoNGraphRefiner")).settings(common: _*).settings(assemblySettings).dependsOn(DataScheme)

lazy val FLNGTaskMaster = (project in file("TaskMasters/FLNGTaskMaster")).settings(common: _*).settings(assemblySettings).dependsOn(DataScheme)

lazy val WEBInterface = (project in file("WEBInterface")).settings(common: _*).settings(assemblySettings).dependsOn(DataScheme)

import AssemblyKeys._
