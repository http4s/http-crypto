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
import scodec.bits.ByteVector

import javax.crypto

private[crypto] trait HmacPlatform[F[_]] {
  def importJavaKey(key: crypto.SecretKey): F[SecretKey[HmacAlgorithm]]
}

private[crypto] trait HmacCompanionPlatform {
  implicit def forApplicativeThrow[F[_]](implicit F: ApplicativeThrow[F]): Hmac[F] =
    new UnsealedHmac[F] {

      override def digest(key: SecretKey[HmacAlgorithm], data: ByteVector): F[ByteVector] =
        F.catchNonFatal {
          val mac = crypto.Mac.getInstance(key.algorithm.toStringJava)
          // val sk = key.toJava
          // mac.init(sk)
          mac.update(data.toByteBuffer)
          ByteVector.view(mac.doFinal())
        }

      override def importKey[A <: HmacAlgorithm](
          key: ByteVector,
          algorithm: A): F[SecretKey[A]] =
        F.pure(SecretKeySpec(key, algorithm))

      override def importJavaKey(key: crypto.SecretKey): F[SecretKey[HmacAlgorithm]] =
        F.fromOption(
          for {
            algorithm <- HmacAlgorithm.fromStringJava(key.getAlgorithm())
            key <- Option(key.getEncoded())
          } yield SecretKeySpec(ByteVector.view(key), algorithm),
          new InvalidKeyException
        )
    }
}
