/*
 * Copyright 2021 Hossein Naderi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lepus.wire

import lepus.protocol.*
import lepus.protocol.domains.*
import lepus.protocol.*
import lepus.protocol.BasicClass.*
import lepus.protocol.constants.*
import lepus.wire.DomainCodecs.*
import scodec.{Codec, Encoder, Decoder}
import scodec.codecs.*

object BasicCodecs {

  private val qosCodec: Codec[Qos] =
    (int32 :: short16 :: (reverseByteAligned(bool)))
      .as[Qos]
      .withContext("qos method")

  private val qosOkCodec: Codec[QosOk.type] =
    provide(QosOk)
      .withContext("qosOk method")

  private val consumeCodec: Codec[Consume] =
    ((short16.unit(0) :: queueName :: consumerTag) ++ (reverseByteAligned(
      noLocal :: noAck :: bool :: noWait
    ) :+ (fieldTable)))
      .as[Consume]
      .withContext("consume method")

  private val consumeOkCodec: Codec[ConsumeOk] =
    (consumerTag)
      .as[ConsumeOk]
      .withContext("consumeOk method")

  private val cancelCodec: Codec[Cancel] =
    (consumerTag :: (reverseByteAligned(noWait)))
      .as[Cancel]
      .withContext("cancel method")

  private val cancelOkCodec: Codec[CancelOk] =
    (consumerTag)
      .as[CancelOk]
      .withContext("cancelOk method")

  private val publishCodec: Codec[Publish] =
    ((short16.unit(0) :: exchangeName :: shortString) ++ (reverseByteAligned(
      bool :: bool
    )))
      .as[Publish]
      .withContext("publish method")

  private val returnCodec: Codec[Return] =
    (replyCode :: replyText :: exchangeName :: shortString)
      .as[Return]
      .withContext("return method")

  private val deliverCodec: Codec[Deliver] =
    ((consumerTag :: deliveryTag) ++ (reverseByteAligned(
      redelivered
    ) :: (exchangeName :: shortString)))
      .as[Deliver]
      .withContext("deliver method")

  private val getCodec: Codec[Get] =
    (short16.unit(0) :: queueName :: (reverseByteAligned(noAck)))
      .as[Get]
      .withContext("get method")

  private val getOkCodec: Codec[GetOk] =
    (deliveryTag :: (reverseByteAligned(
      redelivered
    ) :: (exchangeName :: shortString :: messageCount)))
      .as[GetOk]
      .withContext("getOk method")

  private val getEmptyCodec: Codec[GetEmpty.type] =
    (emptyShortString) ~> provide(GetEmpty)
      .withContext("getEmpty method")

  private val ackCodec: Codec[Ack] =
    (deliveryTag :: (reverseByteAligned(bool)))
      .as[Ack]
      .withContext("ack method")

  private val rejectCodec: Codec[Reject] =
    (deliveryTag :: (reverseByteAligned(bool)))
      .as[Reject]
      .withContext("reject method")

  private val recoverAsyncCodec: Codec[RecoverAsync] =
    (reverseByteAligned(bool))
      .as[RecoverAsync]
      .withContext("recoverAsync method")

  private val recoverCodec: Codec[Recover] =
    (reverseByteAligned(bool))
      .as[Recover]
      .withContext("recover method")

  private val recoverOkCodec: Codec[RecoverOk.type] =
    provide(RecoverOk)
      .withContext("recoverOk method")

  private val nackCodec: Codec[Nack] =
    (deliveryTag :: (reverseByteAligned(bool :: bool)))
      .as[Nack]
      .withContext("nack method")

  val all: Codec[BasicClass] =
    discriminated[BasicClass]
      .by(methodId)
      .subcaseP[Qos](MethodId(10)) { case m: Qos => m }(qosCodec)
      .subcaseP[QosOk.type](MethodId(11)) { case m: QosOk.type => m }(
        qosOkCodec
      )
      .subcaseP[Consume](MethodId(20)) { case m: Consume => m }(consumeCodec)
      .subcaseP[ConsumeOk](MethodId(21)) { case m: ConsumeOk => m }(
        consumeOkCodec
      )
      .subcaseP[Cancel](MethodId(30)) { case m: Cancel => m }(cancelCodec)
      .subcaseP[CancelOk](MethodId(31)) { case m: CancelOk => m }(cancelOkCodec)
      .subcaseP[Publish](MethodId(40)) { case m: Publish => m }(publishCodec)
      .subcaseP[Return](MethodId(50)) { case m: Return => m }(returnCodec)
      .subcaseP[Deliver](MethodId(60)) { case m: Deliver => m }(deliverCodec)
      .subcaseP[Get](MethodId(70)) { case m: Get => m }(getCodec)
      .subcaseP[GetOk](MethodId(71)) { case m: GetOk => m }(getOkCodec)
      .subcaseP[GetEmpty.type](MethodId(72)) { case m: GetEmpty.type => m }(
        getEmptyCodec
      )
      .subcaseP[Ack](MethodId(80)) { case m: Ack => m }(ackCodec)
      .subcaseP[Reject](MethodId(90)) { case m: Reject => m }(rejectCodec)
      .subcaseP[RecoverAsync](MethodId(100)) { case m: RecoverAsync => m }(
        recoverAsyncCodec
      )
      .subcaseP[Recover](MethodId(110)) { case m: Recover => m }(recoverCodec)
      .subcaseP[RecoverOk.type](MethodId(111)) { case m: RecoverOk.type => m }(
        recoverOkCodec
      )
      .subcaseP[Nack](MethodId(120)) { case m: Nack => m }(nackCodec)
      .withContext("basic methods")

}