package com.companion.android.workoutcompanion.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.Transformation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.adapters.SimpleListAdapter
import com.companion.android.workoutcompanion.databinding.ActivityMainBinding
import com.companion.android.workoutcompanion.dialogs.WarningUnusedBPDialog
import com.companion.android.workoutcompanion.fragments.ListFragment
import com.companion.android.workoutcompanion.fragments.MainFragment
import com.companion.android.workoutcompanion.models.SimpleListItem
import com.companion.android.workoutcompanion.objects.BreakNotifyingMode
import com.companion.android.workoutcompanion.objects.WorkoutParams
import com.companion.android.workoutcompanion.objects.Place
import com.companion.android.workoutcompanion.objects.WorkoutProcess
import com.companion.android.workoutcompanion.timeutils.CountDownTimer
import com.companion.android.workoutcompanion.timeutils.ExerciseStopwatch
import com.companion.android.workoutcompanion.timeutils.Stopwatch
import com.companion.android.workoutcompanion.viewmodels.WorkoutViewModel


// [ Константы ]
// Теги для фрагментов
const val TAG_MAIN_FRAGMENT = "main-fragment"
const val TAG_LIST_FRAGMENT = "list-fragment"

// Ключи для восстановления данных секундомера всей тренировки
private const val STOPWATCH_IS_GOING = "stopwatch-is-going"
private const val STOPWATCH_REMAINING_TIME = "stopwatch-current-time"

// Ключи для восстановления данных таймера
private const val TIMER_IS_GOING = "timer-is-going"
private const val TIMER_REMAINING_TIME = "timer-remaining-time"
private const val TIMER_START_TIME = "timer-start-time"
private const val EXERCISE_TIME = "exercise-time"

// Ключи для восстановления иных данных
private const val TAG_OF_LAST_RUN_FRAGMENT = "last-run-fragment"

// Теги для бокового меню
private const val TAG_BODY_PARTS = "tag-body-parts"
private const val TAG_MUSCLES = "tag-muscles"


class MainActivity : AppCompatActivity(),
    WarningUnusedBPDialog.Callback, CountDownTimer.Callback,
    MainFragment.FragmentCallback, SimpleListAdapter.Callback {


//---------------------------------------------------------------[ Данные ]-------------------------

    // Инициализация объекта класса привязки данных
    private lateinit var binding: ActivityMainBinding

    // Объект инкапсулирующий дынные секундомера
    private lateinit var stopwatch: Stopwatch

    // Объект инкапсулирующий дынные таймера
    private lateinit var timer: CountDownTimer

    // Секундомер, служащий для отображения времени выполнения упражнения
    private lateinit var exerciseStopwatch: ExerciseStopwatch

    // Открытое в текущий момент подменю NavigationView
    private var openedSubmenu: View? = null

    // Последний запущенный фрагмент
    private var lastRunFragmentTag: String = TAG_MAIN_FRAGMENT

    // Глобальная ViewModel, инкапсулирующая данные тренировки
    private val viewModel: WorkoutViewModel by viewModels()

    private var restTimeTextWatcher: TextWatcher? = null

//----------------------------------------------[ Переопределение функций жизненного цикла ]--------


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil // определяем привязку данных
            .setContentView(this, R.layout.activity_main)

        setSupportActionBar(binding.toolbar) // Установка собственного toolbar
        supportActionBar?.title = null       // и удаление заголовка

        // Определяем объекты, предназначенные для работы со временем;
        stopwatch = Stopwatch(this)
        timer = CountDownTimer(this)
        exerciseStopwatch = ExerciseStopwatch(binding.generalClock)

        if (savedInstanceState == null
            || viewModel.activeProcess.value == WorkoutProcess.NOT_STARTED
        ) {
            hideToolbarAndDisableSideMenu()
        } else {
            restoreTimeData(savedInstanceState)
        }
        // Запускаем стартовый фрагмент
        launchFragment(lastRunFragmentTag)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()

//        // Устанавливаем анимацию переливания фона у фрагментов
//        (binding.fragmentContainer.background as AnimationDrawable).also {
//            it.setEnterFadeDuration(2000)
//            it.setExitFadeDuration(4000)
//            it.start()
//        }

        //-- -- -- -- -- -- -- -- -- -- -- -- -- -- -[ Обаботка бокового меню ]- -- -- -- -- -- -- -

        // Определяем поведение при открытии и закрытии бокового меню
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerClosed(drawerView: View) {
                closeOpenedSideSubmenu(null, true)
            }

            /** Определение динамических действий бокового меню, при его открытии */
            override fun onDrawerOpened(drawerView: View) { // При открытии бокового меню:

                /* ИНИЦИАЛИЗИРУЕМ СЛУШАТЕЛИ ДЛЯ КАЖДОГО ПОДПУНКТА "МЕСТО ТРЕНИРОВКИ" */

                // Установка места тренировки на "Дом"
                binding.subMenuDynamicPlaceHome.setOnClickListener(
                    getSideMenuItemOnClickListener(
                        R.string.side_menu_reselected_place_toast,
                        R.string.side_menu_reselected_place_item_home
                    ) { viewModel.trainingPlace = Place.TRAINING_AT_HOME })


                // Установка места тренировки на "Тренажерный Зал"
                binding.subMenuDynamicPlaceGym.setOnClickListener(
                    getSideMenuItemOnClickListener(
                        R.string.side_menu_reselected_place_toast,
                        R.string.side_menu_reselected_place_item_gym
                    ) { viewModel.trainingPlace = Place.TRAINING_IN_GYM })


                // Установка места тренировки на "На улице"
                binding.subMenuDynamicPlaceOutdoors.setOnClickListener(
                    getSideMenuItemOnClickListener(
                        R.string.side_menu_reselected_place_toast,
                        R.string.side_menu_reselected_place_item_outdoors
                    ) { viewModel.trainingPlace = Place.TRAINING_OUTDOORS })

                /* ОПРЕДЕЛИМ СЛУШАТЕЛИ ДЛЯ КАЖДОГО ПОДПУНКТА "РЕЖИМ УВЕДОМЛЕНИЯ" */

                // Установка уведомления ввиде "Проигрывание звука"
                binding.subMenuDynamicSound.setOnClickListener(
                    getSideMenuItemOnClickListener(
                        R.string.side_menu_reselected_workout_set_notifying_toast,
                        R.string.side_menu_reselected_ws_item_sound
                    ) {
                        viewModel.breakNotificationMode = BreakNotifyingMode.SOUND.also {
                            timer.setNotifyingType(it)
                        }
                    })


                // Установка уведомления ввиде "Вибрация"
                binding.subMenuDynamicVibration.setOnClickListener(getSideMenuItemOnClickListener(
                    R.string.side_menu_reselected_workout_set_notifying_toast,
                    R.string.side_menu_reselected_ws_item_vibration
                ) {
                    viewModel.breakNotificationMode = BreakNotifyingMode.VIBRATION.also {
                        timer.setNotifyingType(it)
                    }
                })


                // Установка уведомления ввиде "Анимация на экране / сияние экрана"
                binding.subMenuDynamicAnim.setOnClickListener(getSideMenuItemOnClickListener(
                    R.string.side_menu_reselected_workout_set_notifying_toast,
                    R.string.side_menu_reselected_ws_item_animation
                ) { viewModel.breakNotificationMode = BreakNotifyingMode.ANIMATION })

                /* ОПРЕДЕЛИМ СЛУШАТЕЛИ ДЛЯ КАЖДОГО ОБЪЕКТА "ВРЕМЯ ОТДЫХА" */

                // Нажатие на поле ввода времени отдыха вручную
                binding.sideMenuInputRestTime.setOnTouchListener { v, _ ->
                    binding.sideMenuRestTimeAcceptButton.visibility = View.VISIBLE
                    v.performClick()
                }

                // Нажатие кнопки "подтвердить" на клавиатуре после ввода времени отдыха
                binding.sideMenuInputRestTime.onDone {
                    if (binding.sideMenuRestTimeAcceptButton.isEnabled) {
                        viewModel.restTime.value = binding.sideMenuInputRestTime.text
                            .toString().toInt().also {
                                binding.sideMenuCurrentRestTime.text =
                                    getString(R.string.selected_seconds, it)
                            }
                        binding.sideMenuInputRestTime.text = null
                        binding.sideMenuRestTimeAcceptButton.visibility = View.GONE
                        binding.sideMenuInputRestTime.clearFocus()
                        true
                    } else false
                }

                // Нажатие кнопки увеличения времени отдыха вручную на 5
                binding.add5s.setOnClickListener(getSideMenuChangeOnNumButtonListener(5) {
                    viewModel.restTime.value!! >= WorkoutParams.restTimeAdvRange.last
                })

                // Нажатие кнопки уменьшения времени отдыха вручную на 5
                binding.sub5s.setOnClickListener(getSideMenuChangeOnNumButtonListener(-5) {
                    viewModel.restTime.value!! <= WorkoutParams.restTimeDefaultRange.first
                })

                // Ввод времени отдыха в соответствующем поле
                restTimeTextWatcher = object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun afterTextChanged(p0: Editable?) {}
                    override fun onTextChanged(chars: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        if (!chars.isNullOrEmpty()
                            && chars.toString().toInt() in WorkoutParams.restTimeAdvRange
                        ) {
                            ImageViewCompat.setImageTintList(
                                binding.sideMenuRestTimeAcceptButton,
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        this@MainActivity, R.color.md_theme_onSurface
                                    )
                                )
                            )
                            binding.sideMenuRestTimeAcceptButton.isEnabled = true
                        } else {
                            ImageViewCompat.setImageTintList(
                                binding.sideMenuRestTimeAcceptButton,
                                ColorStateList.valueOf(Color.DKGRAY)
                            )
                            binding.sideMenuRestTimeAcceptButton.isEnabled = false
                        }
                    }
                }
                binding.sideMenuInputRestTime.addTextChangedListener(restTimeTextWatcher)

                // Нажатие на кнопку подтверждения выбранного времени отдыха
                binding.sideMenuRestTimeAcceptButton.setOnClickListener {
                    val value = binding.sideMenuInputRestTime.text.toString().toInt()
                    viewModel.restTime.value = value
                    binding.sideMenuCurrentRestTime.text =
                        getString(R.string.selected_seconds, value)
                    binding.sideMenuInputRestTime.text = null
                    it.visibility = View.GONE
                    it.hideKeyboard()
                }
            }
        }) // Конец определения динамических объектов (подпунктов) бокового меню при его открытии.

        /** Определение действий бокового меню еще до включения бокового меню */

        // Отслеживание нажатия на закрытие бокового меню
        binding.sideNavigationView
            .getHeaderView(0)
            .findViewById<ImageView>(R.id.close_nv_button)
            .setOnClickListener {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            }

        // Слушатель запуска подпунктов места тренировки
        binding.sideMenuPlace.setOnClickListener(
            getSideMenuListener(binding.sideMenuDynamicPlace) {}
        )
        // Слушатель запуска подпунктов частей тела
        binding.sideMenuBodyparts.setOnClickListener(
            getSideMenuListener(binding.sideMenuDynamicBodyParts) {
                fillListOfBPInSideMenu(binding.sideMenuDynamicBodyParts)
            }
        )
        // Слушатель запуска подпунктов с мышцами
        binding.sideMenuMuscles.setOnClickListener(
            getSideMenuListener(binding.sideMenuDynamicMuscles) {
                fillListOfMusclesInSideMenu(binding.sideMenuDynamicMuscles)
            }
        )
        // Слушатель запуска подпунктов с временем отдыха
        binding.sideMenuTime.setOnClickListener(
            getSideMenuListener(binding.sideMenuDynamicRestTime) {
                // Выставим исходное значение времени
                binding.sideMenuCurrentRestTime.text =
                    getString(R.string.selected_seconds, viewModel.restTime.value!!)
                // Определим исходное изображение и заблокируем кнопку
                ImageViewCompat.setImageTintList(
                    binding.sideMenuRestTimeAcceptButton,
                    ColorStateList.valueOf(Color.DKGRAY)
                )
                binding.sideMenuRestTimeAcceptButton.isEnabled = false
            }
        )
        // Слушатель запуска подпунктов с режимом уведомления о начале подхода
        binding.sideMenuNotificationMode.setOnClickListener(
            getSideMenuListener(binding.sideMenuDynamicBreakMode) {}
        )

        // Слушатель прекращение тренировки
        binding.subMenuStopStopWorkout.setOnClickListener {
            stopWorkout()
        }


        //-- -- -- -- -- -- -- -- -- -- -- -- -- -- -[ Обаботка нижнего меню ]- -- -- -- -- -- -- --

        // Слушатель выбора элемента Bottom Navigation View
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Если выбираем кнопку для стартовой страницы,
                // то запускаем соответствующий объект
                R.id.bottom_main -> {  // Передаем bundle со списком
                    launchFragment(TAG_MAIN_FRAGMENT) // запускаем фрагмент
                    true
                }
                // Если выбираем кнопку со списком,
                // то запускаем соответствующий объект
                R.id.bottom_list -> {
                    launchFragment(TAG_LIST_FRAGMENT) // запускаем фрагмент
                    true
                }
                else -> false
            }
        }

        // Слушатель перевыбора элемента Bottom Navigation View
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

        //-- -- -- -- -- -- -- -- -- -- -- -- -- -- -[ Обаботка toolbar ]- -- -- -- -- -- -- -- -- -

        // Слушатель нажатия на кнопку паузы общего времени на Toolbar
        binding.pauseResumeButton.setOnClickListener {
            it.isClickable = false
            stopwatch.startOrStop()
            Handler(mainLooper).postDelayed({
                it.isClickable = true
            }, 1000)


        }
        // Слушатель касания на кнопку паузы общего времени на Toolbar (для анимирования)
        binding.pauseResumeButton.setOnTouchListener { view, motionEvent ->
            // Анимации для нажатий
            val fadeAnimPressed = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
            val fadeAnimUnpressed = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> view.startAnimation(fadeAnimPressed)
                MotionEvent.ACTION_UP -> view.startAnimation(fadeAnimUnpressed)
            }
            false
        }
        // Слушатель нажатия для кнопки опций/настроек на Toolbar
        binding.optionsButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }
        // Слушатель касания для кнопки опций/настроек на Toolbar
        binding.optionsButton.setOnTouchListener { view, motionEvent ->
            // Анимации для нажатий
            val fadeAnimPressed = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
            val fadeAnimUnpressed = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> view.startAnimation(fadeAnimPressed)
                MotionEvent.ACTION_UP -> view.startAnimation(fadeAnimUnpressed)
            }
            false
        }

        //-- -- -- -- -- -- -- -- -- -- -- -- -[ Обаботка отслеживаемых LiveData ]- -- -- -- -- -- -

        // Отлеживание изменения времени отдыха
        viewModel.restTime.observe(this) {
            if (viewModel.activeProcess.value != WorkoutProcess.TIMER)
                timer.setTime(viewModel.restTime.value!!)
            Log.d("MyTag", "restTime observed")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putBoolean(STOPWATCH_IS_GOING, stopwatch.isGoing())
            putInt(STOPWATCH_REMAINING_TIME, stopwatch.getRemaining())
            putInt(TIMER_REMAINING_TIME, timer.getTime())
            putBoolean(TIMER_IS_GOING, timer.isGoing)
            putInt(TIMER_START_TIME, timer.startTime)
            if (viewModel.activeProcess.value == WorkoutProcess.EXERCISE_STOPWATCH)
                putInt(EXERCISE_TIME, exerciseStopwatch.time)
            putString(TAG_OF_LAST_RUN_FRAGMENT, lastRunFragmentTag)
        }
        super.onSaveInstanceState(outState)
    }


//-----------------------------------------------[ Приватные функции-члены класса ]-----------------


//                                                                             [ Для бокового меню ]

    /**
     * Получение слушателя нажатий для кнопок
     * увеличения/уменьшения времени отдыха;
     */
    private fun getSideMenuChangeOnNumButtonListener(
        num: Int,
        exitCondition: () -> Boolean
    ): View.OnClickListener {
        return View.OnClickListener {
            // Анимации для нажатий
            it.startAnimation(AlphaAnimation(1f, 0.8f))
            if (exitCondition.invoke()) {
                return@OnClickListener
            }
            viewModel.restTime.value = viewModel.restTime.value?.plus(num).also {
                binding.sideMenuCurrentRestTime.text = getString(R.string.selected_seconds, it)
            }
            binding.sideMenuRestTimeAcceptButton.visibility = View.GONE
            binding.sideMenuInputRestTime.text = null
            binding.root.hideKeyboard()
        }
    }

    /**
     * Получение слушателя нажатий для места
     * отдыха и уведомления этапов тренировки;
     */
    private fun getSideMenuItemOnClickListener(
        toastString: Int,
        toastSubstring: Int,
        action: () -> Unit,
    ) = View.OnClickListener {
        action.invoke()
        Toast.makeText(
            this@MainActivity,
            getString(toastString, getString(toastSubstring)),
            Toast.LENGTH_LONG
        ).show()
        closeOpenedSideSubmenu()
    }

    /**
     * Скрытие или открытие подменю для определенного пункта бокового меню;
     */
    private fun showOrCloseSideSubmenu(container: ViewGroup): Boolean {
        return if (container.isVisible) {
            container.collapse()
            false
        } else {
            openedSubmenu = container
            container.expand()
            true
        }
    }

    /**
     * Закрытие открытого подменю в боковом меню;
     */
    private fun closeOpenedSideSubmenu(except: ViewGroup? = null, woAnim: Boolean = false) {
        if (except != openedSubmenu) {
            if (!woAnim)
                openedSubmenu?.apply {
                    collapse()
                }
            else openedSubmenu?.visibility = View.GONE
        }
        if (openedSubmenu == binding.sideMenuDynamicRestTime) {
            binding.sideMenuDynamicRestTime.hideKeyboard()
            binding.sideMenuRestTimeAcceptButton.visibility = View.GONE
        }
    }

    /**
     * Получение слушателя для каждого из пунктов бокового меню;
     */
    private fun getSideMenuListener(
        container: ViewGroup,
        action: () -> Unit
    ): View.OnClickListener {
        val bounceAnim = AnimationUtils.loadAnimation(this@MainActivity, R.anim.anim_bounce)
        return View.OnClickListener {
            it.startAnimation(bounceAnim)
            closeOpenedSideSubmenu(container)
            action.invoke()
            showOrCloseSideSubmenu(container)
        }
    }

    /**
     * Заполнение списка с мышцами в боковом меню;
     */
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

    /**
     * Заполнение списка с частями тела в боковом меню;
     */
    private fun fillListOfBPInSideMenu(recyclerView: RecyclerView) {
        val arrayList = arrayListOf<SimpleListItem>()
        val stringArray = viewModel.getAllBP(this)
        val boolArray = viewModel.getWhichBPsAreSelected()
        for (i in 0 until WorkoutParams.numberOfBodyParts) {
            arrayList.add(
                SimpleListItem(
                    stringArray[i], boolArray[i]
                )
            )
        }
        recyclerView.adapter =
            SimpleListAdapter(this, TAG_BODY_PARTS).also {
                it.submitList(arrayList)
            }
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Скрытие клавиатуры и удаление фокуса с заполняемого поля;
     */
    private fun View.hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(this.windowToken, 0)
        binding.sideMenuInputRestTime.clearFocus()
    }

    /**
     * Обработка подтверждения ввода из клавиатурного набора текста;
     */
    private fun EditText.onDone(callback: () -> Boolean) {
        setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (callback.invoke())
                    hideKeyboard()
            }
            false
        }
    }


    /**
     * Раскрытие контейнера по вертикали;
     */
    private fun View.expand() {
        val matchParentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec((parent as View).width, View.MeasureSpec.EXACTLY)
        val wrapContentMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        measure(matchParentMeasureSpec, wrapContentMeasureSpec)
        val targetHeight = measuredHeight

        layoutParams.height = 1
        visibility = View.VISIBLE
        val anim: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                layoutParams.height =
                    if (interpolatedTime == 1f)
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    else (targetHeight * interpolatedTime).toInt()
                requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }

        // Скорость - 1dp/ms
        anim.duration = (targetHeight / context.resources.displayMetrics.density).toLong()
        startAnimation(anim)

    }

    /**
     * Закрытие контейнера по вертикали;
     */
    private fun View.collapse(duration: Long? = null) {
        val initialHeight = measuredHeight
        val anim: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    visibility = View.GONE
                } else {
                    layoutParams.height =
                        initialHeight - (initialHeight * interpolatedTime).toInt()
                    requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean = true
        }
        if (duration == null)
            anim.duration = (initialHeight / context.resources.displayMetrics.density).toLong()
        else anim.duration = duration
        startAnimation(anim)
    }

//                                                                     [ Для восстановления данных ]

    /**
     * Получение данных из переданного bundle;
     */
    private fun restoreTimeData(bundle: Bundle) = with(bundle) {
        timer.isGoing = getBoolean(TIMER_IS_GOING)
        timer.setTime(getInt(TIMER_REMAINING_TIME))
        timer.startTime = getInt(TIMER_START_TIME)
        stopwatch.setTime(getInt(STOPWATCH_REMAINING_TIME))
        stopwatch.setGoing(getBoolean(STOPWATCH_IS_GOING))
        binding.generalClock.text = stopwatch.getTimeInFormatHMMSS(stopwatch.getRemaining())
        exerciseStopwatch.time = getInt(EXERCISE_TIME)
        timer.setNotifyingType(viewModel.breakNotificationMode!!)
        if (viewModel.activeProcess.value == WorkoutProcess.EXERCISE_STOPWATCH)
            exerciseStopwatch.`continue`()
        if (stopwatch.isGoing()) {
            binding.pauseResumeButton.setImageResource(R.drawable.ic_pause)
        } else {
            binding.pauseResumeButton.setImageResource(R.drawable.ic_play)
        }
        lastRunFragmentTag = getString(TAG_OF_LAST_RUN_FRAGMENT, lastRunFragmentTag)
    }

//                                                                    [ Для скрытия/отображения UI ]

    /**
     * Функция для запуска фрагмента по переданному тегу;
     */
    private fun launchFragment(tag: String) {
        val fragment =
            if (tag == TAG_MAIN_FRAGMENT) {
                lastRunFragmentTag = TAG_MAIN_FRAGMENT
                MainFragment()
            } else {
                lastRunFragmentTag = TAG_LIST_FRAGMENT
                ListFragment()
            }
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
     * Запустить фрагмент принудительно, даже если он уже запущен;
     */
    private fun launchFragmentForced(tag: String) {
        val fragment =
            if (tag == TAG_MAIN_FRAGMENT) {
                lastRunFragmentTag = TAG_MAIN_FRAGMENT
                MainFragment()
            } else {
                lastRunFragmentTag = TAG_LIST_FRAGMENT
                ListFragment()
            }
        supportFragmentManager
            .beginTransaction() // Начинаем транзакцию
            .replace(R.id.fragment_container, fragment, tag) // Заменяем фрагмент
            .commit() // Закрепляем процесс
    }

    /**
     * Прекращение тренировки; (TODO:1.0)
     */
    private fun stopWorkout() {

        hideToolbarAndDisableSideMenu()

        viewModel.activeProcess.value = WorkoutProcess.NOT_STARTED
        viewModel.clearAllData()
        exerciseStopwatch.detachUI()
        exerciseStopwatch.stop()

        timer.detachUI()
        timer.stopAndUnregister()

        stopwatch.stopAndUnregister()
        launchFragmentForced(TAG_MAIN_FRAGMENT)
        //And others add
        binding.subMenuDynamicPlaceHome.setOnClickListener(null)
        binding.subMenuDynamicPlaceGym.setOnClickListener(null)
        binding.subMenuDynamicPlaceOutdoors.setOnClickListener(null)
        binding.subMenuDynamicSound.setOnClickListener(null)
        binding.subMenuDynamicVibration.setOnClickListener(null)
        binding.subMenuDynamicAnim.setOnClickListener(null)
        binding.add5s.setOnClickListener(null)
        binding.sub5s.setOnClickListener(null)
        binding.sideMenuInputRestTime.removeTextChangedListener(restTimeTextWatcher)
        binding.sideMenuRestTimeAcceptButton.setOnClickListener(null)


        //TODO: Запустить фрагмент со списком;
        // Перенести данные в базу данных;
    }

    /**
     * Скрытие toolbar и блокировка бокового меню;
     */
    private fun hideToolbarAndDisableSideMenu() {
        supportActionBar?.hide()
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    /**
     * Показ toolbar и активация бокового меню;
     */
    private fun showToolbarAndActivateSideMenu() {
        supportActionBar?.show()
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }


//----------------------------------------------[ Переопределение функций обратного вызова ]--------


    /**
     * Callback интерфейса [WarningUnusedBPDialog.Callback];
     * Установка false на неиспользуемыех значениях частей тела.
     */
    override fun unusedBodyPartsRemoved(whichAreUnusedBP: Array<Boolean>) {
        val current = viewModel.getWhichBPsAreSelected()
        for (i in 0 until 5) {
            if (whichAreUnusedBP[i]) {
                current[i] = false
            }
        }
        viewModel.updateData(this, current)
    }

    /**
     * Callback интерфейса [CountDownTimer.Callback];
     * Обработка завершения таймера: переход на секундомер.
     */
    override fun timerFinished() {
        exerciseStopwatch.start()
        viewModel.activeProcess.value = WorkoutProcess.EXERCISE_STOPWATCH
        exerciseStopwatch.attachUI(
            binding.generalClock,
            findViewById(R.id.set_timer),
            findViewById(R.id.set_timer_progress)
        )
    }

    /**
     * Callback интерфейса [MainFragment.FragmentCallback];
     * Обработка нажатия основной кнопки
     */
    override fun mainButtonClicked() {
        when (viewModel.activeProcess.value) {
            WorkoutProcess.TIMER -> {
                timer.startOrStop()
            }
            WorkoutProcess.EXERCISE_STOPWATCH -> {
                //TODO: Запустить диалоговое окно, в котором будет выбор того, что ты потренил
                // Если Пользователь выбрал закончить тренировку, то перебросить его на результаты и изменить viewModel.activeProcess
                exerciseStopwatch.stop()
                viewModel.activeProcess.value = WorkoutProcess.TIMER

                timer.setDefaults(viewModel.restTime.value!!)

                timer.attachUI(
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

    /**
     * Callback интерфейса [MainFragment.FragmentCallback];
     * Обработка уничтожения фрагмента;
     */
    override fun fragmentDestroyed() {
        if (viewModel.activeProcess.value == WorkoutProcess.TIMER)
            timer.detachUI()
    }

    /**
     * Callback интерфейса [MainFragment.FragmentCallback];
     * Обработка создания View фрагмента (onViewCreated);
     */
    override fun fragmentUICreated(textView: TextView, progressBar: ProgressBar) {
        if (viewModel.activeProcess.value == WorkoutProcess.TIMER) {
            timer.attachUI(textView, progressBar)
        } else if (viewModel.activeProcess.value == WorkoutProcess.EXERCISE_STOPWATCH)
            exerciseStopwatch.attachUI(binding.generalClock, textView, progressBar)
    }

    override fun workoutStarted() {
        // Обновляем View, используемые для отображения данных секундомера;
        showToolbarAndActivateSideMenu()
        stopwatch.startOrStop()
        exerciseStopwatch.attachUI(
            binding.generalClock,
            findViewById(R.id.set_timer),
            findViewById(R.id.set_timer_progress)
        )
        exerciseStopwatch.start()
        // Сохраняем текущим процессом секундомер;
        viewModel.activeProcess.value = WorkoutProcess.EXERCISE_STOPWATCH
        timer.setNotifyingType(viewModel.breakNotificationMode!!)
    }

    /**
     * Callback интерфейса [SimpleListAdapter.Callback];
     * Обработка выбранного элемента списка бокового меню (части тела или мышцы);
     */
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

