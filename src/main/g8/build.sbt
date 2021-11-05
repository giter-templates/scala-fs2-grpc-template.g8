import com.typesafe.sbt.packager.docker._
import Dependencies._

val scala2_13 = "2.13.6"

val compileAndTest = "compile->compile;test->test"

lazy val compilerOptions: Seq[String] = Seq(
  "-deprecation",
  "-unchecked",
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:existentials",
  "-language:postfixOps",
  "-Ywarn-dead-code",
  "-Xlint",
  "-Xlint:constant",
  "-Xlint:inaccessible",
  "-Xlint:nullary-unit",
  "-Xlint:type-parameter-shadow",
  "-Xlint:_,-byname-implicit",
  "-Ymacro-annotations",
  "-Wdead-code",
  "-Wnumeric-widen",
  "-Wunused:explicits",
  "-Wunused:implicits",
  "-Wunused:imports",
  "-Wunused:locals",
  "-Wunused:patvars",
  "-Wunused:privates",
  "-Wvalue-discard",
  "-Xlint:deprecation",
  "-Xlint:eta-sam",
  "-Xlint:eta-zero",
  "-Xlint:implicit-not-found",
  "-Xlint:infer-any",
  "-Xlint:nonlocal-return",
  "-Xlint:unused",
  "-Xlint:valpattern"
)

lazy val buildSettings = Seq(
  scalaVersion := scala2_13,
  scalacOptions ++= compilerOptions,
  Test / parallelExecution := false
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    scalaTest % Test
  ),
  addCompilerPlugin(
    ("org.typelevel" %% "kind-projector" % versions.kindProjectorVersion).cross(CrossVersion.full)
  )
)

lazy val $name$ =
  project
    .in(file("."))
    .settings(buildSettings)
    .settings(noPublish)
    .settings(moduleName := "$name$-grpc")
    .aggregate(protobuf, client, server, instrumentation)

lazy val protobuf =
  project
    .in(file("modules/protobuf"))
    .settings(buildSettings)
    .settings(commonSettings)
    .enablePlugins(Fs2Grpc)
    .settings(moduleName := "$name$-grpc-protobuf")
    .settings(
      libraryDependencies ++= Seq(
        "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion
      )
    )

lazy val instrumentation =
  project
    .in(file("modules/instrumentation"))
    .enablePlugins(JavaAppPackaging, DockerPlugin)
    .settings(buildSettings)
    .settings(commonSettings)
    .settings(moduleName := "$name$-grpc-instrumentation")
    .settings(
      libraryDependencies ++= Seq(
        prometheus,
        prometheusHttpServer,
        jaeger
      )
    )
    .dependsOn(protobuf)

lazy val client =
  project
    .in(file("modules/client"))
    .enablePlugins(JavaAppPackaging, DockerPlugin)
    .settings(buildSettings)
    .settings(commonSettings)
    .settings(moduleName := "$name$-grpc-client")
    .settings(
      dockerBaseImage := "openjdk:11-jre-slim",
      Docker / packageName := "$name$-grpc-client",
      Docker / version := "latest"
    )
    .dependsOn(protobuf, instrumentation)

lazy val server =
  project
    .in(file("modules/server"))
    .enablePlugins(JavaAppPackaging, DockerPlugin)
    .settings(buildSettings)
    .settings(commonSettings)
    .settings(moduleName := "$name$-grpc-server")
    .settings(
      dockerBaseImage := "openjdk:11-jre-slim",
      Docker / packageName := "$name$-grpc-server",
      Docker / version := "latest"
    )
    .dependsOn(protobuf, instrumentation)
