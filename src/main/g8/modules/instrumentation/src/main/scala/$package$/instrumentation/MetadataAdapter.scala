package $package$.instrumentation

import io.grpc.Metadata
import io.opentracing.propagation.TextMap

import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters._

final case class MetadataAdapter(headers: Metadata) extends TextMap {
  override def iterator(): util.Iterator[util.Map.Entry[String, String]] = {
    val map = mutable.Map.empty[String, String]
    val keys = headers.keys().asScala

    keys.foreach { key =>
      val value = headers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER))
      map += (key -> value)
    }

    map.asJava.entrySet().iterator()
  }

  override def put(key: String, value: String): Unit =
    headers.put(
      Metadata.Key.of(key.toLowerCase, Metadata.ASCII_STRING_MARSHALLER),
      value.toLowerCase
    )
}
