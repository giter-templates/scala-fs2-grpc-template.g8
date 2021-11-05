package $package$.client

import cats.effect._
import $package$.instrumentation.{Metrics, Tracing}
import $package$.proto.service.{EchoRequest, EchoServiceFs2Grpc}
import fs2.grpc.syntax.all._
import io.grpc.Metadata
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.jaegertracing.internal.JaegerTracer

import scala.concurrent.duration._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      _ <- Metrics.resource[IO](8082)
      tracer <- Tracing.jaeger[IO]("$name$-client")
      client <- createClient[IO](tracer, "server", 8080)
    } yield client).use { stub =>
      val effect = for {
        response <- stub.echo(EchoRequest("sample"), new Metadata())
        _ <- IO.delay(println(s"Response from server: \$response"))
      } yield ()

      fs2.Stream.awakeEvery[IO](1.second).evalMap(_ => effect).compile.drain
    }.as(ExitCode.Success)

  private def createClient[F[_]: Async](
    tracer: JaegerTracer,
    host: String,
    port: Int
  ): Resource[F, EchoServiceFs2Grpc[F, Metadata]] =
    NettyChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      .intercept(new MeteredClientInterceptor(), new TracedClientInterceptor(tracer))
      .resource[F]
      .flatMap(channel => EchoServiceFs2Grpc.stubResource[F](channel))
}
