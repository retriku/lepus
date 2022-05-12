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

package lepus.client.codecs

import lepus.protocol.*
import lepus.protocol.domains.*
import lepus.protocol.classes.*
import lepus.protocol.classes.ExchangeClass.*
import lepus.protocol.constants.*
import lepus.client.codecs.DomainCodecs.*
import scodec.{Codec, Encoder, Decoder}
import scodec.codecs.*

object ExchangeCodecs {

  private val declareCodec: Codec[Declare] =
    ((short16.unit(0) :: exchangeName :: shortString) ++ (reverseByteAligned(
      bool :: bool :: bool :: bool :: noWait
    ) :+ (fieldTable)))
      .as[Declare]
      .withContext("declare method")

  private val declareOkCodec: Codec[DeclareOk.type] =
    provide(DeclareOk)
      .withContext("declareOk method")

  private val deleteCodec: Codec[Delete] =
    ((short16.unit(0) :: exchangeName) ++ (reverseByteAligned(bool :: noWait)))
      .as[Delete]
      .withContext("delete method")

  private val deleteOkCodec: Codec[DeleteOk.type] =
    provide(DeleteOk)
      .withContext("deleteOk method")

  private val bindCodec: Codec[Bind] =
    ((short16.unit(
      0
    ) :: exchangeName :: exchangeName :: shortString) ++ (reverseByteAligned(
      noWait
    ) :: (fieldTable)))
      .as[Bind]
      .withContext("bind method")

  private val bindOkCodec: Codec[BindOk.type] =
    provide(BindOk)
      .withContext("bindOk method")

  private val unbindCodec: Codec[Unbind] =
    ((short16.unit(
      0
    ) :: exchangeName :: exchangeName :: shortString) ++ (reverseByteAligned(
      noWait
    ) :: (fieldTable)))
      .as[Unbind]
      .withContext("unbind method")

  private val unbindOkCodec: Codec[UnbindOk.type] =
    provide(UnbindOk)
      .withContext("unbindOk method")

  val all: Codec[ExchangeClass] =
    discriminated[ExchangeClass]
      .by(methodId)
      .subcaseP[Declare](MethodId(10)) { case m: Declare => m }(declareCodec)
      .subcaseP[DeclareOk.type](MethodId(11)) { case m: DeclareOk.type => m }(
        declareOkCodec
      )
      .subcaseP[Delete](MethodId(20)) { case m: Delete => m }(deleteCodec)
      .subcaseP[DeleteOk.type](MethodId(21)) { case m: DeleteOk.type => m }(
        deleteOkCodec
      )
      .subcaseP[Bind](MethodId(30)) { case m: Bind => m }(bindCodec)
      .subcaseP[BindOk.type](MethodId(31)) { case m: BindOk.type => m }(
        bindOkCodec
      )
      .subcaseP[Unbind](MethodId(40)) { case m: Unbind => m }(unbindCodec)
      .subcaseP[UnbindOk.type](MethodId(51)) { case m: UnbindOk.type => m }(
        unbindOkCodec
      )
      .withContext("exchange methods")

}
