package $package$.client

import $package$.client.MeteredClientInterceptor.MeteredClientCall
import io.grpc.{CallOptions, Channel, ClientCall, ClientInterceptor, ForwardingClientCall, MethodDescriptor}
import io.prometheus.client.Counter

class MeteredClientInterceptor extends ClientInterceptor {
  override def interceptCall[ReqT, RespT](
    method: MethodDescriptor[ReqT, RespT],
    callOptions: CallOptions,
    next: Channel
  ): ClientCall[ReqT, RespT] =
    new MeteredClientCall(next.newCall(method, callOptions))
}

object MeteredClientInterceptor {
  val sent: Counter =
    Counter.build("sent_requests", "sent requests").labelNames("source").register()

  private class MeteredClientCall[ReqT, RespT](delegate: ClientCall[ReqT, RespT])
      extends ForwardingClientCall[ReqT, RespT] {
    override def delegate(): ClientCall[ReqT, RespT] = delegate

    override def sendMessage(message: ReqT): Unit = {
      sent.labels("client").inc()
      super.sendMessage(message)
    }
  }
}
