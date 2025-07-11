import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.6"
PlayKeys.playDefaultPort := 10906

lazy val microservice = Project("trader-goods-profiles-data-store", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(CodeCoverageSettings.settings *)

ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*Routes.*"
ScoverageKeys.coverageMinimumStmtTotal := 90
ScoverageKeys.coverageFailOnMinimum := true
ScoverageKeys.coverageHighlighting := true


Test / javaOptions += "-Dlogger.conf=logback-test.xml"

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)

addCommandAlias("testAndCoverage", ";clean;coverage;test;it/test;coverageReport")
addCommandAlias("prePR", ";scalafmt;test:scalafmt;testAndCoverage")
addCommandAlias("preMerge", ";scalafmtCheckAll;testAndCoverage")
