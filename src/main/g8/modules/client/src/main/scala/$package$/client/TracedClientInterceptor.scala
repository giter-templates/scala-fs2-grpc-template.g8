package $package$.client

import $package$.client.TracedClientInterceptor.TracedClientCall
import $package$.instrumentation.MetadataAdapter
import io.grpc.{
  CallOptions,
  Channel,
  ClientCall,
  ClientInterceptor,
  ForwardingClientCall,
  ForwardingClientCallListener,
  Metadata,
  MethodDescriptor,
  Status
}
import io.jaegertracing.internal.JaegerTracer
import io.opentracing.Span
import io.opentracing.propagation.Format

class TracedClientInterceptor(tracer: JaegerTracer) extends ClientInterceptor {
  override def interceptCall[ReqT, RespT](
    method: MethodDescriptor[ReqT, RespT],
    callOptions: CallOptions,
    next: Channel
  ): ClientCall[ReqT, RespT] = {
    val activeSpan = tracer.activeSpan()

    val span = if (activeSpan == null) {
      tracer.buildSpan("ping_request").start()
    } else {
      activeSpan
    }

    new TracedClientCall(next.newCall(method, callOptions), tracer, span)
  }
}

object TracedClientInterceptor {
  private class TracedClientCall[ReqT, RespT](
    delegate: ClientCall[ReqT, RespT],
    tracer: JaegerTracer,
    span: Span
  ) extends ForwardingClientCall[ReqT, RespT] {
    override def delegate(): ClientCall[ReqT, RespT] = delegate

    override def start(responseListener: ClientCall.Listener[RespT], headers: Metadata): Unit = {
      span.log("start")

      val adapter = MetadataAdapter(headers)
      val format = Format.Builtin.HTTP_HEADERS
      tracer.inject(span.context(), format, adapter)

      super.start(new TracedClientCallListener(responseListener, span), headers)
    }

    override def sendMessage(message: ReqT): Unit = {
      span.log("sendMessage")
      super.sendMessage(message)
    }
  }

  private class TracedClientCallListener[RespT](delegate: ClientCall.Listener[RespT], span: Span)
      extends ForwardingClientCallListener[RespT] {
    override def delegate(): ClientCall.Listener[RespT] = delegate

    override def onMessage(message: RespT): Unit = {
      span.log("onMessage")
      super.onMessage(message)
    }

    override def onReady(): Unit = {
      span.log("onReady")
      super.onReady()
    }

    override def onClose(status: Status, trailers: Metadata): Unit = {
      span.log("onMessage")
      span.setTag("status", status.getCode.name())
      span.finish()
      super.onClose(status, trailers)
    }
  }
}
