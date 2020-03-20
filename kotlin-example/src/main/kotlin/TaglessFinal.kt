import arrow.Kind
import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import java.util.*
import java.util.concurrent.Future

data class User(val id: UUID, val loyaltyPoints: Int) {
    fun copy(points: Int): User = this
}

interface UserRepositoryAlg<F> {
    fun findUser(id: UUID): Kind<F, Option<User>>
    fun updateUser(u: User): Kind<F, Unit>
}

class LoyaltyPoints<F>(val ur: UserRepositoryAlg<F>) {
    fun addPoints(userId: UUID, pointsToAdd: Int): Kind<F, Either<String, Unit>> =
        ur.findUser(userId).flatMap {
            when (it) {
                is None -> Monad.pure(Left("User not found"))
                is Some -> {
                    val updated = user.copy(user.loyaltyPoints + pointsToAdd)
                    ur.updateUser(updated).map { Right(Unit) }
                }
            }
        }
    }
}

class FutureInterpreter: UserRepositoryAlg<Future> {
    override fun findUser(id: UUID): Future<Option<User>> {
        return Future.successful(None)
    }

    override fun updateUser(u: User): Future<Unit> {
        return Future.successful(None)
    }
}

fun main() {
    val result: Future<Either<String, Unit>> = LoyaltyPoints(FutureInterpreter()).addPoints(UUID.randomUUID(), 10)
}