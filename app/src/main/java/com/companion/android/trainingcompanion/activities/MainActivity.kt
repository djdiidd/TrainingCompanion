package com.companion.android.trainingcompanion.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.companion.android.trainingcompanion.R
import com.companion.android.trainingcompanion.adapters.SimpleListAdapter
import com.companion.android.trainingcompanion.databinding.ActivityMainBinding
import com.companion.android.trainingcompanion.dialogs.*
import com.companion.android.trainingcompanion.fragments.ListFragment
import com.companion.android.trainingcompanion.fragments.MainFragment
import com.companion.android.trainingcompanion.models.SimpleListItem
import com.companion.android.trainingcompanion.objects.BreakNotificationMode
import com.companion.android.trainingcompanion.objects.Params
import com.companion.android.trainingcompanion.objects.Place
import com.companion.android.trainingcompanion.objects.WorkoutProcess
import com.companion.android.trainingcompanion.time_utils.*
import com.companion.android.trainingcompanion.viewmodels.WorkoutViewModel


// Константы для тегов фрагментов
const val TAG_MAIN_FRAGMENT = "main-fragment"
const val TAG_LIST_FRAGMENT = "list-fragment"

private const val STOPWATCH_IS_GOING = "stopwatch-is-going"
private const val STOPWATCH_REMAINING_TIME = "stopwatch-current-time"
private const val TIMER_IS_GOING = "timer-is-going"
private const val TIMER_REMAINING_TIME = "timer-remaining-time"
private const val TIMER_START_TIME = "timer-start-time"
private const val EXERCISE_TIME = "exercise-time"

private const val TAG_BODY_PARTS = "tag-body-parts"
private const val TAG_MUSCLES = "tag-muscles"

class MainActivity : AppCompatActivity(), ChangeRestTimeDialog.Callback, ChangePlaceDialog.Callback,
    WarningUnusedBPDialog.Callback, BreakNotificationDialog.Callback, CountDownTimer.Callback,
    MainFragment.FragmentCallback, SimpleListAdapter.Callback {

    // Инициализация объекта класса привязки данных
    private lateinit var binding: ActivityMainBinding

    // Инициализация объекта Intent для общего времени
    private lateinit var stopwatchSIntent: Intent

    // ViewModel для инкапсуляции некоторых данных времени
    private lateinit var stopwatch: Stopwatch

    // Инициализация объекта Intent для общего времени
    private lateinit var timerSIntent: Intent

    // ViewModel для инкапсуляции некоторых данных времени
    private lateinit var timer: CountDownTimer

    private lateinit var exerciseStopwatch: ExerciseStopwatch

    // Основная ViewModel, инкапсулирующая данные тренировки
    private val viewModel: WorkoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LF", "A onCreate")
        binding = DataBindingUtil // определяем привязку данных
            .setContentView(this, R.layout.activity_main)

        // Установка собственного toolbar 'а
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null // и удаление заголовка
        if (savedInstanceState == null)
            hideToolbarAndDisableSideMenu() // Скрываем таймер до начала тренировки

        // Инициализация интента для класса сервиса
        stopwatchSIntent = Intent(applicationContext, StopwatchService::class.java)
        stopwatch = Stopwatch(this, stopwatchSIntent)
        // Связывание объекта отправляющего обновленное время и получающего по интенту TIMER_UPDATED
        registerReceiver(stopwatch.newTimeReceiver, IntentFilter(StopwatchService.TIMER_UPDATED))


        //TODO: переложить ответственность на класс CountDownTimer.
        //Инициализация интента для класса сервиса
        timerSIntent = Intent(applicationContext, CountDownService::class.java)
        timer = CountDownTimer(this, timerSIntent)
        // Связывание объекта отправляющего обновленное время и получающего по интенту TIMER_UPDATED
        registerReceiver(timer.timeReceiver, IntentFilter(CountDownService.TIMER_UPDATED))

        exerciseStopwatch = ExerciseStopwatch(binding.generalClock)

        savedInstanceState?.apply { //TODO: сделать приоритет восстановлений
            timer.isGoing = getBoolean(TIMER_IS_GOING)
            timer.setTime(getInt(TIMER_REMAINING_TIME))
            timer.startTime = getInt(TIMER_START_TIME)
            stopwatch.setTime(getInt(STOPWATCH_REMAINING_TIME))
            stopwatch.setGoing(getBoolean(STOPWATCH_IS_GOING))
            binding.generalClock.text = stopwatch.getTimeInFormatHMMSS(stopwatch.getRemaining())
            exerciseStopwatch.time = getInt(EXERCISE_TIME)
            if (viewModel.activeProcess == WorkoutProcess.EXERCISE_STOPWATCH)
                exerciseStopwatch.`continue`()
        }

        if (stopwatch.isGoing()) {
            binding.pauseResumeButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.pauseResumeButton.setImageResource(R.drawable.ic_play)
        }

        // Запускаем стартовый фрагмент
        startMainFragment()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()
        Log.d("LF", "A onStart")
        (binding.fragmentContainer.background as AnimationDrawable).also {
            it.setEnterFadeDuration(2000)
            it.setExitFadeDuration(4000)
            it.start()
        }

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {
                //TODO: initialize submenus
            }

            override fun onDrawerClosed(drawerView: View) {
                //closeSideSubmenus//TODO
                drawerView.hideKeyboard()
            }

            override fun onDrawerStateChanged(newState: Int) {}

        })

        binding.sideMenuPlace.setOnClickListener {
            when (binding.sideMenuDynamicPlace.isVisible) {
                false -> {
                    binding.sideMenuDynamicBodyParts.visibility = View.GONE
                    binding.sideMenuDynamicMuscles.visibility = View.GONE
                    if (binding.sideMenuDynamicRestTime.visibility == View.VISIBLE) {
                        binding.sideMenuDynamicRestTime.visibility = View.GONE
                        it.hideKeyboard()
                    }
                    binding.sideMenuDynamicBreakMode.visibility = View.GONE
                    binding.sideMenuDynamicPlace.visibility = View.VISIBLE
                }
                else -> binding.sideMenuDynamicPlace.visibility = View.GONE

            }

        }
        binding.sideMenuBodyparts.setOnClickListener {
            binding.sideMenuDynamicBodyParts.visibility =
                if (binding.sideMenuDynamicBodyParts.isVisible) View.GONE
                else {
                    fillListOfBPInSideMenu(binding.sideMenuDynamicBodyParts)
                    binding.sideMenuDynamicPlace.visibility = View.GONE
                    binding.sideMenuDynamicMuscles.visibility = View.GONE
                    binding.sideMenuDynamicBreakMode.visibility = View.GONE
                    if (binding.sideMenuDynamicRestTime.visibility == View.VISIBLE) {
                        binding.sideMenuDynamicRestTime.visibility = View.GONE
                        it.hideKeyboard()
                    }
                    View.VISIBLE
                }
        }
        binding.sideMenuMuscles.setOnClickListener {

            fillListOfMusclesInSideMenu(binding.sideMenuDynamicMuscles)

            binding.sideMenuDynamicMuscles.visibility =
                if (binding.sideMenuDynamicMuscles.isVisible) View.GONE
                else {
                    binding.sideMenuDynamicPlace.visibility = View.GONE
                    binding.sideMenuDynamicBodyParts.visibility = View.GONE
                    binding.sideMenuDynamicBreakMode.visibility = View.GONE
                    if (binding.sideMenuDynamicRestTime.visibility == View.VISIBLE) {
                        binding.sideMenuDynamicRestTime.visibility = View.GONE
                        it.hideKeyboard()
                    }
                    View.VISIBLE
                }
        }

        binding.sideMenuTime.setOnClickListener {
            binding.sideMenuDynamicRestTime.visibility =
                if (binding.sideMenuDynamicRestTime.isVisible) View.GONE
                else {
                    binding.sideMenuCurrentRestTime.text =
                        getString(R.string.selected_seconds, viewModel.restTime.value!!)
                    binding.sideMenuRestTimeAcceptButton.setImageResource(R.drawable.ic_done_disabled)
                    binding.sideMenuRestTimeAcceptButton.isEnabled = false
                    binding.sideMenuRestTimeAcceptButton.visibility = View.GONE
                    binding.sideMenuDynamicPlace.visibility = View.GONE
                    binding.sideMenuDynamicBodyParts.visibility = View.GONE
                    binding.sideMenuDynamicMuscles.visibility = View.GONE
                    binding.sideMenuDynamicBreakMode.visibility = View.GONE
                    View.VISIBLE
                }

        }
        binding.sideMenuNotificationMode.setOnClickListener {
            binding.sideMenuDynamicBreakMode.visibility =
                if (binding.sideMenuDynamicBreakMode.isVisible) View.GONE
                else {
                    if (!binding.sideMenuDynamicBreakMode.isVisible) {
                        binding.sideMenuDynamicBreakMode.visibility = View.VISIBLE
                        binding.sideMenuDynamicPlace.visibility = View.GONE
                        binding.sideMenuDynamicBodyParts.visibility = View.GONE
                        binding.sideMenuDynamicMuscles.visibility = View.GONE
                        if (binding.sideMenuDynamicRestTime.visibility == View.VISIBLE) {
                            binding.sideMenuDynamicRestTime.visibility = View.GONE
                            binding.sideMenuDynamicBreakMode.hideKeyboard()
                        }
                    }
                    View.VISIBLE
                }
        }


        //submenu

        binding.subMenuDynamicPlaceHome.setOnClickListener {
            viewModel.trainingPlace = Place.TRAINING_AT_HOME
            binding.sideMenuDynamicPlace.visibility = View.GONE
        }
        binding.subMenuDynamicPlaceGym.setOnClickListener {
            viewModel.trainingPlace = Place.TRAINING_IN_GYM
            binding.sideMenuDynamicPlace.visibility = View.GONE
        }
        binding.subMenuDynamicPlaceOutdoors.setOnClickListener {
            viewModel.trainingPlace = Place.TRAINING_OUTDOORS
            binding.sideMenuDynamicPlace.visibility = View.GONE
        }

        binding.subMenuDynamicSound.setOnClickListener {
            viewModel.breakNotificationMode = BreakNotificationMode.SOUND
            binding.sideMenuDynamicBreakMode.visibility = View.GONE
        }
        binding.subMenuDynamicVibration.setOnClickListener {
            viewModel.breakNotificationMode = BreakNotificationMode.VIBRATION
            binding.sideMenuDynamicBreakMode.visibility = View.GONE
        }
        binding.subMenuDynamicAnim.setOnClickListener {
            viewModel.breakNotificationMode = BreakNotificationMode.ANIMATION
            binding.sideMenuDynamicBreakMode.visibility = View.GONE
        }

        binding.sideMenuInputRestTime.setOnTouchListener { v, _ ->
            binding.sideMenuRestTimeAcceptButton.visibility = View.VISIBLE
            binding.sideMenuInputRestTime.onDone {
                val seconds = binding.sideMenuInputRestTime.text.toString().toInt()
                if (seconds in Params.restTimeAdvRange) {
                    viewModel.restTime.value = seconds
                    binding.sideMenuCurrentRestTime.text = getString(R.string.selected_seconds, seconds)
                    binding.sideMenuInputRestTime.text = null
                    binding.sideMenuRestTimeAcceptButton.visibility = View.GONE
                }
            }
            v.performClick()
        }

        binding.add5s.setOnClickListener {
            if (viewModel.restTime.value!! >= Params.restTimeAdvRange.last)
                return@setOnClickListener
            viewModel.restTime.value = viewModel.restTime.value?.plus(5).also {
                binding.sideMenuCurrentRestTime.text = getString(R.string.selected_seconds, it)
            }

        }

        binding.sub5s.setOnClickListener {
            if (viewModel.restTime.value!! <= Params.restTimeDefaultRange.first)
                return@setOnClickListener
            viewModel.restTime.value = viewModel.restTime.value?.minus(5).also {
                binding.sideMenuCurrentRestTime.text = getString(R.string.selected_seconds, it)
            }
        }

        binding.sideMenuInputRestTime.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(chars: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (!chars.isNullOrEmpty() && chars.toString().toInt() in Params.restTimeAdvRange) {
                    binding.sideMenuRestTimeAcceptButton.setImageResource(R.drawable.ic_done)
                    binding.sideMenuRestTimeAcceptButton.isEnabled = true
                } else {
                    binding.sideMenuRestTimeAcceptButton.setImageResource(R.drawable.ic_done_disabled)
                    binding.sideMenuRestTimeAcceptButton.isEnabled = false
                }
            }

            override fun afterTextChanged(p0: Editable?) {}

        })

        binding.sideMenuRestTimeAcceptButton.setOnClickListener {
            val value = binding.sideMenuInputRestTime.text.toString().toInt()
            viewModel.restTime.value = value
            binding.sideMenuCurrentRestTime.text = getString(R.string.selected_seconds, value)
            binding.sideMenuInputRestTime.text = null
            it.visibility = View.GONE
            it.hideKeyboard()
        }



        //DEPRECATED
        /** Слушатель нажатия для элементов бокового меню (с изменениями параметров) */
        binding.sideNavigationView.setNavigationItemSelectedListener {

            when (it.itemId) {
                // Изменение списка выбранных частей тела
                R.id.item_body_parts -> {
                    // По ключу SELECT_BODY_PART_DIALOG_TAG
                    supportFragmentManager.setFragmentResultListener(
                        SELECT_BODY_PART_DIALOG, this
                    ) { _, bundle ->
                        // В переменную numbersOfSelectedBodyParts записываем arrayList
                        // полученный из объекта Bundle по ключу BODY_PART_LIST_KEY
                        val whichBPIsSelected = bundle.getBooleanArray(LIST_BUNDLE_TAG)
                        // Если полученный список не изменился, то перезаписывать данные не будем
                        if (!viewModel.getWhichBPsAreSelected().toBooleanArray()
                                .contentEquals(whichBPIsSelected)
                        ) {
                            viewModel.updateData(this, whichBPIsSelected!!.toTypedArray())
                            // Запуск диалогового окна с выбором мышц
                            MultiChoiceDialog(
                                viewModel.getAvailableMuscles(this),
                                viewModel.getWhichMusclesAreSelected().toBooleanArray()
                            ).show(supportFragmentManager, SELECT_MUSCLE_DIALOG)
                        }
                    }
                    // Запуск диалогового окна с выбором частей тела
                    MultiChoiceDialog(
                        viewModel.getAllBP(this),
                        viewModel.getWhichBPsAreSelected().toBooleanArray()
                    ).show(supportFragmentManager, SELECT_BODY_PART_DIALOG)
                }
                // Изменение списка выбранных мышц
                R.id.item_muscles -> {
                    supportFragmentManager
                        .setFragmentResultListener(SELECT_MUSCLE_DIALOG, this) { _, bundle ->
                            val numbersOfSelectedItems = bundle.getBooleanArray(LIST_BUNDLE_TAG)
                            viewModel.saveSelectedMuscles(numbersOfSelectedItems!!.toTypedArray())
                        }
                    // Запуск диалогового окна с выбором мышц
                    MultiChoiceDialog(
                        viewModel.getAvailableMuscles(this),
                        viewModel.getWhichMusclesAreSelected().toBooleanArray()
                    ).show(supportFragmentManager, SELECT_MUSCLE_DIALOG)
                }
                // Изменение времени отдыха между подходами
                R.id.item_rest_time -> {
                    ChangeRestTimeDialog(
                        viewModel.restTime.value!!
                    ).show(supportFragmentManager, "")
                }
                R.id.item_place -> {
                    ChangePlaceDialog(viewModel.trainingPlace)
                        .show(supportFragmentManager, "")
                }
                R.id.item_switch_mute -> {
                    BreakNotificationDialog(viewModel.breakNotificationMode)
                        .show(supportFragmentManager, "")
                }
            }
            true
        }

        // Анимации для нажатий
        val animPressed = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        val animUnpressed = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)


        /** Слушатель для Bottom Navigation View */
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Если выбираем кнопку для стартовой страницы,
                // то запускаем соответствующий объект
                R.id.bottom_main -> {  // Передаем bundle со списком
                    val mainFragment = MainFragment()
                    launchFragment(mainFragment, TAG_MAIN_FRAGMENT) // запускаем фрагмент
                    true
                }
                // Если выбираем кнопку со списком,
                // то запускаем соответствующий объект
                R.id.bottom_list -> {
                    val listFragment = ListFragment()
                    launchFragment(listFragment, TAG_LIST_FRAGMENT) // запускаем фрагмент
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigationView.setOnItemReselectedListener { item ->
            when (item.itemId) {
                R.id.bottom_main -> {
                    // do if reselected
                }
                R.id.bottom_list -> {
                    // do if reselected
                }
            }
        }
        /** Слушатель _нажатия_ для кнопки паузы/продолжения общего времени на Toolbar*/
        binding.pauseResumeButton.setOnClickListener {
            it.isClickable = false
            stopwatch.startOrStop()
            Handler(mainLooper).postDelayed({
                it.isClickable = true
            }, 1000)


        }
        /** Слушатель _касания_ для кнопки паузы/продолжения общего времени на Toolbar */
        binding.pauseResumeButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> view.startAnimation(animPressed)
                MotionEvent.ACTION_UP -> view.startAnimation(animUnpressed)
            }
            false
        }
        /** Слушатель _нажатия_ для кнопки опций/настроек на Toolbar */
        binding.optionsButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
        /** Слушатель _касания_ для кнопки опций/настроек на Toolbar */
        binding.optionsButton.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> view.startAnimation(animPressed)
                MotionEvent.ACTION_UP -> view.startAnimation(animUnpressed)
            }
            false
        }

        /** Отслеживание начала тренировки */
        viewModel.workoutSuccessfullyStarted.observe(this) { workoutStarted ->
            if (!workoutStarted || viewModel.workoutInProgress) {
                return@observe
            }

            timer.attachUI(
                findViewById(R.id.set_timer),
                findViewById(R.id.set_timer_progress)
            )

            viewModel.activeProcess = WorkoutProcess.TIMER
            Log.d("MyTag", "Started from main")
            showToolbarAndActivateSideMenu()
            stopwatch.startOrStop()
            timer.startOrStop()
            // Устанавливаем соответствующую иконку
            updateWorkoutPlaceIcon()
            viewModel.workoutInProgress = true
        }

        viewModel.restTime.observe(this) {
            if (viewModel.activeProcess != WorkoutProcess.TIMER) //TODO: протестировать условие
                timer.setTime(viewModel.restTime.value!! + 1) // На 1 больше, чтобы отсчет начинался с нужного числа
            Log.d("MyTag", "restTime")
        }

        /** Отслеживание нажатия на закрытие бокового меню */
        binding.sideNavigationView
            .getHeaderView(0)
            .findViewById<ImageView>(R.id.close_nv_button)
            .setOnClickListener {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            }
    }

    private fun EditText.onDone(callback: () -> Unit) {
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                callback.invoke()
                hideKeyboard()
            }
            false
        }
    }

    private fun View.hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(this.windowToken, 0)
    }

    /**
     * Функция для запуска переданного фрагмента по тегу:
     * тег для стартового фрагмента, и тег для фрагмента со списком
     */
    private fun launchFragment(fragment: Fragment, tag: String) {
        // Находим фрагмент по заданному тегу
        val currentFragment = supportFragmentManager
            .findFragmentByTag(tag)
        // Если фрагмент с данным тегом не установлен, то создаем
        if (currentFragment == null) {
            supportFragmentManager
                .beginTransaction() // Начинаем транзакцию
                .replace(R.id.fragment_container, fragment, tag) // Заменяем фрагмент
                .commit() // Закрепляем процесс
        }
    }

    /**
     * Функция для запуска стартового фрагмента
     */
    private fun startMainFragment() {
        val fragment = MainFragment() // Запускаем фрагмент
        launchFragment(fragment, TAG_MAIN_FRAGMENT)
    }

    private fun TextView.addError(stringRes: Int, drawableRes: Int) {
        val drawable = AppCompatResources.getDrawable(this@MainActivity, drawableRes)
        drawable?.setBounds(0, 0, textSize.toInt() + 15, textSize.toInt() + 15)
        setError(getString(stringRes), drawable)
    }

    private fun hideToolbarAndDisableSideMenu() {
        supportActionBar?.hide()
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    private fun showToolbarAndActivateSideMenu() {
        supportActionBar?.show()
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    private fun updateWorkoutPlaceIcon() {

        binding.sideMenuPlaceIcon.setImageResource(
            when (viewModel.trainingPlace) {
                Place.TRAINING_AT_HOME -> R.drawable.ic_home
                Place.TRAINING_IN_GYM -> R.drawable.ic_gym
                else -> R.drawable.ic_outdoors
            }
        )
    }

    private fun fillListOfBPInSideMenu(recyclerView: RecyclerView) {
        val arrayList = arrayListOf<SimpleListItem>()
        val stringArray = viewModel.getAllBP(this)
        val boolArray = viewModel.getWhichBPsAreSelected()
        for (i in 0 until Params.numberOfBodyParts) {
            arrayList.add(
                SimpleListItem(
                    stringArray[i], boolArray[i]
                )
            )
        }
        recyclerView.adapter = SimpleListAdapter(this, TAG_BODY_PARTS).also {
            it.submitList(arrayList)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun fillListOfMusclesInSideMenu(recyclerView: RecyclerView) {
        val sizeOfMuscles = viewModel.getAvailableMuscles(this).size
        val availableMuscles = viewModel.getAvailableMuscles(this)
        val whichAreSelected = viewModel.getWhichMusclesAreSelected()
        val arrayList = arrayListOf<SimpleListItem>()
        for (i in 0 until sizeOfMuscles) {
            arrayList.add(i, SimpleListItem(availableMuscles[i], whichAreSelected[i]))
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SimpleListAdapter(this, TAG_MUSCLES).also {
            it.submitList(arrayList)
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("LF", "A onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(stopwatch.newTimeReceiver)
        unregisterReceiver(timer.timeReceiver)
        Log.d("LF", "A onDestroy")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("LF", "A onSaveInstanceState")
        outState.apply {
            putBoolean(STOPWATCH_IS_GOING, stopwatch.isGoing())
            putInt(STOPWATCH_REMAINING_TIME, stopwatch.getRemaining())
            putInt(TIMER_REMAINING_TIME, timer.getTime())
            putBoolean(TIMER_IS_GOING, timer.isGoing)
            putInt(TIMER_START_TIME, timer.startTime)
            if (viewModel.activeProcess == WorkoutProcess.EXERCISE_STOPWATCH)
                putInt(EXERCISE_TIME, exerciseStopwatch.time)
        }
        super.onSaveInstanceState(outState)
    }

    /* Интерфейс */
    /** Сохранение обновленного значения времени */
    override fun newRestTimeSelected(time: Int) {
        viewModel.restTime.value = time
    }

    /* Интерфейс */
    /** Сохранение обновленного места тренировки */
    override fun newWorkoutPlaceSelected(place: Int) {
        viewModel.trainingPlace = place
        updateWorkoutPlaceIcon()
    }

    /* Интерфейс */
    /** Установка false на неиспользуемыех значениях частей тела */
    override fun unusedBodyPartsRemoved(whichAreUnusedBP: Array<Boolean>) {
        val current = viewModel.getWhichBPsAreSelected()
        for (i in 0 until 5) {
            if (whichAreUnusedBP[i]) {
                current[i] = false
            }
        }
        viewModel.updateData(this, current)
    }

    override fun newBreakNotificationModeSelected(mode: Int) {
        viewModel.breakNotificationMode = mode
    }

    override fun timerFinished() {
        exerciseStopwatch.start()
        viewModel.activeProcess = WorkoutProcess.EXERCISE_STOPWATCH
        exerciseStopwatch.attachUI(
            binding.generalClock,
            findViewById(R.id.set_timer),
            findViewById(R.id.set_timer_progress)
        )
    }

    override fun mainButtonClicked() {
        when (viewModel.activeProcess) {
            WorkoutProcess.TIMER -> {
                timer.startOrStop()
            }
            WorkoutProcess.EXERCISE_STOPWATCH -> {
                //TODO: Запустить диалоговое окно, в котором будет выбор того, что ты потренил\
                // Если Пользователь выбрал закончить тренировку, то перебросить его на результаты и изменить viewModel.activeProcess
                exerciseStopwatch.stop()
                viewModel.activeProcess = WorkoutProcess.TIMER

                timer.setDefaults(viewModel.restTime.value!!)


                registerReceiver(timer.timeReceiver, IntentFilter(CountDownService.TIMER_UPDATED))
                timer.attachUI( // STOPSHIP: TESTING
                    findViewById(R.id.set_timer),
                    findViewById(R.id.set_timer_progress)
                )
                timer.startOrStop()
            }
            WorkoutProcess.WORKOUT_PAUSED -> {
                //TODO: не определено.
            }
        }
    }

    override fun fragmentDestroyed() {
        timer.detachUI()
    }

    override fun fragmentUICreated(textView: TextView, progressBar: ProgressBar) {
        Log.d("LF", "A fragmentUICreated")
        if (viewModel.activeProcess == WorkoutProcess.TIMER) {
            timer.attachUI(textView, progressBar)
        } else if (viewModel.activeProcess == WorkoutProcess.EXERCISE_STOPWATCH)
            exerciseStopwatch.attachUI(binding.generalClock, textView, progressBar)
    }

    override fun sideMenuListItemSelected(position: Int, tag: String) {
        if (tag == TAG_MUSCLES) {
            viewModel.saveSelectedMuscles(
                viewModel.getWhichMusclesAreSelected().also {
                    it[position] = !it[position]
                }
            )
        } else {
            viewModel.updateData(
                this, viewModel.getWhichBPsAreSelected().also {
                    it[position] = !it[position]
                }
            )
        }
    }
}

