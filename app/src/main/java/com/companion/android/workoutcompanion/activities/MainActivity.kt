package com.companion.android.workoutcompanion.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewDebug.CapturedViewProperty
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.companion.android.workoutcompanion.R
import com.companion.android.workoutcompanion.adapters.SimpleListAdapter
import com.companion.android.workoutcompanion.databinding.ActivityMainBinding
import com.companion.android.workoutcompanion.dialogs.WarningUnusedBPDialog
import com.companion.android.workoutcompanion.fragments.MainFragment
import com.companion.android.workoutcompanion.models.SimpleListItem
import com.companion.android.workoutcompanion.objects.BreakNotifyingMode
import com.companion.android.workoutcompanion.objects.Place
import com.companion.android.workoutcompanion.objects.WorkoutParams
import com.companion.android.workoutcompanion.objects.WorkoutProcess
import com.companion.android.workoutcompanion.time.ActionManager
import com.companion.android.workoutcompanion.time.ActionManager.Companion.Action
import com.companion.android.workoutcompanion.time.StopwatchService.Companion.ACTION_SHOW_MAIN_FRAGMENT
import com.companion.android.workoutcompanion.viewmodels.WorkoutViewModel


// [ Константы ]

// Теги для бокового меню
const val TAG_BODY_PARTS = "tag-body-parts"
const val TAG_MUSCLES = "tag-muscles"

//todo: bottom...dialog

class MainActivity : AppCompatActivity(),
    WarningUnusedBPDialog.Callback,
    MainFragment.FragmentCallback,
    SimpleListAdapter.Callback {


//---------------------------------------------------------------[ Данные ]-------------------------

    // Инициализация объекта класса привязки данных
    private lateinit var binding: ActivityMainBinding

    // Менеджер, который управляет основными действиями со временем
    private var actionManager: ActionManager? = null

    // Открытое в текущий момент подменю NavigationView
    @CapturedViewProperty
    private var openedSubmenuId: Int? = null

    // Глобальная ViewModel, инкапсулирующая данные тренировки
    private val viewModel: WorkoutViewModel by viewModels()

    private var restTimeTextWatcher: TextWatcher? = null


//----------------------------------------------[ Переопределение функций жизненного цикла ]--------


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigateToMainFragmentIfNeeded(intent)

        binding = DataBindingUtil // определяем привязку данных
            .setContentView(this, R.layout.activity_main)

        setSupportActionBar(binding.toolbar) // Установка собственного toolbar
        supportActionBar?.title = null       // и удаление заголовка

        Log.d("LF", "A onCreate")

        // Определяем объект, предназначенный для работы со временем;
        if (actionManager == null)
            actionManager = ActionManager(this, viewModel)


        if (savedInstanceState == null
            || viewModel.activeProcess.value == WorkoutProcess.NOT_STARTED
        ) {
            hideToolbarAndDisableSideMenu()
        } else {
            restoreData(savedInstanceState)
            hideSelectedSideMenuItems()
        }

        setupBottomNavigation()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToMainFragmentIfNeeded(intent)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStart() {
        super.onStart()
        Log.d("LF", "A onStart")
        //-- -- -- -- -- -- -- -- -- -- -- -- -- -- -[ Обаботка бокового меню ]- -- -- -- -- -- -- -

        // Определяем поведение при открытии и закрытии бокового меню
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerClosed(drawerView: View) {
                closeOpenedSideContainer()
                binding.sideMenu.stopWorkoutAcceptButton.visibility = View.GONE
            }

            /** Определение динамических действий бокового меню, при его открытии */
            override fun onDrawerOpened(drawerView: View) {  // При открытии бокового меню:

                /* ИНИЦИАЛИЗИРУЕМ СЛУШАТЕЛИ ДЛЯ КАЖДОГО ПОДПУНКТА "МЕСТО ТРЕНИРОВКИ" */

                // Установка места тренировки на "Дом"
                binding.sideMenu.placeItemHome.setOnClickListener(
                    getSideMenuItemOnClickListener(
                        R.string.side_menu_reselected_place_toast,
                        R.string.side_menu_reselected_place_item_home
                    ) {
                        viewModel.trainingPlace = Place.TRAINING_AT_HOME
                    })


                // Установка места тренировки на "Тренажерный Зал"
                binding.sideMenu.placeItemGym.setOnClickListener(
                    getSideMenuItemOnClickListener(
                        R.string.side_menu_reselected_place_toast,
                        R.string.side_menu_reselected_place_item_gym
                    ) {
                        viewModel.trainingPlace = Place.TRAINING_IN_GYM
                    })


                // Установка места тренировки на "На улице"
                binding.sideMenu.placeItemOutdoors.setOnClickListener(
                    getSideMenuItemOnClickListener(
                        R.string.side_menu_reselected_place_toast,
                        R.string.side_menu_reselected_place_item_outdoors
                    ) {
                        viewModel.trainingPlace = Place.TRAINING_OUTDOORS
                    })

                /* ОПРЕДЕЛИМ СЛУШАТЕЛИ ДЛЯ КАЖДОГО ПОДПУНКТА "РЕЖИМ УВЕДОМЛЕНИЯ" */

                // Установка уведомления ввиде "Проигрывание звука"
                binding.sideMenu.subMenuDynamicSound.setOnClickListener(
                    getSideMenuItemOnClickListener(
                        R.string.side_menu_reselected_workout_set_notifying_toast,
                        R.string.side_menu_reselected_ws_item_sound
                    ) {
                        viewModel.breakNotifyingMode = BreakNotifyingMode.SOUND
                    })


                // Установка уведомления ввиде "Вибрация"
                binding.sideMenu.subMenuDynamicVibration.setOnClickListener(getSideMenuItemOnClickListener(
                    R.string.side_menu_reselected_workout_set_notifying_toast,
                    R.string.side_menu_reselected_ws_item_vibration
                ) {
                    viewModel.breakNotifyingMode = BreakNotifyingMode.VIBRATION
                })


                // Установка уведомления ввиде "Анимация на экране / сияние экрана"
                binding.sideMenu.subMenuDynamicAnim.setOnClickListener(getSideMenuItemOnClickListener(
                    R.string.side_menu_reselected_workout_set_notifying_toast,
                    R.string.side_menu_reselected_ws_item_animation
                ) {
                    viewModel.breakNotifyingMode = BreakNotifyingMode.ANIMATION
                })

                /* ОПРЕДЕЛИМ СЛУШАТЕЛИ ДЛЯ КАЖДОГО ОБЪЕКТА "ВРЕМЯ ОТДЫХА" */

                // Нажатие на поле ввода времени отдыха вручную
                binding.sideMenu.inputRestTime.setOnTouchListener { v, _ ->
                    binding.sideMenu.restTimeAcceptButton.visibility = View.VISIBLE
                    v.performClick()
                }

                binding.sideMenu.currentRestTime.setOnTouchListener { _, event ->
                    binding.sideMenu.inputRestTime.dispatchTouchEvent(event)
                }

                // Нажатие кнопки "подтвердить" на клавиатуре после ввода времени отдыха
                binding.sideMenu.inputRestTime.onDone {
                    if (binding.sideMenu.restTimeAcceptButton.isEnabled) {
                        viewModel.restTime.value = binding.sideMenu.inputRestTime.text
                            .toString().toInt().also {
                                binding.sideMenu.currentRestTime.text =
                                    getString(R.string.selected_seconds, it)
                            }
                        binding.sideMenu.inputRestTime.text = null
                        binding.sideMenu.restTimeAcceptButton.visibility = View.GONE
                        binding.sideMenu.inputRestTime.clearFocus()
                        true
                    } else false
                }

                // Нажатие кнопки увеличения времени отдыха вручную на 5
                binding.sideMenu.add5sButton.setOnClickListener(getSideMenuChangeOnNumButtonListener(5) {
                    viewModel.restTime.value!! >= WorkoutParams.restTimeAdvRange.last
                })

                // Нажатие кнопки уменьшения времени отдыха вручную на 5
                binding.sideMenu.sub5sButton.setOnClickListener(getSideMenuChangeOnNumButtonListener(-5) {
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
                                binding.sideMenu.restTimeAcceptButton,
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        this@MainActivity, R.color.on_surface
                                    )
                                )
                            )
                            binding.sideMenu.restTimeAcceptButton.isEnabled = true
                        } else {
                            ImageViewCompat.setImageTintList(
                                binding.sideMenu.restTimeAcceptButton,
                                ColorStateList.valueOf(Color.DKGRAY)
                            )
                            binding.sideMenu.restTimeAcceptButton.isEnabled = false
                        }
                    }
                }
                binding.sideMenu.inputRestTime.addTextChangedListener(restTimeTextWatcher)

                // Нажатие на кнопку подтверждения выбранного времени отдыха
                binding.sideMenu.restTimeAcceptButton.setOnClickListener {
                    val value = binding.sideMenu.inputRestTime.text.toString().toInt()
                    viewModel.restTime.value = value
                    binding.sideMenu.currentRestTime.text =
                        getString(R.string.selected_seconds, value)
                    binding.sideMenu.inputRestTime.text = null
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
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }

        // Слушатель запуска подпунктов места тренировки
        binding.sideMenu.place.setOnClickListener(
            getSideMenuListener(binding.sideMenu.placeItems)
        )
        // Слушатель запуска подпунктов частей тела
        binding.sideMenu.bodyParts.setOnClickListener(
            getSideMenuListener(binding.sideMenu.bodyPartsItems) {
                fillListOfBPInSideMenu(binding.sideMenu.bodyPartsItems)
            }
        )
        // Слушатель запуска подпунктов с мышцами
        binding.sideMenu.muscles.setOnClickListener(
            getSideMenuListener(binding.sideMenu.musclesItems) {
                fillListOfMusclesInSideMenu(binding.sideMenu.musclesItems)
            }
        )
        // Слушатель запуска подпунктов с временем отдыха
        binding.sideMenu.restTime.setOnClickListener(
            getSideMenuListener(binding.sideMenu.restTimeItems) {
                // Выставим исходное значение времени
                binding.sideMenu.currentRestTime.text =
                    getString(R.string.selected_seconds, viewModel.restTime.value!!)
                // Определим исходное изображение и заблокируем кнопку
                ImageViewCompat.setImageTintList(
                    binding.sideMenu.restTimeAcceptButton,
                    ColorStateList.valueOf(Color.DKGRAY)
                )
                binding.sideMenu.restTimeAcceptButton.isEnabled = false
            }
        )
        // Слушатель запуска подпунктов с режимом уведомления о начале подхода
        binding.sideMenu.breakMode.setOnClickListener(
            getSideMenuListener(binding.sideMenu.breakModeItems)
        )

        // Слушатель прекращение тренировки
        binding.sideMenu.subMenuStopWorkout.setOnClickListener {
            if (binding.sideMenu.stopWorkoutAcceptButton.isVisible)
                binding.sideMenu.stopWorkoutAcceptButton.visibility = View.GONE
            else
                binding.sideMenu.stopWorkoutAcceptButton.visibility = View.VISIBLE
        }

        // Слушатель подтверждения прекращение тренировки
        binding.sideMenu.stopWorkoutAcceptButton.setOnClickListener {
            stopWorkout()
        }

        //-- -- -- -- -- -- -- -- -- -- -- -- -- -- -[ Обаботка toolbar ]- -- -- -- -- -- -- -- -- -


        // Слушатель нажатия для кнопки опций/настроек на Toolbar
        binding.toolbar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.workout_toolbar_menu, menu)
        if (viewModel.activeProcess.value == WorkoutProcess.PAUSED) {
            binding.toolbar.menu.getItem(0).icon =
                ContextCompat.getDrawable(this, R.drawable.ic_resume_workout)
        }
        return true
    }

    // Слушатель касания на кнопку паузы общего времени на Toolbar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        actionManager?.launchOrPause(Action.GENERAL_STOPWATCH)
        if (actionManager?.isActionPaused(Action.GENERAL_STOPWATCH) == true)
            actionManager?.pauseAllActions()
        else
            actionManager?.resumeAllActions()

        if (viewModel.activeProcess.value == WorkoutProcess.PAUSED) {
            if (actionManager!!.isActionEnabled(Action.BREAK_TIMER))
                viewModel.activeProcess.value = WorkoutProcess.TIMER
            else
                viewModel.activeProcess.value = WorkoutProcess.EXERCISE_STOPWATCH
            binding.toolbar.menu.getItem(0).icon =
                ContextCompat.getDrawable(this, R.drawable.ic_resume_workout)
        } else {
            binding.toolbar.menu.getItem(0).icon =
                ContextCompat.getDrawable(this, R.drawable.ic_pause_workout)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (actionManager != null) {
            val savedInstanceState = actionManager!!.getInstanceState()
            if (savedInstanceState != null)
                outState.putAll(savedInstanceState)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        Log.d("LF", "A onStop")
        binding.drawerLayout.closeDrawer(GravityCompat.START, false)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        binding.drawerLayout.closeDrawer(GravityCompat.START, true)
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
                binding.sideMenu.currentRestTime.text = getString(R.string.selected_seconds, it)
            }
            binding.sideMenu.restTimeAcceptButton.visibility = View.GONE
            binding.sideMenu.inputRestTime.text = null
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
        endAction: () -> Unit,
    ) = View.OnClickListener {
        Toast.makeText(
            this@MainActivity,
            getString(toastString, getString(toastSubstring)),
            Toast.LENGTH_LONG
        ).show()
        closeOpenedSideContainer { endAction.invoke() }
    }

    /**
     * Скрытие или открытие подменю для определенного пункта бокового меню;
     */
    private fun showOrCloseSideContainer(container: View): Boolean {
        return if (container is RecyclerView) {
            if (container.isVisible) {
                container.visibility = View.GONE
                false
            } else {
                openedSubmenuId = container.id
                container.visibility = View.VISIBLE
                true
            }
        } else {
            if (findViewById<View?>((container as Group).referencedIds[0]).isVisible
                || findViewById<View?>(container.referencedIds[1]).isVisible
            ) {
                container.visibility = View.GONE
                false
            } else {
                openedSubmenuId = container.id
                when (container.id) {
                    R.id.place_items -> {
                        when (viewModel.trainingPlace) {
                            Place.TRAINING_AT_HOME -> {
                                binding.sideMenu.placeItemHome.isGone = true
                                binding.sideMenu.placeItemGym.isVisible = true
                                binding.sideMenu.placeItemOutdoors.isVisible = true
                            }
                            Place.TRAINING_IN_GYM -> {
                                binding.sideMenu.placeItemHome.isVisible = true
                                binding.sideMenu.placeItemGym.isGone = true
                                binding.sideMenu.placeItemOutdoors.isVisible = true
                            }
                            Place.TRAINING_OUTDOORS -> {
                                binding.sideMenu.placeItemHome.isVisible = true
                                binding.sideMenu.placeItemGym.isVisible = true
                                binding.sideMenu.placeItemOutdoors.isGone = true
                            }
                            else -> container.visibility = View.VISIBLE
                        }
                    }
                    R.id.break_mode_items -> {
                        when (viewModel.breakNotifyingMode) {
                            BreakNotifyingMode.SOUND -> {
                                binding.sideMenu.subMenuDynamicSound.isGone = true
                                binding.sideMenu.subMenuDynamicVibration.isVisible = true
                                binding.sideMenu.subMenuDynamicAnim.isVisible = true
                            }
                            BreakNotifyingMode.VIBRATION -> {
                                binding.sideMenu.subMenuDynamicSound.isVisible = true
                                binding.sideMenu.subMenuDynamicVibration.isGone = true
                                binding.sideMenu.subMenuDynamicAnim.isVisible = true
                            }
                            BreakNotifyingMode.ANIMATION -> {
                                binding.sideMenu.subMenuDynamicSound.isVisible = true
                                binding.sideMenu.subMenuDynamicVibration.isVisible = true
                                binding.sideMenu.subMenuDynamicAnim.isGone = true
                            }
                            else -> container.visibility = View.VISIBLE
                        }
                    }
                    else -> container.visibility = View.VISIBLE
                }
                true
            }
        }
    }

    /**
     * Закрытие открытого подменю в боковом меню;
     */
    private fun closeOpenedSideContainer(
        except: View? = null,
        action: (() -> Unit)? = null
    ) {
        if (except?.id != openedSubmenuId) {
            openedSubmenuId?.also {
                action?.invoke()
                getSideMenuContainerById(it)?.isGone = true
            }
        }
        if (openedSubmenuId == binding.sideMenu.restTimeItems.id) {
            binding.sideMenu.restTimeItems.hideKeyboard()
            binding.sideMenu.restTimeAcceptButton.isGone = true
        }
        when (openedSubmenuId) {
            R.id.place_items ->
                binding.sideMenu.place.setRightDrawable(R.drawable.ic_arrow_down)
            R.id.body_parts_items ->
                binding.sideMenu.bodyParts.setRightDrawable(R.drawable.ic_arrow_down)
            R.id.muscles_items ->
                binding.sideMenu.muscles.setRightDrawable(R.drawable.ic_arrow_down)
            R.id.rest_time_items ->
                binding.sideMenu.restTime.setRightDrawable(R.drawable.ic_arrow_down)
            R.id.break_mode_items ->
                binding.sideMenu.breakMode.setRightDrawable(R.drawable.ic_arrow_down)
        }
        //TODO: стоит ли обнулить openedSubmenuId?
    }

    private fun getSideMenuContainerById(id: Int): View? {
        return when (id) {
            R.id.place_items -> binding.sideMenu.placeItems
            R.id.body_parts_items -> binding.sideMenu.bodyPartsItems
            R.id.muscles_items -> binding.sideMenu.musclesItems
            R.id.rest_time_items -> binding.sideMenu.restTimeItems
            R.id.break_mode_items -> binding.sideMenu.breakModeItems
            else -> null
        }
    }

    /**
     * Получение слушателя для каждого из пунктов бокового меню;
     */
    private fun getSideMenuListener(
        container: View,
        action: () -> Unit = {}
    ): View.OnClickListener {
        val bounceAnim = AnimationUtils
            .loadAnimation(this@MainActivity, R.anim.bounce)
        return View.OnClickListener {
            it.startAnimation(bounceAnim)
            closeOpenedSideContainer(container)
            action.invoke()
            showOrCloseSideContainer(container).also { opened ->
                if (opened) (it as TextView).setRightDrawable(R.drawable.ic_arrow_up)
                else (it as TextView).setRightDrawable(R.drawable.ic_arrow_down)
            }
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
        if (recyclerView.layoutManager == null)
            recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter =
            SimpleListAdapter(this, TAG_MUSCLES).also {
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
            arrayList.add(SimpleListItem(stringArray[i], boolArray[i]))
        }
        if (recyclerView.layoutManager == null)
            recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter =
            SimpleListAdapter(this, TAG_BODY_PARTS).also {
                it.submitList(arrayList)
            }
    }

    /**
     * Скрытие клавиатуры и удаление фокуса с заполняемого поля;
     */
    private fun View.hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.hideSoftInputFromWindow(this.windowToken, 0)
        binding.sideMenu.inputRestTime.clearFocus()
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

//                                                                     [ Для восстановления данных ]

    /**
     * Получение данных из переданного bundle;
     */
    private fun restoreData(bundle: Bundle) {
        if (actionManager != null)
            actionManager!!.restoreInstanceState(bundle)
    }

//                                                                    [ Для скрытия/отображения UI ]

    /**
     * Прекращение тренировки; (TODO:1.1)
     */
    private fun stopWorkout() = with(binding) {

        hideToolbarAndDisableSideMenu()

        actionManager?.clear()

        sideMenu.placeItemHome.setOnClickListener(null)
        sideMenu.placeItemGym.setOnClickListener(null)
        sideMenu.placeItemOutdoors.setOnClickListener(null)
        sideMenu.subMenuDynamicSound.setOnClickListener(null)
        sideMenu.subMenuDynamicVibration.setOnClickListener(null)
        sideMenu.subMenuDynamicAnim.setOnClickListener(null)
        sideMenu.add5sButton.setOnClickListener(null)
        sideMenu.sub5sButton.setOnClickListener(null)
        sideMenu.inputRestTime.removeTextChangedListener(restTimeTextWatcher)
        sideMenu.restTimeAcceptButton.setOnClickListener(null)

        viewModel.activeProcess.value = WorkoutProcess.NOT_STARTED
        viewModel.clearAllData()

        binding.sideMenu.stopWorkoutAcceptButton.visibility = View.GONE

        //TODO: запустить диалоговое окно с результатами
        // -> передать от этого диалогового окна safe args с workoutStartedSuccessfully как false
        // (Предотвратит ошибку с вылетом, переключаясь после завершения тренировки со списка в start)
        // запустить фрагмент со списком;
        // Перенести данные в базу данных;
    }

    private fun TextView.setRightDrawable(@DrawableRes res: Int) {
        setCompoundDrawablesWithIntrinsicBounds(
            compoundDrawables[0],
            null,
            ContextCompat.getDrawable(this@MainActivity, res),
            null
        )
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

    /**
     * Скрытие элементов бокового меню, которые уже выбраны,
     * для отсутствия нелогичного выбора уже выбранных параметров;
     */
    private fun hideSelectedSideMenuItems() {
        when (viewModel.trainingPlace) {
            Place.TRAINING_AT_HOME -> binding.sideMenu.placeItemHome.isGone = true
            Place.TRAINING_OUTDOORS -> binding.sideMenu.placeItemOutdoors.isGone = true
            Place.TRAINING_IN_GYM -> binding.sideMenu.placeItemGym.isGone = true
        }
        when (viewModel.breakNotifyingMode) {
            BreakNotifyingMode.SOUND -> binding.sideMenu.subMenuDynamicSound.isGone = true
            BreakNotifyingMode.VIBRATION -> binding.sideMenu.subMenuDynamicVibration.isGone = true
            BreakNotifyingMode.ANIMATION -> binding.sideMenu.subMenuDynamicAnim.isGone = true
        }
    }

    /**
     * Установка взаимосвязи между нижней панелью управления и компонентом Navigation;
     * При запущенном диалоговом окне начала тренировки, bottomNavigation будет скрыт.
     */
    private fun setupBottomNavigation() {
        val navController = findNavController(R.id.fragment_container)
        binding.bottomNavigationView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.workoutStartFSDialog -> binding.bottomNavigationView.isGone = true
                R.id.mainFragment -> binding.bottomNavigationView.isVisible = true
                R.id.listFragment -> binding.bottomNavigationView.isVisible = true
            }
        }
    }

    private fun navigateToMainFragmentIfNeeded(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_MAIN_FRAGMENT) {
            findNavController(R.id.fragment_container)
                .navigate(R.id.action_global_to_mainFragment)
        }
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
     * Callback интерфейса [MainFragment.FragmentCallback];
     * Обработка нажатия основной кнопки
     */
    override fun onMainButtonClicked() {
        when (viewModel.activeProcess.value) {
            WorkoutProcess.TIMER -> {
                actionManager?.launchOrPause(Action.BREAK_TIMER)
            }
            WorkoutProcess.EXERCISE_STOPWATCH -> {
                //TODO: Запустить диалоговое окно, в котором будет выбор того, что ты потренил
                // Если Пользователь выбрал закончить тренировку, то перебросить его на результаты и изменить viewModel.activeProcess
                actionManager?.launch(
                    Action.BREAK_TIMER,
                    viewModel.restTime.value!!
                )
            }
            WorkoutProcess.PAUSED -> {
                //TODO: не определено.
            }
            else -> throw Exception("MainActivity -> overridden mainButtonClicked() -> No such WorkoutProcess")
        }
    }

    /**
     * Callback интерфейса [MainFragment.FragmentCallback];
     * Обработка уничтожения фрагмента;
     */
    override fun onFragmentStopped() {
        actionManager?.notifyLifecycleOwnerStopped()
    }

    /**
     * Callback интерфейса [MainFragment.FragmentCallback];
     * Обработка создания View фрагмента (onViewCreated);
     */
    override fun onFragmentUICreated(
        textView: TextView,
        progressBar: ProgressBar,
        circleView: View,
        pulseView: View
    ) {
        actionManager?.updateUI(textView, progressBar, circleView, pulseView)
        actionManager?.notifyLifecycleOwnerCreated()
    }

    override fun onWorkoutStarted() {
        // Обновляем View, используемые для отображения данных секундомера;
        showToolbarAndActivateSideMenu()
        actionManager?.let { manager ->
            manager.launch(Action.GENERAL_STOPWATCH, fromStart = true)
            manager.launch(Action.EXERCISE_STOPWATCH, fromStart = true)
        }
        // Сохраняем текущим процессом секундомер;
        viewModel.activeProcess.value = WorkoutProcess.EXERCISE_STOPWATCH
        Log.d("MyTag", "override fun workoutStarted")
        // Скрываем те параметры тренировки, которые уже выбранны;
        hideSelectedSideMenuItems()
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

