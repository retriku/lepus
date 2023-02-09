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

package lepus.client
package internal

import cats.effect.IO
import cats.effect.Ref
import fs2.Stream
import lepus.protocol.BasicClass
import lepus.protocol.ConnectionClass
import lepus.protocol.Frame
import lepus.protocol.Method
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import munit.Location
import scala.concurrent.duration.*
import java.util.concurrent.TimeoutException

class StartupNegotiationSuite extends InternalTestSuite {
  private val fakeSaslMechanism =
    SaslMechanism(ShortString("fake1"), IO(LongString("initial")), IO(_))
  private val auth = AuthenticationConfig(fakeSaslMechanism)

  private val clientProps = FieldTable(
    ShortString("product") -> 1,
    ShortString("version") -> 1,
    ShortString("platform") -> 1
  )
  private def noSend(using Location) = (_: Frame) =>
    IO(fail("No send expected"))

  check("Closes connection when transfer is terminated") {
    for {
      sut <- StartupNegotiation(auth)
      _ <- sut.pipe(noSend)(Stream.empty).compile.toList.assertEquals(Nil)
      _ <- sut.config.intercept[NegotiationFailed.type]
    } yield ()
  }

  check("Closes connection when encounters an error") {
    val error = new Exception()
    for {
      sut <- StartupNegotiation(auth)
      _ <- sut
        .pipe(noSend)(Stream.raiseError(error))
        .compile
        .drain
        .attempt
        .assertEquals(Left(error))
      _ <- sut.config.attempt.assertEquals(Left(error))
    } yield ()
  }

  check("Closes connection when server proposes no known mechanism") {
    val serverResponses = fs2.Stream.emit(
      method(
        ConnectionClass.Start(
          0,
          9,
          FieldTable.empty,
          LongString("unknown"),
          locales = LongString("")
        )
      )
    )

    for {
      sut <- StartupNegotiation(auth)
      _ <- sut
        .pipe(noSend)(serverResponses)
        .compile
        .drain
        .intercept[NoSupportedSASLMechanism.type]
      _ <- sut.config.intercept[NoSupportedSASLMechanism.type]
    } yield ()
  }

  check("Closes connection when server protocol does not match") {
    val serverResponses = fs2.Stream.emit(
      method(
        ConnectionClass.Start(
          1,
          0,
          FieldTable.empty,
          LongString("fake1"),
          locales = LongString("")
        )
      )
    )

    for {
      sut <- StartupNegotiation(auth)
      _ <- sut
        .pipe(noSend)(serverResponses)
        .compile
        .drain
        .intercept[NegotiationError.type]
      _ <- sut.config.intercept[NegotiationError.type]
    } yield ()
  }

  check("Selects first matching mechanism") {
    val serverResponses = fs2.Stream.emit(
      method(
        ConnectionClass.Start(
          0,
          9,
          FieldTable.empty,
          LongString("fake1 fake2"),
          locales = LongString("")
        )
      )
    )
    val expected = method(
      ConnectionClass.StartOk(
        clientProps,
        mechanism = ShortString("fake1"),
        response = LongString("initial"),
        locale = ShortString("en-US")
      )
    )

    for {
      sut <- StartupNegotiation(auth)
      send <- ExpectedQueue(expected)
      _ <- sut
        .pipe(send.assert)(serverResponses)
        .compile
        .toList
        .assertEquals(Nil)
      _ <- sut.config.intercept[NegotiationFailed.type]
    } yield ()
  }

  check("Responds to all SASL challenges") {
    val serverResponses = fs2.Stream(
      method(
        ConnectionClass.Start(
          0,
          9,
          FieldTable.empty,
          LongString("fake1 fake2"),
          locales = LongString("")
        )
      ),
      method(ConnectionClass.Secure(LongString("abc"))),
      method(ConnectionClass.Secure(LongString("def"))),
      method(ConnectionClass.Secure(LongString("ghi")))
    )

    val expected = List(
      method(
        ConnectionClass.StartOk(
          clientProps,
          mechanism = ShortString("fake1"),
          response = LongString("initial"),
          locale = ShortString("en-US")
        )
      ),
      method(ConnectionClass.SecureOk(LongString("abc"))),
      method(ConnectionClass.SecureOk(LongString("def"))),
      method(ConnectionClass.SecureOk(LongString("ghi")))
    )

    for {
      sut <- StartupNegotiation(auth)
      send <- ExpectedQueue(expected)
      _ <- sut
        .pipe(send.assert)(serverResponses)
        .compile
        .toList
        .assertEquals(Nil)
      _ <- sut.config.intercept[NegotiationFailed.type]
    } yield ()
  }

  check("Accepts tuning parameters from server") {
    val serverResponses = fs2.Stream(
      method(
        ConnectionClass.Start(
          0,
          9,
          FieldTable.empty,
          LongString("fake1 fake2"),
          locales = LongString("")
        )
      ),
      method(ConnectionClass.Tune(1, 2, 3))
    )

    val expected = List(
      method(
        ConnectionClass.StartOk(
          clientProps,
          mechanism = ShortString("fake1"),
          response = LongString("initial"),
          locale = ShortString("en-US")
        )
      ),
      method(ConnectionClass.TuneOk(1, 2, 3))
    )

    for {
      sut <- StartupNegotiation(auth)
      send <- ExpectedQueue(expected)
      _ <- sut
        .pipe(send.assert)(serverResponses)
        .compile
        .toList
        .assertEquals(Nil)
      _ <- sut.config.assertEquals(NegotiatedConfig(1, 2, 3))
      _ <- send.assertEmpty
    } yield ()
  }

  check("Outputs frames from server after negotiation is done") {
    val serverResponses = fs2.Stream(
      method(
        ConnectionClass.Start(
          0,
          9,
          FieldTable.empty,
          LongString("fake1 fake2"),
          locales = LongString("")
        )
      ),
      method(ConnectionClass.Tune(1, 2, 3)),
      method(ConnectionClass.OpenOk)
    )

    val expected = List(
      method(
        ConnectionClass.StartOk(
          clientProps,
          mechanism = ShortString("fake1"),
          response = LongString("initial"),
          locale = ShortString("en-US")
        )
      ),
      method(ConnectionClass.TuneOk(1, 2, 3))
    )

    for {
      sut <- StartupNegotiation(auth)
      send <- ExpectedQueue(expected)
      _ <- sut
        .pipe(_ => IO.unit)(serverResponses)
        .compile
        .toList
        .assertEquals(List(method(ConnectionClass.OpenOk)))
    } yield ()
  }

  private def method(value: Method) = Frame.Method(ChannelNumber(0), value)
}

private final case class ExpectedQueue(values: Ref[IO, List[Frame]])
    extends AnyVal {
  import munit.CatsEffectAssertions.*
  def assert(frame: Frame)(using Location): IO[Unit] =
    values.modify(l => (l.tail, l.headOption)).assertEquals(Some(frame))
  def assertEmpty(using Location) = values.get.assertEquals(Nil)
}
private object ExpectedQueue {
  def apply(values: List[Frame]): IO[ExpectedQueue] =
    IO.ref(values).map(new ExpectedQueue(_))
  def apply(value: Frame): IO[ExpectedQueue] = apply(List(value))
}
