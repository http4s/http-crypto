/*
 * Copyright 2021 http4s.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import org.openqa.selenium.firefox.{FirefoxOptions, FirefoxProfile}
import org.openqa.selenium.remote.server.{DriverFactory, DriverProvider}
import org.scalajs.jsenv.selenium.SeleniumJSEnv

import JSEnv._

name := "http4s-crypto"

ThisBuild / baseVersion := "0.2"

ThisBuild / organization := "org.http4s"
ThisBuild / organizationName := "http4s.org"
ThisBuild / publishGithubUser := "armanbilge"
ThisBuild / publishFullName := "Arman Bilge"

enablePlugins(SonatypeCiReleasePlugin)
ThisBuild / spiewakCiReleaseSnapshots := true
ThisBuild / spiewakMainBranches := Seq("main")

ThisBuild / homepage := Some(url("https://github.com/http4s/http4s-crypto"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/http4s/http4s-crypto"),
    "https://github.com/http4s/http4s-crypto.git"))

ThisBuild / crossScalaVersions := Seq("3.1.0", "2.12.15", "2.13.7")

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8"))

ThisBuild / githubWorkflowBuildPreamble ++= Seq(
  WorkflowStep.Use(
    UseRef.Public("actions", "setup-node", "v2.4.0"),
    name = Some("Setup NodeJS v14 LTS"),
    params = Map("node-version" -> "14")
  )
)

replaceCommandAlias("ci", CI.AllCIs.map(_.toString).mkString)
addCommandAlias("ciJVM", CI.JVM.toString)
addCommandAlias("ciNodeJS", CI.NodeJS.toString)
addCommandAlias("ciFirefox", CI.Firefox.toString)
addCommandAlias("ciChrome", CI.Chrome.toString)

addCommandAlias("prePR", "; root/clean; scalafmtSbt; +root/scalafmtAll; +root/headerCreate")

lazy val useJSEnv =
  settingKey[JSEnv]("Use Node.js or a headless browser for running Scala.js tests")
Global / useJSEnv := NodeJS

ThisBuild / Test / jsEnv := {
  val old = (Test / jsEnv).value

  useJSEnv.value match {
    case NodeJS => old
    case Firefox =>
      val options = new FirefoxOptions()
      options.setHeadless(true)
      new SeleniumJSEnv(options)
    case Chrome =>
      val options = new ChromeOptions()
      options.setHeadless(true)
      new SeleniumJSEnv(options)
  }
}

val catsVersion = "2.7.0"
val catsEffectVersion = "3.3.3"
val scodecBitsVersion = "1.1.30"
val munitVersion = "0.7.29"
val munitCEVersion = "1.0.7"
val disciplineMUnitVersion = "1.0.9"

lazy val root =
  project.in(file(".")).aggregate(rootJS, rootJVM).enablePlugins(NoPublishPlugin)
lazy val rootJVM =
  project.aggregate(crypto.jvm, testRuntime.jvm).enablePlugins(NoPublishPlugin)
lazy val rootJS =
  project.aggregate(crypto.js, testRuntime.js).enablePlugins(NoPublishPlugin)

lazy val crypto = crossProject(JSPlatform, JVMPlatform)
  .in(file("crypto"))
  .settings(
    name := "http4s-crypto",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsVersion,
      "org.typelevel" %%% "cats-effect-kernel" % catsEffectVersion,
      "org.scodec" %%% "scodec-bits" % scodecBitsVersion,
      "org.scalameta" %%% "munit" % munitVersion % Test,
      "org.typelevel" %%% "cats-laws" % catsVersion % Test,
      "org.typelevel" %%% "cats-effect" % catsEffectVersion % Test,
      "org.typelevel" %%% "discipline-munit" % disciplineMUnitVersion % Test,
      "org.typelevel" %%% "munit-cats-effect-3" % munitCEVersion % Test
    )
  )
  .dependsOn(testRuntime % Test)

lazy val testRuntime = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("test-runtime"))
  .enablePlugins(BuildInfoPlugin, NoPublishPlugin)
  .settings(
    buildInfoPackage := "org.http4s.crypto"
  )
  .jvmSettings(
    buildInfoKeys := Seq(
      BuildInfoKey.sbtbuildinfoConstantEntry("runtime" -> "JVM")
    )
  )
  .jsSettings(
    buildInfoKeys := Seq(
      BuildInfoKey("runtime" -> useJSEnv.value.toString)
    )
  )
