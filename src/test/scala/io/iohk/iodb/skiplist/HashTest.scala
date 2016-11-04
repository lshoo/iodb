package io.iohk.iodb.skiplist

import io.iohk.iodb.TestUtils._
import org.junit.Test
import org.mapdb.DBMaker
import org.scalatest.Assertions
import scorex.crypto.hash.{CommutativeHash, CryptographicHash}

import scala.util.Random

class HashTest extends Assertions {

  val store = DBMaker.memoryDB().make().getStore

  implicit val comHash = new CommutativeHash[CryptographicHash](defaultHasher)

  def verifyHash(expected: List[Hash], keys: K*): Unit = {
    val list = AuthSkipList.createFrom(
      source = keys.map(k => (k, k)).sorted.reverse,
      store = store, keySize = 8)

    list.verifyHash()

    assert(list.loadHead().hashes == expected)

    val list2 = AuthSkipList.createEmpty(store = store, keySize = 8)
    keys.foreach(a => list2.put(a, a))
    assert(list2.loadHead().hashes == expected)

  }

  @Test def emptyHash(): Unit = {
    val list = AuthSkipList.createEmpty(store, keySize = 8)
    val expected = hashNode(hashEntry(negativeInfinity._1, negativeInfinity._2), hashEntry(positiveInfinity._1, positiveInfinity._2))
    assert(list.loadHead().hashes == List(expected))
  }

  @Test def emptyHash2(): Unit = {
    val expected = hashNode(
      hashEntry(negativeInfinity._1, negativeInfinity._2),
      hashEntry(positiveInfinity._1, positiveInfinity._2)
    )
    verifyHash(List(expected))
  }

  //
  //  def updatedElement(e: NormalSLElement): NormalSLElement = {
  //    e.copy(value = (1: Byte) +: e.value)
  //  }


  @Test
  def oneEntry(): Unit = {
    val key = fromLong(4L)
    val expected =
      hashNode(
        hashEntry(negativeInfinity._1, negativeInfinity._2),
        hashNode(
          hashEntry(key, key),
          hashEntry(positiveInfinity._1, positiveInfinity._2)))
    verifyHash(List(expected), key)
  }


  @Test
  def twoEntries(): Unit = {
    val key = fromLong(4L)
    val key2 = fromLong(8L)
    assert(0 == levelFromKey(key)) // we need key which only creates base level
    assert(0 == levelFromKey(key2))

    val expected =
      hashNode(
        hashEntry(negativeInfinity._1, negativeInfinity._2),
        hashNode(
          hashEntry(key, key),
          hashNode(
            hashEntry(key2, key2),
            hashEntry(positiveInfinity._1, positiveInfinity._2))))
    verifyHash(List(expected), key, key2)
  }


  @Test
  def oneEntryHigher(): Unit = {
    val key = fromLong(1L)
    assert(1 == levelFromKey(key)) // need two levels

    val list = AuthSkipList.createFrom(source = List((key, key)), store = store, keySize = 8)
    assert(2 == list.loadHead().right.size)
    val headBottom = hashNode(
      hashEntry(negativeInfinity._1, negativeInfinity._2),
      hashEntry(key, key)
    )

    val expected =
      hashNode(
        headBottom,
        hashNode(
          hashEntry(key, key),
          hashEntry(positiveInfinity._1, positiveInfinity._2)))
    verifyHash(List(headBottom, expected), key)
  }

  @Test def put(): Unit = {
    val data = Random.shuffle((0L until 40L).map(fromLong).toBuffer)

    // list generate by inserts
    val put = AuthSkipList.createEmpty(store = store, keySize = 8)
    data.foreach { k =>
      put.put(k, k)
      put.verifyHash()
      put.verifyStructure()
    }


    // list generated by imports
    val imported = AuthSkipList.createFrom(
      source = data.sorted.reverse.map(k => (k, k)),
      store = store, keySize = 8
    )

    imported.verifyHash()
    imported.verifyStructure()

    assert(imported.loadHead().hashes == put.loadHead().hashes)
  }


  @Test def delete(): Unit = {
    val data = Random.shuffle((0L until 100L).map(fromLong).toBuffer)

    // list generate by inserts
    val put = AuthSkipList.createEmpty(store = store, keySize = 8)
    data.foreach(k => put.put(k, k))
    //now delete key one by one, compare hashes
    while (!data.isEmpty) {
      // list generated by imports
      val imported = AuthSkipList.createFrom(
        source = data.sorted.reverse.map(k => (k, k)),
        store = store, keySize = 8
      )
      assert(imported.loadHead().hashes == put.loadHead().hashes)
      put.verifyHash()
      put.verifyStructure()

      val key = data.head
      data.remove(0, 1)
      put.remove(key)
    }

    put.verifyHash()
    put.verifyStructure()
    val imported = AuthSkipList.createEmpty(store = store, keySize = 8)
    assert(imported.loadHead().hashes == put.loadHead().hashes)

  }

}
