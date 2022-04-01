/*
 * Copyright 2021 http4s.org
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

package org.http4s.crypto

import cats.ApplicativeThrow
import cats.MonadThrow
import cats.effect.kernel.Async
import cats.syntax.all._
import scodec.bits.ByteVector

private[crypto] trait HashCompanionPlatform {
  @deprecated("Preserved for bincompat", "0.2.3")
  def forAsyncOrMonadThrow[F[_]](implicit F: Priority[Async[F], MonadThrow[F]]): Hash[F] =
    forApplicativeThrow(F.join)

  implicit def forApplicativeThrow[F[_]](implicit F: ApplicativeThrow[F]): Hash[F] =
    if (facade.isNodeJSRuntime)
      new UnsealedHash[F] {
        override def digest(algorithm: HashAlgorithm, data: ByteVector): F[ByteVector] =
          F.catchNonFatal {
            val hash = facade.node.crypto.createHash(algorithm.toStringNodeJS)
            hash.update(data.toUint8Array)
            ByteVector.view(hash.digest())
          }
      }
    else
      Some(F)
        .collect { case f: Async[F] => f }
        .fold(
          throw new UnsupportedOperationException("Hash[F] on browsers requires Async[F]")
        ) { implicit F: Async[F] =>
          new UnsealedHash[F] {
            import facade.browser._
            override def digest(algorithm: HashAlgorithm, data: ByteVector): F[ByteVector] =
              F.fromPromise(F.delay(
                crypto.subtle.digest(algorithm.toStringWebCrypto, data.toUint8Array.buffer)))
                .map(ByteVector.view)
          }
        }

}
