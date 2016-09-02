package io.iohk.iodb.bench

import java.io.File

import io.iohk.iodb.{ByteArrayWrapper, TestUtils}
import io.iohk.iodb.skiplist._
import org.mapdb.DBMaker


object SLUpdateBench extends Benchmark {

  val defaultLimit = 44e6.toLong

  def main(args: Array[String]): Unit = {
    val limit: Long = args.headOption.map(_.toLong).getOrElse(defaultLimit)
    val file = File.createTempFile("iodb", "mapdb")
    def size() = file.length() / 1024 / 1024 / 1024.0

    file.delete()
    val store = DBMaker.fileDB(file).fileMmapEnable().make().getStore
    file.deleteOnExit()


    val source = (limit to 0 by -1).iterator.map{a =>
      val appendix = randomBytes(KeySize - 8)
      ByteArrayWrapper(TestUtils.fromLong(a).data ++ appendix) -> TestUtils.fromLong(a)
    }
    object iterable extends Iterable[(K, V)] {
      override def iterator: Iterator[(K, V)] = source
    }

    println("===============Starting to build a skiplist===============")
    val (t0, sl) = TestUtils.runningTime(AuthSkipList.createFrom(source = iterable, store = store, keySize = KeySize))
    println(f"$limit%,d items - ${t0 / 1000}%,d seconds - ${size()}%,f GB")
    println("===============Built===============")


    //updates
    //todo: for now an update is only about appends, add removals

    val NumUpdates = 10
    val UpdateSize = 1000

    println(s"===============$NumUpdates updates to be done now==========")
    (1 to NumUpdates).foreach { i =>
      val (t, _) = TestUtils.runningTime {
        (1 to UpdateSize).foreach { _ =>
          val (k, v) = randomKV()
          sl.put(k,v)
        }
      }
      println(f"${limit + i * UpdateSize}%,d items - ${t / 1000}%,d seconds - ${size()}%,f GB")
      println(s"===============Update #$i done===============")
    }

    store.close()
  }
}
