import sbt._

object Dependencies {
  object versions {
    lazy val kindProjectorVersion = "0.13.0"
    lazy val logstageVersion = "1.0.8"
    lazy val jaegerVersion = "1.6.0"
    lazy val prometheusVersion = "0.11.0"
    lazy val scalatestVersion = "3.2.8"
  }

  lazy val scalaTest = "org.scalatest" %% "scalatest" % versions.scalatestVersion
  lazy val prometheus = "io.prometheus" % "simpleclient" % versions.prometheusVersion
  lazy val prometheusHttpServer = "io.prometheus" % "simpleclient_httpserver" % versions.prometheusVersion
  lazy val jaeger = "io.jaegertracing" % "jaeger-client" % versions.jaegerVersion

}
