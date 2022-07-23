package com.companion.android.workoutcompanion.interfaces

import android.view.View

interface TimeCounting {

    var isGoing: Boolean  // Идет ли счет времени;
    var isPaused: Boolean // Приостановлен ли процесс отсчета;

    fun enable(vararg views: View) // Активация объекта / подготовка к работе / возобновление механизмов;
    fun cancel(animate: Boolean)   // Деактивация объекта / очистка;

    fun start()  // Запуск/возобновление отсчета;
    fun pause()  // Временная остановка объекта с сохранением состояния;
    fun stop()   // Полная остановка механизмов отсчета;

}