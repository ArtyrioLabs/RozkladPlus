package com.mkr.schedule.data.api

import com.mkr.schedule.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface MkrApiService {
    
    @POST("login")
    suspend fun login(@Body loginForm: LoginForm): Response<AccessToken>
    
    @GET("user/info")
    suspend fun getUserInfo(
        @Header("Authorization") token: String
    ): Response<User>
    
    @GET("student/info")
    suspend fun getStudentInfo(
        @Header("Authorization") token: String
    ): Response<StudentData>
    
    @GET("student/semester-list")
    suspend fun getStudentSemesterList(
        @Header("Authorization") token: String,
        @Query("onlyActive") onlyActive: Boolean? = null
    ): Response<List<Semester>>
    
    @POST("time-table/student")
    suspend fun getStudentTimeTable(
        @Header("Authorization") token: String,
        @Body form: StudentTimeTableForm
    ): Response<List<TimeTableDate>>
    
    @POST("time-table/group")
    suspend fun getGroupTimeTable(
        @Header("Authorization") token: String,
        @Body form: GroupTimeTableForm
    ): Response<List<TimeTableDate>>
    
    @POST("time-table/call-schedule")
    suspend fun getCallSchedule(
        @Header("Authorization") token: String
    ): Response<List<CallSchedule>>
    
    @POST("list/groups")
    suspend fun getGroups(
        @Header("Authorization") token: String,
        @Body form: GroupsFilterForm
    ): Response<List<Group>>
    
    @POST("list/faculties")
    suspend fun getFaculties(
        @Header("Authorization") token: String,
        @Body form: FacultiesFilterForm
    ): Response<List<Faculty>>
    
    @GET("list/structures")
    suspend fun getStructures(
        @Header("Authorization") token: String
    ): Response<List<Structure>>
    
    // Public endpoints that do not require student-level authorization
    @POST("list/courses")
    suspend fun getCourses(
        @Body form: CoursesFilterForm
    ): Response<List<Course>>
    
    @POST("list/students-by-group")
    suspend fun getStudentsByGroup(
        @Header("Authorization") token: String? = null,
        @Body form: StudentsByGroupFilterForm
    ): Response<List<Student>>
    
    @POST("time-table/student")
    suspend fun getStudentTimeTablePublic(
        @Header("Authorization") token: String? = null,
        @Body form: StudentTimeTableForm
    ): Response<List<TimeTableDate>>

    // Подтягиваем объявление к паре (оттуда достаём ссылки на онлайн-конференции)
    @POST("time-table/schedule-ad")
    suspend fun getScheduleAd(
        @Header("Authorization") token: String? = null,
        @Body form: ScheduleAdForm
    ): Response<ScheduleAdResponse>
}

