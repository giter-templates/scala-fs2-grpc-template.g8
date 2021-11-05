package $package$.server

import cats.effect.kernel.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import $package$.proto.service.{EchoRequest, EchoResponse, EchoServiceFs2Grpc}
import io.grpc.Metadata

class EchoServiceImpl[F[_]](implicit F: Sync[F]) extends EchoServiceFs2Grpc[F, Metadata] {
  override def echo(request: EchoRequest, ctx: Metadata): F[EchoResponse] = for {
    _ <- F.delay(println(s"Get echo request: \$request"))
    response = EchoResponse(originalMessage = request.message, echoMessage = s"Echo \${request.message}")
    _ <- F.delay(println(s"Response: \$response"))
  } yield response
}
