import cats.free.Free
import cats.free.Free.liftF
import cats.arrow.FunctionK
import cats.{Id, ~>}
import scala.collection.mutable

sealed trait KVStoreA[A] {}
case class Put[T](key: String, value: T) extends KVStoreA[Unit]
case class Get[T](key: String) extends KVStoreA[Option[T]]
case class Delete(key: String) extends KVStoreA[Unit]

object Main extends App {
  type KVStore[A] = Free[KVStoreA, A]
  def put[T](key: String, value: T): KVStore[Unit] = liftF[KVStoreA, Unit](Put[T](key, value))
  def get[T](key: String): KVStore[Option[T]] = liftF[KVStoreA, Option[T]](Get[T](key))
  def delete(key: String): KVStore[Unit] = liftF(Delete(key))

  def program: KVStore[Option[Int]] =
    for {
      _ <- put("wild-cats", 2)
      _ <- put("tame-cats", 5)
      n <- get[Int]("wild-cats")
      _ <- delete("tame-cats")
    } yield n

  def compiler: KVStoreA ~> Id  = new (KVStoreA ~> Id) {
      val kvs = mutable.Map.empty[String, Any]

      def apply[A](fa: KVStoreA[A]): Id[A] =
        fa match {
          case Put(key, value) =>
            println(s"put($key, $value)")
            kvs(key) = value
            ()
          case Get(key) =>
            println(s"get($key)")
            kvs.get(key).map(_.asInstanceOf[A])
          case Delete(key) =>
            println(s"delete($key)")
            kvs.remove(key)
            ()
        }
    }

  val result: Option[Int] = program.foldMap(compiler)
  println(result)
}
