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

package lepus.protocol.classes

import lepus.protocol.*
import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum BasicClass(methodId: MethodId)
    extends Class(ClassId(60))
    with Method(methodId) {

  case Qos(prefetchSize: Int, prefetchCount: Short, global: Boolean)
      extends BasicClass(MethodId(10))
      with Response

  case QosOk extends BasicClass(MethodId(11)) with Request

  case Consume(
      queue: QueueName,
      consumerTag: ConsumerTag,
      noLocal: NoLocal,
      noAck: NoAck,
      exclusive: Boolean,
      noWait: NoWait,
      arguments: FieldTable
  ) extends BasicClass(MethodId(20)) with Response

  case ConsumeOk(consumerTag: ConsumerTag)
      extends BasicClass(MethodId(21))
      with Request

  case Cancel(consumerTag: ConsumerTag, noWait: NoWait)
      extends BasicClass(MethodId(30))
      with Response
      with Request

  case CancelOk(consumerTag: ConsumerTag)
      extends BasicClass(MethodId(31))
      with Response
      with Request

  case Publish(
      exchange: ExchangeName,
      routingKey: ShortString,
      mandatory: Boolean,
      immediate: Boolean
  ) extends BasicClass(MethodId(40)) with Response

  case Return(
      replyCode: ReplyCode,
      replyText: ReplyText,
      exchange: ExchangeName,
      routingKey: ShortString
  ) extends BasicClass(MethodId(50)) with Request

  case Deliver(
      consumerTag: ConsumerTag,
      deliveryTag: DeliveryTag,
      redelivered: Redelivered,
      exchange: ExchangeName,
      routingKey: ShortString
  ) extends BasicClass(MethodId(60)) with Request

  case Get(queue: QueueName, noAck: NoAck)
      extends BasicClass(MethodId(70))
      with Response

  case GetOk(
      deliveryTag: DeliveryTag,
      redelivered: Redelivered,
      exchange: ExchangeName,
      routingKey: ShortString,
      messageCount: MessageCount
  ) extends BasicClass(MethodId(71)) with Request

  case GetEmpty extends BasicClass(MethodId(72)) with Request

  case Ack(deliveryTag: DeliveryTag, multiple: Boolean)
      extends BasicClass(MethodId(80))
      with Response
      with Request

  case Reject(deliveryTag: DeliveryTag, requeue: Boolean)
      extends BasicClass(MethodId(90))
      with Response

  case RecoverAsync(requeue: Boolean)
      extends BasicClass(MethodId(100))
      with Response

  case Recover(requeue: Boolean) extends BasicClass(MethodId(110)) with Response

  case RecoverOk extends BasicClass(MethodId(111)) with Request

  case Nack(deliveryTag: DeliveryTag, multiple: Boolean, requeue: Boolean)
      extends BasicClass(MethodId(120))
      with Response
      with Request

}
