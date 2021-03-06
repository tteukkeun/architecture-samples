/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.presentation.ui.taskdetail

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.architecture.blueprints.todoapp.Event
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.domain.utils.Result
import com.example.android.architecture.blueprints.todoapp.domain.utils.Result.Success
import com.example.android.architecture.blueprints.todoapp.data.source.local.TaskModel
import com.example.android.architecture.blueprints.todoapp.domain.ActivateTaskUseCase
import com.example.android.architecture.blueprints.todoapp.domain.CompleteTaskUseCase
import com.example.android.architecture.blueprints.todoapp.domain.DeleteTaskUseCase
import com.example.android.architecture.blueprints.todoapp.domain.GetTaskUseCase
import com.example.android.architecture.blueprints.todoapp.domain.entity.Task
import com.example.android.architecture.blueprints.todoapp.presentation.entity.PresenterEntity
import com.example.android.architecture.blueprints.todoapp.presentation.mapper.PresenterEntityMapper
import com.example.android.architecture.blueprints.todoapp.presentation.mapper.PresenterListMapper
import com.example.android.architecture.blueprints.todoapp.presentation.util.wrapEspressoIdlingResource
import kotlinx.coroutines.launch

/**
 * ViewModel for the Details screen.
 */
class TaskDetailViewModel(
    private val getTaskUseCase: GetTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val activateTaskUseCase: ActivateTaskUseCase

) : ViewModel() {

    private val presenterListMapper = PresenterListMapper()
    private val presenterEntityMapper = PresenterEntityMapper()

    private val _task = MutableLiveData<PresenterEntity>()
    val taskModel: LiveData<PresenterEntity> = _task

    private val _isDataAvailable = MutableLiveData<Boolean>()
    val isDataAvailable: LiveData<Boolean> = _isDataAvailable

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _editTaskEvent = MutableLiveData<Event<Unit>>()
    val editTaskEvent: LiveData<Event<Unit>> = _editTaskEvent

    private val _deleteTaskEvent = MutableLiveData<Event<Unit>>()
    val deleteTaskEvent: LiveData<Event<Unit>> = _deleteTaskEvent

    private val _snackbarText = MutableLiveData<Event<Int>>()
    val snackbarText: LiveData<Event<Int>> = _snackbarText

    private val taskId: String?
        get() = _task.value?.entryid

    // This LiveData depends on another so we can use a transformation.
    val completed: LiveData<Boolean> = Transformations.map(_task) { input: PresenterEntity? ->
        input?.completed ?: false
    }


    fun deleteTask() = viewModelScope.launch {
        taskId?.let {
            deleteTaskUseCase(it)
            _deleteTaskEvent.value = Event(Unit)
        }
    }

    fun editTask() {
        _editTaskEvent.value = Event(Unit)
    }

    fun setCompleted(completed: Boolean) = viewModelScope.launch {
        val task = _task.value ?: return@launch
        if (completed) {
            completeTaskUseCase(presenterEntityMapper.toEntity(task))
            showSnackbarMessage(R.string.task_marked_complete)
        } else {
            activateTaskUseCase(presenterEntityMapper.toEntity(task))
            showSnackbarMessage(R.string.task_marked_active)
        }
    }

    fun start(taskId: String?, forceRefresh: Boolean = false) {
        if (_isDataAvailable.value == true && !forceRefresh || _dataLoading.value == true) {
            return
        }

        // Show loading indicator
        _dataLoading.value = true

        wrapEspressoIdlingResource {
            viewModelScope.launch {
                if (taskId != null) {
                    getTaskUseCase(taskId, false).let { result ->
                        if (result is Success) {
                            onTaskLoaded(presenterEntityMapper.toView(result.data))
                        } else {
                            onDataNotAvailable(presenterEntityMapper.toView(result as Task))
                        }
                    }
                }
                _dataLoading.value = false
            }
        }
    }

    private fun setTask(tasks: PresenterEntity?) {
        this._task.value = tasks
        _isDataAvailable.value = taskModel != null
    }

    private fun onTaskLoaded(tasks: PresenterEntity) {
        setTask(tasks)
    }

    private fun onDataNotAvailable(result: PresenterEntity) {
        _task.value = null
        _isDataAvailable.value = false
    }

    fun refresh() {
        taskId?.let { start(it, true) }
    }

    private fun showSnackbarMessage(@StringRes message: Int) {
        _snackbarText.value = Event(message)
    }
}
