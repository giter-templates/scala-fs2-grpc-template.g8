package com.example.instrumentation

import cats.effect._
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.HTTPServer

import java.net.InetSocketAddress

object Metrics {
  def resource[F[_]](port: Int = 8081)(implicit F: Sync[F]): Resource[F, HTTPServer] = {
    def acquire(): F[HTTPServer] = F.delay {
      val registry = CollectorRegistry.defaultRegistry
      new HTTPServer(new InetSocketAddress(port), registry)
    }

    def release(server: HTTPServer): F[Unit] = F.delay(server.stop())

    Resource.make(acquire())(release)
  }
}
