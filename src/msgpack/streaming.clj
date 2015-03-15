(ns msgpack.streaming
  (:require [msgpack.io :refer :all])
  (:import java.io.DataOutputStream
           java.io.ByteArrayOutputStream))

(defprotocol Packable
  "A protocol for objects that can be serialized as a MessagePack type."
  (pack-stream [this output-stream]))

(declare pack-number)

(extend-protocol Packable
  nil
  (pack-stream
    [_ output-stream]
    (.writeByte output-stream 0xc0))

  Boolean
  (pack-stream
    [bool output-stream]
    (if bool
      (.writeByte output-stream 0xc3)
      (.writeByte output-stream 0xc2)))

  Byte
  (pack-stream
    [n output-stream]
    (pack-number n output-stream))

  Short
  (pack-stream
    [n output-stream]
    (pack-number n output-stream))

  Integer
  (pack-stream
    [n output-stream]
    (pack-number n output-stream))

  Long
  (pack-stream
    [n output-stream]
    (pack-number n output-stream))

  clojure.lang.BigInt
  (pack-stream
    [n output-stream]
    (pack-number n output-stream)))

(defn- pack-number
  "Pack n using the most compact representation possible"
  [n s]
  (cond
    ; +fixnum
    (<= 0 n 127)                  (.writeByte s n)
    ; -fixnum
    (<= -32 n -1)                 (.writeByte s n)
    ; uint 8
    (<= 0 n 0xff)                 (do (.writeByte s 0xcc) (.writeByte s n))
    ; uint 16
    (<= 0 n 0xffff)               (do (.writeByte s 0xcd) (.writeShort s n))
    ; uint 32
    (<= 0 n 0xffffffff)           (do (.writeByte s 0xce) (.writeInt s n))
    ; uint 64
    (<= 0 n 0xffffffffffffffff)   (do (.writeByte s 0xcf) (.writeLong s n))
    ; int 8
    (<= -0x80 n -1)               (do (.writeByte s 0xd0) (.writeByte s n))
    ; int 16
    (<= -0x8000 n -1)             (do (.writeByte s 0xd1) (.writeShort s n))
    ; int 32
    (<= -0x80000000 n -1)         (do (.writeByte s 0xd2) (.writeInt s n))
    ; int 64
    (<= -0x8000000000000000 n -1) (do (.writeByte s 0xd3) (.writeLong s n))))

(defn pack [obj]
  (let [baos (ByteArrayOutputStream.)
        dos (DataOutputStream. baos)]
    (do
      (pack-stream obj dos)
      (seq (.toByteArray baos)))))
