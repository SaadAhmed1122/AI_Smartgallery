package com.ai.smartgallery.data.local.dao

import androidx.room.*
import com.ai.smartgallery.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for accessing person data (face grouping)
 */
@Dao
interface PersonDao {

    @Query("SELECT * FROM persons ORDER BY face_count DESC")
    fun getAllPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE id = :personId")
    suspend fun getPersonById(personId: Long): PersonEntity?

    @Query("SELECT * FROM persons WHERE name IS NOT NULL ORDER BY name ASC")
    fun getNamedPersons(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE name IS NULL ORDER BY face_count DESC")
    fun getUnnamedPersons(): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("UPDATE persons SET face_count = :count WHERE id = :personId")
    suspend fun updateFaceCount(personId: Long, count: Int)

    @Query("UPDATE persons SET name = :name WHERE id = :personId")
    suspend fun updatePersonName(personId: Long, name: String)
}
