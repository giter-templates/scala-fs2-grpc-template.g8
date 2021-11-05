package $package$.server

import cats.effect._
import $package$.instrumentation.{Metrics, Tracing}
import $package$.proto.service.EchoServiceFs2Grpc
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder

import scala.jdk.CollectionConverters._
import fs2.grpc.syntax.all._
import io.grpc.{ServerInterceptors, ServerServiceDefinition}
import io.jaegertracing.internal.JaegerTracer

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      _ <- Metrics.resource[IO](8081)
      tracer <- Tracing.jaeger[IO]("$name$-server")
      services <- createServices[IO](tracer)
    } yield services).use { services =>
      runServer[IO](services)
    }

  private def createServices[F[_]: Async](
    tracer: JaegerTracer
  ): Resource[F, List[ServerServiceDefinition]] =
    for {
      echoService <- EchoServiceFs2Grpc
        .bindServiceResource(new EchoServiceImpl[F]())
        .map(service =>
          ServerInterceptors.intercept(service, new MeteredServerInterceptor(), new TracedServerInterceptor(tracer))
        )
    } yield List(echoService)

  private def runServer[F[_]: Async](services: List[ServerServiceDefinition]): F[Nothing] =
    NettyServerBuilder.forPort(8080).addServices(services.asJava).resource[F].map(server => server.start()).useForever
}
