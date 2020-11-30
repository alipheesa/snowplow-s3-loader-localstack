/*
 * Copyright (c) 2014-2020 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

 // SBT
import sbt._
import Keys._

import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker
import com.typesafe.sbt.packager.docker._

object BuildSettings {

  // Basic settings for our app
  lazy val basicSettings = Seq(
    organization          :=  "com.snowplowanalytics",
    scalaVersion          :=  "2.13.2",
    scalacOptions         :=  compilerOptions,
    javacOptions          :=  javaCompilerOptions,
    resolvers             ++= Dependencies.resolvers
  )

  lazy val compilerOptions = Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Ywarn-numeric-widen",
    "-Wdead-code",
    "-Werror",
    "-Wunused:imports",
    "-Xlint",
    "-Xlint:adapted-args"
  )

  lazy val dockerSettings = Seq(
    maintainer in Docker := "Snowplow Analytics Ltd. <support@snowplowanalytics.com>",
    daemonUser in Docker := "snowplow",
    packageName in Docker := "snowplow/snowplow-s3-loader",
    dockerBaseImage := "snowplow-docker-registry.bintray.io/snowplow/base-debian:0.1.0",
    dockerUpdateLatest := true,
    dockerCommands := {
      val installLzo = Seq(Cmd("RUN", "mkdir -p /var/lib/apt/lists/partial && apt-get update && apt-get install -y lzop && apt-get purge -y"))
      val (h, t) = dockerCommands.value.splitAt(dockerCommands.value.size-4)
      h ++ installLzo ++ t
    }
  )

  lazy val javaCompilerOptions = Seq(
    "-source", "1.8",
    "-target", "1.8"
  )

  // Makes our SBT app settings available from within the app
  lazy val scalifySettings = Seq(
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "settings.scala"
      IO.write(file, """package com.snowplowanalytics.s3.loader.generated
        |object Settings {
        |  val organization = "%s"
        |  val version = "%s"
        |  val name = "%s"
        |}
        |""".stripMargin.format(organization.value, version.value, name.value))
      Seq(file)
    }.taskValue
  )

  lazy val buildSettings = basicSettings ++ scalifySettings

  // sbt-assembly settings for building a fat jar
  import sbtassembly.AssemblyPlugin.autoImport._
  lazy val sbtAssemblySettings = Seq(
    assemblyJarName in assembly := { s"${name.value}-${version.value}.jar" },
    assemblyMergeStrategy in assembly := {
      case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
      case PathList("org", "objectweb", "asm", xs @ _*)  => MergeStrategy.first
      case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
      case "application.conf"                            => MergeStrategy.concat
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
}
