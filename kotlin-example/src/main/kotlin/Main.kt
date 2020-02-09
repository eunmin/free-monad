import arrow.Kind
import arrow.core.*
import arrow.core.extensions.id.monad.monad
import arrow.free.Free
import arrow.free.extensions.FreeMonad
import arrow.free.fix
import arrow.free.foldMap
import arrow.higherkind

@higherkind sealed class KVStoreA<out A>: KVStoreAOf<A> {
    data class Put<T>(val key: String, val value: T) : KVStoreA<Unit>()
    data class Get<T>(val key: String) : KVStoreA<Option<T>>()
    data class Delete(val key: String) : KVStoreA<Unit>()

    companion object: FreeMonad<ForKVStoreA> {
        fun <T> put(key: String, value: T): Free<ForKVStoreA, Unit> = Free.liftF(Put(key, value))
        fun <T> get(key: String): Free<ForKVStoreA, Option<T>> = Free.liftF(Get(key))
        fun delete(key: String): Free<ForKVStoreA, Unit> = Free.liftF(Delete(key))
    }
}

fun main() {
    val program = KVStoreA.fx.monad {
        !KVStoreA.put("wild-cats", 2)
        !KVStoreA.put("tame-cats", 5)
        val n = !KVStoreA.get<Int>("wild-cats")
        !KVStoreA.delete("tame-cats")
        n
    }.fix()

    @Suppress("UNCHECKED_CAST")
    val compiler: FunctionK<ForKVStoreA, ForId> = object : FunctionK<ForKVStoreA, ForId> {
        val kvs = HashMap<String, Any?>()

        override fun <A> invoke(fa: Kind<ForKVStoreA, A>): Id<A> {
            val op = fa.fix()
            return when(op) {
                is KVStoreA.Get<*> -> {
                    println("get(${op.key})")
                    Id(Option.fromNullable(kvs.get(op.key)))
                }
                is KVStoreA.Put<*> -> {
                    println("put(${op.key}, ${op.value})")
                    kvs.put(op.key, op.value)
                    Id(Unit)
                }
                is KVStoreA.Delete -> {
                    println("delete(${op.key})")
                    kvs.remove(op.key)
                    Id(Unit)
                }
            } as Id<A>
        }
    }

    val result = program.foldMap(compiler, Id.monad()).value()
    println(result)
}
