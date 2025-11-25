package com.docuwallet.app.data.remote.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val totalDocuments: Int = 0,
    val storageUsedMB: Double = 0.0
)

class FirebaseUserService {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun createUserProfile(uid: String, email: String, name: String): Result<Unit> {
        return try {
            val userProfile = UserProfile(
                uid = uid,
                email = email,
                name = name
            )
            usersCollection.document(uid).set(userProfile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val profile = snapshot.toObject(UserProfile::class.java)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(uid: String, name: String, photoUrl: String): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "photoUrl" to photoUrl
            )
            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDocumentCount(uid: String, count: Int): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update("totalDocuments", count)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStorageUsed(uid: String, storageMB: Double): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update("storageUsedMB", storageMB)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserProfile(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}