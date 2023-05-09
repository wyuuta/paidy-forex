package forex.http
package rates

import forex.domain.Currency.show
import forex.domain.Rate.Pair
import forex.domain._
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

import java.time.OffsetDateTime

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  final case class GetApiResponse(
      from: Currency,
      to: Currency,
      price: Price,
      timestamp: Timestamp
  )

  final case class ErrorResponse(
      code: String,
      message: String
  )

  implicit val errorResponseEncoder : Encoder[ErrorResponse] =
    deriveConfiguredEncoder[ErrorResponse]

  implicit val currencyEncoder: Encoder[Currency] =
    Encoder.instance[Currency] { show.show _ andThen Json.fromString }

  implicit val pairEncoder: Encoder[Pair] =
    deriveConfiguredEncoder[Pair]

  implicit val rateEncoder: Encoder[Rate] =
    deriveConfiguredEncoder[Rate]

  implicit val responseEncoder: Encoder[GetApiResponse] =
    deriveConfiguredEncoder[GetApiResponse]


  implicit val currencyDecoder: Decoder[Currency] = (cursor : HCursor) =>
    for {
      value <- cursor.as[String]
      result = Currency.fromString(value)
    } yield result

  implicit val timestampDecoder: Decoder[OffsetDateTime] = (cursor : HCursor) =>
    for {
      value <- cursor.as[String]
      result = OffsetDateTime.parse(value)
    } yield result

  implicit val rateDecoder: Decoder[Rate] = (cursor: HCursor) =>
    for {
      from <- cursor.downField("from").as[Currency]
      to <- cursor.downField("to").as[Currency]
      price <- cursor.downField("price").as[BigDecimal]
      timestamp <- cursor.downField("time_stamp").as[OffsetDateTime]
    } yield {
      Rate(from, to, Price(price), Timestamp(timestamp))
    }

  implicit val redisRateDecoder: Decoder[Rate] = (cursor: HCursor) =>
    for {
      from <- cursor.downField("from").as[Currency]
      to <- cursor.downField("to").as[Currency]
      price <- cursor.downField("price").as[BigDecimal]
      timestamp <- cursor.downField("timestamp").as[OffsetDateTime]
    } yield {
      Rate(from, to, Price(price), Timestamp(timestamp))
    }

  implicit val rateListDecoder: Decoder[List[Rate]] = Decoder.decodeList(rateDecoder)
}
