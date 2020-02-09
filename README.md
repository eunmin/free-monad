# Free Monad

## Concept

- IO 모나드가 연산의 표현과 실행 시점 및 정해진 방법을 분리 할 수 있다면 Free Monad는 표현과 실행을 분리하면서 실행 방법을 자유롭게 
  만들 수 있다고 보면 될것 같다.

### 간단한 Free Monad (fpis Ch.13)

``` scala
sealed trait Free[F[_],A]
case class Return[F[_],A](a: A) extends Free[F,A]
case class Suspend[F[_],A](s: F[A]) extends Free[A,A]
case class FlatMap[F[_],A,B](s: Free[F,A], f: A => Free[F,B]) extends Free[F,B]

type IO[A] = Free[IO,A]

sealed trait IO[A] {
	def flatMap[B](f: A => IO[B]): IO[B] = FlatMap(this, f)
	def map[B](f: A => B): IO[B] = faltMap(f andThen (Return(_)))
}

@annotaion.tailrec def run[A](io: IO[A]): A = io match {
	case Return(a) => a
	case Suspend(r) => r()
	case FlatMap(x, f) => x match {
		case Return(a) => run(f(a))
		case Suspend(r) => run(f(r()))
		case FlatMap(y, g) => run(y flatMap (a => g(a) flatMap f))
	}
}

FlatMap(a1, a1 =>
	FlatMap(a2, a2 =>
		FaltMap(a3, a3 =>
			...
			FlatMap(aN, aN => Return(aN)))))
```

## Scala Cats

https://typelevel.org/cats/datatypes/freemonad.html

### 문법

``` scala
sealed trait KVStoreA[A] {}
case class Put[T](key: String, value: T) extends KVStoreA[Unit]
case class Get[T](key: String) extends KVStoreA[Option[T]]
case class Delete(key: String) extends KVStoreA[Unit]
```

### 리프팅 함수

``` scala
type KVStore[A] = Free[KVStoreA, A]
def put[T](key: String, value: T): KVStore[Unit] = liftF[KVStoreA, Unit](Put[T](key, value))
def get[T](key: String): KVStore[Option[T]] = liftF[KVStoreA, Option[T]](Get[T](key))
def delete(key: String): KVStore[Unit] = liftF(Delete(key))
```

### 프로그램

``` scala
def program: KVStore[Option[Int]] =
    for {
      _ <- put("wild-cats", 2)
      _ <- put("tame-cats", 5)
      n <- get[Int]("wild-cats")
      _ <- delete("tame-cats")
    } yield n
```

### 인터프리터

- `~>`은 Functionk 연산자

``` scala
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
```

### 실행

``` scala
program.foldMap(compiler)
```


## Kotlin Arrow

https://arrow-kt.io/docs/apidocs/arrow-free-data/arrow.free/-free/index.html

### 문법

- kotlin은 higher kinded type을 지원하지 않기 때문에 Kind<Type,A>을 쓰는데 @higherkind 애노테이션은 
  `타입명Of`와 `For타입명`을 자동으로 생성해준다.

``` kotlin
@higherkind sealed class KVStoreA<out A>: KVStoreAOf<A> {
    data class Put<T>(val key: String, val value: T) : KVStoreA<Unit>()
    data class Get<T>(val key: String) : KVStoreA<Option<T>>()
    data class Delete(val key: String) : KVStoreA<Unit>()
	
	...
}
```

### 리프팅 함수

- 리프팅 함수를 companion object 밖에 만들 수 도 있는데 안에 만들어서 쓰는 것이 보통인것 같다.

``` kotlin
@higherkind sealed class KVStoreA<out A>: KVStoreAOf<A> {
    ...
    companion object: FreeMonad<ForKVStoreA> {
        fun <T> put(key: String, value: T): Free<ForKVStoreA, Unit> = Free.liftF(Put(key, value))
        fun <T> get(key: String): Free<ForKVStoreA, Option<T>> = Free.liftF(Get(key))
        fun delete(key: String): Free<ForKVStoreA, Unit> = Free.liftF(Delete(key))
    }
}
```

### 프로그램

- `!`는 처음보는데 찾아봐야할것 같다.

``` kotlin
val program = KVStoreA.fx.monad {
        !KVStoreA.put("wild-cats", 2)
        !KVStoreA.put("tame-cats", 5)
        val n = !KVStoreA.get<Int>("wild-cats")
        !KVStoreA.delete("tame-cats")
        n
    }.fix()
```

### 인터프리터

- 역시 언어적 한계로 인해 다소 복잡하게 생겼지만 스칼라와 비교해보면 의미상으로는 같다.

``` kotlin
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
```

### 실행

``` kotlin
program.foldMap(compiler, Id.monad()).value()
```


## Hasekll

http://hackage.haskell.org/package/control-monad-free-0.6.2/docs/Control-Monad-Free.html

### 문법

- 쉽게 하려고 `Map String a` 대신 `Map String Int`를 썻다.
- 리턴 타입을 문법 타입의 마지막 타입으로 지정한다.
- next 타입변수가 있다. (context 전달을 위한 것인가?)

``` haskell
data KVStore next = Get' String (Maybe Int -> next)
                  | Put' String Int next
                  | Delete' String next
                    deriving (Functor)
```

### 리프팅 함수

- 자동으로 lift 함수를 만들어주는 `makefree` 함수가 있다.

``` haskell
makeFree ''KVStore
```

### 프로그램

``` haskell
program :: KVStoreM (Maybe Int)
program = do
  put' "wild-cats" 2
  put' "tame-cats" 5
  n <- get' "wild-cats"
  delete' "tome-cats"
  return n
```

### 인터프리터

- 하스켈은 순수하기 때문에 State Monad로 Map을 넘겨서 써야한다.

``` haskell
compiler :: MonadState (Map String Int) m => KVStoreM a -> m a
compiler = Free.iterM run
  where
    run (Get' k f) = f =<< gets (M.lookup k)
    run (Delete' k n) = do
      modify $ M.delete k
      n
    run (Put' k v n) = do
      modify $ M.insert k v
      n
```

### 실행

``` haskell
runState (compiler program) M.empty
```

## DSL의 가치

- OOP에서는 어떻게 할까? 추상 클래스에 문법을 추상 메서드로 정의하고 프로그램 역시 추상 메서드(run)로 만들고 상속하면 된다.
  하지만 DSL과 다른점은 추상 클래스 Code는 컴파일 시점에 미리 고정되지만 DSL은 런타임에 코드를 생성할 수 있어 유연하다.
  (런타임 최적화, 조건에 따른 코드 생성)
- Code is Data?
- (런타임 최적화, 조건에 따른 코드 생성)

## 어디에 적용하기 좋을까?

- Usecase?
- 서비스 레이어에 비즈니스 논리에 적용하면 좋을 것 같다. 테스트용 인터프리터 따로 만들어 테스트 할 수 있다.

## Lisp은?

- Lisp에서는 코드가 이미 데이터 구조(Homoiconicity)라서 DSL이 원래 코드와 다르지 않게 생겼다.
- 인터프리터 구현에 틀이 없기 때문에 항상 새로 만들어야 하지만 그리 어렵지 않을 것 같다. 
  뭔가 쉽게 구현할 수 있는 라이브러리가 있으면 좋을것 같기도하다.

## 더 고민해봐야 할 것?

- 재귀적이 ADT를 가지는 DSL을 어떻게 적용할까? 적용하는 것이 적합할까? (Lisp / JSON 파서)
- 대세는 Tagless Final이라는데 Tagless Final과 Free Monad는 완벽 호환이 가능한가? Free Monad로만 가능한 것이 있을까?
