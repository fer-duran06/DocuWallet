package com.docuwallet.app.data.repository

import com.docuwallet.app.data.remote.firebase.FirebaseAuthService
import com.docuwallet.app.data.remote.firebase.FirebaseUserService
import com.docuwallet.app.domain.model.User
import com.docuwallet.app.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AuthRepository(
    private val authService: FirebaseAuthService = FirebaseAuthService(),
    private val userService: FirebaseUserService = FirebaseUserService()
) {

    fun login(email: String, password: String): Flow<Resource<User>> = flow {
        try {
            emit(Resource.Loading())

            val result = authService.login(email, password)

            result.onSuccess { firebaseUser ->
                val userProfile = userService.getUserProfile(firebaseUser.uid)

                userProfile.onSuccess { profile ->
                    if (profile != null) {
                        val user = User(
                            uid = profile.uid,
                            email = profile.email,
                            name = profile.name,
                            photoUrl = profile.photoUrl,
                            createdAt = profile.createdAt,
                            totalDocuments = profile.totalDocuments,
                            storageUsedMB = profile.storageUsedMB
                        )
                        emit(Resource.Success(user))
                    } else {
                        emit(Resource.Error("Perfil de usuario no encontrado"))
                    }
                }.onFailure { e ->
                    emit(Resource.Error(e.message ?: "Error al obtener perfil"))
                }
            }.onFailure { e ->
                emit(Resource.Error(e.message ?: "Error al iniciar sesión"))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    fun register(email: String, password: String, name: String): Flow<Resource<User>> = flow {
        try {
            emit(Resource.Loading())

            val result = authService.register(email, password, name)

            result.onSuccess { firebaseUser ->
                val createProfile = userService.createUserProfile(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: email,
                    name = name
                )

                createProfile.onSuccess {
                    val user = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: email,
                        name = name
                    )
                    emit(Resource.Success(user))
                }.onFailure { e ->
                    emit(Resource.Error(e.message ?: "Error al crear perfil"))
                }
            }.onFailure { e ->
                emit(Resource.Error(e.message ?: "Error al registrar usuario"))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }

    fun logout() {
        authService.logout()
    }

    fun getCurrentUser(): User? {
        val firebaseUser = authService.getCurrentUser()
        return if (firebaseUser != null) {
            User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: ""
            )
        } else {
            null
        }
    }

    fun isUserLoggedIn(): Boolean {
        return authService.isUserLoggedIn()
    }

    fun resetPassword(email: String): Flow<Resource<Unit>> = flow {
        try {
            emit(Resource.Loading())

            val result = authService.resetPassword(email)

            result.onSuccess {
                emit(Resource.Success(Unit))
            }.onFailure { e ->
                emit(Resource.Error(e.message ?: "Error al enviar correo"))
            }

        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error desconocido"))
        }
    }
}