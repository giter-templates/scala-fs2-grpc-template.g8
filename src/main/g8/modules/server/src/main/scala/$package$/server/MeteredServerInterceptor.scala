package $package$.server

import $package$.server.MeteredServerInterceptor.{MeteredServerCall, MeteredServerCallListener}
import io.grpc.{
  ForwardingServerCall,
  ForwardingServerCallListener,
  Metadata,
  ServerCall,
  ServerCallHandler,
  ServerInterceptor
}
import io.prometheus.client.Counter

class MeteredServerInterceptor extends ServerInterceptor {
  override def interceptCall[ReqT, RespT](
    call: ServerCall[ReqT, RespT],
    headers: Metadata,
    next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = {
    val meteredServerCall = new MeteredServerCall(call)
    new MeteredServerCallListener(next.startCall(meteredServerCall, headers))
  }
}

object MeteredServerInterceptor {
  val sent: Counter =
    Counter.build("sent_requests", "sent requests").labelNames("source").register()
  val received: Counter = Counter.build("received_responses", "received responses").register()

  private class MeteredServerCallListener[Req](
    delegate: ServerCall.Listener[Req]
  ) extends ForwardingServerCallListener[Req] {

    override def delegate(): ServerCall.Listener[Req] = delegate

    override def onMessage(message: Req): Unit = {
      received.inc()
      super.onMessage(message)
    }
  }

  private class MeteredServerCall[Req, Res](
    delegate: ServerCall[Req, Res]
  ) extends ForwardingServerCall.SimpleForwardingServerCall[Req, Res](delegate) {
    override def sendMessage(message: Res): Unit = {
      sent.labels("server").inc()
      super.sendMessage(message)
    }
  }
}
