package com.example.android.architecture.blueprints.todoapp.domain

import com.example.android.architecture.blueprints.todoapp.domain.repository.TasksRepository
import com.example.android.architecture.blueprints.todoapp.presentation.util.wrapEspressoIdlingResource

class ClearCompletedTasksUseCase(
    private val tasksRepository: TasksRepository
) {
    suspend operator fun invoke() {

        wrapEspressoIdlingResource {
            tasksRepository.clearCompletedTasks()
        }
    }
}