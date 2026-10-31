package com.mkr.schedule.data.model

import com.google.gson.annotations.SerializedName

data class Group(
    val id: Int,
    val name: String,
    val course: Int,
    val priority: Int,
    @SerializedName("educationForm") val educationForm: Int
)

data class Faculty(
    val id: Int,
    @SerializedName("shortName") val shortName: String?,
    @SerializedName("fullName") val fullName: String?
)

data class Structure(
    val id: Int,
    @SerializedName("shortName") val shortName: String?,
    @SerializedName("fullName") val fullName: String?
)

data class GroupsFilterForm(
    @SerializedName("facultyId") val facultyId: Int,
    val course: Int? = null
)

data class FacultiesFilterForm(
    @SerializedName("structureId") val structureId: Int? = null
)

data class Course(
    val course: Int
)

data class CoursesFilterForm(
    @SerializedName("facultyId") val facultyId: Int
)

