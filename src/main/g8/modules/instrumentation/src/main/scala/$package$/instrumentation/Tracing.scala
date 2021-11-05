package $package$.instrumentation

import cats.effect.Resource
import cats.effect.kernel.Sync
import io.jaegertracing.Configuration
import io.jaegertracing.internal.JaegerTracer
import io.jaegertracing.internal.propagation.B3TextMapCodec
import io.jaegertracing.internal.reporters.RemoteReporter
import io.jaegertracing.internal.samplers.ConstSampler
import io.jaegertracing.thrift.internal.senders.UdpSender
import io.opentracing.propagation.Format

object Tracing {
  def jaeger[F[_]](serviceName: String)(implicit F: Sync[F]): Resource[F, JaegerTracer] = {
    def acquire(): F[JaegerTracer] = F.delay {
      val reporter =
        new RemoteReporter.Builder().withSender(new UdpSender("jaeger", 6831, 0)).withFlushInterval(100).build()

      val b3Codec = new B3TextMapCodec.Builder().build()

      new Configuration(serviceName).getTracerBuilder
        .withSampler(new ConstSampler(true))
        .withReporter(reporter)
        .registerInjector(Format.Builtin.HTTP_HEADERS, b3Codec)
        .registerExtractor(Format.Builtin.HTTP_HEADERS, b3Codec)
        .build
    }

    def release(tracer: JaegerTracer): F[Unit] = F.delay(tracer.close())

    Resource.make(acquire())(release)
  }
}
