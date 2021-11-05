package $package$.server

import $package$.instrumentation.MetadataAdapter
import $package$.server.TracedServerInterceptor.{TracedServerCall, TracedServerCallListener}
import io.grpc.{
  ForwardingServerCall,
  ForwardingServerCallListener,
  Metadata,
  ServerCall,
  ServerCallHandler,
  ServerInterceptor,
  Status
}
import io.jaegertracing.internal.{JaegerSpan, JaegerTracer}
import io.opentracing.propagation.Format

class TracedServerInterceptor(tracer: JaegerTracer) extends ServerInterceptor {
  override def interceptCall[ReqT, RespT](
    call: ServerCall[ReqT, RespT],
    headers: Metadata,
    next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    val adapter = MetadataAdapter(headers)
    val parentSpan = tracer.extract(Format.Builtin.HTTP_HEADERS, adapter)

    val span: JaegerSpan = if (parentSpan != null) {
      tracer.buildSpan("ping_response").asChildOf(parentSpan).start()
    } else {
      tracer.buildSpan("ping_response").start()
    }

    val meteredServerCall = new TracedServerCall(call, span)
    new TracedServerCallListener(next.startCall(meteredServerCall, headers), span)
  }
}

object TracedServerInterceptor {
  private class TracedServerCallListener[Req](
    delegate: ServerCall.Listener[Req],
    span: JaegerSpan
  ) extends ForwardingServerCallListener[Req] {

    override def delegate(): ServerCall.Listener[Req] = delegate

    override def onMessage(message: Req): Unit = {
      span.log("onMessage")
      super.onMessage(message)
    }

    override def onHalfClose(): Unit = {
      span.log("onHalfClose")
      super.onHalfClose()
    }

    override def onCancel(): Unit = {
      span.log("onCancel")
      super.onCancel()
    }

    override def onComplete(): Unit = {
      span.log("onComplete")
      super.onComplete()
    }
  }

  private class TracedServerCall[Req, Res](
    delegate: ServerCall[Req, Res],
    span: JaegerSpan
  ) extends ForwardingServerCall.SimpleForwardingServerCall[Req, Res](delegate) {
    override def sendMessage(message: Res): Unit = {
      span.log("sendMessage")
      super.sendMessage(message)
    }

    override def close(status: Status, trailers: Metadata): Unit = {
      span.log("close")
      span.setTag("status", status.getCode.name())
      span.finish()
      super.close(status, trailers)
    }
  }
}
